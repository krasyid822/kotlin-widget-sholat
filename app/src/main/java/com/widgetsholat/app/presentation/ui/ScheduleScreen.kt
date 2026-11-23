package com.widgetsholat.app.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.widgetsholat.app.domain.repository.AppSettings
import com.widgetsholat.app.domain.repository.PrayerRepository
import com.widgetsholat.app.domain.repository.SettingsRepository
import com.widgetsholat.app.domain.util.CoordinateParser
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@Composable
fun ScheduleScreen(
    modifier: Modifier = Modifier,
    settingsRepository: SettingsRepository = koinInject(),
    prayerRepository: PrayerRepository = koinInject()
) {
    val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val coords = settings.coordinates

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Jadwal Sholat (Kemenag RI)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (coords == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Koordinat lokasi belum diatur. Buka tab Pengaturan untuk mengambil GPS luring atau memasukkan koordinat secara manual.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@Column
        }

        val tz = TimeZone.currentSystemDefault()
        val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val today = now.toLocalDateTime(tz).date
        val schedule = prayerRepository.getScheduleForDate(today, coords)

        Text(
            text = coords.label.ifBlank { CoordinateParser.format(coords) },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        ScheduleRowCard("Subuh", formatTime(schedule.subuh, tz))
        ScheduleRowCard(
            "Syuruq (7 Hasta)",
            "${formatTime(schedule.syuruqStartTime, tz)} - ${formatTime(schedule.syuruqEndTime, tz)} (${schedule.syuruqDuration.inWholeMinutes} mnt)"
        )
        ScheduleRowCard("Dzuhur", formatTime(schedule.dzuhur, tz))
        ScheduleRowCard("Ashar", formatTime(schedule.ashar, tz))
        ScheduleRowCard("Maghrib", formatTime(schedule.maghrib, tz))
        ScheduleRowCard("Isya", formatTime(schedule.isya, tz))

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ScheduleRowCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontWeight = FontWeight.Medium)
            Text(text = value, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatTime(instant: Instant, tz: TimeZone): String {
    val local = instant.toLocalDateTime(tz)
    return "%02d:%02d".format(local.hour, local.minute)
}
