package com.codekage.showup.domain.usecase

import com.codekage.showup.domain.model.Job
import com.codekage.showup.domain.repository.AttendanceRepository
import com.codekage.showup.domain.repository.NonWorkDayRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

class GeneratePlanUseCaseTest {

    private val attendanceRepo: AttendanceRepository = mockk()
    private val nonWorkRepo: NonWorkDayRepository = mockk()
    private val useCase = GeneratePlanUseCase(attendanceRepo, nonWorkRepo)

    private val jobId = UUID.randomUUID()
    private val job = Job(
        id = jobId,
        name = "DfE",
        officeAddress = "",
        officeLat = 0.0,
        officeLng = 0.0,
        geofenceRadiusMeters = 100,
        monthlyGoalPercent = 60,
        workDays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        workStartTime = LocalTime.of(9, 0),
        workEndTime = LocalTime.of(17, 0),
        isActive = true,
        createdAt = LocalDateTime.now(),
    )

    @Test
    fun `plan suggests enough days to hit the goal`() = runTest {
        val month = YearMonth.now()
        every { attendanceRepo.getAttendanceRecordsForJobInRange(any(), any(), any()) } returns flowOf(emptyList())
        every { nonWorkRepo.getNonWorkDaysInRange(any(), any(), any()) } returns flowOf(emptyList())

        val plan = useCase(job, month)

        // total working days >= recommended count
        assertTrue(plan.totalWorkingDays >= plan.recommendedOfficeDates.size)
        // target should be ceil(totalWorkingDays * 60 / 100)
        val expectedTarget = Math.ceil(plan.totalWorkingDays * 60 / 100.0).toInt()
        assertEquals(expectedTarget, plan.targetOfficeDays)
        // recommended dates are all within the month
        plan.recommendedOfficeDates.forEach { date ->
            assertEquals(month, YearMonth.from(date))
        }
        // recommended dates are work days (not weekends)
        plan.recommendedOfficeDates.forEach { date ->
            assertTrue(date.dayOfWeek in job.workDays)
        }
    }

    @Test
    fun `plan returns empty when goal already reached`() = runTest {
        val highGoalJob = job.copy(monthlyGoalPercent = 0) // 0% goal — always achieved
        every { attendanceRepo.getAttendanceRecordsForJobInRange(any(), any(), any()) } returns flowOf(emptyList())
        every { nonWorkRepo.getNonWorkDaysInRange(any(), any(), any()) } returns flowOf(emptyList())

        val plan = useCase(highGoalJob)
        assertTrue(plan.recommendedOfficeDates.isEmpty())
    }
}
