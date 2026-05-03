package com.codekage.showup.domain.usecase

import com.codekage.showup.domain.model.Job
import com.codekage.showup.domain.repository.JobRepository
import com.codekage.showup.service.GeofenceManager
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class SaveJobUseCaseTest {

    private val jobRepo: JobRepository = mockk(relaxed = true)
    private val geofenceManager: GeofenceManager = mockk(relaxed = true)
    private val useCase = SaveJobUseCase(jobRepo, geofenceManager)

    private val job = Job(
        id = UUID.randomUUID(),
        name = "DfE",
        officeAddress = "Bishopsgate House",
        officeLat = 54.5260,
        officeLng = -1.5510,
        geofenceRadiusMeters = 100,
        monthlyGoalPercent = 45,
        workDays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        workStartTime = LocalTime.of(8, 0),
        workEndTime = LocalTime.of(16, 0),
        isActive = true,
        createdAt = LocalDateTime.now(),
    )

    @Test
    fun `insert path called when not editing`() = runTest {
        useCase(job, isEditing = false)
        coVerify(exactly = 1) { jobRepo.insertJob(job) }
        coVerify(exactly = 0) { jobRepo.updateJob(any()) }
    }

    @Test
    fun `update path called when editing`() = runTest {
        useCase(job, isEditing = true)
        coVerify(exactly = 1) { jobRepo.updateJob(job) }
        coVerify(exactly = 0) { jobRepo.insertJob(any()) }
    }

    @Test
    fun `geofence registered for active job`() = runTest {
        useCase(job, isEditing = false)
        verify(exactly = 1) { geofenceManager.registerGeofence(job) }
    }

    @Test
    fun `geofence not registered for inactive job`() = runTest {
        useCase(job.copy(isActive = false), isEditing = false)
        verify(exactly = 0) { geofenceManager.registerGeofence(any()) }
    }
}
