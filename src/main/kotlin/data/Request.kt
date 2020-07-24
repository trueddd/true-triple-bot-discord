package data

import com.google.gson.annotations.SerializedName

data class Request(
    @SerializedName("title") val title: String,
    @SerializedName("totalResults") val totalResults: Int,
    @SerializedName("searchTerms") val searchTerms: String,
    @SerializedName("count") val count: Int,
    @SerializedName("startIndex") val startIndex: Int,
    @SerializedName("inputEncoding") val inputEncoding: String,
    @SerializedName("outputEncoding") val outputEncoding: String,
    @SerializedName("safe") val safe: String,
    @SerializedName("cx") val cx: String
)