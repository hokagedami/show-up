package com.codekage.showup.v2.domain.model

import java.time.LocalDate
import java.util.UUID

data class NonWorkDay(
    val id: UUID,
    val jobId: UUID?,
    val date: LocalDate,
    val type: NonWorkDayType,
    val label: String,
)
