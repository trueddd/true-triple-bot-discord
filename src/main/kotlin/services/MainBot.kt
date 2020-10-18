package services

import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.gateway.ReadyEvent
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.MessageDeleteEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.core.event.message.ReactionRemoveEvent
import com.gitlab.kordlib.core.on
import db.GuildsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import services.dispatchers.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MainBot(
    guildsManager: GuildsManager,
    epicGamesService: EpicGamesService,
    steamGamesService: SteamGamesService,
    client: Kord
) : BaseBot(guildsManager, epicGamesService, steamGamesService, client) {

    private val moviesDispatcher = MoviesDispatcher(guildsManager, client)

    private val gamesDispatcher = GamesDispatcher(guildsManager, epicGamesService, steamGamesService, client)

    private val commonDispatcher = CommonDispatcher(guildsManager, client)

    private val roleGetterDispatcher = RoleGetterDispatcher(guildsManager, client)

    private val addReactionListeners: Set<ReactionAddListener> by lazy {
        setOf(moviesDispatcher, roleGetterDispatcher)
    }

    private val removeReactionListeners: Set<ReactionRemoveListener> by lazy {
        setOf(roleGetterDispatcher)
    }

    private val botPrefixPattern = Regex("^$BOT_PREFIX.*", RegexOption.DOT_MATCHES_ALL)
    private val gamesPattern = Regex("^${gamesDispatcher.getPrefix()}.*", RegexOption.DOT_MATCHES_ALL)
    private val moviesPattern = Regex("^${moviesDispatcher.getPrefix()}.*", RegexOption.DOT_MATCHES_ALL)

    override suspend fun attach() {
        client.on<ReactionAddEvent> {
            addReactionListeners.forEach {
                it.onReactionAdd(this)
            }
        }
        client.on<ReactionRemoveEvent> {
            removeReactionListeners.forEach {
                it.onReactionRemove(this)
            }
        }
        client.on<MessageDeleteEvent> {
            roleGetterDispatcher.onMessageDelete(this)
        }
        client.on<MessageCreateEvent> {
            if (!message.content.matches(botPrefixPattern)) {
                return@on
            }
            val messageText = message.content.removePrefix(BOT_PREFIX)
            val dispatcher = when {
                gamesPattern.matches(messageText) -> gamesDispatcher
                moviesPattern.matches(messageText) -> moviesDispatcher
                else -> commonDispatcher
            }
            val trimmedMessage = if (dispatcher is CommonDispatcher) {
                messageText
            } else {
                messageText.removePrefix("${dispatcher.getPrefix()}${BaseDispatcher.PREFIX_DELIMITER}")
            }
            dispatcher.onMessageCreate(this, trimmedMessage)
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
                    val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
                    epicGamesService.load().let { games ->
                        gamesGuildsAndChannels.forEach {
                            gamesDispatcher.showEgsGames(it.second, games)
                        }
                    }
                    val guildsWithRegions = gamesGuildsAndChannels.map { it.first to (guildsManager.getGuildRegion(it.first) ?: "en") }
                    val steamGames = steamGamesService.loadGames(guildsWithRegions.map { it.second }.distinct())
                    gamesGuildsAndChannels.forEach { (guildId, channelId) ->
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