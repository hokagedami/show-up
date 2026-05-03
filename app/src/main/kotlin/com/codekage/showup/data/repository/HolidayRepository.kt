package com.codekage.showup.data.repository

import com.codekage.showup.data.remote.HolidayApiService
import com.codekage.showup.domain.model.NonWorkDay
import com.codekage.showup.domain.model.NonWorkDayType
import com.codekage.showup.domain.repository.NonWorkDayRepository
import java.time.LocalDate
import java.util.UUID

class HolidayRepository(
    private val holidayApiService: HolidayApiService,
    private val nonWorkDayRepository: NonWorkDayRepository,
) {
    suspend fun fetchAndStoreHolidays(year: Int, countryCode: String = "GB"): Int {
        val holidays = holidayApiService.getPublicHolidays(year, countryCode)
        val nonWorkDays = holidays.map { dto ->
            val date = LocalDate.parse(dto.date)
            NonWorkDay(
                // Deterministic UUID derived from country + date so re-syncing the same year
                // replaces existing rows (REPLACE on PK) instead of inserting duplicates.
                id = UUID.nameUUIDFromBytes("holiday-$countryCode-$date".toByteArray()),
                jobId = null,
                date = date,
                type = NonWorkDayType.BANK_HOLIDAY,
                label = dto.localName,
            )
        }
        nonWorkDayRepository.insertAll(nonWorkDays)
        return nonWorkDays.size
    }

    suspend fun getAvailableCountries(): Result<List<Pair<String, String>>> = runCatching {
        holidayApiService.getAvailableCountries().map { it.countryCode to it.name }
    }
}
