package com.codekage.showup.presentation.addjob

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codekage.showup.AppContainer
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditJobScreen(
    appContainer: AppContainer,
    jobId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: AddEditJobViewModel = viewModel(factory = AddEditJobViewModel.factory(appContainer, context, jobId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { perms ->
        if (perms.values.any { it }) viewModel.useCurrentLocation()
    }

    fun onUseCurrentLocationClick() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.useCurrentLocation()
        else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(state.isSaved) { if (state.isSaved) onSaved() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Job" else "Add Job") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedTextField(
                    value = state.name, onValueChange = viewModel::updateName,
                    label = { Text("Job Name") }, modifier = Modifier.fillMaxWidth().testTag("job_name_field"),
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.officeAddress, onValueChange = viewModel::updateAddress,
                        label = { Text("Office Address") },
                        supportingText = { Text("Paste a Maps URL or coordinates and lat/lng auto-fill", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth().testTag("job_address_field"),
                    )
                    OutlinedButton(onClick = ::onUseCurrentLocationClick, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null)
                        Text("  Use Current Location")
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.officeLat.toString(),
                        onValueChange = { viewModel.updateLocation(it.toDoubleOrNull() ?: 0.0, state.officeLng) },
                        label = { Text("Latitude") }, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.officeLng.toString(),
                        onValueChange = { viewModel.updateLocation(state.officeLat, it.toDoubleOrNull() ?: 0.0) },
                        label = { Text("Longitude") }, modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Column {
                    Text("Geofence Radius: ${state.geofenceRadiusMeters}m")
                    Slider(
                        value = state.geofenceRadiusMeters.toFloat(),
                        onValueChange = { viewModel.updateGeofenceRadius(it.toInt()) },
                        valueRange = 50f..500f,
                    )
                }
            }
            item {
                Column {
                    Text("Monthly Office Goal: ${state.monthlyGoalPercent}%")
                    Slider(
                        value = state.monthlyGoalPercent.toFloat(),
                        onValueChange = { viewModel.updateGoalPercent(it.toInt()) },
                        valueRange = 0f..100f,
                    )
                }
            }
            item {
                Column {
                    Text("Work Days", style = MaterialTheme.typography.titleSmall)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            FilterChip(
                                selected = state.workDays.contains(day),
                                onClick = { viewModel.toggleWorkDay(day) },
                                label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                            )
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.workStartTime.toString(),
                        onValueChange = { runCatching { viewModel.updateStartTime(java.time.LocalTime.parse(it)) } },
                        label = { Text("Start Time") }, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.workEndTime.toString(),
                        onValueChange = { runCatching { viewModel.updateEndTime(java.time.LocalTime.parse(it)) } },
                        label = { Text("End Time") }, modifier = Modifier.weight(1f),
                    )
                }
            }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                Button(onClick = viewModel::saveJob, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().testTag("save_job_button")) {
                    Text(if (state.isEditing) "Save Changes" else "Add Job")
                }
            }
        }
    }
}
