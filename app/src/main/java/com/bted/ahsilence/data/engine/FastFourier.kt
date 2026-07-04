package com.bted.ahsilence.data.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class FastFourier(private val bufferSize: Int, private val sampleRate: Int) {

    init {
        require(bufferSize > 0 && bufferSize and (bufferSize - 1) == 0) { "Buffer size must be a power of 2" }
        require(sampleRate > 0) { "Sample rate must be positive" }
    }

    private val real = FloatArray(bufferSize)
    private val imag = FloatArray(bufferSize)
    private val cosTable = FloatArray(bufferSize / 2)
    private val sinTable = FloatArray(bufferSize / 2)

    init {
        for (i in 0 until bufferSize / 2) {
            val angle = -2.0 * PI * i / bufferSize
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
    }

    fun extractDominantFrequency(pcmData: ShortArray): Float {
        for (i in 0 until bufferSize) {
            real[i] = pcmData[i].toFloat()
            imag[i] = 0f
        }

        computeFFT()

        var maxMagnitudeSq = 0f
        var peakIndex = 0

        for (i in 1 until bufferSize / 2) {
            val magnitudeSq = (real[i] * real[i]) + (imag[i] * imag[i])
            if (magnitudeSq > maxMagnitudeSq) {
                maxMagnitudeSq = magnitudeSq
                peakIndex = i
            }
        }

        return (peakIndex.toFloat() * sampleRate) / bufferSize
    }

    private fun computeFFT() {
        val n = bufferSize

        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempReal = real[i]; real[i] = real[j]; real[j] = tempReal
                val tempImag = imag[i]; imag[i] = imag[j]; imag[j] = tempImag
            }
            var k = n / 2
            while (k <= j) { j -= k; k /= 2 }
            j += k
        }

        // Butterfly operations
        var step = 1
        while (step < n) {
            val jump = step * 2
            val tableStep = n / jump

            for (group in 0 until step) {
                val cosW = cosTable[group * tableStep]
                val sinW = sinTable[group * tableStep]

                for (pair in group until n step jump) {
                    val match = pair + step
                    val tReal = cosW * real[match] - sinW * imag[match]
                    val tImag = cosW * imag[match] + sinW * real[match]

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
