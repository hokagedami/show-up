package com.codekage.showup.domain.usecase

import com.codekage.showup.domain.model.AttendanceRecordStatus
import com.codekage.showup.domain.model.Job
import com.codekage.showup.domain.model.NonWorkDayType
import com.codekage.showup.domain.repository.AttendanceRepository
import com.codekage.showup.domain.repository.NonWorkDayRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

class GetReportDataUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val nonWorkDayRepository: NonWorkDayRepository,
) {
    suspend operator fun invoke(
        job: Job,
        startDate: LocalDate,
        endDate: LocalDate,
        includeWeeklyBreakdown: Boolean = true,
    ): ReportResult {
        val records = attendanceRepository
            .getAttendanceRecordsForJobInRange(job.id, startDate, endDate)
            .first()
        val nonWorkDays = nonWorkDayRepository
            .getNonWorkDaysInRange(job.id, startDate, endDate)
            .first()
        val nonWorkDayByDate = nonWorkDays.associateBy { it.date }
        val nonWorkDates = nonWorkDayByDate.keys
        val workDays = job.workDays.toSet()
        val excluded = records.nonWorkingDates()

        // Tally categories and collect the dates contributing to each.
        val recordsByDate = records.associateBy { it.date }
        val workingDates = mutableListOf<ReportDateEntry>()
        val officeDates = mutableListOf<ReportDateEntry>()
        val remoteDates = mutableListOf<ReportDateEntry>()
        val sickDates = mutableListOf<ReportDateEntry>()
        val leaveDates = mutableListOf<ReportDateEntry>()
        val bankHolidayDates = mutableListOf<ReportDateEntry>()
        val absentDates = mutableListOf<ReportDateEntry>()

        var d = startDate
        while (!d.isAfter(endDate)) {
            if (d.dayOfWeek in workDays) {
                val nwd = nonWorkDayByDate[d]
                when {
                    // Bank holidays / annual leave / sick stored in non_work_days (e.g. from
                    // the holiday sync) take precedence over any attendance record on that
                    // date and are counted by their non-work-day type.
                    nwd?.type == NonWorkDayType.BANK_HOLIDAY ->
                        bankHolidayDates += ReportDateEntry(d, nwd.label)
                    nwd?.type == NonWorkDayType.ANNUAL_LEAVE ->
                        leaveDates += ReportDateEntry(d, nwd.label)
                    nwd?.type == NonWorkDayType.SICK ->
                        sickDates += ReportDateEntry(d, nwd.label)
                    nwd?.type == NonWorkDayType.OTHER -> { /* not counted in any tile */ }
                    else -> {
                        // Working day (no non-work-day override); add to the working list.
                        workingDates += ReportDateEntry(d)
                        when (recordsByDate[d]?.status) {
                            AttendanceRecordStatus.OFFICE -> officeDates += ReportDateEntry(d)
                            AttendanceRecordStatus.REMOTE -> remoteDates += ReportDateEntry(d)
                            AttendanceRecordStatus.SICK -> sickDates += ReportDateEntry(d)
                            AttendanceRecordStatus.LEAVE, AttendanceRecordStatus.ANNUAL_LEAVE ->
                                leaveDates += ReportDateEntry(d)
                            AttendanceRecordStatus.BANK_HOLIDAY -> bankHolidayDates += ReportDateEntry(d)
                            AttendanceRecordStatus.ABSENT -> absentDates += ReportDateEntry(d)
                            null -> {
                                // Unmarked working day in the past counts as Absent for the
                                // purposes of the tile drill-down — but we don't bump the
                                // tile count itself (that would change reported totals).
                                if (d.isBefore(LocalDate.now())) absentDates += ReportDateEntry(d)
                            }
                        }
                    }
                }
            }
            d = d.plusDays(1)
        }
        val officeDays = officeDates.size
        val remoteDays = remoteDates.size
        val sickDays = sickDates.size
        val leaveDays = leaveDates.size
        val bankHolidayDays = bankHolidayDates.size
        val absentDays = absentDates.count {
            recordsByDate[it.date]?.status == AttendanceRecordStatus.ABSENT
        }

        // ...but the working-day denominator excludes sick/leave/annual-leave/bank-holiday days
        // so percentages reflect *expected* working days, not raw weekday count.
        val totalWorkingDays = countWorkingDays(startDate, endDate, workDays, nonWorkDates, excluded)

        val officePercentage = if (totalWorkingDays > 0) {
            officeDays.toFloat() / totalWorkingDays.toFloat() * 100f
        } else 0f

        val weeklyStats: List<WeeklyStatsData> = if (includeWeeklyBreakdown) {
            buildWeeklyStats(startDate, endDate, recordsByDate, nonWorkDayByDate, workDays, excluded)
        } else emptyList()

        return ReportResult(
            totalWorkingDays = totalWorkingDays,
            officeDays = officeDays,
            remoteDays = remoteDays,
            sickDays = sickDays,
            leaveDays = leaveDays,
            bankHolidayDays = bankHolidayDays,
            absentDays = absentDays,
            officePercentage = officePercentage,
            goalPercentage = job.monthlyGoalPercent,
            weeklyStats = weeklyStats,
            workingDates = workingDates,
            officeDates = officeDates,
            remoteDates = remoteDates,
            sickDates = sickDates,
            leaveDates = leaveDates,
            bankHolidayDates = bankHolidayDates,
            absentDates = absentDates,
        )
    }

    private fun buildWeeklyStats(
        startDate: LocalDate,
        endDate: LocalDate,
        recordsByDate: Map<LocalDate, com.codekage.showup.domain.model.AttendanceRecord>,
        nonWorkDayByDate: Map<LocalDate, com.codekage.showup.domain.model.NonWorkDay>,
        workDays: Set<java.time.DayOfWeek>,
        excluded: Set<LocalDate>,
    ): List<WeeklyStatsData> {
        val weekFields = WeekFields.of(Locale.getDefault())
        data class Bucket(
            var office: Int = 0,
            var remote: Int = 0,
            var sick: Int = 0,
            var leave: Int = 0,
            var bankHoliday: Int = 0,
            var total: Int = 0,
        )
        val buckets = linkedMapOf<Int, Bucket>()
        var d = startDate
        while (!d.isAfter(endDate)) {
            if (d.dayOfWeek in workDays) {
                val week = d.get(weekFields.weekOfWeekBasedYear())
                val b = buckets.getOrPut(week) { Bucket() }
                val nwd = nonWorkDayByDate[d]
                // total counts working-eligible days (workDay AND not a sick/leave/holiday)
                if (nwd == null && d !in excluded) b.total++
                when {
                    nwd?.type == NonWorkDayType.BANK_HOLIDAY -> b.bankHoliday++
                    nwd?.type == NonWorkDayType.ANNUAL_LEAVE -> b.leave++
                    nwd?.type == NonWorkDayType.SICK -> b.sick++
                    nwd?.type == NonWorkDayType.OTHER -> { /* skip */ }
                    else -> when (recordsByDate[d]?.status) {
                        AttendanceRecordStatus.OFFICE -> b.office++
                        AttendanceRecordStatus.REMOTE -> b.remote++
                        AttendanceRecordStatus.SICK -> b.sick++
                        AttendanceRecordStatus.LEAVE, AttendanceRecordStatus.ANNUAL_LEAVE -> b.leave++
                        AttendanceRecordStatus.BANK_HOLIDAY -> b.bankHoliday++
                        else -> {}
                    }
                }
            }
            d = d.plusDays(1)
        }
        return buckets.map { (week, b) ->
            WeeklyStatsData(
                weekNumber = week,
                officeDays = b.office,
                remoteDays = b.remote,
                sickDays = b.sick,
                leaveDays = b.leave,
                bankHolidayDays = b.bankHoliday,
                totalWorkingDays = b.total,
            )
        }
    }
}
