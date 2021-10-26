package data.cracked

import com.google.gson.annotations.SerializedName

data class Response(
    @SerializedName("hot_games")
    val list: List<Game>,
)
