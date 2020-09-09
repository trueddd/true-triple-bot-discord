package data

import com.google.gson.annotations.SerializedName

data class Movie(
    @SerializedName("image") val image: String?,
    @SerializedName("actors") val actors: String?,
    @SerializedName("director") val director: String?,
    @SerializedName("alternativeheadline") val alternativeHeadline: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("musicby") val musicBy: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("producer") val producer: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("datecreated") val dateCreated: String?
)