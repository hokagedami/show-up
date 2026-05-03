package com.codekage.showup.presentation.dashboard

import com.codekage.showup.domain.model.AttendanceRecordStatus
import com.codekage.showup.domain.model.Job
import com.codekage.showup.domain.usecase.GoalPaceStatus
import java.time.LocalDate

data class JobDashboardItem(
    val job: Job,
    val officeDays: Int,
    val totalWorkingDays: Int,
    val workingDaysSoFar: Int,
    val remainingWorkingDays: Int,
    val currentPercentage: Float,
    val projectedPercentage: Float,
    val daysNeededForGoal: Int,
    val goalPaceStatus: GoalPaceStatus,
    val todayStatus: AttendanceRecordStatus?,
    /** Future office days the user has accepted from a generated plan. Drives the
     *  expandable "planned reminders" section on the dashboard card. */
    val plannedOfficeDates: List<LocalDate> = emptyList(),
)
