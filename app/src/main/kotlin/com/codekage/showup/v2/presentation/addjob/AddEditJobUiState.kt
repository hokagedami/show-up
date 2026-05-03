package com.codekage.showup.v2.presentation.addjob

import java.time.DayOfWeek
import java.time.LocalTime

data class AddEditJobUiState(
    val name: String = "",
    val officeAddress: String = "",
    val officeLat: Double = 0.0,
    val officeLng: Double = 0.0,
    val officeGoogleMapsLink: String? = null,
    val geofenceRadiusMeters: Int = 100,
    val monthlyGoalPercent: Int = 60,
    val workDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    ),
    val workStartTime: LocalTime = LocalTime.of(9, 0),
    val workEndTime: LocalTime = LocalTime.of(17, 0),
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
)
