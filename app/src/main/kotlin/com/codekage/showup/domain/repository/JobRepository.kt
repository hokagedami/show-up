package com.codekage.showup.domain.repository

import com.codekage.showup.domain.model.Job
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface JobRepository {
    fun getAllJobs(): Flow<List<Job>>
    fun getActiveJobs(): Flow<List<Job>>
    fun getJobByIdFlow(jobId: UUID): Flow<Job?>
    suspend fun getJobById(jobId: UUID): Job?
    suspend fun insertJob(job: Job)
    suspend fun updateJob(job: Job)
    suspend fun deleteJob(job: Job)
}
