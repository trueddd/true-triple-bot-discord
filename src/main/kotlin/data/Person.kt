package data

import com.google.gson.annotations.SerializedName

data class Person(
    @SerializedName("image") val image: String,
    @SerializedName("name") val name: String
)