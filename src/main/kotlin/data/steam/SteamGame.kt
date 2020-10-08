package data.steam

data class SteamGame(
    val id: String,
    val name: String,
    val price: SteamGamePrice,
    val url: String
)

data class SteamGamePrice(
    val currentPrice: String?,
    val originalPrice: String?,
    val discount: String
)