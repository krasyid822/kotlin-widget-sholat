package com.widgetsholat.app.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widgetsholat.app.domain.model.DailyPrayerSchedule
import com.widgetsholat.app.domain.repository.PrayerRepository
import com.widgetsholat.app.domain.repository.SettingsRepository
import com.widgetsholat.app.domain.util.CoordinateParser
import com.widgetsholat.app.presentation.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PrayerGlanceWidget : GlanceAppWidget(), KoinComponent {

    private val settingsRepository: SettingsRepository by inject()
    private val prayerRepository: PrayerRepository by inject()

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(140.dp, 60.dp),
            DpSize(250.dp, 110.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = settingsRepository.settingsFlow.first()
        val coords = settings.coordinates
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val tz = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(tz).date
        val schedule = coords?.let { prayerRepository.getScheduleForDate(today, it) }

        provideContent {
            GlanceTheme {
                if (schedule == null) {
                    EmptyLocationWidgetContent()
                } else {
                    ScheduleWidgetContent(schedule = schedule, now = now, tz = tz)
                }
            }
        }
    }

    @Composable
    private fun EmptyLocationWidgetContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Lokasi belum diatur",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Button(
                text = "Atur Lokasi",
                onClick = actionStartActivity<MainActivity>()
            )
        }
    }

    @Composable
    private fun ScheduleWidgetContent(
        schedule: DailyPrayerSchedule,
        now: Instant,
        tz: TimeZone
    ) {
        val size = LocalSize.current
        val isExpanded = size.height >= 100.dp
        val activeWindow = schedule.resolveActiveWindow(now)
        val locationLabel = schedule.coordinates.label.ifBlank {
            CoordinateParser.format(schedule.coordinates)
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp)
        ) {
            Text(
                text = locationLabel,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = if (activeWindow?.isSyuruqForbidden == true) {
                    "Syuruq (Larangan) • Selesai ${formatTime(schedule.syuruqEndTime, tz)}"
                } else if (activeWindow != null) {
                    "${activeWindow.prayerName.displayName} • Habis ${formatTime(activeWindow.endTime, tz)}"
                } else {
                    "Menuju Subuh • ${formatTime(schedule.subuh, tz)}"
                },
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            if (isExpanded) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    PrayerCell("Subuh", formatTime(schedule.subuh, tz), GlanceModifier.defaultWeight())
                    PrayerCell("Syuruq", formatTime(schedule.syuruqStartTime, tz), GlanceModifier.defaultWeight())
                    PrayerCell("Dzuhur", formatTime(schedule.dzuhur, tz), GlanceModifier.defaultWeight())
                }
                Spacer(modifier = GlanceModifier.height(4.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    PrayerCell("Ashar", formatTime(schedule.ashar, tz), GlanceModifier.defaultWeight())
                    PrayerCell("Maghrib", formatTime(schedule.maghrib, tz), GlanceModifier.defaultWeight())
                    PrayerCell("Isya", formatTime(schedule.isya, tz), GlanceModifier.defaultWeight())
                }
            }
        }
    }

    @Composable
    private fun PrayerCell(name: String, time: String, modifier: GlanceModifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp)
            )
            Text(
                text = time,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }

    private fun formatTime(instant: Instant, tz: TimeZone): String {
        val local = instant.toLocalDateTime(tz)
        return "%02d:%02d".format(local.hour, local.minute)
    }
}
