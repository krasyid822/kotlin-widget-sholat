package com.widgetsholat.app.presentation.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.widgetsholat.app.PrayerApp
import com.widgetsholat.app.domain.model.PrayerName
import com.widgetsholat.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Locale
import kotlin.math.max

class PrayerAlertService : Service(), TextToSpeech.OnInitListener {

    private val settingsRepository: SettingsRepository by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var originalAlarmVolume: Int? = null
    private var originalMusicVolume: Int? = null

    private var pendingPrayerName: PrayerName = PrayerName.SUBUH
    private var pendingRemainingHours: Long = 1L
    private var customAudioUri: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            cleanupAndStop()
            return START_NOT_STICKY
        }

        val prayerRaw = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: PrayerName.SUBUH.name
        pendingPrayerName = runCatching { PrayerName.valueOf(prayerRaw) }.getOrDefault(PrayerName.SUBUH)
        pendingRemainingHours = max(1L, intent?.getLongExtra(EXTRA_REMAINING_HOURS, 1L) ?: 1L)

        startForeground(NOTIFICATION_ID, buildNotification(pendingPrayerName))

        serviceScope.launch {
            customAudioUri = settingsRepository.settingsFlow.first().customAudioUri
            prepareAudioAndMaxVolume()
            tts = TextToSpeech(this@PrayerAlertService, this@PrayerAlertService)
        }

        return START_NOT_STICKY
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            playBeepFallbackAndFinish()
            return
        }
        val engine = tts ?: run {
            playBeepFallbackAndFinish()
            return
        }

        val indonesian = Locale("id", "ID")
        val langResult = engine.setLanguage(indonesian)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            playBeepFallbackAndFinish()
            return
        }

        val offlineVoice = engine.voices?.firstOrNull { voice ->
            voice.locale.language == "id" && !voice.isNetworkConnectionRequired
        }
        if (offlineVoice != null) {
            engine.voice = offlineVoice
        }

        val message = if (pendingPrayerName == PrayerName.SYURUQ) {
            "Memasuki Syuruq, waktu sholat subuh telah berakhir. Tunggu syuruq berakhir sebelum mengerjakan sholat sunah apapun."
        } else {
            "Waktu sholat ${pendingPrayerName.displayName} telah tiba. Segera bersiap sebelum habis $pendingRemainingHours jam lagi."
        }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                serviceScope.launch {
                    if (pendingPrayerName == PrayerName.SYURUQ) {
                        playBeepFallbackAndFinish()
                    } else {
                        playCustomAudioOrFinish()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                serviceScope.launch { playBeepFallbackAndFinish() }
            }
        })

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        engine.setAudioAttributes(attrs)

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        engine.speak(message, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
    }

    private fun prepareAudioAndMaxVolume() {
        val manager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = manager

        runCatching {
            originalAlarmVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxAlarm = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            manager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
        }
        runCatching {
            originalMusicVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxMusic = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
        }

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attrs)
            .build()
        focusRequest = req
        manager.requestAudioFocus(req)
    }

    private fun playCustomAudioOrFinish() {
        val uriString = customAudioUri
        if (uriString.isNullOrBlank()) {
            cleanupAndStop()
            return
        }
        runCatching {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(this@PrayerAlertService, Uri.parse(uriString))
                setVolume(1.0f, 1.0f)
                setOnCompletionListener { cleanupAndStop() }
                setOnErrorListener { _, _, _ ->
                    cleanupAndStop()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
        }.onFailure {
            cleanupAndStop()
        }
    }

    private fun playBeepFallbackAndFinish() {
        serviceScope.launch {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
                delay(600)
                tone.release()
            }
            cleanupAndStop()
        }
    }

    private fun cleanupAndStop() {
        runCatching { tts?.stop(); tts?.shutdown() }
        tts = null
        runCatching { mediaPlayer?.stop(); mediaPlayer?.release() }
        mediaPlayer = null

        val manager = audioManager
        if (manager != null) {
            originalAlarmVolume?.let { savedVol ->
                runCatching { manager.setStreamVolume(AudioManager.STREAM_ALARM, savedVol, 0) }
            }
            originalMusicVolume?.let { savedVol ->
                runCatching { manager.setStreamVolume(AudioManager.STREAM_MUSIC, savedVol, 0) }
            }
        }
        focusRequest?.let { req ->
            runCatching { manager?.abandonAudioFocusRequest(req) }
        }
        originalAlarmVolume = null
        originalMusicVolume = null
        focusRequest = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(prayerName: PrayerName): Notification {
        val stopIntent = Intent(this, PrayerAlertService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, PrayerApp.CHANNEL_PRAYER_ALERT)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Waktu ${prayerName.displayName}")
            .setContentText("Pengingat waktu sholat sedang berjalan")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hentikan", stopPendingIntent)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.widgetsholat.app.ACTION_STOP_ALERT"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_REMAINING_HOURS = "extra_remaining_hours"
        private const val NOTIFICATION_ID = 1001
        private const val UTTERANCE_ID = "prayer_alert_tts"
    }
}
