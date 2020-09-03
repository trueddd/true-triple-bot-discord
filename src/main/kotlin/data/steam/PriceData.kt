package data.steam

import com.google.gson.annotations.SerializedName

data class PriceData(
    @SerializedName(value = "price_overview")
    val priceOverview: PriceOverview
)