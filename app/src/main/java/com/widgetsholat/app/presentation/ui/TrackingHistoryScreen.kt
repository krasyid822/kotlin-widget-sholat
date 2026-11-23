package com.widgetsholat.app.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.widgetsholat.app.domain.model.PrayerTrackingSession
import com.widgetsholat.app.domain.repository.TrackingRepository
import com.widgetsholat.app.domain.util.TrackingChartAggregator
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@Composable
fun TrackingHistoryScreen(
    modifier: Modifier = Modifier,
    trackingRepository: TrackingRepository = koinInject()
) {
    val sessions by trackingRepository.observeAllCompleted()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Tabel Tracking Sholat",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Belum ada riwayat tracking sholat yang selesai.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            val today = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis())
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            val chartSummary = remember(sessions, today) {
                TrackingChartAggregator.aggregate(sessions = sessions, today = today)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item(key = "tracking_charts") {
                    TrackingChartsSection(summary = chartSummary)
                }
                items(sessions, key = { it.id }) { session ->
                    TrackingSessionCard(session)
                }
            }
        }
    }
}

@Composable
private fun TrackingSessionCard(session: PrayerTrackingSession) {
    val tz = TimeZone.currentSystemDefault()
    val p0Str = session.p0Timestamp?.let {
        val local = it.toLocalDateTime(tz)
        "%02d:%02d".format(local.hour, local.minute)
    } ?: "-"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${session.date} • ${session.prayerName.displayName}",
                    fontWeight = FontWeight.Bold
                )
                session.category?.let { cat ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "P0: $p0Str • P1: ${session.p1Mode?.label ?: "-"} • P2: ${session.p2Location?.label ?: "-"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
