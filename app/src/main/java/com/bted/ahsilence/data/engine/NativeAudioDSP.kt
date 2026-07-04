package com.bted.ahsilence.data.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import com.bted.ahsilence.core.constants.AudioConstants
import com.bted.ahsilence.core.logging.AppLogger
import com.bted.ahsilence.domain.port.AudioEngine
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin

class NativeAudioDSP : AudioEngine {

    private val dspJob = SupervisorJob()
    private val dspScope = CoroutineScope(Dispatchers.Default + dspJob)
    private var emissionJob: Job? = null

    private val sampleRate = AudioConstants.SAMPLE_RATE
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val fftBufferSize = AudioConstants.FFT_BUFFER_SIZE
    private val fftEngine = FastFourier(fftBufferSize, sampleRate)

    @Volatile private var isRunning = false
    @Volatile private var amplitudeFactor = 0.5f
    @Volatile private var phaseOffsetRads = PI.toFloat()
    @Volatile private var detectedFrequency = 0f

    @SuppressLint("MissingPermission")
    override suspend fun captureAndAnalyzeEnv(durationSeconds: Int): Float = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate, channelConfigIn,
            audioFormat, minBufferSize.coerceAtLeast(fftBufferSize * 2)
        )
        val readBuffer = ShortArray(fftBufferSize)

        try {
            record.startRecording()
            delay(durationSeconds * 1000L)

            var read = 0
            var zeroByteCount = 0
            while (read < fftBufferSize) {
                val result = record.read(readBuffer, read, fftBufferSize - read)
                if (result < 0) {
                    AppLogger.e("AudioRecord failed: $result")
                    break
                } else if (result == 0) {
                    if (++zeroByteCount > AudioConstants.ZERO_READ_ESCAPE_THRESHOLD) break
                } else {
                    read += result
                    zeroByteCount = 0
                }
            }

            detectedFrequency = fftEngine.extractDominantFrequency(readBuffer)
        } finally {
            record.stop()
            record.release()
        }

        return@withContext detectedFrequency
    }

    override fun startAntiNoiseEmission() {
        if (isRunning) return
        isRunning = true

        emissionJob = dspScope.launch {
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
            val track = AudioTrack.Builder()
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(audioFormat)
                        .setChannelMask(channelConfigOut)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val playBuffer = ShortArray(minBufferSize)

            try {
                track.play()
                val twoPi = 2.0 * PI
                var currentPhase = 0.0

                while (isRunning && isActive) {
                    val currentAmp = amplitudeFactor
                    val currentPhaseOffset = phaseOffsetRads
                    val currentFreq = detectedFrequency
                    val phaseIncrement = (twoPi * currentFreq) / sampleRate

                    for (i in playBuffer.indices) {
                        val finalPhase = (currentPhase + currentPhaseOffset) % twoPi
                        playBuffer[i] = (sin(finalPhase) * 32767 * currentAmp).toInt().toShort()
                        currentPhase += phaseIncrement
                        if (currentPhase >= twoPi) currentPhase -= twoPi
                    }

                    track.write(playBuffer, 0, playBuffer.size, AudioTrack.WRITE_BLOCKING)
                }
            } finally {
                track.stop()
                track.release()
            }
        }
    }

    override fun updateParameters(amplitudePercentage: Float, phaseDegrees: Float) {
        amplitudeFactor = (amplitudePercentage / 100f).coerceIn(0f, 1f)
        phaseOffsetRads = (phaseDegrees * (PI / 180.0)).toFloat()
    }

    override fun stop() {
        isRunning = false
        emissionJob?.cancel()
    }
}
