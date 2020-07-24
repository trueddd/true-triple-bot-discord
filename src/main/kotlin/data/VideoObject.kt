package data

import com.google.gson.annotations.SerializedName

data class VideoObject(
    @SerializedName("duration") val duration: String,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("isfamilyfriendly") val isFamilyFriendly: Boolean,
    @SerializedName("uploaddate") val uploadDate: String,
    @SerializedName("videoquality") val videoQuality: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("thumbnailurl") val thumbnailUrl: String
)