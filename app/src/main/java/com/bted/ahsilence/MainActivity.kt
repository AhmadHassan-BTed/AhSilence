package com.bted.ahsilence

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.bted.ahsilence.framework.service.ActiveHumService
import com.bted.ahsilence.presentation.ControlViewModel
import com.bted.ahsilence.ui.screens.DashboardScreen
import com.bted.ahsilence.ui.theme.AhSilenceTheme

class MainActivity : ComponentActivity() {

    // Inject the ViewModel natively. No heavy third-party DI frameworks required.
    private val viewModel: ControlViewModel by viewModels()

    // ==========================================
    // 1. OS PERMISSION HANDLING
    // ==========================================

    // Asks the user for microphone and background notification access on first launch
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!audioGranted) {
            // In a production app, you would show a clean UI error state here
            // explaining that the app physically cannot cancel noise without the mic.
        }
    }

    // ==========================================
    // 2. LIFECYCLE ENTRY POINT
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRequiredPermissions()

        setContent {
            // Wraps the app in your strict OLED-Black / Neon Amber aesthetic
            AhSilenceTheme {

                // Observe the pure Kotlin StateFlow at 120fps
                val uiState by viewModel.state.collectAsState()

                // Render the "Dumb" UI
                DashboardScreen(
                    state = uiState,
                    onPhaseChanged = { newPhase -> viewModel.updatePhase(newPhase) },
                    onAmplitudeChanged = { newAmp -> viewModel.updateAmplitude(newAmp) },
                    onTogglePower = {
                        // 1. Update the internal state machine
                        viewModel.togglePowerStatus()

                        // 2. Translate the state change to an Android OS Service Intent
                        if (!uiState.isEmitting) {
                            startAcousticService()
                        } else {
                            stopAcousticService()
                        }
                    }
                )
            }
        }
    }

    // ==========================================
    // 3. OS SERVICE BRIDGING
    // ==========================================

    /**
     * Fires the intent to ignite the Foreground Service, putting the OS shield up
     * so Android doesn't kill your DSP loop.
     */
    private fun startAcousticService() {
        val intent = Intent(this, ActiveHumService::class.java).apply {
            action = ActiveHumService.ACTION_START
        }

        // Android O+ requires startForegroundService to guarantee the notification displays
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    /**
     * Safely tears down the OS shield and destroys the service.
     */
    private fun stopAcousticService() {
        val intent = Intent(this, ActiveHumService::class.java).apply {
            action = ActiveHumService.ACTION_STOP
        }
        startService(intent)
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)

        // API 33+ strictly requires explicit permission to post the Foreground Service notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}