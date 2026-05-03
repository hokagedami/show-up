package com.codekage.showup.v2.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class CountryDto(
    val countryCode: String,
    val name: String,
)
