package data.gog.prices

import com.google.gson.annotations.SerializedName

data class GogPricesResponse(
    @SerializedName("_embedded")
    val embedded: Embedded
)