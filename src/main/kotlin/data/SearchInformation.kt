package data

import com.google.gson.annotations.SerializedName

data class SearchInformation(
    @SerializedName("searchTime") val searchTime: Double,
    @SerializedName("formattedSearchTime") val formattedSearchTime: Double,
    @SerializedName("totalResults") val totalResults: Int,
    @SerializedName("formattedTotalResults") val formattedTotalResults: String
)