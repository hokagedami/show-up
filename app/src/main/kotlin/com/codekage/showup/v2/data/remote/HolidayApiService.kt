package com.codekage.showup.v2.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface HolidayApiService {
    @GET("api/v3/AvailableCountries")
    suspend fun getAvailableCountries(): List<CountryDto>

    @GET("api/v3/PublicHolidays/{year}/{countryCode}")
    suspend fun getPublicHolidays(
        @Path("year") year: Int,
        @Path("countryCode") countryCode: String,
    ): List<PublicHolidayDto>
}
