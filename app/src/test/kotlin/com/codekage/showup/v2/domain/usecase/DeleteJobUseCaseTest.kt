package com.codekage.showup.v2.domain.usecase

import com.codekage.showup.v2.domain.model.Job
import com.codekage.showup.v2.domain.repository.AttendanceRepository
import com.codekage.showup.v2.domain.repository.JobRepository
import com.codekage.showup.v2.domain.repository.NonWorkDayRepository
import com.codekage.showup.v2.service.GeofenceManager
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class DeleteJobUseCaseTest {

    private val jobRepo: JobRepository = mockk(relaxed = true)
    private val attendanceRepo: AttendanceRepository = mockk(relaxed = true)
    private val nonWorkRepo: NonWorkDayRepository = mockk(relaxed = true)
    private val geofenceManager: GeofenceManager = mockk(relaxed = true)
    private val useCase = DeleteJobUseCase(jobRepo, attendanceRepo, nonWorkRepo, geofenceManager)

    @Test
    fun `cascade delete order is records, non-work-days, geofence, job`() = runTest {
        val job = Job(
            id = UUID.randomUUID(),
            name = "Test",
            officeAddress = "",
            officeLat = 0.0,
            officeLng = 0.0,
            geofenceRadiusMeters = 100,
            monthlyGoalPercent = 50,
            workDays = listOf(DayOfWeek.MONDAY),
            workStartTime = LocalTime.NOON,
            workEndTime = LocalTime.NOON,
            isActive = true,
            createdAt = LocalDateTime.now(),
        )

        useCase(job)

        coVerifyOrder {
            attendanceRepo.deleteAllForJob(job.id)
            nonWorkRepo.deleteAllForJob(job.id)
            geofenceManager.removeGeofence(job.id.toString())
            jobRepo.deleteJob(job)
        }
    }
}
