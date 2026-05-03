package com.codekage.showup.v2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.codekage.showup.v2.domain.model.AttendanceRecordStatus
import com.codekage.showup.v2.domain.model.EntryMethod
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
    @PrimaryKey val id: UUID,
    val jobId: UUID,
    val date: LocalDate,
    val status: AttendanceRecordStatus,
    val entryMethod: EntryMethod,
    val gpsConfirmed: Boolean,
    val notes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
