package com.codekage.showup.v2.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.codekage.showup.v2.data.local.entity.JobEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs")
    fun getAllJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE isActive = 1")
    fun getActiveJobs(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE id = :jobId")
    suspend fun getJobById(jobId: UUID): JobEntity?

    @Query("SELECT * FROM jobs WHERE id = :jobId")
    fun getJobByIdFlow(jobId: UUID): Flow<JobEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobEntity)

    @Update
    suspend fun updateJob(job: JobEntity)

    @Delete
    suspend fun deleteJob(job: JobEntity)
}
