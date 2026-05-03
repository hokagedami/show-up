package com.codekage.showup.v2.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val officeAddress: String,
    val officeLat: Double,
    val officeLng: Double,
    val geofenceRadiusMeters: Int,
    val monthlyGoalPercent: Int,
    val workDays: List<DayOfWeek>,
    val workStartTime: LocalTime,
    val workEndTime: LocalTime,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
)
