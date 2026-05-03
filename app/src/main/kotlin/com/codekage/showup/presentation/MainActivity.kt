package com.codekage.showup.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.codekage.showup.OfficeAttendanceApp
import com.codekage.showup.data.repository.ThemeMode
import com.codekage.showup.presentation.navigation.AppNavigation
import com.codekage.showup.presentation.theme.OfficeAttendanceTheme

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as OfficeAttendanceApp).appContainer

        setContent {
            val state by viewModel.uiState.collectAsState()
            val dark = when (state.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            OfficeAttendanceTheme(darkTheme = dark, accentColor = state.accentColor) {
                Box(Modifier.semantics { testTagsAsResourceId = true }) {
                    AppNavigation(appContainer)
                }
            }
        }
    }
}
