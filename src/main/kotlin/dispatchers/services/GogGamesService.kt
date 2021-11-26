package dispatchers.services

import data.gog.GogOnSaleResponse
import data.gog.Product
import data.gog.prices.GogPricesResponse
import data.gog.prices.Price
import io.ktor.client.request.*

class GogGamesService : BaseGamesService<Product>() {

    private val storeUrl = "https://www.gog.com/games/ajax/filtered?mediaType=game&page=1&price=discounted&sort=popularity"
    private val pricesUrl = "https://api.gog.com/products/prices"

    override suspend fun load(regions: List<String>): Map<String, List<Product>>? {
        return try {
            val response = client.get<GogOnSaleResponse>(storeUrl)
            val gamesWithRegions = mutableMapOf<String, List<Product>>()
            regions.forEach { code ->
                val prices = client.get<GogPricesResponse>(pricesUrl) {
                    parameter("countryCode", code)
                    parameter("ids", response.products.joinToString(",") { it.id.toString() })
                }
                gamesWithRegions[code] = response.products.map { product ->
                    product.copy().apply {
                        val local = prices.embedded.items
                            .firstOrNull { i -> i.embedded.product.id == product.id }
                            ?.embedded?.prices?.let { code.primaryCurrencyPrices(it) } ?: return@apply
                        localPrice = Product.LocalPrice(
                            local.basePrice.formatPrice(local.currency.code),
                            local.finalPrice.formatPrice(local.currency.code),
                        )
                    }
                }
            }
            gamesWithRegions
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun String.formatPrice(code: String): String {
        return this.replace(code, "")
            .trim()
            .toInt()
            .div(100.0f)
            .let { String.format("%.2f %s", it, code.formatCurrency()) }
    }

    private fun String.formatCurrency(): String {
        return when (this) {
            "USD" -> "$"
            "RUB" -> "₽"
            else -> this
        }
    }

    private fun String.primaryCurrencyPrices(prices: List<Price>): Price {
        return when (this) {
            "by" -> "USD"
            "ru" -> "RUB"
            else -> null
        }?.let {
            prices.firstOrNull { pr -> pr.currency.code == it }
        } ?: prices.first()
    }
}