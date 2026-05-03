package com.codekage.showup.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.codekage.showup.AppContainer
import com.codekage.showup.domain.model.AttendanceRecordStatus
import com.codekage.showup.domain.model.Job
import com.codekage.showup.domain.repository.AttendanceRepository
import com.codekage.showup.domain.repository.JobRepository
import com.codekage.showup.domain.repository.NonWorkDayRepository
import com.codekage.showup.domain.usecase.MarkAttendanceUseCase
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class CalendarViewModel(
    private val jobRepository: JobRepository,
    private val attendanceRepository: AttendanceRepository,
    private val nonWorkDayRepository: NonWorkDayRepository,
    private val markAttendanceUseCase: MarkAttendanceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    /** Live observer for the currently-displayed (job, month) pair. Cancelled and replaced
     *  whenever the selection changes so we react to new bank holidays / records in real time. */
    private var monthDataJob: CoroutineJob? = null

    init { loadJobs() }

    private fun loadJobs() {
        viewModelScope.launch {
            jobRepository.getActiveJobs().collect { jobs ->
                _uiState.update { current ->
                    val selected = current.selectedJob ?: jobs.firstOrNull()
                    current.copy(jobs = jobs, selectedJob = selected)
                }
                _uiState.value.selectedJob?.let { loadAttendanceForMonth(it.id) }
            }
        }
    }

    fun selectJob(job: Job) {
        _uiState.update { it.copy(selectedJob = job) }
        loadAttendanceForMonth(job.id)
    }

    fun changeMonth(yearMonth: YearMonth) {
        _uiState.update { it.copy(currentMonth = yearMonth) }
        _uiState.value.selectedJob?.let { loadAttendanceForMonth(it.id) }
    }

    fun selectDate(date: LocalDate) {
        if (_uiState.value.isBulkSelectionMode) {
            toggleBulkDate(date)
        } else {
            _uiState.update { it.copy(selectedDate = date, showStatusPicker = true) }
        }
    }

    fun startBulkSelection(date: LocalDate) {
        _uiState.update { it.copy(selectedDates = setOf(date), isBulkSelectionMode = true) }
    }

    fun toggleBulkDate(date: LocalDate) {
        _uiState.update { current ->
            val next = current.selectedDates.toMutableSet().apply {
                if (!add(date)) remove(date)
            }
            current.copy(selectedDates = next, isBulkSelectionMode = next.isNotEmpty())
        }
    }

    fun openBulkStatusPicker() {
        if (_uiState.value.selectedDates.isEmpty()) return
        _uiState.update { it.copy(showBulkStatusPicker = true) }
    }

    fun dismissStatusPicker() {
        _uiState.update { it.copy(showStatusPicker = false, showBulkStatusPicker = false) }
    }

    fun clearBulkSelection() {
        _uiState.update { it.copy(selectedDates = emptySet(), isBulkSelectionMode = false) }
    }

    fun setStatus(date: LocalDate, status: AttendanceRecordStatus) {
        val job = _uiState.value.selectedJob ?: return
        viewModelScope.launch {
            markAttendanceUseCase(job.id, date, status)
            dismissStatusPicker()
            loadAttendanceForMonth(job.id)
        }
    }

    fun clearStatus(date: LocalDate) {
        val job = _uiState.value.selectedJob ?: return
        viewModelScope.launch {
            val existing = attendanceRepository.getAttendanceRecordForDay(job.id, date)
            if (existing != null) attendanceRepository.deleteAttendanceRecord(existing)
            dismissStatusPicker()
            loadAttendanceForMonth(job.id)
        }
    }

    fun setStatusForSelectedDates(status: AttendanceRecordStatus) {
        val job = _uiState.value.selectedJob ?: return
        val dates = _uiState.value.selectedDates.toList()
        if (dates.isEmpty()) return
        viewModelScope.launch {
            dates.forEach { markAttendanceUseCase(job.id, it, status) }
            clearBulkSelection()
            loadAttendanceForMonth(job.id)
        }
    }

    fun markDateRange(startDate: LocalDate, endDate: LocalDate, status: AttendanceRecordStatus) {
        val job = _uiState.value.selectedJob ?: return
        viewModelScope.launch {
            var d = startDate
            while (!d.isAfter(endDate)) {
                markAttendanceUseCase(job.id, d, status)
                d = d.plusDays(1)
            }
            loadAttendanceForMonth(job.id)
        }
    }

    private fun loadAttendanceForMonth(jobId: UUID) {
        monthDataJob?.cancel()
        val month = _uiState.value.currentMonth
        val start = month.atDay(1)
        val end = month.atEndOfMonth()
        monthDataJob = viewModelScope.launch {
            combine(
                attendanceRepository.getAttendanceRecordsForJobInRange(jobId, start, end),
                nonWorkDayRepository.getNonWorkDaysInRange(jobId, start, end),
            ) { records, nonWorkDays ->
                records.associateBy { it.date } to nonWorkDays.associateBy { it.date }
            }.collect { (records, nonWorkDays) ->
                _uiState.update {
                    it.copy(attendanceRecords = records, nonWorkDays = nonWorkDays, isLoading = false)
                }
            }
        }
    }

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CalendarViewModel(
                    appContainer.jobRepository,
                    appContainer.attendanceRepository,
                    appContainer.nonWorkDayRepository,
                    appContainer.markAttendanceUseCase,
                )
            }
        }
    }
}
