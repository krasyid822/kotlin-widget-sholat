package com.widgetsholat.app.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.widgetsholat.app.data.scheduler.PrayerAlarmScheduler
import com.widgetsholat.app.domain.repository.PrayerRepository
import com.widgetsholat.app.domain.repository.SettingsRepository
import com.widgetsholat.app.domain.util.AnalogPrayerDialMapper
import com.widgetsholat.app.presentation.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.roundToInt

class AnalogPrayerGlanceWidget : GlanceAppWidget(), KoinComponent {

    private val settingsRepository: SettingsRepository by inject()
    private val prayerRepository: PrayerRepository by inject()

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 180.dp),
            DpSize(260.dp, 260.dp),
            DpSize(320.dp, 320.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = settingsRepository.settingsFlow.first()
        val coords = settings.coordinates
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val tz = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(tz).date
        val schedule = coords?.let { prayerRepository.getScheduleForDate(today, it) }
        val density = context.resources.displayMetrics.density
        PrayerAlarmScheduler(context).scheduleNextAnalogWidgetTick()

        provideContent {
            val prefs = currentState<Preferences>()
            val tickMs = prefs[TICK_EPOCH_MS_KEY] ?: System.currentTimeMillis()
            val tickNow = Instant.fromEpochMilliseconds(tickMs)

            GlanceTheme {
                if (schedule == null) {
                    EmptyLocationContent()
                } else {
                    val size = LocalSize.current
                    val widthPx = (size.width.value * density).roundToInt()
                    val heightPx = (size.height.value * density).roundToInt()
                    val dialState = AnalogPrayerDialMapper.map(schedule, tickNow, tz)
                    val bitmap = AnalogPrayerClockBitmapRenderer.render(
                        state = dialState,
                        widthPx = widthPx,
                        heightPx = heightPx
                    )
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = dialState.statusLabel,
                        modifier = GlanceModifier.fillMaxSize()
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyLocationContent() {
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

    companion object {
        val TICK_EPOCH_MS_KEY = longPreferencesKey("analog_tick_epoch_ms")

        suspend fun updateAllWithTick(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(AnalogPrayerGlanceWidget::class.java)
            if (ids.isEmpty()) return
            val nowMs = System.currentTimeMillis()
            val widget = AnalogPrayerGlanceWidget()
            ids.forEach { id ->
                updateAppWidgetState(context, id) { prefs ->
                    prefs[TICK_EPOCH_MS_KEY] = nowMs
                }
                widget.update(context, id)
            }
        }
    }
}
