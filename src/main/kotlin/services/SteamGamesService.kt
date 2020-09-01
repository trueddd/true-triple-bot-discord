package services

import data.steam.SteamGame
import io.ktor.client.request.*
import org.jetbrains.exposed.sql.Database
import org.jsoup.Jsoup

class SteamGamesService(database: Database) : BaseService(database) {

    private val baseUrl = "https://store.steampowered.com/specials#p=0&tab=TopSellers"

    suspend fun loadGames(): List<SteamGame> {
        return try {
            val response = client.get<String>(baseUrl) {
                header("Accept-Language", "ru-RU")
            }
            val document = Jsoup.parse(response)
            document.getElementById("TopSellersRows").children().map {
                val url = it.attr("href")
                val name = it.getElementsByClass("tab_item_name").first().text()
                val currentPrice = it.getElementsByClass("discount_final_price").firstOrNull()?.text()
                val originalPrice = it.getElementsByClass("discount_original_price").firstOrNull()?.text()
                val discount = it.getElementsByClass("discount_pct").first().text()
                SteamGame(name, currentPrice, originalPrice, discount, url)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error steam loading")
            emptyList()
        }
    }
}