package data.cracked

import com.google.gson.annotations.SerializedName
import java.util.*

data class Game(
    @SerializedName("id")
    val id: String,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("crack_date")
    val crackDate: Date?,
)