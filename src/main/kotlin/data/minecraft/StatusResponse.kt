package data.minecraft

data class StatusResponse(
    val online: Boolean,
    val players: Players?,
    val query: String?,
    val dns: Dns?,
)