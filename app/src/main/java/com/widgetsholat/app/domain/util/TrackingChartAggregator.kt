package com.widgetsholat.app.domain.util

import com.widgetsholat.app.domain.model.CategoryCount
import com.widgetsholat.app.domain.model.DailyTrackingPoint
import com.widgetsholat.app.domain.model.PrayerTrackingSession
import com.widgetsholat.app.domain.model.TrackingCategory
import com.widgetsholat.app.domain.model.TrackingChartSummary
import com.widgetsholat.app.domain.model.TrackingExecutionMode
import com.widgetsholat.app.domain.model.TrackingStatus
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

object TrackingChartAggregator {

    fun aggregate(
        sessions: List<PrayerTrackingSession>,
        today: LocalDate,
        days: Int = 7
    ): TrackingChartSummary {
        val completed = sessions.filter {
            it.status == TrackingStatus.COMPLETED && it.category != null
        }
        val totalSessions = completed.size

        val onTimeCount = completed.count { it.category != TrackingCategory.QODHO }
        val jamaahCount = completed.count { it.p1Mode == TrackingExecutionMode.JAMAAH }

        val onTimePercentage = if (totalSessions > 0) (onTimeCount * 100) / totalSessions else 0
        val jamaahPercentage = if (totalSessions > 0) (jamaahCount * 100) / totalSessions else 0

        val countsByCat = completed.groupingBy { requireNotNull(it.category) }.eachCount()
        val categoryDistribution = TrackingCategory.entries.map { category ->
            val count = countsByCat[category] ?: 0
            val fraction = if (totalSessions > 0) count.toFloat() / totalSessions else 0f
            CategoryCount(
                category = category,
                count = count,
                fraction = fraction
            )
        }

        val safeDays = days.coerceAtLeast(1)
        val startDate = today.minus(safeDays - 1, DateTimeUnit.DAY)
        val sessionsByDate = completed.groupBy { it.date }

        val dailyPoints = (0 until safeDays).map { offset ->
            val date = startDate.plus(offset, DateTimeUnit.DAY)
            val daySessions = sessionsByDate[date].orEmpty()
            val dayCounts = daySessions
                .mapNotNull { it.category }
                .groupingBy { it }
                .eachCount()
            DailyTrackingPoint(
                date = date,
                countsByCategory = dayCounts,
                totalCount = daySessions.size
            )
        }

        return TrackingChartSummary(
            totalSessions = totalSessions,
            onTimePercentage = onTimePercentage,
            jamaahPercentage = jamaahPercentage,
            categoryDistribution = categoryDistribution,
            dailyPoints = dailyPoints
        )
    }
}
