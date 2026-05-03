package com.codekage.showup.v2.domain.usecase

import com.codekage.showup.v2.domain.model.AttendanceRecord
import com.codekage.showup.v2.domain.model.AttendanceRecordStatus
import java.time.DayOfWeek
import java.time.LocalDate

internal val NON_WORKING_STATUSES = setOf(
    AttendanceRecordStatus.SICK,
    AttendanceRecordStatus.LEAVE,
    AttendanceRecordStatus.ANNUAL_LEAVE,
    AttendanceRecordStatus.BANK_HOLIDAY,
)

internal fun List<AttendanceRecord>.nonWorkingDates(): Set<LocalDate> =
    filter { it.status in NON_WORKING_STATUSES }.map { it.date }.toSet()

// Count actual working days in [from..to]: dayOfWeek must be in workDays, date must not be a
// scheduled non-work date (e.g. bank holiday), and any attendance record on that date must
// not be a non-working status (sick/leave/annual leave/bank holiday).
internal fun countWorkingDays(
    from: LocalDate,
    to: LocalDate,
    workDays: Set<DayOfWeek>,
    nonWorkDates: Set<LocalDate>,
    excludedRecordDates: Set<LocalDate> = emptySet(),
): Int {
    if (to.isBefore(from)) return 0
    var d = from
    var count = 0
    while (!d.isAfter(to)) {
        if (d.dayOfWeek in workDays && d !in nonWorkDates && d !in excludedRecordDates) count++
        d = d.plusDays(1)
    }
    return count
}
