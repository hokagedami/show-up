package com.codekage.showup.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class PublicHolidayDto(
    val date: String,
    val localName: String,
    val name: String,
    val countryCode: String,
    val global: Boolean = false,
    val types: List<String> = emptyList(),
)
