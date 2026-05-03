package com.codekage.showup.v2.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconToggleButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codekage.showup.v2.AppContainer
import com.codekage.showup.v2.data.repository.AccentColor
import com.codekage.showup.v2.data.repository.BackupFrequency
import com.codekage.showup.v2.data.repository.ThemeMode
import java.io.File
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(appContainer: AppContainer, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(appContainer, context))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importBackup)
    }
    val portableExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let(viewModel::beginPortableExport) }
    val portableImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::beginPortableImport) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var showBackupFrequencyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Section("Appearance") {
                    SubHeader("Theme")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        ThemeMode.entries.forEachIndexed { i, mode ->
                            SegmentedButton(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                                modifier = Modifier.testTag("theme_${mode.name.lowercase()}_button"),
                                icon = {
                                    Icon(
                                        imageVector = themeIcon(mode),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                },
                            ) { Text(themeLabel(mode)) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    SubHeader("Accent colour")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AccentColor.entries.forEach { accent ->
                            AccentSwatch(
                                accent = accent,
                                selected = settings.accentColor == accent,
                                onClick = { viewModel.setAccentColor(accent) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                Section("Notifications") {
                    ToggleRow(
                        icon = Icons.Filled.NotificationsActive,
                        title = "Office day reminders",
                        subtitle = "Morning ping on office days",
                        checked = settings.officeReminderEnabled,
                        onChange = viewModel::setOfficeReminder,
                    )
                    ListDivider()
                    ToggleRow(
                        icon = Icons.Filled.LocationOn,
                        title = "GPS failure alerts",
                        subtitle = "Reminder if GPS didn't detect office visit",
                        checked = settings.gpsFailureReminderEnabled,
                        onChange = viewModel::setGpsFailureReminder,
                    )
                    ListDivider()
                    ToggleRow(
                        icon = Icons.Filled.Flag,
                        title = "Goal alerts",
                        subtitle = "Alert when attendance drops below goal",
                        checked = settings.goalAlertEnabled,
                        onChange = viewModel::setGoalAlert,
                    )
                    ListDivider()
                    ToggleRow(
                        icon = Icons.Filled.Today,
                        title = "Weekly summary",
                        subtitle = "Monday morning attendance summary",
                        checked = settings.weeklySummaryEnabled,
                        onChange = viewModel::setWeeklySummary,
                    )
                    ListDivider()
                    ActionRow(
                        icon = Icons.Filled.Schedule,
                        title = "Reminder time",
                        subtitle = "%02d:%02d".format(settings.reminderTimeHour, settings.reminderTimeMinute),
                        onClick = { showTimePicker = true },
                    )
                }
            }

            item {
                Section("Security") {
                    ToggleRow(
                        icon = Icons.Filled.Lock,
                        title = "App lock",
                        subtitle = "Require biometric or PIN to open app",
                        checked = settings.appLockEnabled,
                        onChange = viewModel::setAppLock,
                    )
                }
            }

            item {
                Section("Backup") {
                    ToggleRow(
                        icon = Icons.Filled.Backup,
                        title = "Auto backup",
                        subtitle = "Encrypted backup on a schedule",
                        checked = settings.autoBackupEnabled,
                        onChange = viewModel::setAutoBackup,
                    )
                    if (settings.autoBackupEnabled) {
                        ListDivider()
                        ActionRow(
                            icon = Icons.Filled.EventNote,
                            title = "Backup frequency",
                            subtitle = settings.backupFrequency.name.lowercase().replaceFirstChar { it.titlecase() },
                            onClick = { showBackupFrequencyDialog = true },
                        )
                    }
                    ListDivider()
                    ActionRow(
                        icon = Icons.Filled.CloudUpload,
                        title = "Back up now",
                        subtitle = state.backupStatus ?: "Create a manual encrypted backup",
                        onClick = viewModel::manualBackup,
                    )
                    ListDivider()
                    ActionRow(
                        icon = Icons.Filled.FileUpload,
                        title = "Import backup file",
                        subtitle = "Restore from a backup on this device",
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                    )
                    ListDivider()
                    ActionRow(
                        icon = Icons.Filled.SwapHoriz,
                        title = "Export portable backup",
                        subtitle = "Passphrase-encrypted backup that opens on the desktop app",
                        onClick = {
                            val ts = java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                            portableExportLauncher.launch("showup_${ts}.showupbak")
                        },
                    )
                    ListDivider()
                    ActionRow(
                        icon = Icons.Filled.FileDownload,
                        title = "Import portable backup",
                        subtitle = "Restore a .showupbak file with its passphrase",
                        onClick = { portableImportLauncher.launch(arrayOf("*/*")) },
                    )
                    state.portableStatus?.let { status ->
                        Text(
                            status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    if (state.backupFiles.isNotEmpty()) {
                        ListDivider()
                        Text(
                            "Recent backups",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        state.backupFiles.take(3).forEachIndexed { i, file ->
                            if (i > 0) ListDivider()
                            ActionRow(
                                icon = Icons.Filled.CloudDownload,
                                title = file.name,
                                subtitle = "Tap to restore",
                                onClick = { viewModel.confirmRestore(file) },
                            )
                        }
                    }
                    state.restoreStatus?.let { status ->
                        Text(
                            status,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            item {
                Section("Holidays") {
                    ActionRow(
                        icon = Icons.Filled.Flag,
                        title = "Country",
                        subtitle = settings.holidayCountryCode,
                        onClick = { showCountryDialog = true },
                    )
                    ListDivider()
                    ActionRow(
                        icon = Icons.Filled.CalendarMonth,
                        title = "Refresh bank holidays",
                        subtitle = state.holidaySyncStatus ?: "Fetch holidays for ${settings.holidayCountryCode}",
                        onClick = viewModel::syncHolidays,
                    )
                    ListDivider()
                    ActionRow(
                        icon = Icons.Filled.EventNote,
                        title = if (state.showHolidayManager) "Hide bank holidays" else "Manage bank holidays",
                        subtitle = "${state.bankHolidays.size} on file this year",
                        onClick = viewModel::toggleHolidayManager,
                    )
                    if (state.showHolidayManager && state.bankHolidays.isNotEmpty()) {
                        ListDivider()
                        state.bankHolidays.forEach { holiday ->
                            ListItem(
                                headlineContent = { Text(holiday.label) },
                                supportingContent = {
                                    Text(
                                        "${holiday.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} · ${holiday.date}",
                                    )
                                },
                                trailingContent = {
                                    OutlinedButton(onClick = { viewModel.removeBankHoliday(holiday.id) }) {
                                        Text("Remove")
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            )
                        }
                    }
                }
            }

            item {
                Section("About") {
                    InfoRow(
                        icon = Icons.Filled.Info,
                        title = "Version",
                        value = "1.0.0",
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showTimePicker) {
        ReminderTimePicker(
            initialHour = settings.reminderTimeHour,
            initialMinute = settings.reminderTimeMinute,
            onConfirm = { h, m -> viewModel.setReminderTime(h, m); showTimePicker = false },
            onDismiss = { showTimePicker = false },
        )
    }

    if (showCountryDialog) {
        CountryCodeDialog(
            current = settings.holidayCountryCode,
            onConfirm = { code -> viewModel.setHolidayCountryCode(code); showCountryDialog = false },
            onDismiss = { showCountryDialog = false },
        )
    }

    if (showBackupFrequencyDialog) {
        BackupFrequencyDialog(
            current = settings.backupFrequency,
            onSelect = { f -> viewModel.setBackupFrequency(f); showBackupFrequencyDialog = false },
            onDismiss = { showBackupFrequencyDialog = false },
        )
    }

    if (state.showRestoreDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRestoreDialog,
            icon = { Icon(Icons.Filled.CloudDownload, contentDescription = null) },
            title = { Text("Restore backup?") },
            text = {
                Text(
                    "This will replace all current data with the contents of " +
                        "${state.pendingRestoreFile?.name ?: "this backup"}. This cannot be undone.",
                )
            },
            confirmButton = { Button(onClick = viewModel::confirmRestore) { Text("Restore") } },
            dismissButton = { TextButton(onClick = viewModel::dismissRestoreDialog) { Text("Cancel") } },
        )
    }

    state.portablePrompt?.let { prompt ->
        PortablePassphraseDialog(
            isExport = prompt.action == PortableBackupAction.EXPORT,
            onConfirm = { viewModel.confirmPortablePrompt(it) },
            onDismiss = viewModel::cancelPortablePrompt,
        )
    }
}

@Composable
private fun PortablePassphraseDialog(
    isExport: Boolean,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var revealed by remember { mutableStateOf(false) }
    val mismatch = isExport && confirm.isNotEmpty() && confirm != passphrase
    val canSubmit = passphrase.length >= 6 && (!isExport || passphrase == confirm)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.LockReset, contentDescription = null) },
        title = { Text(if (isExport) "Choose an export passphrase" else "Enter the backup passphrase") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isExport)
                        "You'll need this passphrase to open the backup on the desktop app or import it back here. Minimum 6 characters."
                    else
                        "Enter the passphrase the backup was created with.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase") },
                    singleLine = true,
                    visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconToggleButton(checked = revealed, onCheckedChange = { revealed = it }) {
                            Icon(
                                if (revealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                )
                if (isExport) {
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Confirm passphrase") },
                        singleLine = true,
                        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = mismatch,
                        supportingText = if (mismatch) { { Text("Passphrases don't match") } } else null,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSubmit,
                onClick = { onConfirm(passphrase.toCharArray()) },
            ) { Text(if (isExport) "Export" else "Decrypt") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ─────────────────────── Section primitives ───────────────────────

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp),
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SubHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ListDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
) {
    ListItem(
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        trailingContent = { Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun AccentSwatch(
    accent: AccentColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = accentSwatchColor(accent)
    // Each swatch is given a weighted share of the row's width by the parent. The Column
    // takes the full share so the circle stays centred and never has to compete for space
    // with neighbouring labels — that was the bug that clipped the last swatch on narrow
    // screens.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        // Outer ring shows when selected; inner filled circle is the colour itself.
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(
                    width = if (selected) 3.dp else 0.dp,
                    color = if (selected) color else Color.Transparent,
                    shape = CircleShape,
                )
                .padding(if (selected) 5.dp else 0.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            accentLabel(accent),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

private fun accentSwatchColor(accent: AccentColor): Color = when (accent) {
    AccentColor.GREEN -> Color(0xFF3F8C3D)
    AccentColor.BLUE -> Color(0xFF1F628E)
    AccentColor.PURPLE -> Color(0xFF6F50AD)
    AccentColor.ORANGE -> Color(0xFFCC780C)
    AccentColor.ROSE -> Color(0xFFD33F88)
}

private fun accentLabel(accent: AccentColor): String = when (accent) {
    AccentColor.GREEN -> "Green"
    AccentColor.BLUE -> "Blue"
    AccentColor.PURPLE -> "Purple"
    AccentColor.ORANGE -> "Orange"
    AccentColor.ROSE -> "Rose"
}

private fun themeIcon(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> Icons.Filled.Brightness6
    ThemeMode.LIGHT -> Icons.Filled.WbSunny
    ThemeMode.DARK -> Icons.Filled.Brightness4
}

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

// ─────────────────────── Dialogs ───────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Reminder time") },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
    )
}

@Composable
private fun CountryCodeDialog(
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Holiday country code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Two-letter ISO 3166-1 code (e.g. GB, US, NG). Used to fetch public holidays.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { v -> value = v.uppercase().take(2).filter { it.isLetter() } },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (value.length == 2) onConfirm(value) },
                enabled = value.length == 2,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun BackupFrequencyDialog(
    current: BackupFrequency,
    onSelect: (BackupFrequency) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backup frequency") },
        text = {
            Column {
                BackupFrequency.entries.forEach { f ->
                    ListItem(
                        headlineContent = { Text(f.name.lowercase().replaceFirstChar { it.titlecase() }) },
                        leadingContent = {
                            if (f == current) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                            else Spacer(Modifier.size(24.dp))
                        },
                        modifier = Modifier.clickable { onSelect(f) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
