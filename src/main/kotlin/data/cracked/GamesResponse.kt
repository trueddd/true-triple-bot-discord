package data.cracked

import com.google.gson.annotations.SerializedName

data class GamesResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("msg")
    val msg: String,
    @SerializedName("result")
    val result: Result
)