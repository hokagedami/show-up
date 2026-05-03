package com.codekage.showup.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val OFFICE_REMINDER = booleanPreferencesKey("office_reminder_enabled")
        val GPS_FAILURE_REMINDER = booleanPreferencesKey("gps_failure_reminder_enabled")
        val GOAL_ALERT = booleanPreferencesKey("goal_alert_enabled")
        val WEEKLY_SUMMARY = booleanPreferencesKey("weekly_summary_enabled")
        val APP_LOCK = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_TIMEOUT = intPreferencesKey("app_lock_timeout_minutes")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup_enabled")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val REMINDER_HOUR = intPreferencesKey("reminder_time_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_time_minute")
        val LAST_HOLIDAY_SYNC_YEAR = intPreferencesKey("last_holiday_sync_year")
        val HOLIDAY_COUNTRY_CODE = stringPreferencesKey("holiday_country_code")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            accentColor = prefs[Keys.ACCENT_COLOR]?.let { runCatching { AccentColor.valueOf(it) }.getOrNull() } ?: AccentColor.GREEN,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            officeReminderEnabled = prefs[Keys.OFFICE_REMINDER] ?: true,
            gpsFailureReminderEnabled = prefs[Keys.GPS_FAILURE_REMINDER] ?: true,
            goalAlertEnabled = prefs[Keys.GOAL_ALERT] ?: true,
            weeklySummaryEnabled = prefs[Keys.WEEKLY_SUMMARY] ?: true,
            appLockEnabled = prefs[Keys.APP_LOCK] ?: false,
            appLockTimeoutMinutes = prefs[Keys.APP_LOCK_TIMEOUT] ?: 5,
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP] ?: true,
            backupFrequency = prefs[Keys.BACKUP_FREQUENCY]?.let { runCatching { BackupFrequency.valueOf(it) }.getOrNull() } ?: BackupFrequency.DAILY,
            reminderTimeHour = prefs[Keys.REMINDER_HOUR] ?: 8,
            reminderTimeMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            lastHolidaySyncYear = prefs[Keys.LAST_HOLIDAY_SYNC_YEAR] ?: 0,
            holidayCountryCode = prefs[Keys.HOLIDAY_COUNTRY_CODE] ?: "GB",
        )
    }

    suspend fun updateThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun updateAccentColor(color: AccentColor) = edit { it[Keys.ACCENT_COLOR] = color.name }
    suspend fun updateOnboardingCompleted(completed: Boolean) = edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    suspend fun updateOfficeReminder(enabled: Boolean) = edit { it[Keys.OFFICE_REMINDER] = enabled }
    suspend fun updateGpsFailureReminder(enabled: Boolean) = edit { it[Keys.GPS_FAILURE_REMINDER] = enabled }
    suspend fun updateGoalAlert(enabled: Boolean) = edit { it[Keys.GOAL_ALERT] = enabled }
    suspend fun updateWeeklySummary(enabled: Boolean) = edit { it[Keys.WEEKLY_SUMMARY] = enabled }
    suspend fun updateAppLock(enabled: Boolean) = edit { it[Keys.APP_LOCK] = enabled }
    suspend fun updateAppLockTimeout(minutes: Int) = edit { it[Keys.APP_LOCK_TIMEOUT] = minutes }
    suspend fun updateAutoBackup(enabled: Boolean) = edit { it[Keys.AUTO_BACKUP] = enabled }
    suspend fun updateBackupFrequency(frequency: BackupFrequency) = edit { it[Keys.BACKUP_FREQUENCY] = frequency.name }
    suspend fun updateReminderTime(hour: Int, minute: Int) = edit {
        it[Keys.REMINDER_HOUR] = hour
        it[Keys.REMINDER_MINUTE] = minute
    }
    suspend fun updateLastHolidaySyncYear(year: Int) = edit { it[Keys.LAST_HOLIDAY_SYNC_YEAR] = year }
    suspend fun updateHolidayCountryCode(code: String) = edit { it[Keys.HOLIDAY_COUNTRY_CODE] = code }

    private suspend inline fun edit(crossinline block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
