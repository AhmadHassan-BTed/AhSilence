#pragma once

#include <array>
#include <cstddef>

namespace anc::dsp {

/**
 * Filtered-X Normalized LMS adaptive filter (FxNLMS).
 *
 * Generic over the number of taps L so it can serve either topology:
 *  - L = 2, fed with {cos, sin} reference samples: narrowband/tonal
 *    ANC (what AncEngine uses -- ideal for a stationary 40-150 Hz hum,
 *    and what makes "phase prediction" tractable with a single mic).
 *  - L = N, fed with a raw delay-line of a reference-mic signal:
 *    classic broadband FxLMS, if a dedicated reference mic is added
 *    later for non-tonal noise.
 *
 * Sign convention -- read this before changing anything:
 *   e(n) = d(n) + S(z) * y(n)          [acoustic superposition, a physical "+"]
 *   y(n) = w(n)^T * r(n)               [controller output]
 * Gradient descent on E[e^2] w.r.t. w gives:
 *   w(n+1) = w(n) - mu * e(n) * r'(n)  [r' = r filtered through the Ŝ(z) estimate]
 * If the error signal *grows* instead of shrinking once this is wired
 * to real hardware, the first suspect is a sign inversion somewhere in
 * the acoustic/electrical path (e.g. an inverted mic capsule or a
 * speaker wired out of phase) -- not this equation. This is the single
 * most common bring-up bug in a first ANC implementation.
 *
 * Step size: FxLMS tolerates smaller mu than plain NLMS, because
 * S(z)/Ŝ(z) inject extra loop delay into the adaptation path. A
 * standalone simulation of this exact class (100 Hz tone, 30-sample
 * true secondary-path delay) converged cleanly at mu=0.02 (~7.6x RMS
 * error reduction in 3s) but diverged by mu=0.04 -- and diverged at
 * *every* mu when the secondary-path estimate's phase error exceeded
 * ~90 degrees (the classic FxLMS stability bound). Treat 0.02 as a
 * starting point, not a universal constant, and increase it cautiously
 * while watching for growth in the error signal.
 *
 * Real-time contract: allocation-free and lock-free after construction.
 */
template <std::size_t L>
class FxLmsFilter {
public:
    explicit FxLmsFilter(float stepSize = 0.02f, float leakage = 0.9999f)
            : mStepSize(stepSize), mLeakage(leakage) {
        mWeights.fill(0.0f);
    }

    /// y(n) = w . referenceVector -- the sample to send to the loudspeaker.
    [[nodiscard]] inline float computeOutput(const std::array<float, L> &referenceVector) const noexcept {
        float y = 0.0f;
        for (std::size_t i = 0; i < L; ++i) {
            y += mWeights[i] * referenceVector[i];
        }
        return y;
    }

    /**
     * Updates weights from the true acoustic error using the *filtered*
     * reference vector (referenceVector passed through the secondary-path
     * estimate Ŝ(z) -- see SecondaryPathModel.h).
     *
     * NLMS normalization (dividing by the filtered-reference power) keeps
     * the effective step size well-behaved regardless of input level. The
     * small leakage term (<1.0) does double duty: it bounds weight growth
     * if the secondary-path estimate is imperfect, and it continuously
     * nudges idle weights back toward zero -- which keeps them out of the
     * denormal range during quiet periods (denormals are a classic, silent
     * cause of missed real-time deadlines on some FPUs).
     */
    inline void adapt(const std::array<float, L> &filteredReferenceVector, float error) noexcept {
        float power = kEpsilon;
        for (std::size_t i = 0; i < L; ++i) {
            power += filteredReferenceVector[i] * filteredReferenceVector[i];
        }
        const float mu = mStepSize / power;

        for (std::size_t i = 0; i < L; ++i) {
            mWeights[i] = mLeakage * mWeights[i] - mu * error * filteredReferenceVector[i];
        }
    }

    inline void reset() noexcept { mWeights.fill(0.0f); }

    void setStepSize(float stepSize) noexcept { mStepSize = stepSize; }
    [[nodiscard]] const std::array<float, L> &weights() const noexcept { return mWeights; }

private:
    // Regularization: avoids dividing by ~0 when the reference is briefly silent.
    static constexpr float kEpsilon = 1e-6f;

    std::array<float, L> mWeights{};
    float mStepSize;
    float mLeakage;
};

} // namespace anc::dsp
