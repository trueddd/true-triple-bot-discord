package services

import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.gateway.ReadyEvent
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.core.on
import db.GuildsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import services.dispatchers.BaseDispatcher
import services.dispatchers.CommonDispatcher
import services.dispatchers.GamesDispatcher
import services.dispatchers.MoviesDispatcher
import utils.createBotMessage
import java.awt.Color
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class MainBot(
    guildsManager: GuildsManager,
    epicGamesService: EpicGamesService,
    steamGamesService: SteamGamesService,
    client: Kord
) : BaseBot(guildsManager, epicGamesService, steamGamesService, client) {

    private val moviesDispatcher = MoviesDispatcher(guildsManager, client)

    private val gamesDispatcher = GamesDispatcher(guildsManager, epicGamesService, steamGamesService, client)

    private val commonDispatcher = CommonDispatcher(client)

    private val dispatchers: Set<BaseDispatcher> by lazy {
        setOf(moviesDispatcher, gamesDispatcher, commonDispatcher)
    }

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
            if (!message.content.startsWith(BOT_PREFIX)) {
                return@on
            }
            val messageText = message.content.removePrefix(BOT_PREFIX)
            dispatchers.firstOrNull { messageText.startsWith(it.dispatcherPrefix) }?.let {
                it.onMessageCreate(this, messageText.removePrefix("${it.dispatcherPrefix}${BaseDispatcher.PREFIX_DELIMITER}"))
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
                                gamesDispatcher.showEgsGames(it.second, games)
                            }
                    }
                    val guildsWithRegions = observing.map { it.first to (guildsManager.getGuildRegion(it.first) ?: "en") }
                    val steamGames = steamGamesService.loadGames(guildsWithRegions.map { it.second }.distinct())
                    observing.forEach { (guildId, channelId) ->
                        val region = guildsWithRegions.first { it.first == guildId }.second
                        val games = steamGames[region] ?: return@forEach
                        gamesDispatcher.showSteamGames(channelId, games)
                    }

                    delay(Duration.ofHours(24).toMillis())
                } while (true)
            }
        }
    }
}