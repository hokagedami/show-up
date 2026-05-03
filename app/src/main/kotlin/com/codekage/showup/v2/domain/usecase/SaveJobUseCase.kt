package com.codekage.showup.v2.domain.usecase

import com.codekage.showup.v2.domain.model.Job
import com.codekage.showup.v2.domain.repository.JobRepository
import com.codekage.showup.v2.service.GeofenceManager

class SaveJobUseCase(
    private val jobRepository: JobRepository,
    private val geofenceManager: GeofenceManager,
) {
    suspend operator fun invoke(job: Job, isEditing: Boolean) {
        if (isEditing) jobRepository.updateJob(job) else jobRepository.insertJob(job)
        if (job.isActive) geofenceManager.registerGeofence(job)
    }
}
