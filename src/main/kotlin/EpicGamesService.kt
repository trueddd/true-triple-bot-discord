import data.egs.*
import data.egs.GiveAwayGames.toGiveAwayGame
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.util.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.egsDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@InternalAPI
class EpicGamesService(
    private val database: Database
) {

    private val client by lazy {
        HttpClient(OkHttp) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
    }

    private val baseUrl = "https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=en-US"

    suspend fun load(forceRefresh: Boolean = false): List<GiveAwayGame> {
        val cachedGames = if (forceRefresh) null else transaction(database) {
            GiveAwayGames.selectAll().map { it.toGiveAwayGame() }.let { games ->
                games.firstOrNull()?.lastUpdated?.let {
                    if (ChronoUnit.HOURS.between(LocalDateTime.now(), it) < 3) games else null
                }
            }
        }
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
            elements.map {
                val dates = it.promotions?.current?.firstOrNull()?.offers?.firstOrNull()?.let { offer -> offer.startDate.egsDate() to offer.endDate.egsDate() }
                    ?: it.promotions?.upcoming?.firstOrNull()?.offers?.minByOrNull { item -> item.startDate }?.let { offer -> offer.startDate.egsDate() to offer.endDate.egsDate() }
                GiveAwayGame(it.id, it.title, dates?.let { d -> OfferDates(d.first.toLocalDateTime(), d.second.toLocalDateTime()) }, LocalDateTime.now(), it.productSlug)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}