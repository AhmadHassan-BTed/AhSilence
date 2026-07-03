package com.bted.ahsilence.framework.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bted.ahsilence.domain.ports.AudioEngine
import com.bted.ahsilence.framework.engine.NativeAudioDSP

/**
 * The OS-level shield. This keeps the pure Kotlin DSP engine alive in the background
 * by attaching it to a persistent user notification, preventing Android from throttling the CPU.
 */
class ActiveHumService : Service() {

    // Retrieve the singleton DSP instance
    private val engine: AudioEngine = AudioEngineLocator.engine

    companion object {
        private const val CHANNEL_ID = "AhSilence_Active_Hum"
        private const val NOTIFICATION_ID = 8842

        // Intent Actions
        const val ACTION_START = "com.bted.ahsilence.START_EMISSION"
        const val ACTION_UPDATE_PARAMS = "com.bted.ahsilence.UPDATE_PARAMS"
        const val ACTION_STOP = "com.bted.ahsilence.STOP_EMISSION"

        // Intent Extras
        const val EXTRA_AMPLITUDE = "EXTRA_AMPLITUDE"
        const val EXTRA_PHASE = "EXTRA_PHASE"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // 1. Lock the service into the foreground instantly
                startForegroundSafely()
                // 2. Ignite the native audio emission loop
                engine.startAntiNoiseEmission()
            }
            ACTION_UPDATE_PARAMS -> {
                // Extract lightweight primitive floats sent from the Jetpack Compose UI
                val amplitude = intent.getFloatExtra(EXTRA_AMPLITUDE, 50f)
                val phase = intent.getFloatExtra(EXTRA_PHASE, 180f)

                // Inject them directly into the running math loop
                engine.updateParameters(amplitude, phase)
            }
            ACTION_STOP -> {
                engine.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        // START_NOT_STICKY ensures that if the OS *does* manage to kill it under extreme memory load,
        // it won't try to automatically restart a ghost audio loop without the user's UI input.
        return START_NOT_STICKY
    }

    // We do not bind this service to the UI. The UI pushes data down via Intents.
    // This enforces absolute uncoupling.
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engine.stop()
        super.onDestroy()
    }

    // ==========================================
    // OS NOTIFICATION BOILERPLATE
    // ==========================================

    private fun startForegroundSafely() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AhSilence Active")
            .setContentText("Phase cancellation is running in the background.")
            // Note: You will need to add a clean, minimal icon to your res/drawable folder named ic_sine_wave
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setPriority(NotificationCompat.PRIORITY_LOW) // Low priority so it doesn't vibrate the phone
            .setOngoing(true)
            .build()

        // API 34+ requires explicit declaration of the foreground service type
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active Noise Control",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the anti-noise DSP engine running without interruption."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

/**
 * Lightweight Dependency Locator.
 * Guarantees that the Jetpack Compose UI and the Foreground Service
 * modify the exact same physical byte buffers in memory.
 */
object AudioEngineLocator {
    val engine: AudioEngine by lazy { NativeAudioDSP() }
}