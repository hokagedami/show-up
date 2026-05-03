package com.codekage.showup.v2.util

data class WeeklyPdfStats(
    val weekNumber: Int,
    val officeDays: Int,
    val remoteDays: Int,
    val sickDays: Int,
    val leaveDays: Int,
    val bankHolidayDays: Int,
    val totalWorkingDays: Int,
)

data class PdfReportData(
    val jobName: String,
    val period: String,
    val totalWorkingDays: Int,
    val officeDays: Int,
    val remoteDays: Int,
    val sickDays: Int,
    val leaveDays: Int,
    val bankHolidayDays: Int,
    val absentDays: Int,
    val officePercentage: Float,
    val goalPercentage: Int,
    val weeklyStats: List<WeeklyPdfStats>,
)
