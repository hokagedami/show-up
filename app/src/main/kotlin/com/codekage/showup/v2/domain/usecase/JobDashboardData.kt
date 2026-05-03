package com.codekage.showup.v2.domain.usecase

import com.codekage.showup.v2.domain.model.AttendanceRecordStatus
import com.codekage.showup.v2.domain.model.Job

data class JobDashboardData(
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
)
