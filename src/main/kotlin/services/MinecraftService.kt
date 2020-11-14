package services

import data.minecraft.StatusResponse
import io.ktor.client.request.*
import org.jetbrains.exposed.sql.Database

class MinecraftService(database: Database) : BaseService(database) {

    private val baseUrl = "https://eu.mc-api.net/v3/server/ping/"

    suspend fun load(serverIp: String): StatusResponse? {
        return try {
            client.get<StatusResponse>("$baseUrl$serverIp")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}