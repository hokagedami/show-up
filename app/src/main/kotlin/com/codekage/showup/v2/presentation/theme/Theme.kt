package com.codekage.showup.v2.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import com.codekage.showup.v2.data.repository.AccentColor

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F8C3D),
    onPrimary = Color(0xFF005E0D),
    primaryContainer = Color(0xFFC2EBC3),
    onPrimaryContainer = Color(0xFF002B18),
    secondary = Color(0xFF7AC8FF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5EBFF),
    onSecondaryContainer = Color(0xFF003C4A),
    tertiary = Color(0xFFFFB343),
    onTertiary = Color.White,
    background = Color(0xFFF6F8F3),
    onBackground = Color(0xFF1B1F22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1F22),
    surfaceVariant = Color(0xFFE8E2D5),
    onSurfaceVariant = Color(0xFF464E45),
    error = Color(0xFFFF6E6C),
    onError = Color.White,
    outline = Color(0xFFC8CDC6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3F8C3D),
    onPrimary = Color(0xFF005E0D),
    primaryContainer = Color(0xFF0B5C20),
    onPrimaryContainer = Color(0xFFA1D8A6),
    secondary = Color(0xFF7AC8FF),
    onSecondary = Color(0xFF003C4A),
    secondaryContainer = Color(0xFF0F4055),
    onSecondaryContainer = Color(0xFFC5EBFF),
    tertiary = Color(0xFFFFB343),
    onTertiary = Color(0xFF0F2D43),
    background = Color(0xFF10141D),
    onBackground = Color(0xFFE5E1DB),
    surface = Color(0xFF181F25),
    onSurface = Color(0xFFE5E1DB),
    surfaceVariant = Color(0xFF1E2932),
    onSurfaceVariant = Color(0xFFB0B8AC),
    error = Color(0xFFFF6E6C),
    onError = Color(0xFF40110B),
    outline = Color(0xFF6B7770),
)

private fun accentPrimary(accent: AccentColor, dark: Boolean): Color = when (accent) {
    AccentColor.GREEN -> Color(0xFF3F8C3D)
    AccentColor.BLUE -> if (dark) Color(0xFF36A0FA) else Color(0xFF1F628E)
    AccentColor.PURPLE -> if (dark) Color(0xFFA084FA) else Color(0xFF6F50AD)
    AccentColor.ORANGE -> if (dark) Color(0xFFE38B3C) else Color(0xFFCC780C)
    AccentColor.ROSE -> if (dark) Color(0xFFEC4585) else Color(0xFFD33F88)
}

@Composable
fun OfficeAttendanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AccentColor = AccentColor.GREEN,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val scheme = baseScheme.copy(primary = accentPrimary(accentColor, darkTheme))
    MaterialTheme(colorScheme = scheme, content = content)
}
