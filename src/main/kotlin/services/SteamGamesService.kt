package services

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import data.adapter.SteamPriceOverviewAdapter
import data.steam.GamePriceInfo
import data.steam.PriceOverview
import data.steam.SteamGame
import data.steam.SteamGamePrice
import io.ktor.client.request.*
import org.jsoup.Jsoup

class SteamGamesService : BaseGamesService<SteamGame>() {

    private val storeUrl = "https://store.steampowered.com/specials#p=0&tab=TopSellers"
    private val pricesUrl = "https://store.steampowered.com/api/appdetails/"

    override fun GsonBuilder.configGson() {
        registerTypeAdapter(PriceOverview::class.java, SteamPriceOverviewAdapter())
    }

    override suspend fun load(regions: List<String>): Map<String, List<SteamGame>>? {
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
                val discount = it.getElementsByClass("discount_pct").firstOrNull()?.text()
                SteamGame(id, name, SteamGamePrice(currentPrice, originalPrice, discount ?: "-0%"), url)
            }
            val steamGamesWithRegions = mutableMapOf<String, List<SteamGame>>()
            val gameIds = games.joinToString(",") { it.id }
            regions.forEach { region ->
                val gamesPrices = client.get<String>(pricesUrl) {
                    parameter("cc", region)
                    parameter("filters", "price_overview")
                    parameter("appids", gameIds)
                }.let {
                    // fixme: clean up this shit
                    val raw = it.replace("\"data\":[]", "\"data\":null")
                    val type = TypeToken.getParameterized(Map::class.java, String::class.java, GamePriceInfo::class.java)
                    gson.fromJson<Map<String, GamePriceInfo>>(raw, type.type)
                }
                steamGamesWithRegions[region] = games.map {
                    val regionalPrice = gamesPrices[it.id]?.data?.priceOverview
                    val finalPrice = if (regionalPrice?.final != null) {
                        SteamGamePrice(regionalPrice.final, regionalPrice.initial, "-${regionalPrice.discountPercent}%")
                    } else {
                        SteamGamePrice(null, null, it.price.discount)
                    }
                    SteamGame(it.id, it.name, finalPrice, it.url)
                }
            }
            steamGamesWithRegions
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error steam loading")
            null
        }
    }
}