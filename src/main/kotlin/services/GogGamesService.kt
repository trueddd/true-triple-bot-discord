package services

import data.gog.GogOnSaleResponse
import data.gog.Product
import io.ktor.client.request.*
import org.jetbrains.exposed.sql.Database

class GogGamesService(database: Database) : BaseService(database) {

    private val storeUrl = "https://www.gog.com/games/ajax/filtered?mediaType=game&page=1&price=discounted&sort=popularity"

    suspend fun loadGames(): List<Product>? {
        return try {
            val response = client.get<GogOnSaleResponse>(storeUrl)
            response.products
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}