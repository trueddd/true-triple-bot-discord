package data.gog.prices

import com.google.gson.annotations.SerializedName

data class EmbeddedX(
    @SerializedName("prices")
    val prices: List<Price>,
    @SerializedName("product")
    val product: Product
)