package data

import com.google.gson.annotations.SerializedName

data class CseThumbnail(
    @SerializedName("src") val src: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)