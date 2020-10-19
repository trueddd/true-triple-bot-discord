package data.gog

data class Price(
    val amount: String,
    val baseAmount: String,
    val discount: Int,
    val discountPercentage: Int,
    val finalAmount: String,
    val isDiscounted: Boolean,
    val isFree: Boolean,
    val symbol: String,
)