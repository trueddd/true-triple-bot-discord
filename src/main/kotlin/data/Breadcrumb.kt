package data

import com.google.gson.annotations.SerializedName

data class Breadcrumb(
    @SerializedName("title") val title: String,
    @SerializedName("url") val url: String
)