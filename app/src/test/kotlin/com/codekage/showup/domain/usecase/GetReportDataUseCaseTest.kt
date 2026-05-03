package com.codekage.showup.domain.usecase

import com.codekage.showup.domain.model.AttendanceRecord
import com.codekage.showup.domain.model.AttendanceRecordStatus
import com.codekage.showup.domain.model.EntryMethod
import com.codekage.showup.domain.model.Job
import com.codekage.showup.domain.model.NonWorkDay
import com.codekage.showup.domain.model.NonWorkDayType
import com.codekage.showup.domain.repository.AttendanceRepository
import com.codekage.showup.domain.repository.NonWorkDayRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class GetReportDataUseCaseTest {

    private val attendanceRepo: AttendanceRepository = mockk()
    private val nonWorkRepo: NonWorkDayRepository = mockk()
    private val useCase = GetReportDataUseCase(attendanceRepo, nonWorkRepo)

    private val jobId = UUID.randomUUID()
    private val job = Job(
        id = jobId,
        name = "DfE",
        officeAddress = "",
        officeLat = 0.0,
        officeLng = 0.0,
        geofenceRadiusMeters = 100,
        monthlyGoalPercent = 45,
        workDays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        workStartTime = LocalTime.of(8, 0),
        workEndTime = LocalTime.of(16, 0),
        isActive = true,
        createdAt = LocalDateTime.now(),
    )

    private fun rec(date: LocalDate, status: AttendanceRecordStatus): AttendanceRecord = AttendanceRecord(
        id = UUID.randomUUID(),
        jobId = jobId,
        date = date,
        status = status,
        entryMethod = EntryMethod.MANUAL,
        gpsConfirmed = false,
        notes = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

    @Test
    fun `aggregates a full week with mixed statuses`() = runTest {
        val monday = LocalDate.of(2026, 4, 27) // Monday
        val sunday = monday.plusDays(6)

        val records = listOf(
            rec(monday, AttendanceRecordStatus.OFFICE),
            rec(monday.plusDays(1), AttendanceRecordStatus.OFFICE),
            rec(monday.plusDays(2), AttendanceRecordStatus.REMOTE),
            rec(monday.plusDays(3), AttendanceRecordStatus.REMOTE),
            rec(monday.plusDays(4), AttendanceRecordStatus.REMOTE),
        )
        every { attendanceRepo.getAttendanceRecordsForJobInRange(jobId, monday, sunday) } returns flowOf(records)
        every { nonWorkRepo.getNonWorkDaysInRange(jobId, monday, sunday) } returns flowOf(emptyList())

        val report = useCase(job, monday, sunday, includeWeeklyBreakdown = false)

        assertEquals(5, report.totalWorkingDays)
        assertEquals(2, report.officeDays)
        assertEquals(3, report.remoteDays)
        assertEquals(0, report.sickDays)
        assertEquals(0, report.leaveDays)
        assertEquals(0, report.absentDays)
        assertEquals(45, report.goalPercentage)
        assertEquals(40f, report.officePercentage, 0.01f)
    }

    @Test
    fun `bank holiday is excluded from working days`() = runTest {
        val monday = LocalDate.of(2026, 5, 4) // Early May Bank Holiday
        val friday = monday.plusDays(4)

        every { attendanceRepo.getAttendanceRecordsForJobInRange(jobId, monday, friday) } returns flowOf(emptyList())
        every { nonWorkRepo.getNonWorkDaysInRange(jobId, monday, friday) } returns flowOf(
            listOf(
                NonWorkDay(
                    id = UUID.randomUUID(),
                    jobId = null,
                    date = monday,
                    type = NonWorkDayType.BANK_HOLIDAY,
                    label = "Early May Bank Holiday",
                )
            )
        )

        val report = useCase(job, monday, friday, includeWeeklyBreakdown = false)
        assertEquals(4, report.totalWorkingDays)
    }

    @Test
    fun `annual leave counts as leave days`() = runTest {
        val monday = LocalDate.of(2026, 5, 11)
        val friday = monday.plusDays(4)

        val records = listOf(
            rec(monday, AttendanceRecordStatus.ANNUAL_LEAVE),
            rec(monday.plusDays(1), AttendanceRecordStatus.LEAVE),
            rec(monday.plusDays(2), AttendanceRecordStatus.OFFICE),
        )
        every { attendanceRepo.getAttendanceRecordsForJobInRange(jobId, monday, friday) } returns flowOf(records)
        every { nonWorkRepo.getNonWorkDaysInRange(jobId, monday, friday) } returns flowOf(emptyList())

        val report = useCase(job, monday, friday, includeWeeklyBreakdown = false)
        assertEquals(2, report.leaveDays)
        assertEquals(1, report.officeDays)
    }
}
