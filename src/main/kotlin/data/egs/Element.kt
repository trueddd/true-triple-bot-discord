package data.egs

data class Element(
    val id: String,
    val title: String,
    val effectiveDate: String,
    val promotions: Promotions?
)