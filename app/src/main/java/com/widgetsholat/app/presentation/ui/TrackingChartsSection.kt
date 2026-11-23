package com.widgetsholat.app.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.widgetsholat.app.domain.model.TrackingCategory
import com.widgetsholat.app.domain.model.TrackingChartSummary
import kotlin.math.roundToInt

fun TrackingCategory.chartColor(): Color = when (this) {
    TrackingCategory.JAMAAH_MASJID -> Color(0xFF2E7D32)
    TrackingCategory.JAMAAH_NON_MASJID -> Color(0xFF00897B)
    TrackingCategory.SENDIRI_MASJID -> Color(0xFF1976D2)
    TrackingCategory.SENDIRI_NON_MASJID -> Color(0xFFF57C00)
    TrackingCategory.QODHO -> Color(0xFFD32F2F)
}

private fun TrackingCategory.shortLabel(): String = when (this) {
    TrackingCategory.JAMAAH_MASJID -> "Jamaah Masjid"
    TrackingCategory.JAMAAH_NON_MASJID -> "Jamaah Non-Masjid"
    TrackingCategory.SENDIRI_MASJID -> "Sendiri Masjid"
    TrackingCategory.SENDIRI_NON_MASJID -> "Sendiri Non-Masjid"
    TrackingCategory.QODHO -> "Qodho"
}

@Composable
fun TrackingChartsSection(
    summary: TrackingChartSummary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryDistributionCard(summary = summary)
        DailyTrendChartCard(summary = summary)
    }
}

@Composable
private fun SummaryDistributionCard(summary: TrackingChartSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Ringkasan & Distribusi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(label = "Total", value = "${summary.totalSessions}")
                MetricItem(label = "Tepat Waktu", value = "${summary.onTimePercentage}%")
                MetricItem(label = "Jamaah", value = "${summary.jamaahPercentage}%")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                summary.categoryDistribution.forEach { item ->
                    val pct = (item.fraction * 100f).roundToInt()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.category.shortLabel(),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${item.count} ($pct%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (item.fraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(item.fraction.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(item.category.chartColor())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyTrendChartCard(summary: TrackingChartSummary) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxScale = maxOf(5, summary.dailyPoints.maxOfOrNull { it.totalCount } ?: 5)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Tren 7 Hari Terakhir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val width = size.width
                val height = size.height

                // Horizontal grid lines for 0..maxScale
                for (step in 0..maxScale) {
                    val y = height - (step.toFloat() / maxScale) * height
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val count = summary.dailyPoints.size
                if (count > 0) {
                    val slotWidth = width / count
                    val barWidth = (slotWidth * 0.5f).coerceAtMost(28.dp.toPx())

                    summary.dailyPoints.forEachIndexed { index, point ->
                        val left = index * slotWidth + (slotWidth - barWidth) / 2f
                        var currentBottom = height

                        TrackingCategory.entries.forEach { category ->
                            val catCount = point.countsByCategory[category] ?: 0
                            if (catCount > 0) {
                                val segmentHeight = (catCount.toFloat() / maxScale) * height
                                val top = currentBottom - segmentHeight
                                drawRoundRect(
                                    color = category.chartColor(),
                                    topLeft = Offset(left, top),
                                    size = Size(barWidth, segmentHeight),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                                currentBottom = top
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                summary.dailyPoints.forEach { point ->
                    Text(
                        text = "%02d/%02d".format(point.date.dayOfMonth, point.date.monthNumber),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TrackingCategory.entries.forEach { category ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(category.chartColor())
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = category.shortLabel(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
