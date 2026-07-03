package com.bted.ahsilence.domain.ports

/**
 * Pure Kotlin contract for the DSP Engine.
 * Conforms to the Dependency Inversion Principle: the UI knows nothing about Android's AudioTrack.
 */
interface AudioEngine {
    /**
     * Listens to the environment for [durationSeconds], extracts the dominant hum,
     * and returns the detected frequency in Hz.
     */
    suspend fun captureAndAnalyzeEnv(durationSeconds: Int): Float

    /**
     * Ignites the continuous anti-noise emission loop.
     */
    fun startAntiNoiseEmission()

    /**
     * Thread-safe parameter injection.
     * @param amplitudePercentage 0f to 100f
     * @param phaseDegrees 0f to 360f
     */
    fun updateParameters(amplitudePercentage: Float, phaseDegrees: Float)

    /**
     * Halts all DSP execution and safely releases hardware resources.
     */
    fun stop()
}