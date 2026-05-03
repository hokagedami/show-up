package com.codekage.showup.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.codekage.showup.OfficeAttendanceApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = (application as OfficeAttendanceApp).appContainer.settingsRepository

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.value = MainUiState(
                    appLockEnabled = settings.appLockEnabled,
                    appLockTimeoutMinutes = settings.appLockTimeoutMinutes,
                    themeMode = settings.themeMode,
                    accentColor = settings.accentColor,
                    isSettingsLoaded = true,
                )
            }
        }
    }
}
