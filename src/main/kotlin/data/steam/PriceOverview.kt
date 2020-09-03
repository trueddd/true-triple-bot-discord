package data.steam

import com.google.gson.annotations.SerializedName

data class PriceOverview(
    @SerializedName(value = "initial_formatted")
    val initial: String,
    @SerializedName(value = "final_formatted")
    val final: String?
)