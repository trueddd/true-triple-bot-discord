package data.minecraft

data class StatusResponse(
    val online: Boolean,
    val ip: String,
    val port: Int,
    val players: Players?,
    val hostname: String?,
)