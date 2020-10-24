package services

import data.egs.*
import data.egs.GiveAwayGames.toGiveAwayGame
import io.ktor.client.request.get
import io.ktor.util.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.egsDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class EpicGamesService(database: Database) : BaseService(database) {

    private val baseUrl = "https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=en-US"

    suspend fun loadDistinct(): List<GiveAwayGame>? {
        val cachedGames = transaction(database) {
            GiveAwayGames.selectAll().map { it.toGiveAwayGame() }
        }
        val newGames = loadFromNetwork()
        if (!newGames.isSame(cachedGames)) {
            transaction(database) {
                GiveAwayGames.deleteAll()
                newGames.forEach { game ->
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
            return newGames
        } else {
            return null
        }
    }

    suspend fun load(forceRefresh: Boolean = false): List<GiveAwayGame> {
        val cachedGames: List<GiveAwayGame>? = null
        // fixme: rework caching
//        if (forceRefresh) null else transaction(database) {
//            GiveAwayGames.selectAll().map { it.toGiveAwayGame() }.let { games ->
//                games.firstOrNull()?.lastUpdated?.let {
//                    if (ChronoUnit.HOURS.between(LocalDateTime.now(), it) < 3) games else null
//                }
//            }
//        }
        return if (cachedGames == null) {
            val new = loadFromNetwork()
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
            new
        } else {
            cachedGames
        }
    }

    private suspend fun loadFromNetwork(): List<GiveAwayGame> {
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
            emptyList()
        }
    }

    private fun Iterable<GiveAwayGame>.isSame(other: Iterable<GiveAwayGame>): Boolean {
        if (this.count() != other.count()) {
            return false
        }
        return this.all { item -> other.any { it.id == item.id } }
    }
}