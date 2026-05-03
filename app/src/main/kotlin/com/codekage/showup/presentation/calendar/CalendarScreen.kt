package com.codekage.showup.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codekage.showup.AppContainer
import com.codekage.showup.domain.model.AttendanceRecordStatus
import com.codekage.showup.domain.model.NonWorkDayType
import com.codekage.showup.presentation.common.AttendanceColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(appContainer: AppContainer) {
    val viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.factory(appContainer))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var rangeDialog by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Calendar", style = MaterialTheme.typography.headlineMedium)

            MonthNav(state.currentMonth, viewModel::changeMonth)
            OutlinedButton(onClick = { rangeDialog = true }) { Text("Add Date Range") }

            DayHeaderRow()
            CalendarGrid(state, viewModel::selectDate, viewModel::startBulkSelection)
            Spacer(Modifier.padding(vertical = 6.dp))
            CalendarLegend()
        }
    }

    if (state.showStatusPicker && state.selectedDate != null) {
        val date = state.selectedDate!!
        val currentStatus = state.attendanceRecords[date]?.status
        val nonWorkDay = state.nonWorkDays[date]
        StatusPickerSheet(
            date = date,
            currentStatus = currentStatus,
            nonWorkDayLabel = nonWorkDay?.let { "${it.type.name.replace('_', ' ')}: ${it.label}" },
            nonWorkDayType = nonWorkDay?.type,
            onDismiss = viewModel::dismissStatusPicker,
            onSelect = { viewModel.setStatus(date, it) },
            onClear = { viewModel.clearStatus(date) }.takeIf { currentStatus != null },
        )
    }
    if (state.showBulkStatusPicker) {
        StatusPickerSheet(
            date = null,
            title = "Set status for ${state.selectedDates.size} day(s)",
            currentStatus = null,
            nonWorkDayLabel = null,
            nonWorkDayType = null,
            onDismiss = viewModel::dismissStatusPicker,
            onSelect = { viewModel.setStatusForSelectedDates(it) },
            onClear = null,
        )
    }
    if (rangeDialog) {
        AddDateRangeDialog(
            yearMonth = state.currentMonth,
            onDismiss = { rangeDialog = false },
            onApply = { start, end, status ->
                viewModel.markDateRange(start, end, status)
                rangeDialog = false
            },
        )
    }
}

@Composable
private fun MonthNav(month: YearMonth, onChange: (YearMonth) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = { onChange(month.minusMonths(1)) }) { Icon(Icons.Filled.ChevronLeft, "Previous month") }
        Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())), style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = { onChange(month.plusMonths(1)) }) { Icon(Icons.Filled.ChevronRight, "Next month") }
    }
}

@Composable
private fun DayHeaderRow() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).forEach { d ->
            Text(d.getDisplayName(TextStyle.SHORT, Locale.getDefault()), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CalendarGrid(state: CalendarUiState, onDateClick: (LocalDate) -> Unit, onLongPress: (LocalDate) -> Unit) {
    val month = state.currentMonth
    val firstDay = month.atDay(1)
    val leading = ((firstDay.dayOfWeek.value + 6) % 7) // Mon=0
    val daysInMonth = month.lengthOfMonth()
    val cells = leading + daysInMonth
    val rows = (cells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - leading + 1
                    if (dayNumber in 1..daysInMonth) {
                        val date = month.atDay(dayNumber)
                        val record = state.attendanceRecords[date]
                        val nwd = state.nonWorkDays[date]
                        val color = when {
                            record != null -> when (record.status) {
                                AttendanceRecordStatus.OFFICE -> AttendanceColors.office
                                AttendanceRecordStatus.REMOTE -> AttendanceColors.remote
                                AttendanceRecordStatus.SICK -> AttendanceColors.sick
                                AttendanceRecordStatus.LEAVE, AttendanceRecordStatus.ANNUAL_LEAVE -> AttendanceColors.leave
                                AttendanceRecordStatus.BANK_HOLIDAY -> AttendanceColors.bankHoliday
                                AttendanceRecordStatus.ABSENT -> AttendanceColors.absent
                            }
                            nwd != null -> when (nwd.type) {
                                NonWorkDayType.BANK_HOLIDAY -> AttendanceColors.bankHoliday
                                NonWorkDayType.ANNUAL_LEAVE -> AttendanceColors.leave
                                NonWorkDayType.SICK -> AttendanceColors.sick
                                NonWorkDayType.OTHER -> AttendanceColors.absent
                            }
                            else -> Color.Transparent
                        }
                        DayCell(
                            day = dayNumber,
                            statusColor = color,
                            today = date == LocalDate.now(),
                            selected = state.selectedDates.contains(date),
                            onClick = { onDateClick(date) },
                            onLongPress = { onLongPress(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    statusColor: Color,
    today: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasStatus = statusColor != Color.Transparent
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
        hasStatus -> statusColor.copy(alpha = 0.40f)
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(
                if (today) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp),
                ) else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.toString(),
            fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CalendarLegend() {
    val items = listOf(
        "Office" to AttendanceColors.office,
        "Remote" to AttendanceColors.remote,
        "Leave" to AttendanceColors.leave,
        "Sick" to AttendanceColors.sick,
        "Bank holiday" to AttendanceColors.bankHoliday,
        "Absent" to AttendanceColors.absent,
    )
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatusPickerSheet(
    date: LocalDate?,
    title: String? = null,
    currentStatus: AttendanceRecordStatus?,
    nonWorkDayLabel: String?,
    nonWorkDayType: NonWorkDayType? = null,
    onDismiss: () -> Unit,
    onSelect: (AttendanceRecordStatus) -> Unit,
    onClear: (() -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val heading = title ?: date?.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())) ?: "Set status"
            Text(heading, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            CurrentStateBadge(currentStatus, nonWorkDayLabel, nonWorkDayType)

            Text("Set status", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            statusButtons.forEach { (label, status, color) ->
                val isCurrent = currentStatus == status
                Button(
                    onClick = { onSelect(status) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isCurrent) {
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = color,
                        )
                    } else {
                        androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                    },
                ) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label)
                    if (isCurrent) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text("✓", fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (onClear != null) {
                OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear")
                }
            }
        }
    }
}

@Composable
private fun CurrentStateBadge(
    currentStatus: AttendanceRecordStatus?,
    nonWorkDayLabel: String?,
    nonWorkDayType: NonWorkDayType? = null,
) {
    val (label, color) = when {
        currentStatus != null -> currentStatus.displayLabel() to currentStatus.color()
        nonWorkDayLabel != null -> nonWorkDayLabel to nonWorkDayColor(nonWorkDayType)
        else -> "Not set" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f)).padding(12.dp),
    ) {
        Column {
            Text("Current state", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun AttendanceRecordStatus.displayLabel(): String = when (this) {
    AttendanceRecordStatus.OFFICE -> "Office"
    AttendanceRecordStatus.REMOTE -> "Remote / WFH"
    AttendanceRecordStatus.SICK -> "Sick"
    AttendanceRecordStatus.LEAVE -> "Leave"
    AttendanceRecordStatus.ANNUAL_LEAVE -> "Annual Leave"
    AttendanceRecordStatus.BANK_HOLIDAY -> "Bank Holiday"
    AttendanceRecordStatus.ABSENT -> "Absent"
}

private fun nonWorkDayColor(type: NonWorkDayType?): Color = when (type) {
    NonWorkDayType.BANK_HOLIDAY -> AttendanceColors.bankHoliday
    NonWorkDayType.ANNUAL_LEAVE -> AttendanceColors.leave
    NonWorkDayType.SICK -> AttendanceColors.sick
    NonWorkDayType.OTHER -> AttendanceColors.absent
    null -> AttendanceColors.bankHoliday
}

private fun AttendanceRecordStatus.color(): Color = when (this) {
    AttendanceRecordStatus.OFFICE -> AttendanceColors.office
    AttendanceRecordStatus.REMOTE -> AttendanceColors.remote
    AttendanceRecordStatus.SICK -> AttendanceColors.sick
    AttendanceRecordStatus.LEAVE, AttendanceRecordStatus.ANNUAL_LEAVE -> AttendanceColors.leave
    AttendanceRecordStatus.BANK_HOLIDAY -> AttendanceColors.bankHoliday
    AttendanceRecordStatus.ABSENT -> AttendanceColors.absent
}

private val statusButtons = listOf(
    Triple("Office", AttendanceRecordStatus.OFFICE, AttendanceColors.office),
    Triple("Remote / WFH", AttendanceRecordStatus.REMOTE, AttendanceColors.remote),
    Triple("Leave", AttendanceRecordStatus.LEAVE, AttendanceColors.leave),
    Triple("Sick", AttendanceRecordStatus.SICK, AttendanceColors.sick),
    Triple("Bank Holiday", AttendanceRecordStatus.BANK_HOLIDAY, AttendanceColors.bankHoliday),
    Triple("Annual Leave", AttendanceRecordStatus.ANNUAL_LEAVE, AttendanceColors.leave),
)

@Composable
private fun AddDateRangeDialog(
    yearMonth: YearMonth,
    onDismiss: () -> Unit,
    onApply: (LocalDate, LocalDate, AttendanceRecordStatus) -> Unit,
) {
    val maxDay = yearMonth.lengthOfMonth()
    var startDay by remember { mutableStateOf(1) }
    var endDay by remember { mutableStateOf(maxDay) }
    var status by remember { mutableStateOf(AttendanceRecordStatus.LEAVE) }

    val orderedStart = minOf(startDay, endDay)
    val orderedEnd = maxOf(startDay, endDay)
    val rangeDays = orderedEnd - orderedStart + 1
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    val rangeText = "${yearMonth.atDay(orderedStart).format(fmt)}  →  ${yearMonth.atDay(orderedEnd).format(fmt)}"

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add date range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = startDay.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { startDay = it.coerceIn(1, maxDay) } },
                        label = { Text("Start day") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = endDay.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { endDay = it.coerceIn(1, maxDay) } },
                        label = { Text("End day") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "$rangeText  ·  $rangeDays day${if (rangeDays == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Mark as", style = MaterialTheme.typography.titleSmall)
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    statusButtons.forEach { (label, s, c) ->
                        androidx.compose.material3.FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(label) },
                            leadingIcon = {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(c))
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val start = yearMonth.atDay(orderedStart)
                val end = yearMonth.atDay(orderedEnd)
                onApply(start, end, status)
            }) { Text("Apply") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
