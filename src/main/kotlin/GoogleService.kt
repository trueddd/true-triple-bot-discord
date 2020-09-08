import data.GoogleSearchResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import java.io.Closeable
import java.lang.Exception

class GoogleService : Closeable {

    private val client by lazy {
        HttpClient(OkHttp) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
    }

    private val key = "AIzaSyCqqabmzaH4OVYpDtoZ00CjkU6T0wH-2KU"

    private val searchEngineId = "012140673412975639412:3qxfnzouyog"

    private val baseUrl = "https://www.googleapis.com/customsearch/v1"

    suspend fun load(query: String): GoogleSearchResponse? {
        return try {
            val response = client.get<GoogleSearchResponse>(baseUrl) {
                parameter("key", key)
                parameter("cx", searchEngineId)
                parameter("q", query)
            }
            println(response)
            response
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun close() {
        client.close()
    }
}