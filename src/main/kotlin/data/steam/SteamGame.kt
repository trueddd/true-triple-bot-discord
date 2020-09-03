package data.steam

data class SteamGame(
    val id: String,
    val name: String,
    val currentPrice: String?,
    val originalPrice: String?,
    val url: String
)