package data

import com.google.gson.annotations.SerializedName

data class MovieReview(
    @SerializedName("ratingstars") val ratingStars: Double,
    @SerializedName("directed_by") val directedBy: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("image_href") val imageHref: String,
    @SerializedName("name") val name: String,
    @SerializedName("genre") val genre: String,
    @SerializedName("starring") val starring: String,
    @SerializedName("best") val best: Int,
    @SerializedName("votes") val votes: Int,
    @SerializedName("originalrating") val originalRating: Double
)