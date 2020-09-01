package data.steam

data class SteamGame(
    val name: String,
    val currentPrice: String?,
    val originalPrice: String?,
    val discount: String?,
    val url: String
)