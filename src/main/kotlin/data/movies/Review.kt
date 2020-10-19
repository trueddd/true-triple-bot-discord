package data.movies

import com.google.gson.annotations.SerializedName

data class Review(
    @SerializedName("reviewbody") val reviewBody: String,
    @SerializedName("headline") val headline: String
)