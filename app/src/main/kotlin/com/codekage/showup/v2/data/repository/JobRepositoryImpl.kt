package com.codekage.showup.v2.data.repository

import com.codekage.showup.v2.data.local.dao.JobDao
import com.codekage.showup.v2.data.local.entity.toDomain
import com.codekage.showup.v2.data.local.entity.toEntity
import com.codekage.showup.v2.domain.model.Job
import com.codekage.showup.v2.domain.repository.JobRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class JobRepositoryImpl(private val jobDao: JobDao) : JobRepository {
    override fun getAllJobs(): Flow<List<Job>> =
        jobDao.getAllJobs().map { list -> list.map { it.toDomain() } }

    override fun getActiveJobs(): Flow<List<Job>> =
        jobDao.getActiveJobs().map { list -> list.map { it.toDomain() } }

    override fun getJobByIdFlow(jobId: UUID): Flow<Job?> =
        jobDao.getJobByIdFlow(jobId).map { it?.toDomain() }

    override suspend fun getJobById(jobId: UUID): Job? =
        jobDao.getJobById(jobId)?.toDomain()

    override suspend fun insertJob(job: Job) = jobDao.insertJob(job.toEntity())
    override suspend fun updateJob(job: Job) = jobDao.updateJob(job.toEntity())
    override suspend fun deleteJob(job: Job) = jobDao.deleteJob(job.toEntity())
}
