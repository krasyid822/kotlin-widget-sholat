package com.widgetsholat.app.presentation.aod

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.widgetsholat.app.PrayerApp
import com.widgetsholat.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ScreenOffReceiver : BroadcastReceiver(), KoinComponent {

    private val settingsRepository: SettingsRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_SCREEN_OFF) return
        if (AodClockActivity.isActive || AodClockActivity.wasRecentlyActive()) {
            AodClockActivity.exitFromPowerButton(context)
            return
        }
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val partialLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WidgetSholat:AodScreenOff"
        )?.apply { acquire(5_000L) }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var screenWakeLock: PowerManager.WakeLock? = null
            try {
                if (settingsRepository.settingsFlow.first().aodEnabled) {
                    delay(400L)
                    if (powerManager?.isInteractive == true) return@launch

                    @Suppress("DEPRECATION")
                    screenWakeLock = powerManager?.newWakeLock(
                        PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "WidgetSholat:AodScreenWake"
                    )?.apply { acquire(3_000L) }

                    val aodIntent = Intent(context, AodClockActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_NO_USER_ACTION
                        )
                    }

                    val launchedDirectly = if (Settings.canDrawOverlays(context)) {
                        runCatching {
                            withContext(Dispatchers.Main) {
                                context.startActivity(aodIntent)
                            }
                        }.isSuccess
                    } else {
                        false
                    }

                    if (launchedDirectly) {
                        delay(400L)
                        if (!AodClockActivity.isActive) {
                            postFullScreenAodNotification(context, aodIntent)
                        }
                    } else {
                        postFullScreenAodNotification(context, aodIntent)
                    }
                }
            } finally {
                if (screenWakeLock?.isHeld == true) {
                    runCatching { screenWakeLock.release() }
                }
                if (partialLock?.isHeld == true) {
                    runCatching { partialLock.release() }
                }
                pendingResult.finish()
            }
        }
    }

    private fun postFullScreenAodNotification(context: Context, aodIntent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_LAUNCH,
            aodIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, PrayerApp.CHANNEL_AOD_LAUNCH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("AOD Analog")
            .setContentText("Menampilkan jam Always-On Display")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(3_000L)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID_LAUNCH, notification)
    }

    companion object {
        const val NOTIFICATION_ID_LAUNCH = 1004
    }
}
