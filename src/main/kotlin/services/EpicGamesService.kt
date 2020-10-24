package services

import data.egs.*
import io.ktor.client.request.get
import io.ktor.util.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.egsDate
import java.time.LocalDateTime

class EpicGamesService(database: Database) : BaseService<GiveAwayGame>(database) {

    private val baseUrl = "https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=en-US"

    // todo: rework caching
    override suspend fun load(regions: List<String>): Map<String, List<GiveAwayGame>>? {
        val new = loadFromNetwork() ?: return null
        transaction(database) {
            GiveAwayGames.deleteAll()
            new.forEach { game ->
                GiveAwayGames.insert {
                    it[id] = game.id
                    it[title] = game.title
                    it[offerStartDate] = game.promotion?.start
                    it[offerEndDate] = game.promotion?.end
                    it[lastUpdated] = game.lastUpdated
                    it[productSlug] = game.productSlug
                }
            }
        }
        return mutableMapOf<String, List<GiveAwayGame>>().apply {
            regions.map { this[it] = new }
        }
    }

    private suspend fun loadFromNetwork(): List<GiveAwayGame>? {
        return try {
            println("Loading games from network")
            val response = client.get<FreeGamesResponse>(baseUrl)
            val elements = response.data.catalog.searchStore.elements
            elements.map { element ->
                val dates = element.promotions?.current?.firstOrNull()?.offers?.firstOrNull()
                    ?.let { it.startDate.egsDate() to it.endDate.egsDate() }
                    ?: element.promotions?.upcoming?.firstOrNull()?.offers?.minByOrNull { item -> item.startDate }
                    ?.let { it.startDate.egsDate() to it.endDate.egsDate() }
                GiveAwayGame(
                    element.id,
                    element.title,
                    dates?.let { OfferDates(it.first.toLocalDateTime(), it.second.toLocalDateTime()) },
                    LocalDateTime.now(),
                    element.productSlug
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}