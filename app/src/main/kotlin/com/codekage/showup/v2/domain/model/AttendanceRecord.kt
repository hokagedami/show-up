package com.codekage.showup.v2.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class AttendanceRecord(
    val id: UUID,
    val jobId: UUID,
    val date: LocalDate,
    val status: AttendanceRecordStatus,
    val entryMethod: EntryMethod,
    val gpsConfirmed: Boolean,
    val notes: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
