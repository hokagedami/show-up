package com.codekage.showup.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.codekage.showup.domain.usecase.OfficeDayPlan
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlanDialog(
    plan: OfficeDayPlan,
    onApply: () -> Unit,
    onModify: () -> Unit,
    onDismiss: () -> Unit,
) {
    val fmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suggested plan for ${plan.job.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${plan.alreadyOfficeDates.size} office days so far. Need ${plan.recommendedOfficeDates.size} more to hit ${plan.job.monthlyGoalPercent}% (${plan.targetOfficeDays}/${plan.totalWorkingDays}).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (plan.recommendedOfficeDates.isEmpty()) {
                    Text("You're on track — no additional office days needed.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn(modifier = Modifier.heightInBound()) {
                        items(plan.recommendedOfficeDates) { date ->
                            Text(date.format(fmt), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (plan.recommendedOfficeDates.isNotEmpty()) {
                Button(onClick = onApply) { Text("Apply") }
            } else {
                Button(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (plan.recommendedOfficeDates.isNotEmpty()) {
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = onModify) { Text("Modify") }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        },
    )
}

private fun Modifier.heightInBound(): Modifier = this.heightIn(max = 240.dp)
