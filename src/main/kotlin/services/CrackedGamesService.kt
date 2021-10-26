package services

import com.google.gson.GsonBuilder
import data.cracked.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database

class CrackedGamesService(database: Database) : BaseGamesService<Game>(database) {

    private val baseUrl = "https://gamestatus.info/back/api/gameinfo/game/"

    override fun GsonBuilder.configGson() {
        setDateFormat("yyyy-MM-dd")
    }

    override suspend fun load(regions: List<String>): Map<String, List<Game>>? {
        return try {
            val response = client.get<Response>(baseUrl)
            val cracked = withContext(Dispatchers.Default) {
                response.list.take(9)
                    .filter { it.crackDate != null }
                    .sortedByDescending { it.crackDate }
            }
            val gamesWithRegions = mutableMapOf<String, List<Game>>()
            regions.forEach { code ->
                gamesWithRegions[code] = cracked
            }
            gamesWithRegions
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}