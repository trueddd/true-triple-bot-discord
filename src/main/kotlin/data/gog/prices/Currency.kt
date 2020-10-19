package data.gog.prices


import com.google.gson.annotations.SerializedName

data class Currency(
    @SerializedName("code")
    val code: String
)