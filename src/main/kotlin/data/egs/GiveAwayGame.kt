package data.egs

import java.time.LocalDateTime

data class GiveAwayGame(
    val id: String,
    val title: String,
    val promotion: OfferDates?,
    val lastUpdated: LocalDateTime
)

data class OfferDates(
    val start: LocalDateTime,
    val end: LocalDateTime
)