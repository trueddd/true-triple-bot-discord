import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.core.on
import db.GuildsManager
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.event.Level
import utils.AppEnvironment

@ExperimentalStdlibApi
@InternalAPI
@KtorExperimentalAPI
fun main(args: Array<String>) {
    embeddedServer(Netty, port = AppEnvironment.getPort(), module = Application::module).start(wait = true)
}

@ExperimentalStdlibApi
@InternalAPI
@KtorExperimentalAPI
fun Application.module() {

    val guildsManager = GuildsManager()
    val epicGamesService = EpicGamesService()

    GlobalScope.launch {
        val client = Kord(AppEnvironment.getBotSecret())
        val dispatcher = Dispatcher(client)

        client.on<ReactionAddEvent> {
            if (guildsManager.getMoviesListChannel(guildId?.value ?: "") == channel.id.value && emoji.name == "✅") {
                println("check mark detected")
                val watchedChannelId = guildsManager.getWatchedMoviesChannelId(guildId?.value ?: "") ?: return@on
                val movieMessage = message.asMessage()
                message.delete()
                client.rest.channel.createMessage(watchedChannelId) {
                    content = movieMessage.content.replace("✅", "").trim()
                }
            }
        }
        client.on<MessageCreateEvent> {
            if (!message.content.startsWith("ttb!")) {
                return@on
            }
            when (val messageText = message.content.removePrefix("ttb!")) {
                "top" -> {
                    if (guildsManager.getMoviesListChannel(guildId?.value ?: "") == null)
                        return@on
                    dispatcher.showMoviesToRoll(message.channel)
                }
                "movies-help" -> dispatcher.showMoviesHelp(message.channel)
                "set-movies" -> guildsManager.setMoviesListChannel(guildId?.value ?: "", message.channelId.value)
                "set-watched" -> guildsManager.setWatchedMoviesListChannel(guildId?.value ?: "", message.channelId.value)
                "set-games" -> guildsManager.setGamesChannel(guildId?.value ?: "", message.channelId.value)
                "egs-free" -> {
                    if (guildsManager.getGamesChannelId(guildId?.value ?: "") == null)
                        return@on
                    val games = epicGamesService.load()
                    dispatcher.showGames(message.channel, games)
                }
                else -> when {
                    messageText.startsWith("roll") -> {
                        if (guildsManager.getMoviesListChannel(guildId?.value ?: "") == null)
                            return@on
                        dispatcher.rollMovies(message.channel, messageText.contains("-s"))
                    }
                    messageText.startsWith("search") -> {
                        dispatcher.searchForMovie(message.channel, messageText.removePrefix("search").trim())
                    }
                }
            }
        }

        client.login()
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        anyHost()
    }

    routing {

        get("/") {
            call.respond(HttpStatusCode.OK, "Server is up")
        }
    }
}