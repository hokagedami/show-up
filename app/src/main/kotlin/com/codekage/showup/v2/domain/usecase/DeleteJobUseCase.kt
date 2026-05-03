package com.codekage.showup.v2.domain.usecase

import com.codekage.showup.v2.domain.model.Job
import com.codekage.showup.v2.domain.repository.AttendanceRepository
import com.codekage.showup.v2.domain.repository.JobRepository
import com.codekage.showup.v2.domain.repository.NonWorkDayRepository
import com.codekage.showup.v2.service.GeofenceManager

class DeleteJobUseCase(
    private val jobRepository: JobRepository,
    private val attendanceRepository: AttendanceRepository,
    private val nonWorkDayRepository: NonWorkDayRepository,
    private val geofenceManager: GeofenceManager,
) {
    suspend operator fun invoke(job: Job) {
        attendanceRepository.deleteAllForJob(job.id)
        nonWorkDayRepository.deleteAllForJob(job.id)
        geofenceManager.removeGeofence(job.id.toString())
        jobRepository.deleteJob(job)
    }
}
