#pragma once

#include <array>
#include <cmath>
#include <cstdint>

namespace anc::dsp {

/**
 * Wavetable Numerically-Controlled Oscillator (NCO).
 *
 * Generates a synchronized cosine/sine pair for a single tracked tone.
 * This is the "reference generator" for narrowband ANC: because the
 * waveform is synthesized locally rather than measured from the mic,
 * it carries zero capture latency. Combined with the phaseOffset
 * parameter (used to pre-compensate for the known output + acoustic
 * delay), this is what makes the cancellation "predictive" rather
 * than reactive -- see the explanation at the end of the response.
 *
 * Real-time contract: after construction, every method here is
 * allocation-free, lock-free, and O(1) per call. Safe to call from
 * an Oboe audio callback.
 */
class Oscillator {
public:
    Oscillator() { buildTables(); }

    /// Update the tracked frequency. Cheap: just recomputes a phase increment.
    inline void setFrequency(float frequencyHz, float sampleRateHz) noexcept {
        mPhaseIncrement = (static_cast<double>(frequencyHz) / sampleRateHz) *
                          static_cast<double>(kTableSize);
    }

    /**
     * Advances the oscillator by one sample and writes cos/sin of
     * (currentPhase + phaseOffsetRad) into outCos/outSin.
     *
     * phaseOffsetRad is applied only at read-time -- it nudges which
     * table entry we read without perturbing the underlying phase
     * accumulator, so live UI changes to the phase trim never cause
     * the frequency-tracking phase itself to jump or drift.
     */
    inline void nextSample(float &outCos, float &outSin, float phaseOffsetRad) noexcept {
        const double offsetSteps =
                (static_cast<double>(phaseOffsetRad) * kTableSize) / kTwoPi;

        double readPhase = mPhase + offsetSteps;
        while (readPhase >= kTableSize) readPhase -= kTableSize;
        while (readPhase < 0.0) readPhase += kTableSize;

        const int index = static_cast<int>(readPhase);
        outCos = mCosineTable[index];
        outSin = mSineTable[index];

        mPhase += mPhaseIncrement;
        if (mPhase >= kTableSize) mPhase -= kTableSize;
        else if (mPhase < 0.0) mPhase += kTableSize;
    }

private:
    static constexpr int kTableSize = 4096;
    static constexpr double kPi = 3.14159265358979323846;
    static constexpr double kTwoPi = 2.0 * kPi;

    std::array<float, kTableSize> mSineTable{};
    std::array<float, kTableSize> mCosineTable{};

    // Phase lives in "table-index units" (0..kTableSize), stored as double
    // so phase error cannot practically accumulate even after hours of
    // continuous playback at low frequencies.
    double mPhase = 0.0;
    double mPhaseIncrement = 0.0;

    void buildTables() {
        for (int i = 0; i < kTableSize; ++i) {
            const double theta = (kTwoPi * i) / kTableSize;
            mSineTable[i] = static_cast<float>(std::sin(theta));
            mCosineTable[i] = static_cast<float>(std::cos(theta));
        }
    }
};

} // namespace anc::dsp
