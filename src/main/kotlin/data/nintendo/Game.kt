package data.nintendo

import com.google.gson.annotations.SerializedName

data class Game(
    val url: String,
    val title: String,
    @SerializedName("price_regular_f")
    val regularPrice: Float,
    @SerializedName("price_lowest_f")
    val lowestPrice: Float,
    @SerializedName("nsuid_txt")
    val nsuid: List<String>,
)
