package services

import data.egs.*
import io.ktor.client.request.get
import io.ktor.util.*
import org.jetbrains.exposed.sql.*
import utils.egsDate
import java.time.LocalDateTime

class EpicGamesService(database: Database) : BaseGamesService<GiveAwayGame>(database) {

    private val baseUrl = "https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=en-US"

    // todo: rework caching
    override suspend fun load(regions: List<String>): Map<String, List<GiveAwayGame>>? {
        val new = loadFromNetwork() ?: return null
        return mutableMapOf<String, List<GiveAwayGame>>().apply {
            regions.map { this[it] = new }
        }
    }

    private suspend fun loadFromNetwork(): List<GiveAwayGame>? {
        return try {
            println("Loading games from network")
            val response = client.get<FreeGamesResponse>(baseUrl)
            val elements = response.data.catalog.searchStore.elements
            elements
                .mapNotNull { element ->
                    val dates = element.promotions?.current?.firstOrNull()?.offers
                        ?.firstOrNull { it.discountSetting.discountPercentage == 0 }
                        ?.let { it.startDate.egsDate() to it.endDate.egsDate() }
                        ?: element.promotions?.upcoming?.firstOrNull()?.offers
                            ?.minByOrNull { item -> item.startDate }
                            ?.let { if (it.discountSetting.discountPercentage == 0) it else null }
                            ?.let { it.startDate.egsDate() to it.endDate.egsDate() }
                        ?: return@mapNotNull null
                    GiveAwayGame(
                        element.id,
                        element.title,
                        dates.let { OfferDates(it.first.toLocalDateTime(), it.second.toLocalDateTime()) },
                        LocalDateTime.now(),
                        element.productSlug
                    )
                }
                .sortedBy { it.promotion?.start }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}