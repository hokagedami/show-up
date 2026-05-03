package com.codekage.showup.v2.presentation.dashboard

import com.codekage.showup.v2.domain.usecase.OfficeDayPlan
import com.codekage.showup.v2.service.GeofenceState

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
