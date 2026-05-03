package com.codekage.showup.v2.domain.usecase

import com.codekage.showup.v2.data.repository.SettingsRepository
import com.codekage.showup.v2.domain.model.AttendanceRecord
import com.codekage.showup.v2.domain.model.AttendanceRecordStatus
import com.codekage.showup.v2.domain.model.EntryMethod
import com.codekage.showup.v2.domain.repository.AttendanceRepository
import com.codekage.showup.v2.domain.repository.JobRepository
import com.codekage.showup.v2.domain.repository.NonWorkDayRepository
import com.codekage.showup.v2.service.NotificationScheduler
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.math.ceil

class MarkAttendanceUseCase(
    private val attendanceRepository: AttendanceRepository,
    private val jobRepository: JobRepository,
    private val nonWorkDayRepository: NonWorkDayRepository,
    private val notificationScheduler: NotificationScheduler,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(
        jobId: UUID,
        date: LocalDate,
        status: AttendanceRecordStatus,
        entryMethod: EntryMethod = EntryMethod.MANUAL,
    ) {
        val existing = attendanceRepository.getAttendanceRecordForDay(jobId, date)
        val now = LocalDateTime.now()

        if (existing == null) {
            attendanceRepository.insertAttendanceRecord(
                AttendanceRecord(
                    id = UUID.randomUUID(),
                    jobId = jobId,
                    date = date,
                    status = status,
                    entryMethod = entryMethod,
                    gpsConfirmed = entryMethod == EntryMethod.AUTO_GPS,
                    notes = null,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        } else {
            attendanceRepository.updateAttendanceRecord(
                existing.copy(
                    status = status,
                    entryMethod = entryMethod,
                    gpsConfirmed = entryMethod == EntryMethod.AUTO_GPS,
                    updatedAt = now,
                )
            )
        }

        runCatching { checkGoalPace(jobId) }
    }

    private suspend fun checkGoalPace(jobId: UUID) {
        val settings = settingsRepository.settings.first()
        if (!settings.goalAlertEnabled) return

        val job = jobRepository.getJobById(jobId) ?: return
        val today = LocalDate.now()
        val ym = YearMonth.from(today)
        val monthStart = ym.atDay(1)
        val monthEnd = ym.atEndOfMonth()

        val nonWorkDays = nonWorkDayRepository
            .getNonWorkDaysInRange(jobId, monthStart, monthEnd)
            .first()
            .map { it.date }
            .toSet()
        val records = attendanceRepository.getAttendanceRecordsForJobInRange(jobId, monthStart, monthEnd).first()
        val workDays = job.workDays.toSet()
        val excluded = records.nonWorkingDates()

        val totalWorkingDays = countWorkingDays(monthStart, monthEnd, workDays, nonWorkDays, excluded)
        val workingDaysSoFar = countWorkingDays(monthStart, today, workDays, nonWorkDays, excluded)
        if (totalWorkingDays == 0) return

        val officeDays = records.count { it.status == AttendanceRecordStatus.OFFICE }
        val goalDaysAbsolute = ceil(totalWorkingDays * job.monthlyGoalPercent / 100.0).toInt()
        val daysNeeded = (goalDaysAbsolute - officeDays).coerceAtLeast(0)
        val remaining = (totalWorkingDays - workingDaysSoFar).coerceAtLeast(0)

        if (daysNeeded > remaining) {
            notificationScheduler.scheduleGoalAlert(jobId.toString(), job.name, daysNeeded)
        }
    }
}
