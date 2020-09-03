import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.gateway.ReadyEvent
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.core.on
import data.egs.GiveAwayGames
import db.Guilds
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.event.Level
import services.EpicGamesService
import services.SteamGamesService
import utils.AppEnvironment
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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

    val database: Database = run {
        val uri = URI(AppEnvironment.getDatabaseUrl())
        val username = uri.userInfo.split(":")[0]
        val password = uri.userInfo.split(":")[1]
        val dbUrl = "jdbc:postgresql://${uri.host}:${uri.port}${uri.path}?sslmode=require"
        Database.connect(dbUrl, "org.postgresql.Driver", username, password).also {
            println("Connecting DB at ${it.url}")
            transaction(it) {
                println("Creating tables")
                SchemaUtils.create(Guilds, GiveAwayGames)
            }
        }
    }
    val guildsManager = GuildsManager(database)
    val epicGamesService = EpicGamesService(database)
    val steamGamesService = SteamGamesService(database)

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
                    if (guildsManager.getMoviesListChannel(guildId?.value ?: "") == null) {
                        dispatcher.createErrorMessage(message.channelId.value, "Не установлен канал со списком фильмов (`ttb!set-movies` в канале с фильмами)")
                    } else {
                        dispatcher.showMoviesToRoll(message.channel)
                    }
                }
                "movies-help" -> dispatcher.showMoviesHelp(message.channel)
                "set-movies" -> {
                    val changed = guildsManager.setMoviesListChannel(guildId?.value ?: "", message.channelId.value)
                    dispatcher.markRequest(message, changed)
                }
                "unset-movies" -> {
                    val changed = guildsManager.setMoviesListChannel(guildId?.value ?: "", null)
                    dispatcher.markRequest(message, changed)
                }
                "set-watched" -> {
                    val changed = guildsManager.setWatchedMoviesListChannel(guildId?.value ?: "", message.channelId.value)
                    dispatcher.markRequest(message, changed)
                }
                "unset-watched" -> {
                    val changed = guildsManager.setWatchedMoviesListChannel(guildId?.value ?: "", null)
                    dispatcher.markRequest(message, changed)
                }
                "set-games" -> {
                    val changed = guildsManager.setGamesChannel(guildId?.value ?: "", message.channelId.value)
                    dispatcher.markRequest(message, changed)
                }
                "unset-games" -> {
                    val changed = guildsManager.setGamesChannel(guildId?.value ?: "", null)
                    dispatcher.markRequest(message, changed)
                }
                "egs-free" -> {
                    val games = epicGamesService.load()
                    dispatcher.showEgsGames(message.channel.id.value, games)
                }
                "steam" -> {
                    val region = guildsManager.getGuildRegion(message.getGuild().id.value)
                    val games = steamGamesService.loadGames(listOf(region ?: "en"))[region] ?: return@on
                    dispatcher.showSteamGames(message.channel.id.value, games)
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

        client.on<ReadyEvent> {
            launch {
                val startDelay = LocalDateTime.now()
                    .withHour(18)
                    .withMinute(0)
                    .withSecond(0)
                    .let {
                        val temp = if (LocalDateTime.now().hour >= 18) {
                            it.plusDays(1)
                        } else {
                            it
                        }
                        println("Next notify is scheduled on $it")
                        return@let ChronoUnit.MILLIS.between(LocalDateTime.now(), temp).also { d ->
                            println("Delay is $d")
                        }
                    }
                delay(startDelay)
                do {
                    val observing = guildsManager.getGamesChannelsIds()
                    epicGamesService.loadDistinct()?.let { games ->
                        observing
                            .forEach {
                                dispatcher.showEgsGames(it.second, games)
                            }
                    }
                    val guildsWithRegions = observing.map { it.first to (guildsManager.getGuildRegion(it.first) ?: "en") }
                    val steamGames = steamGamesService.loadGames(guildsWithRegions.map { it.second }.distinct())
                    observing.forEach { (guildId, channelId) ->
                        val region = guildsWithRegions.first { it.first == guildId }.second
                        val games = steamGames[region] ?: return@forEach
                        dispatcher.showSteamGames(channelId, games)
                    }

                    delay(Duration.ofHours(24).toMillis())
                } while (true)
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