import data.egs.Element
import data.egs.FreeGamesResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get

class EpicGamesService {

    private val client by lazy {
        HttpClient(OkHttp) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
    }

    private val baseUrl = "https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=en-US"

    suspend fun load(): List<Element> {
        return try {
            val response = client.get<FreeGamesResponse>(baseUrl)
            response.data.catalog.searchStore.elements
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}