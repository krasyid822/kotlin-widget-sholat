package com.widgetsholat.app

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.widgetsholat.app.data.scheduler.DailySyncWorker
import com.widgetsholat.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class PrayerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PrayerApp)
            workManagerFactory()
            modules(appModule)
        }
        createNotificationChannels()
        DailySyncWorker.enqueuePeriodic(this)
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val alertChannel = NotificationChannel(
            CHANNEL_PRAYER_ALERT,
            "Pengingat Waktu Sholat",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikasi dan suara pengingat waktu sholat serta Syuruq"
        }
        val trackerChannel = NotificationChannel(
            CHANNEL_TRACKER_OVERLAY,
            "Tracking Sholat",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Layanan modal tracking sholat"
        }
        val aodChannel = NotificationChannel(
            CHANNEL_AOD_MONITOR,
            "Always-On Display (AOD)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Memantau layar mati untuk menampilkan jam AOD"
        }
        val aodLaunchChannel = NotificationChannel(
            CHANNEL_AOD_LAUNCH,
            "Peluncuran AOD",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Menampilkan layar jam AOD saat layar dimatikan"
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannels(
            listOf(alertChannel, trackerChannel, aodChannel, aodLaunchChannel)
        )
    }

    companion object {
        const val CHANNEL_PRAYER_ALERT = "prayer_alert_channel"
        const val CHANNEL_TRACKER_OVERLAY = "tracker_overlay_channel"
        const val CHANNEL_AOD_MONITOR = "aod_monitor_channel"
        const val CHANNEL_AOD_LAUNCH = "aod_launch_channel"
    }
}
