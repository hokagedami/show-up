package com.codekage.showup.v2.domain.usecase

import com.codekage.showup.v2.domain.model.AttendanceRecordStatus
import com.codekage.showup.v2.domain.model.Job
import com.codekage.showup.v2.domain.repository.AttendanceRepository
import com.codekage.showup.v2.domain.repository.NonWorkDayRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

data class OfficeDayPlan(
    val job: Job,
    val month: YearMonth,
    val recommendedOfficeDates: List<LocalDate>,
    val alreadyOfficeDates: List<LocalDate>,
    val totalWorkingDays: Int,
    val targetOfficeDays: Int,
)

class GeneratePlanUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val nonWorkDayRepository: NonWorkDayRepository,
) {
    suspend operator fun invoke(job: Job, month: YearMonth = YearMonth.now()): OfficeDayPlan {
        val today = LocalDate.now()
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val nonWorkDates = nonWorkDayRepository.getNonWorkDaysInRange(job.id, monthStart, monthEnd).first()
            .map { it.date }.toSet()
        val records = attendanceRepository.getAttendanceRecordsForJobInRange(job.id, monthStart, monthEnd).first()
        val excluded = records.nonWorkingDates()
        val workDays = job.workDays.toSet()

        val workingDates = mutableListOf<LocalDate>()
        var d = monthStart
        while (!d.isAfter(monthEnd)) {
            if (d.dayOfWeek in workDays && d !in nonWorkDates && d !in excluded) workingDates += d
            d = d.plusDays(1)
        }

        val alreadyOffice = records.filter { it.status == AttendanceRecordStatus.OFFICE }.map { it.date }
        val alreadyMarkedSet = records.map { it.date }.toSet()

        val target = ceil(workingDates.size * job.monthlyGoalPercent / 100.0).toInt()
        val needed = (target - alreadyOffice.size).coerceAtLeast(0)

        val available = workingDates
            .filter { !it.isBefore(today) && it !in alreadyMarkedSet }
        val suggestion = distributeEvenly(available, needed)

        return OfficeDayPlan(
            job = job,
            month = month,
            recommendedOfficeDates = suggestion,
            alreadyOfficeDates = alreadyOffice,
            totalWorkingDays = workingDates.size,
            targetOfficeDays = target,
        )
    }

    /** Spread n picks evenly across the available list so the user gets a balanced mix of days. */
    private fun distributeEvenly(available: List<LocalDate>, n: Int): List<LocalDate> {
        if (n <= 0 || available.isEmpty()) return emptyList()
        if (n >= available.size) return available
        val step = available.size.toDouble() / n
        return (0 until n).map { i -> available[(i * step).toInt().coerceAtMost(available.size - 1)] }
    }
}
