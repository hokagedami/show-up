package com.codekage.showup.v2.data.repository

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.GREEN,
    val onboardingCompleted: Boolean = false,
    val officeReminderEnabled: Boolean = true,
    val gpsFailureReminderEnabled: Boolean = true,
    val goalAlertEnabled: Boolean = true,
    val weeklySummaryEnabled: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockTimeoutMinutes: Int = 5,
    val autoBackupEnabled: Boolean = true,
    val backupFrequency: BackupFrequency = BackupFrequency.DAILY,
    val reminderTimeHour: Int = 8,
    val reminderTimeMinute: Int = 0,
    val lastHolidaySyncYear: Int = 0,
    val holidayCountryCode: String = "GB",
)
