package com.codekage.showup.presentation.dashboard

import com.codekage.showup.domain.usecase.OfficeDayPlan
import com.codekage.showup.service.GeofenceState

data class DashboardUiState(
    val jobItems: List<JobDashboardItem> = emptyList(),
    val isLoading: Boolean = true,
    val hasJobs: Boolean = true,
    val currentPlan: OfficeDayPlan? = null,
    /** Per-job geofence state keyed by job UUID string. */
    val geofenceStates: Map<String, GeofenceState> = emptyMap(),
    /** Transient toast text — cleared by the screen after it's been shown. */
    val transientMessage: String? = null,
)
