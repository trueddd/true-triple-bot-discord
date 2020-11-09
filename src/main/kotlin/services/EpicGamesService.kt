package services

import data.egs.*
import io.ktor.client.request.get
import io.ktor.util.*
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.egsDate
import utils.safeGet
import java.time.LocalDateTime

class EpicGamesService(database: Database) : BaseService<GiveAwayGame>(database) {

    private val baseUrl = "https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=en-US"

    // todo: rework caching
    override suspend fun load(regions: List<String>): Map<String, List<GiveAwayGame>>? {
        return null
    }

    override suspend fun loadResponse(regions: List<String>): ServiceResponse<Map<String, List<GiveAwayGame>>> {
        val response = try {
            client.get<FreeGamesResponse>(baseUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            return e.httpError()
        }
        val elements = response.data.catalog.searchStore.elements
        val new = elements.map { element ->
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
        return mutableMapOf<String, List<GiveAwayGame>>().apply {
            regions.map { this[it] = new }
        }.success()
    }
}