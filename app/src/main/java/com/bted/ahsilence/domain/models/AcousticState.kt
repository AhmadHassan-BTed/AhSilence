package com.bted.ahsilence.domain.models

/**
 * An immutable representation of the app's current acoustic configuration.
 * This acts as the single source of truth passed between the UI and the Audio Engine.
 */
data class AcousticState(
    /**
     * The dominant background frequency (e.g., an AC hum) extracted via the 7-second FFT analysis.
     * Measured in Hertz (Hz).
     */
    val detectedFrequencyHz: Float = 0f,

    /**
     * The user's manual amplitude (volume) calibration.
     * Bound between 0f and 100f. Defaults to 50%.
     */
    val amplitudePercentage: Float = 50f,

    /**
     * The user's manual phase offset calibration.
     * Bound between 0f and 360f. Defaults to 180 degrees (perfect mathematical inversion).
     */
    val phaseDegrees: Float = 180f,

    /**
     * Represents whether the Active Hum Service is currently running and emitting the anti-wave.
     */
    val isEmitting: Boolean = false
)