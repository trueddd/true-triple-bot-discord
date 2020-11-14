package data.minecraft

import com.google.gson.annotations.SerializedName

data class Players(
    @SerializedName("online")
    val count: Int,
    val max: Int,
    @SerializedName("sample")
    val list: List<Player>?,
)