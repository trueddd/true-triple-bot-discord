package services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import data.cracked.Game
import data.cracked.GamesResponse
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.sql.Database
import java.text.DateFormat

class CrackedGamesService(database: Database) : BaseService<Game>(database) {

    private val socketClient = HttpClient {
        install(WebSockets)
    }

    private val gson: Gson by lazy {
        GsonBuilder()
            .setDateFormat(DateFormat.FULL)
            .create()
    }

    suspend fun loadFlow(): Flow<List<Game>?> {
        return flow {
            socketClient.webSocket("wss://crackwatch.com/sockjs/653/kocd4aig/websocket") {
                while (true) {
                    val frame = incoming.receive()
                    val frameContent = (frame as? Frame.Text)?.readText() ?: "qwe"
                    if (frameContent.startsWith("a[\"{\\\"server_id")) {
                        send("\"{\\\"msg\\\":\\\"connect\\\",\\\"version\\\":\\\"1\\\",\\\"support\\\":[\\\"1\\\",\\\"pre2\\\",\\\"pre1\\\"]}\"")
                    }
                    if (frameContent.startsWith("a[\"{\\\"msg\\\":\\\"connected\\\",")) {
                        send("\"{\\\"msg\\\":\\\"method\\\",\\\"method\\\":\\\"games.page\\\",\\\"params\\\":[{\\\"page\\\":0,\\\"orderType\\\":\\\"crackDate\\\",\\\"orderDown\\\":true,\\\"search\\\":\\\"\\\",\\\"unset\\\":0,\\\"released\\\":0,\\\"cracked\\\":0,\\\"isAAA\\\":0}],\\\"id\\\":\\\"1\\\"}\"")
                    }
                    if (frameContent.startsWith("a[\"{\\\"msg\\\":\\\"result\\\"")) {
                        val trimmed = frameContent
                            .removePrefix("a[\"")
                            .removeSuffix("\"]")
                            .replace("\\", "")
                        try {
                            val games = gson.fromJson(trimmed, GamesResponse::class.java)
                            emit(games.result.games)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            emit(null)
                        } finally {
                            close(CloseReason(CloseReason.Codes.NORMAL, "regular shit"))
                        }
                    }
                }
            }
        }
    }

    override suspend fun load(regions: List<String>): Map<String, List<Game>>? {
        // TODO: refactor services to flow
        return null
    }
}