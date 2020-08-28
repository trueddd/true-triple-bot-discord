package data.egs

import com.google.gson.annotations.SerializedName

data class Promotions(
    @SerializedName("promotionalOffers")
    val current: List<PromotionalOffers>,
    @SerializedName("upcomingPromotionalOffers")
    val upcoming: List<PromotionalOffers>
)