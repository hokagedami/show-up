package com.codekage.showup.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID

private val Context.plannedDayDataStore by preferencesDataStore(name = "planned_office_days")

/**
 * Stores the user-accepted office-day plan per job so the dashboard can keep showing it
 * after the plan dialog is dismissed. Past dates are filtered on read so the UI only ever
 * displays upcoming reminders.
 */
class PlannedDayRepository(private val context: Context) {

    val plannedDates: Flow<Map<UUID, List<LocalDate>>> = context.plannedDayDataStore.data.map { prefs ->
        val today = LocalDate.now()
        prefs.asMap().mapNotNull { (key, value) ->
            val name = key.name
            if (!name.startsWith(KEY_PREFIX) || value !is String) return@mapNotNull null
            val jobId = runCatching { UUID.fromString(name.removePrefix(KEY_PREFIX)) }.getOrNull()
                ?: return@mapNotNull null
            val dates = value.split(',')
                .mapNotNull { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }
                .filter { !it.isBefore(today) }
                .sorted()
            if (dates.isEmpty()) null else jobId to dates
        }.toMap()
    }

    suspend fun setDatesFor(jobId: UUID, dates: List<LocalDate>) {
        context.plannedDayDataStore.edit { prefs ->
            val key = stringPreferencesKey(KEY_PREFIX + jobId.toString())
            if (dates.isEmpty()) prefs.remove(key)
            else prefs[key] = dates.sorted().joinToString(",") { it.toString() }
        }
    }

    suspend fun clearFor(jobId: UUID) = setDatesFor(jobId, emptyList())

    private companion object {
        const val KEY_PREFIX = "plan_"
    }
}
