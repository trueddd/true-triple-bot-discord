package data.gog.prices

import com.google.gson.annotations.SerializedName

data class Embedded(
    @SerializedName("items")
    val items: List<Item>
)