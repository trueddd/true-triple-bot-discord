package data.gog.prices

import com.google.gson.annotations.SerializedName

data class Price(
    @SerializedName("basePrice")
    val basePrice: String,
    @SerializedName("currency")
    val currency: Currency,
    @SerializedName("finalPrice")
    val finalPrice: String
)