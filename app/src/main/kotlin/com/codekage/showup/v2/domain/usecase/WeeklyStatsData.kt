package com.codekage.showup.v2.domain.usecase

data class WeeklyStatsData(
    val weekNumber: Int,
    val officeDays: Int,
    val remoteDays: Int,
    val sickDays: Int,
    val leaveDays: Int,
    val bankHolidayDays: Int,
    val totalWorkingDays: Int,
)
