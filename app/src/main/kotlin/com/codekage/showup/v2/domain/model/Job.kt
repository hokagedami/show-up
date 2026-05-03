package com.codekage.showup.v2.domain.model

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class Job(
    val id: UUID,
    val name: String,
    val officeAddress: String,
    val officeLat: Double,
    val officeLng: Double,
    val geofenceRadiusMeters: Int = 100,
    val monthlyGoalPercent: Int,
    val workDays: List<DayOfWeek>,
    val workStartTime: LocalTime,
    val workEndTime: LocalTime,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
)
