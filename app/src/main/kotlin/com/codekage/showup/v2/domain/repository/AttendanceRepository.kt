package com.codekage.showup.v2.domain.repository

import com.codekage.showup.v2.domain.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID

interface AttendanceRepository {
    fun getAttendanceRecordsForJob(jobId: UUID): Flow<List<AttendanceRecord>>
    fun getAttendanceRecordsForJobInRange(jobId: UUID, startDate: LocalDate, endDate: LocalDate): Flow<List<AttendanceRecord>>
    suspend fun getAttendanceRecordForDay(jobId: UUID, date: LocalDate): AttendanceRecord?
    suspend fun insertAttendanceRecord(record: AttendanceRecord)
    suspend fun updateAttendanceRecord(record: AttendanceRecord)
    suspend fun deleteAttendanceRecord(record: AttendanceRecord)
    suspend fun deleteAllForJob(jobId: UUID)
}
