package services

import data.steam.GamePriceInfo
import data.steam.SteamGame
import io.ktor.client.request.*
import org.jetbrains.exposed.sql.Database
import org.jsoup.Jsoup

class SteamGamesService(database: Database) : BaseService(database) {

    private val storeUrl = "https://store.steampowered.com/specials#p=0&tab=TopSellers"
    private val pricesUrl = "https://store.steampowered.com/api/appdetails/"

    suspend fun loadGames(regions: List<String> = listOf("en")): Map<String, List<SteamGame>> {
        return try {
            val response = client.get<String>(storeUrl) {
                header("Accept-Language", "ru-RU")
            }
            val document = Jsoup.parse(response)
            val games = document.getElementById("TopSellersRows").children().map {
                val url = it.attr("href")
                val id = url.removePrefix("https://store.steampowered.com/app/").substringBefore("/")
                val name = it.getElementsByClass("tab_item_name").first().text()
                val currentPrice = it.getElementsByClass("discount_final_price").firstOrNull()?.text()
                val originalPrice = it.getElementsByClass("discount_original_price").firstOrNull()?.text()
                SteamGame(id, name, currentPrice, originalPrice, url)
            }
            val steamGamesWithRegions = mutableMapOf<String, List<SteamGame>>()
            val gameIds = games.joinToString(",") { it.id }
            regions.forEach { region ->
                val gamesPrices = client.get<Map<String, GamePriceInfo>>(pricesUrl) {
                    parameter("cc", region)
                    parameter("filters", "price_overview")
                    parameter("appids", gameIds)
                }
                steamGamesWithRegions[region] = games.map {
                    val price = gamesPrices[it.id]?.data?.priceOverview
                    SteamGame(it.id, it.name, price?.final ?: it.currentPrice, price?.initial ?: it.originalPrice, it.url)
                }
            }
            steamGamesWithRegions
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error steam loading")
            emptyMap()
        }
    }
}