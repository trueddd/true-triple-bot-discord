package data.egs

import com.google.gson.annotations.SerializedName

data class PromotionalOffers(
    @SerializedName("promotionalOffers")
    val offers: List<Offer>
)