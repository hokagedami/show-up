package com.codekage.showup.v2.domain.usecase

import com.codekage.showup.v2.data.repository.HolidayRepository
import com.codekage.showup.v2.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Year

class SyncHolidaysUseCase(
    private val holidayRepository: HolidayRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<Int> {
        return runCatching {
            val settings = settingsRepository.settings.first()
            val year = Year.now().value
            if (!forceRefresh && settings.lastHolidaySyncYear == year) return@runCatching 0
            val count = holidayRepository.fetchAndStoreHolidays(year, settings.holidayCountryCode)
            settingsRepository.updateLastHolidaySyncYear(year)
            count
        }
    }
}
