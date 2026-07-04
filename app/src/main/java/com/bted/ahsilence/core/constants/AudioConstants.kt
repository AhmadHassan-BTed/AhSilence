package com.bted.ahsilence.core.constants

object AudioConstants {
    const val SAMPLE_RATE: Int = 44100
    const val FFT_BUFFER_SIZE: Int = 8192
    const val AMBIENT_SCAN_DURATION_SECONDS: Int = 7
    const val ZERO_READ_ESCAPE_THRESHOLD: Int = 50

    const val NOTIFICATION_CHANNEL_ID: String = "AhSilence_Active_Hum"
    const val NOTIFICATION_ID: Int = 8842

    const val ACTION_START: String = "com.bted.ahsilence.START_EMISSION"
    const val ACTION_UPDATE_PARAMS: String = "com.bted.ahsilence.UPDATE_PARAMS"
    const val ACTION_STOP: String = "com.bted.ahsilence.STOP_EMISSION"

    const val EXTRA_AMPLITUDE: String = "EXTRA_AMPLITUDE"
    const val EXTRA_PHASE: String = "EXTRA_PHASE"

    const val DEFAULT_AMPLITUDE_PERCENT: Float = 50f
    const val DEFAULT_PHASE_DEGREES: Float = 180f
}
