package com.bted.ahsilence.framework.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import com.bted.ahsilence.domain.ports.AudioEngine
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin

class NativeAudioDSP : AudioEngine {

    // ==========================================
    // ARCHITECTURE & STATE
    // ==========================================
    private val dspJob = SupervisorJob()
    private val dspScope = CoroutineScope(Dispatchers.Default + dspJob)
    private var emissionJob: Job? = null

    private val sampleRate = 44100
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val fftBufferSize = 8192
    private val fftEngine = FastFourier(fftBufferSize, sampleRate)

    // Using @Volatile for GC-Free, lock-free synchronization.
    // The UI thread writes to these; the audio loop reads them instantly without blocking.
    @Volatile private var isRunning = false
    @Volatile private var amplitudeFactor = 0.5f
    @Volatile private var phaseOffsetRads = PI.toFloat() // Defaults to 180 degrees (perfect inversion)
    @Volatile private var detectedFrequency = 0f

    // ==========================================
    // CAPTURE & ANALYZE (FFT)
    // ==========================================
    @SuppressLint("MissingPermission") // Handled at the UI/Manifest layer
    override suspend fun captureAndAnalyzeEnv(durationSeconds: Int): Float = withContext(Dispatchers.IO) {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfigIn,
            audioFormat,
            minBufferSize.coerceAtLeast(fftBufferSize * 2)
        )

        // Pre-allocate buffer for the capture
        val readBuffer = ShortArray(fftBufferSize)

        Log.d("AhSilence", "Engine: Mic engaged. Recording for $durationSeconds seconds...")
        record.startRecording()

        // Simulate the analysis window (allowing the environment to stabilize)
        delay(durationSeconds * 1000L)

        Log.d("AhSilence", "Engine: Capture finished. Running FFT math...")

        // Capture the final stable frame for FFT analysis with OS Hardware Failsafe
        var read = 0
        var zeroByteCount = 0 // Escape hatch counter

        while (read < fftBufferSize) {
            val result = record.read(readBuffer, read, fftBufferSize - read)

            if (result < 0) {
                Log.e("AhSilence", "CRITICAL: AudioRecord hardware failed with OS error code: $result")
                break
            } else if (result == 0) {
                zeroByteCount++
                if (zeroByteCount > 50) {
                    Log.e("AhSilence", "CRITICAL: Mic is engaged but returning 0 bytes. OS is blocking the stream.")
                    break
                }
            } else {
                read += result
                zeroByteCount = 0 // Reset on successful read
            }
        }

        record.stop()
        record.release()

        // Extract Frequency via our highly-cohesive mathematical module
        detectedFrequency = fftEngine.extractDominantFrequency(readBuffer)
        Log.d("AhSilence", "Engine: FFT Complete. Dominant frequency locked at $detectedFrequency Hz")
        return@withContext detectedFrequency
    }

    // ==========================================
    // THE GC-FREE EMISSION LOOP
    // ==========================================
    override fun startAntiNoiseEmission() {
        if (isRunning) return
        isRunning = true
        Log.d("AhSilence", "Engine: IGNITION. Firing continuous anti-wave at $detectedFrequency Hz")

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

            // Pre-allocate the emission buffer exactly once. No objects are created past this line.
            val playBuffer = ShortArray(minBufferSize)
            track.play()

            val twoPi = 2.0 * PI
            var currentPhase = 0.0

            // The inner loop executes millions of times.
            while (isRunning && isActive) {
                // Snapshot volatile variables once per buffer to prevent tearing
                val currentAmp = amplitudeFactor
                val currentPhaseOffset = phaseOffsetRads

                // FIX: Grab the latest frequency inside the loop so it updates
                // the moment the 7-second FFT analysis completes!
                val currentFreq = detectedFrequency
                val phaseIncrement = (twoPi * currentFreq) / sampleRate

                for (i in playBuffer.indices) {
                    // Apply UI phase shift to the raw sine wave
                    val finalPhase = (currentPhase + currentPhaseOffset) % twoPi

                    // Convert float sine (-1.0 to 1.0) to 16-bit PCM Short (-32768 to 32767)
                    val sample = (sin(finalPhase) * 32767 * currentAmp).toInt()
                    playBuffer[i] = sample.toShort()

                    // Advance the phase accumulator, wrapping to prevent float precision decay
                    currentPhase += phaseIncrement
                    if (currentPhase >= twoPi) {
                        currentPhase -= twoPi
                    }
                }

                // Push to hardware (WRITE_BLOCKING synchronizes the loop with the actual hardware clock)
                track.write(playBuffer, 0, playBuffer.size, AudioTrack.WRITE_BLOCKING)
            }

            track.stop()
            track.release()
        }
    }

    // ==========================================
    // PARAMETER INJECTION (UI THREAD)
    // ==========================================
    override fun updateParameters(amplitudePercentage: Float, phaseDegrees: Float) {
        // UI sends 0-100%, we map to a 0.0-1.0 coefficient for the DSP multiplier
        amplitudeFactor = (amplitudePercentage / 100f).coerceIn(0f, 1f)

        // UI sends 0-360 degrees, we map strictly to Radians for Math.sin()
        phaseOffsetRads = (phaseDegrees * (PI / 180.0)).toFloat()
    }

    override fun stop() {
        Log.d("AhSilence", "Engine: TERMINATED. DSP loop halted.")
        isRunning = false
        emissionJob?.cancel()
    }
}