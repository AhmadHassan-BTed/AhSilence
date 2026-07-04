#pragma once

#include <array>
#include <atomic>
#include <cstddef>

namespace anc::dsp {

/**
 * Fixed-length FIR estimate of the secondary path S(z): the real,
 * physical route a sample takes from "written to the output stream"
 * to "picked up again by the error mic" -- DAC, amplifier, air gap,
 * mic capsule, ADC, and any OS-level buffering in between.
 *
 * Sizing kTaps: it must be at least as long as your worst-case
 * round-trip latency in samples, or the model *cannot* represent the
 * true delay and FxLMS will fail to converge no matter how mu is
 * tuned. At 48 kHz, even Oboe's low-latency path (your own estimate:
 * 5-10 ms) is 240-480 samples -- so a short, "reasonable-looking"
 * 32/64-tap FIR is a common first-implementation bug. The default
 * below is sized with headroom; treat it as a placeholder and replace
 * it with a measured impulse response as soon as you wire up a
 * calibration pass (sketched in the explanation at the end).
 *
 * Thread-safety: process() is called only from the audio callback.
 * updateCoefficients() may be called concurrently from a calibration
 * thread; it uses a lock-free double buffer (single writer / single
 * reader) so the audio thread never blocks or torn-reads a coefficient
 * set mid-update.
 */
template <std::size_t kTaps>
class SecondaryPathModel {
public:
    SecondaryPathModel() {
        mHistory.fill(0.0f);
        for (auto &buf : mCoefficientBuffers) buf.fill(0.0f);
        // Rough placeholder: assume a pure delay of kDefaultDelaySamples with
        // unity gain, until real calibration data is loaded via updateCoefficients().
        static_assert(kDefaultDelaySamples < kTaps, "default delay must fit inside the FIR length");
        mCoefficientBuffers[0][kDefaultDelaySamples] = 1.0f;
    }

    /// Push one new input sample through Ŝ(z); returns the filtered output. O(kTaps), zero allocation.
    inline float process(float input) noexcept {
        mHistory[mHead] = input;

        const auto &coeffs = mCoefficientBuffers[mActiveBuffer.load(std::memory_order_acquire)];
        float output = 0.0f;
        std::size_t idx = mHead;
        for (std::size_t i = 0; i < kTaps; ++i) {
            output += coeffs[i] * mHistory[idx];
            idx = (idx == 0) ? (kTaps - 1) : (idx - 1);
        }

        mHead = (mHead + 1 == kTaps) ? 0 : (mHead + 1);
        return output;
    }

    /// Safe to call from a non-realtime calibration thread while the stream is running.
    void updateCoefficients(const std::array<float, kTaps> &taps) noexcept {
        const int inactive = 1 - mActiveBuffer.load(std::memory_order_relaxed);
        mCoefficientBuffers[inactive] = taps;
        mActiveBuffer.store(inactive, std::memory_order_release);
    }

private:
    static constexpr std::size_t kDefaultDelaySamples = 200; // ~4.2 ms @ 48 kHz -- SEE CLASS COMMENT

    std::array<std::array<float, kTaps>, 2> mCoefficientBuffers{};
    std::atomic<int> mActiveBuffer{0};
    std::array<float, kTaps> mHistory{};
    std::size_t mHead = 0;
};

} // namespace anc::dsp
