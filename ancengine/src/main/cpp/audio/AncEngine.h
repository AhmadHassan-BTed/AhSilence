#pragma once

#include <oboe/Oboe.h>
#include <atomic>
#include <array>
#include <cstdint>
#include <memory>

#include "../dsp/Oscillator.h"
#include "../dsp/FxLmsFilter.h"
#include "../dsp/SecondaryPathModel.h"

namespace anc::audio {

/**
 * Hardware interface layer. Owns the Oboe duplex streams and implements
 * the real-time callback, but contains NO DSP math itself -- it only
 * wires together dsp::Oscillator, dsp::FxLmsFilter, and
 * dsp::SecondaryPathModel once per sample. That separation is what lets
 * the DSP classes be unit-tested on a desktop with no Android/Oboe
 * dependency at all (see the verification notes at the end).
 *
 * Built on oboe::FullDuplexStream, Oboe's own helper for synchronizing
 * an input and output stream without hand-rolled cross-stream locking.
 */
class AncEngine : public oboe::FullDuplexStream, public oboe::AudioStreamErrorCallback {
public:
    explicit AncEngine(int32_t preferredSampleRate);
    ~AncEngine() override;

    AncEngine(const AncEngine &) = delete;
    AncEngine &operator=(const AncEngine &) = delete;

    /// Opens and starts the duplex Oboe streams. Not real-time-safe; call from any non-audio thread.
    oboe::Result start() override;

    /// Stops and closes the streams. Not real-time-safe.
    oboe::Result stop() override;

    /// UI/analysis-thread entry point: the coarse FFT frequency estimate to track.
    void setTargetFrequency(float frequencyHz) noexcept;

    /// UI-thread entry point: gain [0-100]% and phase trim [degrees]. Atomic, lock-free.
    void updateParameters(float gainPercent, float phaseDegrees) noexcept;

    // --- oboe::FullDuplexStream -----------------------------------------
    oboe::DataCallbackResult onBothStreamsReady(
            const void *inputData, int numInputFrames,
            void *outputData, int numOutputFrames) override;

    // --- oboe::AudioStreamErrorCallback ----------------------------------
    void onErrorAfterClose(oboe::AudioStream *stream, oboe::Result error) override;

private:
    static constexpr int kReferenceTaps = 2;        // {cos, sin} narrowband basis
    static constexpr int kSecondaryPathTaps = 512;  // ~10.6 ms headroom @ 48 kHz -- see SecondaryPathModel.h

    const int32_t mPreferredSampleRate;
    int32_t mActualSampleRate = 0;

    std::shared_ptr<oboe::AudioStream> mOutputStream;
    std::shared_ptr<oboe::AudioStream> mInputStream;

    dsp::Oscillator mReferenceOscillator;
    dsp::FxLmsFilter<kReferenceTaps> mAdaptiveFilter;
    dsp::SecondaryPathModel<kSecondaryPathTaps> mSecondaryPathCos;
    dsp::SecondaryPathModel<kSecondaryPathTaps> mSecondaryPathSin;

    std::atomic<float> mGainCoefficient{0.5f};
    std::atomic<float> mPhaseOffsetRad{0.0f};
    std::atomic<float> mTargetFrequencyHz{0.0f};
    std::atomic<bool> mIsRunning{false};
};

} // namespace anc::audio
