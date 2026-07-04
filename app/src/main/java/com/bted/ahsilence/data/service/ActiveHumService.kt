package com.bted.ahsilence.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bted.ahsilence.core.constants.AudioConstants
import com.bted.ahsilence.core.di.AudioEngineLocator
import com.bted.ahsilence.domain.port.AudioEngine

class ActiveHumService : Service() {

    private val engine: AudioEngine = AudioEngineLocator.engine

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AudioConstants.ACTION_START -> {
                startForegroundSafely()
                engine.startAntiNoiseEmission()
            }
            AudioConstants.ACTION_UPDATE_PARAMS -> {
                val amplitude = intent.getFloatExtra(AudioConstants.EXTRA_AMPLITUDE, AudioConstants.DEFAULT_AMPLITUDE_PERCENT)
                val phase = intent.getFloatExtra(AudioConstants.EXTRA_PHASE, AudioConstants.DEFAULT_PHASE_DEGREES)
                engine.updateParameters(amplitude, phase)
            }
            AudioConstants.ACTION_STOP -> {
                engine.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        engine.stop()
        super.onDestroy()
    }

    private fun startForegroundSafely() {
        val notification = NotificationCompat.Builder(this, AudioConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AhSilence Active")
            .setContentText("Phase cancellation is running in the background.")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(AudioConstants.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(AudioConstants.NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                AudioConstants.NOTIFICATION_CHANNEL_ID,
                "Active Noise Control",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
