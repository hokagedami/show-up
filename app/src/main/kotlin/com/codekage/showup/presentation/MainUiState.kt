package com.codekage.showup.presentation

import com.codekage.showup.data.repository.AccentColor
import com.codekage.showup.data.repository.ThemeMode

data class MainUiState(
    val appLockEnabled: Boolean = false,
    val appLockTimeoutMinutes: Int = 5,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.GREEN,
    val isSettingsLoaded: Boolean = false,
)
