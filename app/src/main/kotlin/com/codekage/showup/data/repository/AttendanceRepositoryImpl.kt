package com.codekage.showup.data.repository

import com.codekage.showup.data.local.dao.AttendanceRecordDao
import com.codekage.showup.data.local.entity.toDomain
import com.codekage.showup.data.local.entity.toEntity
import com.codekage.showup.domain.model.AttendanceRecord
import com.codekage.showup.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

class AttendanceRepositoryImpl(
    private val attendanceRecordDao: AttendanceRecordDao,
) : AttendanceRepository {
    override fun getAttendanceRecordsForJob(jobId: UUID): Flow<List<AttendanceRecord>> =
        attendanceRecordDao.getAttendanceRecordsForJob(jobId)
            .map { list -> list.map { it.toDomain() } }

    override fun getAttendanceRecordsForJobInRange(
        jobId: UUID, startDate: LocalDate, endDate: LocalDate,
    ): Flow<List<AttendanceRecord>> =
        attendanceRecordDao.getAttendanceRecordsForJobInRange(jobId, startDate, endDate)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getAttendanceRecordForDay(jobId: UUID, date: LocalDate): AttendanceRecord? =
        attendanceRecordDao.getAttendanceRecordForDay(jobId, date)?.toDomain()

    override suspend fun insertAttendanceRecord(record: AttendanceRecord) =
        attendanceRecordDao.insertAttendanceRecord(record.toEntity())

    override suspend fun updateAttendanceRecord(record: AttendanceRecord) =
        attendanceRecordDao.updateAttendanceRecord(record.toEntity())

    override suspend fun deleteAttendanceRecord(record: AttendanceRecord) =
        attendanceRecordDao.deleteAttendanceRecord(record.toEntity())

    override suspend fun deleteAllForJob(jobId: UUID) =
        attendanceRecordDao.deleteAllForJob(jobId)
}
