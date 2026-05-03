package com.codekage.showup.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen {
    @Serializable data object Dashboard : Screen()
    @Serializable data object Calendar : Screen()
    @Serializable data object Reports : Screen()
    @Serializable data object Settings : Screen()
    @Serializable data object AddJob : Screen()
    @Serializable data class EditJob(val jobId: String) : Screen()
    @Serializable data class JobDetail(val jobId: String) : Screen()
    @Serializable data object Onboarding : Screen()
}
