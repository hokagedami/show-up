package com.codekage.showup.presentation.calendar

import com.codekage.showup.domain.model.AttendanceRecord
import com.codekage.showup.domain.model.Job
import com.codekage.showup.domain.model.NonWorkDay
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val jobs: List<Job> = emptyList(),
    val selectedJob: Job? = null,
    val currentMonth: YearMonth = YearMonth.now(),
    val attendanceRecords: Map<LocalDate, AttendanceRecord> = emptyMap(),
    val nonWorkDays: Map<LocalDate, NonWorkDay> = emptyMap(),
    val selectedDates: Set<LocalDate> = emptySet(),
    val isBulkSelectionMode: Boolean = false,
    val selectedDate: LocalDate? = null,
    val showStatusPicker: Boolean = false,
    val showBulkStatusPicker: Boolean = false,
    val isLoading: Boolean = true,
)
