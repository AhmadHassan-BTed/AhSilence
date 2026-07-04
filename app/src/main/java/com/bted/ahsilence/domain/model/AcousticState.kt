package com.bted.ahsilence.domain.model

import com.bted.ahsilence.core.constants.AudioConstants

data class AcousticState(
    val detectedFrequencyHz: Float = 0f,
    val amplitudePercentage: Float = AudioConstants.DEFAULT_AMPLITUDE_PERCENT,
    val phaseDegrees: Float = AudioConstants.DEFAULT_PHASE_DEGREES,
    val isEmitting: Boolean = false
)
