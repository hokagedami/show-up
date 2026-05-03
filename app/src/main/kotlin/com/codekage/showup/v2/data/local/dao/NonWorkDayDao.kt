package com.codekage.showup.v2.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.codekage.showup.v2.data.local.entity.NonWorkDayEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

@Dao
interface NonWorkDayDao {
    @Query("SELECT * FROM non_work_days WHERE jobId = :jobId OR jobId IS NULL ORDER BY date DESC")
    fun getNonWorkDaysForJob(jobId: UUID?): Flow<List<NonWorkDayEntity>>

    @Query("SELECT * FROM non_work_days WHERE (jobId = :jobId OR jobId IS NULL) AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getNonWorkDaysInRange(
        jobId: UUID?,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<NonWorkDayEntity>>

    @Query("SELECT * FROM non_work_days WHERE date = :date AND (jobId = :jobId OR jobId IS NULL)")
    suspend fun getNonWorkDayForDate(date: LocalDate, jobId: UUID?): NonWorkDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNonWorkDay(nonWorkDay: NonWorkDayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(nonWorkDays: List<NonWorkDayEntity>)

    @Update
    suspend fun updateNonWorkDay(nonWorkDay: NonWorkDayEntity)

    @Delete
    suspend fun deleteNonWorkDay(nonWorkDay: NonWorkDayEntity)

    @Query("DELETE FROM non_work_days WHERE jobId = :jobId")
    suspend fun deleteAllForJob(jobId: UUID)
}
