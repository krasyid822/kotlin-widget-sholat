package com.widgetsholat.app.domain.model

import kotlinx.datetime.LocalDate

data class CategoryCount(
    val category: TrackingCategory,
    val count: Int,
    val fraction: Float
)

data class DailyTrackingPoint(
    val date: LocalDate,
    val countsByCategory: Map<TrackingCategory, Int>,
    val totalCount: Int
)

data class TrackingChartSummary(
    val totalSessions: Int,
    val onTimePercentage: Int,
    val jamaahPercentage: Int,
    val categoryDistribution: List<CategoryCount>,
    val dailyPoints: List<DailyTrackingPoint>
)
