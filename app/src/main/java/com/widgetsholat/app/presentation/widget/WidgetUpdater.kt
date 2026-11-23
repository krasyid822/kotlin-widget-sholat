package com.widgetsholat.app.presentation.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        PrayerGlanceWidget().updateAll(context)
        AnalogPrayerGlanceWidget.updateAllWithTick(context)
    }
}
