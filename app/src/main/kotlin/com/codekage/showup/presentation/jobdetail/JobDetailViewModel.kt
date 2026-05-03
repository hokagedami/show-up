package com.codekage.showup.presentation.jobdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codekage.showup.AppContainer
import com.codekage.showup.domain.model.AttendanceRecordStatus
import com.codekage.showup.domain.repository.AttendanceRepository
import com.codekage.showup.domain.repository.JobRepository
import com.codekage.showup.domain.repository.NonWorkDayRepository
import com.codekage.showup.domain.usecase.DeleteJobUseCase
import com.codekage.showup.domain.usecase.countWorkingDays
import com.codekage.showup.domain.usecase.nonWorkingDates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class JobDetailViewModel(
    private val jobRepository: JobRepository,
    private val attendanceRepository: AttendanceRepository,
    private val nonWorkDayRepository: NonWorkDayRepository,
    private val deleteJobUseCase: DeleteJobUseCase,
    jobId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobDetailUiState())
    val uiState = _uiState.asStateFlow()
    private val jobId: UUID = UUID.fromString(jobId)

    init { loadJobDetail() }

    private fun loadJobDetail() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val ym = YearMonth.from(today)
            val monthStart = ym.atDay(1)
            val monthEnd = ym.atEndOfMonth()

            jobRepository.getJobByIdFlow(jobId).collect { job ->
                if (job == null) {
                    _uiState.update { it.copy(job = null) }
                    return@collect
                }
                val records = attendanceRepository
                    .getAttendanceRecordsForJobInRange(jobId, monthStart, monthEnd).first()
                val nonWorkDays = nonWorkDayRepository
                    .getNonWorkDaysInRange(jobId, monthStart, monthEnd).first()
                    .map { it.date }.toSet()
                val workDays = job.workDays.toSet()
                val excluded = records.nonWorkingDates()
                val total = countWorkingDays(monthStart, monthEnd, workDays, nonWorkDays, excluded)
                val soFar = countWorkingDays(monthStart, today, workDays, nonWorkDays, excluded)
                val remaining = (total - soFar).coerceAtLeast(0)
                val officeDays = records.count {
                    it.status == AttendanceRecordStatus.OFFICE && !it.date.isAfter(today)
                }
                val remoteDays = records.count { it.status == AttendanceRecordStatus.REMOTE }
                val sickDays = records.count { it.status == AttendanceRecordStatus.SICK }
                val leaveDays = records.count { it.status == AttendanceRecordStatus.LEAVE || it.status == AttendanceRecordStatus.ANNUAL_LEAVE }
                val pct = if (soFar > 0) officeDays.toFloat() / soFar * 100f else 0f

                _uiState.update {
                    JobDetailUiState(
                        job = job,
                        officeDays = officeDays,
                        remoteDays = remoteDays,
                        sickDays = sickDays,
                        leaveDays = leaveDays,
                        totalWorkingDays = total,
                        remainingDays = remaining,
                        currentPercentage = pct,
                    )
                }
            }
        }
    }

    fun toggleActive() {
        viewModelScope.launch {
            val job = _uiState.value.job ?: return@launch
            val updated = job.copy(isActive = !job.isActive)
            jobRepository.updateJob(updated)
            _uiState.update { it.copy(job = updated) }
        }
    }

    fun deleteJob() {
        viewModelScope.launch {
            val job = _uiState.value.job ?: return@launch
            deleteJobUseCase(job)
            _uiState.update { it.copy(deleted = true) }
        }
    }

    companion object {
        fun factory(appContainer: AppContainer, jobId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                JobDetailViewModel(
                    appContainer.jobRepository,
                    appContainer.attendanceRepository,
                    appContainer.nonWorkDayRepository,
                    appContainer.deleteJobUseCase,
                    jobId,
                )
            }
        }
    }
}
