package com.codekage.showup.v2.domain.usecase

data class DashboardData(
    val jobItems: List<JobDashboardData> = emptyList(),
    val isLoading: Boolean = false,
    val hasJobs: Boolean = false,
)
