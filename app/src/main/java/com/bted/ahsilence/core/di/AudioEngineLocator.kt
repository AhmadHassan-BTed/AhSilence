package com.bted.ahsilence.core.di

import com.bted.ahsilence.data.engine.NativeAudioDSP
import com.bted.ahsilence.domain.port.AudioEngine

object AudioEngineLocator {
    val engine: AudioEngine by lazy { NativeAudioDSP() }
}
