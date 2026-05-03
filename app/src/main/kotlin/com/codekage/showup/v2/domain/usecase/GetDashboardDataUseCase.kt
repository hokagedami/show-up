package com.codekage.showup.v2.domain.usecase

import com.codekage.showup.v2.domain.model.AttendanceRecord
import com.codekage.showup.v2.domain.model.AttendanceRecordStatus
import com.codekage.showup.v2.domain.model.Job
import com.codekage.showup.v2.domain.repository.AttendanceRepository
import com.codekage.showup.v2.domain.repository.JobRepository
import com.codekage.showup.v2.domain.repository.NonWorkDayRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

class GetDashboardDataUseCase(
    private val jobRepository: JobRepository,
    private val attendanceRepository: AttendanceRepository,
    private val nonWorkDayRepository: NonWorkDayRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<DashboardData> =
        jobRepository.getActiveJobs().flatMapLatest { jobs -> buildDashboardFlow(jobs) }

    private fun buildDashboardFlow(jobs: List<Job>): Flow<DashboardData> {
        if (jobs.isEmpty()) return flowOf(DashboardData(hasJobs = false))

        val today = LocalDate.now()
        val ym = YearMonth.from(today)
        val monthStart = ym.atDay(1)
        val monthEnd = ym.atEndOfMonth()

        val itemFlows: List<Flow<JobDashboardData>> = jobs.map { job ->
            val records = attendanceRepository.getAttendanceRecordsForJobInRange(job.id, monthStart, monthEnd)
            val nonWorkDays = nonWorkDayRepository.getNonWorkDaysInRange(job.id, monthStart, monthEnd)
            combine(records, nonWorkDays) { recordList, nwds ->
                buildItem(job, today, monthStart, monthEnd, recordList, nwds.map { it.date }.toSet())
            }
        }

        return combine(itemFlows) { items ->
            DashboardData(jobItems = items.toList(), isLoading = false, hasJobs = true)
        }
    }

    private fun buildItem(
        job: Job,
        today: LocalDate,
        monthStart: LocalDate,
        monthEnd: LocalDate,
        records: List<AttendanceRecord>,
        nonWorkDates: Set<LocalDate>,
    ): JobDashboardData {
        val workDays = job.workDays.toSet()
        val excluded = records.nonWorkingDates()
        val totalWorkingDays = countWorkingDays(monthStart, monthEnd, workDays, nonWorkDates, excluded)
        val workingDaysSoFar = countWorkingDays(monthStart, today, workDays, nonWorkDates, excluded)
        val remainingWorkingDays = (totalWorkingDays - workingDaysSoFar).coerceAtLeast(0)

        // Office days actually completed (today or earlier) vs total scheduled including future plans.
        // currentPercentage uses the "so far" count so applying a future-dated plan doesn't blow it up.
        val officeDaysSoFar = records.count {
            it.status == AttendanceRecordStatus.OFFICE && !it.date.isAfter(today)
        }
        val officeDaysScheduled = records.count { it.status == AttendanceRecordStatus.OFFICE }
        val currentPercentage = if (workingDaysSoFar > 0) {
            officeDaysSoFar.toFloat() / workingDaysSoFar.toFloat() * 100f
        } else 0f
        val projectedPercentage = if (totalWorkingDays > 0) {
            officeDaysScheduled.toFloat() / totalWorkingDays.toFloat() * 100f
        } else 0f

        val goalDaysAbsolute = ceil(totalWorkingDays * job.monthlyGoalPercent / 100.0).toInt()
        val daysNeededForGoal = (goalDaysAbsolute - officeDaysScheduled).coerceAtLeast(0)

        val goalPaceStatus = when {
            totalWorkingDays == 0 -> GoalPaceStatus.NO_DATA
            officeDaysScheduled >= goalDaysAbsolute -> GoalPaceStatus.ACHIEVED
            daysNeededForGoal > remainingWorkingDays -> GoalPaceStatus.IMPOSSIBLE
            daysNeededForGoal.toFloat() / remainingWorkingDays.coerceAtLeast(1) > 0.5f -> GoalPaceStatus.AT_RISK
            else -> GoalPaceStatus.ON_TRACK
        }
        val officeDays = officeDaysSoFar

        val todayStatus = records.firstOrNull { it.date == today }?.status

        return JobDashboardData(
            job = job,
            officeDays = officeDays,
            totalWorkingDays = totalWorkingDays,
            workingDaysSoFar = workingDaysSoFar,
            remainingWorkingDays = remainingWorkingDays,
            currentPercentage = currentPercentage,
            projectedPercentage = projectedPercentage,
            daysNeededForGoal = daysNeededForGoal,
            goalPaceStatus = goalPaceStatus,
            todayStatus = todayStatus,
        )
    }
}
