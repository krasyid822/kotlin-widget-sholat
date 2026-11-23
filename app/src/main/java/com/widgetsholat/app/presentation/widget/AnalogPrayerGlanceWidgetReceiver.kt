package com.widgetsholat.app.presentation.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.widgetsholat.app.data.scheduler.PrayerAlarmScheduler

class AnalogPrayerGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AnalogPrayerGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PrayerAlarmScheduler(context).scheduleNextAnalogWidgetTick()
    }

    override fun onDisabled(context: Context) {
        PrayerAlarmScheduler(context).cancelAnalogWidgetTick()
        super.onDisabled(context)
    }
}
