package db

data class GameChannel(
    val guildId: String,
    val channelId: String,
    val region: String,
)