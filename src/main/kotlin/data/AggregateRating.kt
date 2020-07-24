package data

import com.google.gson.annotations.SerializedName

data class AggregateRating(
    @SerializedName("ratingvalue") val ratingValue: Double,
    @SerializedName("ratingcount") val ratingCount: Int,
    @SerializedName("bestrating") val bestRating: Int
)