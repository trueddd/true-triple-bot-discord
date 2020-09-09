package services

import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.gateway.ReadyEvent
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.core.on
import db.GuildsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import services.dispatchers.*
import utils.createBotMessage
import java.awt.Color
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.regex.Pattern

class MainBot(
    guildsManager: GuildsManager,
    epicGamesService: EpicGamesService,
    steamGamesService: SteamGamesService,
    client: Kord
) : BaseBot(guildsManager, epicGamesService, steamGamesService, client) {

    private val moviesDispatcher = MoviesDispatcher(guildsManager, client)

    private val gamesDispatcher = GamesDispatcher(guildsManager, epicGamesService, steamGamesService, client)

    private val commonDispatcher = CommonDispatcher(client)

    private val messageListeners: Set<MessageCreateListener> by lazy {
        setOf(moviesDispatcher, gamesDispatcher, commonDispatcher)
    }

    private val reactionListeners: Set<ReactionAddListener> by lazy {
        setOf(moviesDispatcher)
    }

    private val botPrefixPattern = Pattern.compile("^$BOT_PREFIX.*").toRegex()

    override suspend fun attach() {
        val belPattern = Pattern.compile("(жыве(\\s+)беларусь)", Pattern.CASE_INSENSITIVE).toRegex()
        client.on<ReactionAddEvent> {
            reactionListeners.forEach {
                it.onReactionAdd(this)
            }
        }
        client.on<MessageCreateEvent> {
            launch {
                if (message.content.matches(belPattern)
                    && guildsManager.getGuildRegion(message.getGuild().id.value) == "by") {
                    message.channel.createBotMessage(BEL_RESPONSE, embedColor = Color.RED)
                }
            }
            if (!message.content.matches(botPrefixPattern)) {
                return@on
            }
            val messageText = message.content.removePrefix(BOT_PREFIX)
            messageListeners.firstOrNull { messageText.startsWith(it.getPrefix()) }?.let {
                it.onMessageCreate(this, messageText.removePrefix("${it.getPrefix()}${BaseDispatcher.PREFIX_DELIMITER}"))
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

    companion object {
        private const val FLAG_EMOJI = "<:flag:752634825818636407>"
        const val BEL_RESPONSE = "$FLAG_EMOJI$FLAG_EMOJI ЖЫВЕ! $FLAG_EMOJI$FLAG_EMOJI"
    }
}