package com.codekage.showup.v2.presentation.jobdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.codekage.showup.v2.AppContainer
import com.codekage.showup.v2.presentation.common.AttendanceColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    appContainer: AppContainer,
    jobId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit,
) {
    val viewModel: JobDetailViewModel = viewModel(factory = JobDetailViewModel.factory(appContainer, jobId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    // Wait for the delete to commit before popping back — otherwise the viewModelScope
    // gets cancelled mid-flight and the dashboard still shows the "deleted" job.
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.job?.name ?: "Job") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { onEdit(jobId) }, modifier = Modifier.testTag("edit_job_button")) {
                        Icon(Icons.Filled.Edit, "Edit")
                    }
                    IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("delete_job_button")) {
                        Icon(Icons.Filled.Delete, "Delete")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProgressDonut(state.currentPercentage, state.job?.monthlyGoalPercent ?: 0)

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This Month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        StatCol(state.officeDays.toString(), "Office")
                        StatCol(state.remoteDays.toString(), "Remote")
                        StatCol(state.sickDays.toString(), "Sick")
                        StatCol(state.leaveDays.toString(), "Leave")
                    }
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        StatCol(state.totalWorkingDays.toString(), "Working Days")
                        StatCol(state.remainingDays.toString(), "Remaining")
                        StatCol("${state.job?.monthlyGoalPercent ?: 0}%", "Goal")
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Job Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    state.job?.let { job ->
                        DetailRow("Address", job.officeAddress)
                        DetailRow("Geofence Radius", "${job.geofenceRadiusMeters}m")
                        DetailRow("Work Days", job.workDays.joinToString(", ") { it.name.take(3) })
                        DetailRow("Work Hours", "${job.workStartTime} - ${job.workEndTime}")
                        DetailRow("Status", if (job.isActive) "Active" else "Inactive")
                    }
                }
            }

            OutlinedButton(onClick = viewModel::toggleActive, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.job?.isActive == true) "Deactivate Job" else "Activate Job")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete job?") },
            text = { Text("This permanently removes the job and all its attendance data.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteJob()
                        confirmDelete = false
                    },
                    modifier = Modifier.testTag("confirm_delete_button"),
                ) { Text("Delete") }
            },
            dismissButton = { OutlinedButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProgressDonut(percentage: Float, goal: Int) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val color = AttendanceColors.goalColor(percentage, goal)
        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("${percentage.toInt()}%", style = MaterialTheme.typography.headlineLarge, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatCol(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
