package com.codekage.showup.v2.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codekage.showup.v2.AppContainer
import com.codekage.showup.v2.data.repository.PlannedDayRepository
import com.codekage.showup.v2.data.repository.SettingsRepository
import com.codekage.showup.v2.domain.model.AttendanceRecordStatus
import com.codekage.showup.v2.domain.repository.AttendanceRepository
import com.codekage.showup.v2.domain.usecase.GeneratePlanUseCase
import com.codekage.showup.v2.domain.usecase.GetDashboardDataUseCase
import com.codekage.showup.v2.domain.usecase.JobDashboardData
import com.codekage.showup.v2.domain.usecase.MarkAttendanceUseCase
import com.codekage.showup.v2.service.GeofenceManager
import com.codekage.showup.v2.service.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class DashboardViewModel(
    private val getDashboardDataUseCase: GetDashboardDataUseCase,
    private val markAttendanceUseCase: MarkAttendanceUseCase,
    private val generatePlanUseCase: GeneratePlanUseCase,
    private val attendanceRepository: AttendanceRepository,
    private val geofenceManager: GeofenceManager,
    private val notificationScheduler: NotificationScheduler,
    private val settingsRepository: SettingsRepository,
    private val plannedDayRepository: PlannedDayRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDashboard()
        observeGeofenceStates()
    }

    private fun observeGeofenceStates() {
        viewModelScope.launch {
            geofenceManager.states.collect { states ->
                _uiState.update { it.copy(geofenceStates = states) }
            }
        }
    }

    /** Re-runs the geofence preflight + registration for all active jobs. Called by the
     *  Dashboard composable after the user grants a permission or returns from system Settings. */
    fun recheckGeofences() {
        viewModelScope.launch {
            _uiState.value.jobItems.forEach { item ->
                if (item.job.isActive) geofenceManager.registerGeofence(item.job)
            }
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            try {
                combine(
                    getDashboardDataUseCase(),
                    plannedDayRepository.plannedDates,
                ) { data, planned -> data to planned }.collect { (data, planned) ->
                    _uiState.update {
                        it.copy(
                            jobItems = data.jobItems.map { item ->
                                item.toUiItem(planned[item.job.id].orEmpty())
                            },
                            isLoading = data.isLoading,
                            hasJobs = data.hasJobs,
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.update { DashboardUiState() }
            }
        }
    }

    private fun JobDashboardData.toUiItem(plannedOfficeDates: List<LocalDate>) = JobDashboardItem(
        job = job,
        officeDays = officeDays,
        totalWorkingDays = totalWorkingDays,
        workingDaysSoFar = workingDaysSoFar,
        remainingWorkingDays = remainingWorkingDays,
        currentPercentage = currentPercentage,
        projectedPercentage = projectedPercentage,
        daysNeededForGoal = daysNeededForGoal,
        goalPaceStatus = goalPaceStatus,
        todayStatus = todayStatus,
        plannedOfficeDates = plannedOfficeDates,
    )

    fun quickMarkToday(jobId: UUID, status: AttendanceRecordStatus) {
        viewModelScope.launch {
            markAttendanceUseCase(jobId, LocalDate.now(), status)
        }
    }

    fun clearTodayMark(jobId: UUID) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val existing = attendanceRepository.getAttendanceRecordForDay(jobId, today) ?: return@launch
            attendanceRepository.deleteAttendanceRecord(existing)
        }
    }

    fun generatePlan(jobId: UUID) {
        viewModelScope.launch {
            val item = _uiState.value.jobItems.firstOrNull { it.job.id == jobId } ?: return@launch
            val plan = generatePlanUseCase(item.job)
            _uiState.update { it.copy(currentPlan = plan) }
        }
    }

    fun applyCurrentPlan() {
        val plan = _uiState.value.currentPlan ?: return
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val time = LocalTime.of(settings.reminderTimeHour, settings.reminderTimeMinute)
            val now = System.currentTimeMillis()
            val scheduledDates = plan.recommendedOfficeDates.filter { date ->
                val triggerMillis = date.atTime(time)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                triggerMillis > now
            }
            scheduledDates.forEach { date ->
                notificationScheduler.schedulePlannedOfficeReminder(
                    jobId = plan.job.id.toString(),
                    jobName = plan.job.name,
                    date = date,
                    time = time,
                )
            }
            plannedDayRepository.setDatesFor(plan.job.id, scheduledDates)
            val timeLabel = "%02d:%02d".format(time.hour, time.minute)
            val message = when (scheduledDates.size) {
                0 -> "No upcoming days to remind about."
                1 -> "1 office-day reminder scheduled at $timeLabel."
                else -> "${scheduledDates.size} office-day reminders scheduled at $timeLabel."
            }
            _uiState.update { it.copy(currentPlan = null, transientMessage = message) }
        }
    }

    fun clearPlanFor(jobId: UUID) {
        viewModelScope.launch {
            val existing = _uiState.value.jobItems.firstOrNull { it.job.id == jobId }?.plannedOfficeDates
                ?: return@launch
            existing.forEach { date ->
                notificationScheduler.cancelPlannedOfficeReminder(jobId.toString(), date)
            }
            plannedDayRepository.clearFor(jobId)
            _uiState.update { it.copy(transientMessage = "Planned reminders cleared.") }
        }
    }

    fun dismissPlan() = _uiState.update { it.copy(currentPlan = null) }

    fun clearTransientMessage() = _uiState.update { it.copy(transientMessage = null) }

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    appContainer.getDashboardDataUseCase,
                    appContainer.markAttendanceUseCase,
                    appContainer.generatePlanUseCase,
                    appContainer.attendanceRepository,
                    appContainer.geofenceManager,
                    appContainer.notificationScheduler,
                    appContainer.settingsRepository,
                    appContainer.plannedDayRepository,
                )
            }
        }
    }
}
