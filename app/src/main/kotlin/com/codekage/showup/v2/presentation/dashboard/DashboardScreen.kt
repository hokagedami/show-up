package com.codekage.showup.v2.presentation.dashboard

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.codekage.showup.v2.AppContainer
import com.codekage.showup.v2.domain.model.AttendanceRecordStatus
import com.codekage.showup.v2.presentation.common.AttendanceColors
import com.codekage.showup.v2.presentation.common.ScreenHeader
import com.codekage.showup.v2.service.GeofenceState
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    appContainer: AppContainer,
    onSettingsClick: () -> Unit,
    onAddJobClick: () -> Unit,
    onJobClick: (String) -> Unit,
) {
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(appContainer))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.transientMessage) {
        state.transientMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTransientMessage()
        }
    }

    // Foreground location permission launcher. Granting it unblocks fine-location geofences;
    // background still has to be granted in system Settings on Android 11+.
    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ -> viewModel.recheckGeofences() }

    // Re-evaluate every time the Dashboard becomes visible — covers users returning from
    // system Settings after granting background location or turning Location services on.
    LaunchedEffect(state.jobItems.size) { viewModel.recheckGeofences() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (state.hasJobs) {
                FloatingActionButton(onClick = onAddJobClick, modifier = Modifier.testTag("add_job_fab")) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Job")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = greeting(),
                subtitle = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())),
                onSettingsClick = onSettingsClick,
            )

            if (!state.hasJobs && !state.isLoading) {
                EmptyState(onAddJobClick)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Aggregate the worst-case geofence issue across all active jobs and surface
                    // one banner. We only show the banner when something needs the user's action.
                    val problem = aggregateGeofenceProblem(state.geofenceStates.values)
                    if (problem != null) {
                        item {
                            GeofenceBanner(
                                state = problem,
                                onGrantForeground = {
                                    fineLocationLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION,
                                        ),
                                    )
                                },
                                onOpenAppSettings = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null),
                                    )
                                    context.startActivity(intent)
                                },
                                onOpenLocationSettings = {
                                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                },
                            )
                        }
                    }
                    items(state.jobItems) { item ->
                        JobCard(
                            item = item,
                            geofenceState = state.geofenceStates[item.job.id.toString()],
                            onClick = { onJobClick(item.job.id.toString()) },
                            onMarkStatus = { status -> viewModel.quickMarkToday(item.job.id, status) },
                            onClearMark = { viewModel.clearTodayMark(item.job.id) },
                            onPlan = { viewModel.generatePlan(item.job.id) },
                            onClearPlan = { viewModel.clearPlanFor(item.job.id) },
                        )
                    }
                }
            }
        }
    }

    state.currentPlan?.let { plan ->
        PlanDialog(
            plan = plan,
            onApply = viewModel::applyCurrentPlan,
            onModify = viewModel::dismissPlan,
            onDismiss = viewModel::dismissPlan,
        )
    }
}

private fun greeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun EmptyState(onAddJobClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No jobs yet", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Add a job to start tracking attendance.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.material3.Button(
                onClick = onAddJobClick,
                modifier = Modifier.padding(top = 16.dp).testTag("add_job_empty_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Add Job")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobCard(
    item: JobDashboardItem,
    geofenceState: GeofenceState?,
    onClick: () -> Unit,
    onMarkStatus: (AttendanceRecordStatus) -> Unit,
    onClearMark: () -> Unit,
    onPlan: () -> Unit,
    onClearPlan: () -> Unit,
) {
    var expanded by rememberSaveable(item.job.id) { mutableStateOf(item.plannedOfficeDates.isNotEmpty()) }
    LaunchedEffect(item.plannedOfficeDates.size) {
        if (item.plannedOfficeDates.isNotEmpty()) expanded = true
    }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.job.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(item.job.officeAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    GeofencePill(geofenceState)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("This Month", "${item.officeDays} days")
                    StatItem("Goal", "${item.job.monthlyGoalPercent}%")
                    StatItem("Status", "${item.currentPercentage.toInt()}%", color = AttendanceColors.goalColor(item.currentPercentage, item.job.monthlyGoalPercent))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { onMarkStatus(AttendanceRecordStatus.OFFICE) },
                    label = { Text("Office") },
                    leadingIcon = { if (item.todayStatus == AttendanceRecordStatus.OFFICE) Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) },
                    colors = if (item.todayStatus == AttendanceRecordStatus.OFFICE) AssistChipDefaults.assistChipColors(containerColor = AttendanceColors.office.copy(alpha = 0.2f)) else AssistChipDefaults.assistChipColors(),
                    modifier = Modifier.testTag("today_chip_office"),
                )
                AssistChip(
                    onClick = { onMarkStatus(AttendanceRecordStatus.REMOTE) },
                    label = { Text("Remote") },
                    leadingIcon = { if (item.todayStatus == AttendanceRecordStatus.REMOTE) Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) },
                    colors = if (item.todayStatus == AttendanceRecordStatus.REMOTE) AssistChipDefaults.assistChipColors(containerColor = AttendanceColors.remote.copy(alpha = 0.2f)) else AssistChipDefaults.assistChipColors(),
                    modifier = Modifier.testTag("today_chip_remote"),
                )
                if (item.todayStatus != null && item.todayStatus != AttendanceRecordStatus.ABSENT) {
                    AssistChip(onClick = onClearMark, label = { Text("Clear") }, modifier = Modifier.testTag("today_clear_button"))
                }
                Box(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("expand_job_card"),
                ) {
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(chevronRotation),
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (item.plannedOfficeDates.isEmpty()) {
                        Text(
                            "No active plan. Generate one to schedule office-day reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = onPlan, modifier = Modifier.testTag("generate_plan_button")) {
                                Text("Generate Plan")
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.EventAvailable,
                                contentDescription = null,
                                tint = AttendanceColors.office,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "Planned office days · ${item.plannedOfficeDates.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            item.plannedOfficeDates.forEach { date ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(date.format(PLAN_DATE_FORMATTER)) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = AttendanceColors.office.copy(alpha = 0.15f),
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = onClearPlan, modifier = Modifier.testTag("clear_plan_button")) {
                                Text("Clear plan")
                            }
                            TextButton(onClick = onPlan, modifier = Modifier.testTag("regenerate_plan_button")) {
                                Text("Re-plan")
                            }
                        }
                    }
                }
            }
        }
    }
}

private val PLAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

@Composable
private fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

// ───────────────── Geofence status surface ─────────────────

/** Picks the single most-actionable problem to surface across all jobs. Returns `null` when
 *  every job is `Armed` or `NotArmed` (no need for the banner). The priority order matches
 *  what blocks the user first: foreground perm → background perm → location services → other. */
private fun aggregateGeofenceProblem(states: Collection<GeofenceState>): GeofenceState? {
    if (states.isEmpty()) return null
    states.firstOrNull { it is GeofenceState.NeedsForegroundLocation }?.let { return it }
    states.firstOrNull { it is GeofenceState.NeedsBackgroundLocation }?.let { return it }
    states.firstOrNull { it is GeofenceState.LocationServicesOff }?.let { return it }
    states.firstOrNull { it is GeofenceState.InvalidCoordinates }?.let { return it }
    states.firstOrNull { it is GeofenceState.Error }?.let { return it }
    return null
}

@Composable
private fun GeofenceBanner(
    state: GeofenceState,
    onGrantForeground: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    val (title, body, actionLabel, action) = when (state) {
        is GeofenceState.NeedsForegroundLocation -> Quad(
            "Auto-detect needs location",
            "Allow ShowUp to access this device's location so it can record office days when you arrive.",
            "Grant location",
            onGrantForeground,
        )
        is GeofenceState.NeedsBackgroundLocation -> Quad(
            "Allow location all the time",
            "Background location is required for the app to detect you've reached the office when you aren't actively using it. Set Location to “Allow all the time”.",
            "Open app settings",
            onOpenAppSettings,
        )
        is GeofenceState.LocationServicesOff -> Quad(
            "Location services are off",
            "Turn on Location in system settings so geofences can fire.",
            "Open location settings",
            onOpenLocationSettings,
        )
        is GeofenceState.InvalidCoordinates -> Quad(
            "Job missing coordinates",
            "Edit the job and set the office address (or tap “Use current location”) so auto-detect has somewhere to listen for.",
            null,
            null,
        )
        is GeofenceState.Error -> Quad(
            "Geofence error",
            "${state.message}. Auto-detect won't fire until this is resolved.",
            null,
            null,
        )
        else -> return
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(body, style = MaterialTheme.typography.bodySmall)
            if (actionLabel != null && action != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = action) { Text(actionLabel) }
                }
            }
        }
    }
}

@Composable
private fun GeofencePill(state: GeofenceState?) {
    val (label, color, icon) = when (state) {
        is GeofenceState.Armed -> Triple("Armed", AttendanceColors.office, Icons.Filled.LocationOn)
        is GeofenceState.LocationServicesOff -> Triple("GPS off", AttendanceColors.sick, Icons.Filled.LocationOff)
        is GeofenceState.NeedsForegroundLocation,
        is GeofenceState.NeedsBackgroundLocation -> Triple("Permission needed", AttendanceColors.leave, Icons.Filled.Warning)
        is GeofenceState.InvalidCoordinates -> Triple("No coords", AttendanceColors.leave, Icons.Filled.Warning)
        is GeofenceState.Error -> Triple("Error", AttendanceColors.sick, Icons.Filled.Warning)
        null, GeofenceState.NotArmed -> return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color.copy(alpha = 0.18f),
                RoundedCornerShape(50),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(end = 4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
