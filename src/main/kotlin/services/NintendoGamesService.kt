package services

import data.nintendo.Game
import data.nintendo.Response
import io.ktor.client.request.*
import io.ktor.client.statement.*

class NintendoGamesService : BaseGamesService<Game>() {

    private val baseUrl = "https://searching.nintendo-europe.com/ru/select?q=*&fq=type%3AGAME%20AND%20((price_has_discount_b%3A%22true%22)%20AND%20(price_lowest_f%3A%5B1000%20TO%201499.99%5D%20OR%20price_lowest_f%3A%5B1500%20TO%202499.99%5D%20OR%20price_lowest_f%3A%5B2500%20TO%203999.99%5D%20OR%20price_lowest_f%3A%5B4000%20TO%20*%5D))%20AND%20sorting_title%3A*%20AND%20*%3A*&sort=deprioritise_b%20asc%2C%20popularity%20asc&start=0&rows=24&wt=json&bf=linear(ms(priority%2CNOW%2FHOUR)%2C1.1e-11%2C0)&bq=!deprioritise_b%3Atrue%5E999&json.wrf=nindo.net.jsonp.jsonpCallback_1705_2999999523163"

    override suspend fun load(regions: List<String>): Map<String, List<Game>>? {
        return try {
            val rawResponse = loadFromNetwork()
            val rawJson = trimNintendoResponseWrap(rawResponse)
            val gamesResponse = gson.getAdapter(Response::class.java).fromJson(rawJson)
            val games = gamesResponse.response.docs
            regions.associateWith { games }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun loadFromNetwork(): String {
        return client.get<HttpResponse>(baseUrl).readText()
    }

    private fun trimNintendoResponseWrap(raw: String): String {
        return raw
            .substringAfter("(")
            .substringBeforeLast(")")
    }
}
