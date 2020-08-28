package data.egs

import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("Catalog")
    val catalog: Catalog
)