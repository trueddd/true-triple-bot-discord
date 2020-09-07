package services

import Dispatcher
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.gateway.ReadyEvent
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.core.on
import db.GuildsManager
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import utils.createBotMessage
import java.awt.Color
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@ExperimentalStdlibApi
@KtorExperimentalAPI
@InternalAPI
class MainBot(
    guildsManager: GuildsManager,
    epicGamesService: EpicGamesService,
    steamGamesService: SteamGamesService,
    client: Kord,
    dispatcher: Dispatcher
) : BaseBot(guildsManager, epicGamesService, steamGamesService, client, dispatcher) {

    override suspend fun attach() {
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
            launch {
                if (message.content.trim()
                        .toLowerCase(Locale("ru"))
                        .startsWith("жыве беларусь")
                    && guildsManager.getGuildRegion(message.getGuild().id.value) == "by") {
                    message.channel.createBotMessage("<:flag:752634825818636407><:flag:752634825818636407> ЖЫВЕ! <:flag:752634825818636407><:flag:752634825818636407>", embedColor = Color.RED)
                }
            }
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
                    messageText.startsWith("pick") -> {
                        val options = messageText.removePrefix("pick").trim()
                            .split("\n")
                            .mapNotNull { if (it.isEmpty()) null else it }
                        if (options.isEmpty()) {
                            dispatcher.markRequest(message, false)
                            return@on
                        } else {
                            val chosen = options.randomOrNull() ?: run {
                                dispatcher.markRequest(message, false)
                                return@on
                            }
                            message.channel.createBotMessage(chosen, embedColor = Color.ORANGE)
                        }
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
    }
}