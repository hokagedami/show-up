package com.codekage.showup.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.codekage.showup.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

@Dao
interface AttendanceRecordDao {
    @Query("SELECT * FROM attendance_records WHERE jobId = :jobId ORDER BY date DESC")
    fun getAttendanceRecordsForJob(jobId: UUID): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE jobId = :jobId AND date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getAttendanceRecordsForJobInRange(
        jobId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE jobId = :jobId AND date = :date")
    suspend fun getAttendanceRecordForDay(jobId: UUID, date: LocalDate): AttendanceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecordEntity)

    @Update
    suspend fun updateAttendanceRecord(record: AttendanceRecordEntity)

    @Delete
    suspend fun deleteAttendanceRecord(record: AttendanceRecordEntity)

    @Query("DELETE FROM attendance_records WHERE jobId = :jobId")
    suspend fun deleteAllForJob(jobId: UUID)
}
