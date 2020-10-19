package data.movies

import com.google.gson.annotations.SerializedName

data class GoogleSearchResponse(
    @SerializedName("kind") val kind: String,
    @SerializedName("url") val url: Url,
    @SerializedName("items") val items: List<Item>,
)