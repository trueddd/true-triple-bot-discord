package data

import com.google.gson.annotations.SerializedName

data class GoogleSearchResponse(
    @SerializedName("kind") val kind: String,
    @SerializedName("url") val url: Url,
//    @SerializedName("queries") val queries: Queries,
//    @SerializedName("context") val context: Context,
//    @SerializedName("searchInformation") val searchInformation: SearchInformation,
    @SerializedName("items") val items: List<Item>
)