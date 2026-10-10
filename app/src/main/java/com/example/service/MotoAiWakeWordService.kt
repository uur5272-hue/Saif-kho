package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

class MotoAiWakeWordService : Service() {

    companion object {
        const val CHANNEL_ID = "moto_ai_wake_word_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.motoai.action.START_WAKE_WORD"
        const val ACTION_STOP = "com.example.motoai.action.STOP_WAKE_WORD"
        const val ACTION_WAKE_WORD_DETECTED = "com.example.motoai.action.WAKE_WORD_DETECTED"
        const val EXTRA_TRIGGER_QUERY = "extra_trigger_query"

        var isServiceRunning = false
            private set
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundService()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundServiceWithNotification()
                initSpeechRecognizer()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MOTO AI Wake-Word Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors for 'Hey MOTO' wake word triggers in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceWithNotification() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MOTO AI Voice Engine")
            .setContentText("Active — Listening for 'Hey MOTO'")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isServiceRunning = true
    }

    private fun initSpeechRecognizer() {
        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                    return@post
                }
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                    }
                    override fun onError(error: Int) {
                        isListening = false
                        // Restart recognition after a brief cool-off if service is still active
                        if (isServiceRunning) {
                            mainHandler.postDelayed({ restartListening() }, 1500)
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            checkWakeWord(matches)
                        }
                        if (isServiceRunning) {
                            mainHandler.postDelayed({ restartListening() }, 500)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            checkWakeWord(matches)
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                restartListening()
            } catch (e: Exception) {
                // Ignore initialization failures gracefully
            }
        }
    }

    private fun restartListening() {
        if (!isServiceRunning) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            // Re-attempt after delay
            mainHandler.postDelayed({ if (isServiceRunning) restartListening() }, 2000)
        }
    }

    private fun checkWakeWord(phrases: List<String>) {
        for (phrase in phrases) {
            val lower = phrase.lowercase(Locale.ROOT)
            if (lower.contains("moto") || lower.contains("hey moto") || lower.contains("ok moto")) {
                triggerWakeAction(phrase)
                break
            }
        }
    }

    private fun triggerWakeAction(spokenText: String) {
        // Haptic feedback
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(120)
            }
        } catch (e: Exception) {}

        // Launch / bring MainActivity to front with voice prompt intent
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TRIGGER_QUERY, spokenText)
        }
        startActivity(launchIntent)

        // Also send broadcast
        val broadcastIntent = Intent(ACTION_WAKE_WORD_DETECTED).apply {
            putExtra(EXTRA_TRIGGER_QUERY, spokenText)
            setPackage(packageName)
        }
        sendBroadcast(broadcastIntent)
    }

    private fun stopForegroundService() {
        isServiceRunning = false
        mainHandler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopForegroundService()
        super.onDestroy()
    }
}
