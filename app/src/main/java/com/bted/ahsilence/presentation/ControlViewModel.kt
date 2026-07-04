package com.bted.ahsilence.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bted.ahsilence.core.constants.AudioConstants
import com.bted.ahsilence.core.di.AudioEngineLocator
import com.bted.ahsilence.core.logging.AppLogger
import com.bted.ahsilence.domain.model.AcousticState
import com.bted.ahsilence.domain.port.AudioEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The State Management bridge.
 * Strictly decoupled: It knows about the Data Model and the DSP Engine, but knows nothing about the Android OS.
 */
class ControlViewModel : ViewModel() {

    // Retrieve the exact same DSP engine instance that the Foreground Service is running
    private val engine: AudioEngine = AudioEngineLocator.engine

    // The single source of truth for the Compose UI. Reactively updates at 120fps.
    private val _state = MutableStateFlow(AcousticState())
    val state: StateFlow<AcousticState> = _state.asStateFlow()

    init {
        // Automatically scan the room when the app is first opened
        analyzeEnvironment()
    }

    /**
     * Captures ambient audio on a background thread to find the dominant hum.
     * The UI will automatically update when the math is finished.
     */
    private fun analyzeEnvironment() {
        viewModelScope.launch(Dispatchers.IO) {
            AppLogger.d("ViewModel: Starting ${AudioConstants.AMBIENT_SCAN_DURATION_SECONDS}-second room analysis...")
            val frequency = engine.captureAndAnalyzeEnv(
                durationSeconds = AudioConstants.AMBIENT_SCAN_DURATION_SECONDS
            )

            AppLogger.d("ViewModel: Analysis complete. Hum detected at $frequency Hz")
            _state.update { currentState ->
                currentState.copy(detectedFrequencyHz = frequency)
            }
        }
    }

    /**
     * Instantly injects the new amplitude into the running DSP memory loop.
     * Zero Intent overhead. Zero GC allocation.
     */
    fun updateAmplitude(percentage: Float) {
        _state.update { it.copy(amplitudePercentage = percentage) }
        engine.updateParameters(
            amplitudePercentage = percentage,
            phaseDegrees = _state.value.phaseDegrees
        )
    }

    /**
     * Instantly injects the new phase shift into the running DSP memory loop.
     * Zero Intent overhead. Zero GC allocation.
     */
    fun updatePhase(degrees: Float) {
        _state.update { it.copy(phaseDegrees = degrees) }
        engine.updateParameters(
            amplitudePercentage = _state.value.amplitudePercentage,
            phaseDegrees = degrees
        )
    }

    fun togglePowerStatus() {
        _state.update { currentState ->
            val isNowOn = !currentState.isEmitting
            AppLogger.d("ViewModel: User toggled power. Emitting state is now: $isNowOn")
            currentState.copy(isEmitting = isNowOn)
        }
    }

    override fun onCleared() {
        engine.stop()
        engine.release()
        super.onCleared()
    }
}