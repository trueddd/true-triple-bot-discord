package data.gog.prices

import com.google.gson.annotations.SerializedName

data class Item(
    @SerializedName("_embedded")
    val embedded: EmbeddedX,
)