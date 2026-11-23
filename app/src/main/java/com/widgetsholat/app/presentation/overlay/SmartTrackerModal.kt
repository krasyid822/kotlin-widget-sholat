package com.widgetsholat.app.presentation.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.widgetsholat.app.domain.model.PrayerTrackingSession
import com.widgetsholat.app.domain.model.TrackingExecutionMode
import com.widgetsholat.app.domain.model.TrackingLocation
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun SmartTrackerModal(
    session: PrayerTrackingSession,
    isDoneStep: Boolean,
    onDismissOrLater: () -> Unit,
    onSudah: () -> Unit,
    onSelectMode: (TrackingExecutionMode) -> Unit,
    onSelectLocation: (TrackingLocation) -> Unit
) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissOrLater
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tracking Sholat ${session.prayerName.displayName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismissOrLater) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    val summaryChips = buildSummaryChips(session)
                    if (summaryChips.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = summaryChips.joinToString(" • "),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        isDoneStep -> {
                            Text(
                                text = "oke.",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        session.p0Timestamp == null -> {
                            Text(
                                text = "udah sholat ${session.prayerName.displayName} belum?",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismissOrLater,
                                    modifier = Modifier.weight(1f)
                                ) { Text("belum") }
                                Button(
                                    onClick = onSudah,
                                    modifier = Modifier.weight(1f)
                                ) { Text("udah") }
                            }
                        }
                        session.p1Mode == null -> {
                            Text(
                                text = "sendiri atau jamaah?",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onSelectMode(TrackingExecutionMode.SENDIRI) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("sendiri") }
                                Button(
                                    onClick = { onSelectMode(TrackingExecutionMode.JAMAAH) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("jamaah") }
                            }
                        }
                        session.p2Location == null -> {
                            Text(
                                text = "dimana?",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onSelectLocation(TrackingLocation.NON_MASJID) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("nggak dimasjid") }
                                Button(
                                    onClick = { onSelectLocation(TrackingLocation.MASJID) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("masjid") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildSummaryChips(session: PrayerTrackingSession): List<String> {
    val chips = mutableListOf<String>()
    session.p0Timestamp?.let { p0 ->
        val local = p0.toLocalDateTime(TimeZone.currentSystemDefault())
        chips.add("Sudah sholat (%02d:%02d)".format(local.hour, local.minute))
    }
    session.p1Mode?.let { chips.add(it.label) }
    session.p2Location?.let { chips.add(it.label) }
    return chips
}
