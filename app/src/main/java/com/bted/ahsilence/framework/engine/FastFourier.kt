package com.bted.ahsilence.framework.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A GC-Free, highly optimized Cooley-Tukey Radix-2 Fast Fourier Transform.
 * * @param bufferSize Must be a power of 2 (e.g., 4096, 8192, 16384).
 * @param sampleRate The recording sample rate (e.g., 44100 or 48000).
 */
class FastFourier(private val bufferSize: Int, private val sampleRate: Int) {

    init {
        // The algorithm mathematically requires the buffer to be a perfect power of 2.
        require(bufferSize and (bufferSize - 1) == 0) {
            "CRITICAL: Buffer size must be a power of 2 for Radix-2 FFT."
        }
    }

    // ==========================================
    // 1. PRE-ALLOCATED MEMORY POOL
    // ==========================================
    private val real = FloatArray(bufferSize)
    private val imag = FloatArray(bufferSize)

    private val cosTable = FloatArray(bufferSize / 2)
    private val sinTable = FloatArray(bufferSize / 2)

    init {
        // Pre-compute trigonometric tables to save CPU cycles inside the DSP loop
        for (i in 0 until bufferSize / 2) {
            val angle = -2.0 * PI * i / bufferSize
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
    }

    // ==========================================
    // 2. THE PUBLIC EXECUTION METHOD
    // ==========================================

    /**
     * Ingests a raw PCM audio buffer and returns the dominant frequency in Hz.
     * Guaranteed 0 bytes of memory allocation per run.
     */
    fun extractDominantFrequency(pcmData: ShortArray): Float {
        // Step 1: Load PCM data into the real array. Zero out the imaginary array.
        for (i in 0 until bufferSize) {
            real[i] = pcmData[i].toFloat()
            imag[i] = 0f
        }

        // Step 2: Run the math
        computeFFT()

        // Step 3: Find the highest energy peak (ignoring the DC offset at index 0)
        var maxMagnitudeSq = 0f
        var peakIndex = 0

        // We only scan up to bufferSize / 2 (The Nyquist Limit)
        for (i in 1 until bufferSize / 2) {
            // Optimization: We use (a^2 + b^2) and skip Math.sqrt() to save CPU cycles
            val magnitudeSq = (real[i] * real[i]) + (imag[i] * imag[i])
            if (magnitudeSq > maxMagnitudeSq) {
                maxMagnitudeSq = magnitudeSq
                peakIndex = i
            }
        }

        // Step 4: Convert the peak array index back into real-world Hertz
        return (peakIndex.toFloat() * sampleRate) / bufferSize
    }

    // ==========================================
    // 3. THE MATHEMATICAL ENGINE
    // ==========================================

    private fun computeFFT() {
        val n = bufferSize

        // Phase A: Bit-Reversal Permutation
        // Reorders the array in-place so we can merge the waves recursively
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                // Swap real parts
                val tempReal = real[i]
                real[i] = real[j]
                real[j] = tempReal

                // Swap imaginary parts
                val tempImag = imag[i]
                imag[i] = imag[j]
                imag[j] = tempImag
            }
            var k = n / 2
            while (k <= j) {
                j -= k
                k /= 2
            }
            j += k
        }

        // Phase B: Danielson-Lanczos Algorithm (Butterfly Operations)
        var step = 1
        while (step < n) {
            val jump = step * 2
            val tableStep = n / jump // Used to map exactly to our pre-computed Trig Tables

            for (group in 0 until step) {
                // Retrieve pre-computed math
                val cosW = cosTable[group * tableStep]
                val sinW = sinTable[group * tableStep]

                for (pair in group until n step jump) {
                    val match = pair + step

                    // Complex multiplication
                    val tReal = cosW * real[match] - sinW * imag[match]
                    val tImag = cosW * imag[match] + sinW * real[match]

                    // Apply the butterfly overlap
                    real[match] = real[pair] - tReal
                    imag[match] = imag[pair] - tImag
                    real[pair] += tReal
                    imag[pair] += tImag
                }
            }
            step = jump
        }
    }
}