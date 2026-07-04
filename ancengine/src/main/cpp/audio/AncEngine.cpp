#include "AncEngine.h"

#include <algorithm>
#include <android/log.h>

#define TAG "AhSilence-Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace anc::audio {

namespace {
constexpr float kDegreesToRadians = 3.14159265358979323846f / 180.0f;
}

AncEngine::AncEngine(int32_t preferredSampleRate)
        : mPreferredSampleRate(preferredSampleRate) {}

AncEngine::~AncEngine() {
    stop();
}

oboe::Result AncEngine::start() {
    // Per Oboe's FullDuplexStream guidance: open the OUTPUT stream first.
    oboe::AudioStreamBuilder outBuilder;
    outBuilder.setDirection(oboe::Direction::Output)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setSampleRate(mPreferredSampleRate)
            ->setDataCallback(this)
            ->setErrorCallback(this);

    oboe::Result result = outBuilder.openStream(mOutputStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open output stream: %s", oboe::convertToText(result));
        return result;
    }
    setOutputStream(mOutputStream.get());
    mActualSampleRate = mOutputStream->getSampleRate();

    // Input stream: match the output's *actual* negotiated sample rate, and
    // give it extra buffer headroom per the FullDuplexStream wiki guidance
    // so the read cursor never collides with the write cursor.
    oboe::AudioStreamBuilder inBuilder;
    inBuilder.setDirection(oboe::Direction::Input)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::Float)
            ->setChannelCount(oboe::ChannelCount::Mono)
            ->setSampleRate(mActualSampleRate)
            ->setBufferCapacityInFrames(mOutputStream->getBufferCapacityInFrames() * 2);

    result = inBuilder.openStream(mInputStream);
    if (result != oboe::Result::OK) {
        LOGE("Failed to open input stream: %s", oboe::convertToText(result));
        mOutputStream->close();
        mOutputStream.reset();
        return result;
    }
    setInputStream(mInputStream.get());

    mReferenceOscillator.setFrequency(
            mTargetFrequencyHz.load(std::memory_order_relaxed),
            static_cast<float>(mActualSampleRate));

    mIsRunning.store(true, std::memory_order_relaxed);
    result = oboe::FullDuplexStream::start();
    if (result != oboe::Result::OK) {
        LOGE("Failed to start duplex streams: %s", oboe::convertToText(result));
        mIsRunning.store(false, std::memory_order_relaxed);
        return result;
    }

    LOGI("ANC engine started @ %d Hz", mActualSampleRate);
    return oboe::Result::OK;
}

oboe::Result AncEngine::stop() {
    mIsRunning.store(false, std::memory_order_relaxed);
    oboe::Result result = oboe::FullDuplexStream::stop();
    if (mOutputStream) {
        mOutputStream->close();
        mOutputStream.reset();
    }
    if (mInputStream) {
        mInputStream->close();
        mInputStream.reset();
    }
    return result;
}

void AncEngine::setTargetFrequency(float frequencyHz) noexcept {
    mTargetFrequencyHz.store(frequencyHz, std::memory_order_relaxed);
}

void AncEngine::updateParameters(float gainPercent, float phaseDegrees) noexcept {
    mGainCoefficient.store(std::clamp(gainPercent, 0.0f, 100.0f) / 100.0f,
                            std::memory_order_relaxed);
    mPhaseOffsetRad.store(phaseDegrees * kDegreesToRadians, std::memory_order_relaxed);
}

oboe::DataCallbackResult AncEngine::onBothStreamsReady(
        const void *inputData, int numInputFrames,
        void *outputData, int numOutputFrames) {

    const auto *in = static_cast<const float *>(inputData);
    auto *out = static_cast<float *>(outputData);

    // Snapshot the atomics ONCE per callback -- not once per sample -- so the
    // whole buffer is processed against one consistent parameter set and the
    // per-sample loop below never touches std::atomic.
    const float gain = mGainCoefficient.load(std::memory_order_relaxed);
    const float phaseOffset = mPhaseOffsetRad.load(std::memory_order_relaxed);
    const float freqHz = mTargetFrequencyHz.load(std::memory_order_relaxed);
    mReferenceOscillator.setFrequency(freqHz, static_cast<float>(mActualSampleRate));

    const int numFrames = std::min(numInputFrames, numOutputFrames);

    for (int i = 0; i < numFrames; ++i) {
        // (1) The error mic hears both the residual ambient hum AND whatever
        // we just emitted -- on a single device, that acoustic coupling is
        // exactly the error signal FxLMS needs, not a bug to work around.
        const float error = in[i];

        // (2) Locally-synthesized reference: zero capture latency by
        // construction. phaseOffset lets the UI trim sit on top of whatever
        // the adaptive weights are already converging toward.
        float cosRef, sinRef;
        mReferenceOscillator.nextSample(cosRef, sinRef, phaseOffset);
        cosRef *= gain;
        sinRef *= gain;
        const std::array<float, kReferenceTaps> referenceVector{cosRef, sinRef};

        // (3) Controller output -> anti-noise sample. Clamped as a safety
        // ceiling against transient divergence while (re)converging.
        float y = mAdaptiveFilter.computeOutput(referenceVector);
        y = std::clamp(y, -1.0f, 1.0f);
        out[i] = y;

        // (4) Filtered-X: push the REFERENCE (not y) through the secondary
        // path estimate -- the mechanism that keeps LMS stable in the
        // presence of output/acoustic delay. See FxLmsFilter.h.
        const std::array<float, kReferenceTaps> filteredReferenceVector{
                mSecondaryPathCos.process(cosRef),
                mSecondaryPathSin.process(sinRef)};

        // (5) Adapt weights from the true acoustic error.
        mAdaptiveFilter.adapt(filteredReferenceVector, error);
    }

    // Rare edge case (input/output burst sizes differ this callback): pad silence.
    for (int i = numFrames; i < numOutputFrames; ++i) {
        out[i] = 0.0f;
    }

    return mIsRunning.load(std::memory_order_relaxed)
                   ? oboe::DataCallbackResult::Continue
                   : oboe::DataCallbackResult::Stop;
}

void AncEngine::onErrorAfterClose(oboe::AudioStream *stream, oboe::Result error) {
    // Runs on a dedicated Oboe-owned thread, never the audio callback thread,
    // so logging here is safe. Typical cause: a route change (headphone
    // plug/unplug, Bluetooth connect) forcing a stream teardown.
    LOGE("Stream closed after error: %s. direction=%d",
         oboe::convertToText(error), static_cast<int>(stream->getDirection()));
    mIsRunning.store(false, std::memory_order_relaxed);
}

} // namespace anc::audio
