package com.bted.ahsilence.domain.port

interface AudioEngine {
    suspend fun captureAndAnalyzeEnv(durationSeconds: Int): Float
    fun startAntiNoiseEmission()
    fun updateParameters(amplitudePercentage: Float, phaseDegrees: Float)
    fun stop()
    fun release() {}
}
