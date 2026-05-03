package com.codekage.showup.presentation.common

import androidx.compose.ui.graphics.Color

object AttendanceColors {
    val office = Color(0xFF3F8C3D)        // green
    val remote = Color(0xFF4DA3F0)        // medium blue (not the washed-out cyan)
    val leave = Color(0xFFFFB343)         // amber/orange
    val sick = Color(0xFFFF6E6C)          // coral red
    val bankHoliday = Color(0xFF9D7BFF)   // purple — distinct from remote's blue
    val absent = Color(0xFF888A87)        // grey

    fun goalColor(currentPercentage: Float, goalPercentage: Int): Color {
        val target = goalPercentage.toFloat()
        return when {
            currentPercentage >= target -> office
            currentPercentage >= target * 0.8f -> leave
            else -> sick
        }
    }
}
