package com.codekage.showup.domain.usecase

import java.time.LocalDate

/** A single dated entry in one of the report categories — `note` carries the bank-holiday
 *  label or other non-work-day annotation when relevant. */
data class ReportDateEntry(val date: LocalDate, val note: String? = null)

data class ReportResult(
    val totalWorkingDays: Int,
    val officeDays: Int,
    val remoteDays: Int,
    val sickDays: Int,
    val leaveDays: Int,
    val bankHolidayDays: Int,
    val absentDays: Int,
    val officePercentage: Float,
    val goalPercentage: Int,
    val weeklyStats: List<WeeklyStatsData>,
    /** Dates contributing to each category, in chronological order. Lets the UI pop a
     *  tile-tap modal listing the actual days behind each count. */
    val workingDates: List<ReportDateEntry> = emptyList(),
    val officeDates: List<ReportDateEntry> = emptyList(),
    val remoteDates: List<ReportDateEntry> = emptyList(),
    val sickDates: List<ReportDateEntry> = emptyList(),
    val leaveDates: List<ReportDateEntry> = emptyList(),
    val bankHolidayDates: List<ReportDateEntry> = emptyList(),
    val absentDates: List<ReportDateEntry> = emptyList(),
)
