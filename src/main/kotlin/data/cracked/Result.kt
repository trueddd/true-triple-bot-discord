package data.cracked

import com.google.gson.annotations.SerializedName

data class Result(
    @SerializedName("games")
    val games: List<Game>
)