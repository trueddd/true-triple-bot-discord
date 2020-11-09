package services

import data.cracked.Game
import io.ktor.client.request.*
import org.jetbrains.exposed.sql.Database

class CrackedGamesService(database: Database) : BaseGamesService<Game>(database) {

    private val baseUrl = "https://api.crackwatch.com/api/games"

    override suspend fun load(regions: List<String>): Map<String, List<Game>>? {
        return try {
            val response = client.get<List<Game>>(baseUrl) {
                parameter("page", 0)
                parameter("sort_by", "crack_date")
                parameter("is_cracked", true)
            }
            val gamesWithRegions = mutableMapOf<String, List<Game>>()
            regions.forEach { code ->
                gamesWithRegions[code] = response
            }
            gamesWithRegions
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}