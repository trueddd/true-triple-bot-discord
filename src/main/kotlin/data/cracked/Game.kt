package data.cracked

import com.google.gson.annotations.SerializedName
import java.util.*

data class Game(
    @SerializedName("crackDate")
    val crackDate: Date,
    @SerializedName("_id")
    val id: String,
    @SerializedName("image")
    val image: String,
    @SerializedName("isAAA")
    val isAAA: Boolean,
    @SerializedName("ratings")
    val ratings: Int,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("title")
    val title: String
)