package com.codekage.showup.v2.presentation.jobdetail

import com.codekage.showup.v2.domain.model.Job

data class JobDetailUiState(
    val job: Job? = null,
    val officeDays: Int = 0,
    val remoteDays: Int = 0,
    val sickDays: Int = 0,
    val leaveDays: Int = 0,
    val totalWorkingDays: Int = 0,
    val remainingDays: Int = 0,
    val currentPercentage: Float = 0f,
    val deleted: Boolean = false,
)
