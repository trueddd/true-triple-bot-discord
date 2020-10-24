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
import dispatchers.*
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MainBot(
    private val guildsManager: GuildsManager,
    private val epicGamesService: EpicGamesService,
    private val steamGamesService: SteamGamesService,
    private val gogGamesService: GogGamesService,
    client: Kord
) : BaseBot(client) {

    private val moviesDispatcher = MoviesDispatcher(guildsManager, client)

    private val gamesDispatcher = GamesDispatcher(guildsManager, epicGamesService, steamGamesService, gogGamesService, client)

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
            // schedule GOG notifications
            launch {
                delay(countDelayTo(16, tag = "gog"))
                do {
                    val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
                    val guildsWithRegions = gamesGuildsAndChannels.map { it.first to (guildsManager.getGuildRegion(it.first) ?: "en") }
                    val gogGames = gogGamesService.load(guildsWithRegions.map { it.second }.distinct())
                    gamesGuildsAndChannels.forEach { (guildId, channelId) ->
                        val region = guildsWithRegions.first { it.first == guildId }.second
                        val gogGamesForRegion = gogGames?.get(region)
                        gamesDispatcher.showGogGames(channelId, gogGamesForRegion)
                    }

                    delay(Duration.ofHours(24).toMillis())
                } while (true)
            }
            // schedule Steam notifications
            launch {
                delay(countDelayTo(17, tag = "steam"))
                do {
                    val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
                    val guildsWithRegions = gamesGuildsAndChannels.map { it.first to (guildsManager.getGuildRegion(it.first) ?: "en") }
                    val steamGames = steamGamesService.load(guildsWithRegions.map { it.second }.distinct())
                    gamesGuildsAndChannels.forEach { (guildId, channelId) ->
                        val region = guildsWithRegions.first { it.first == guildId }.second
                        val steamGamesForRegion = steamGames?.get(region)
                        gamesDispatcher.showSteamGames(channelId, steamGamesForRegion)
                    }
                    delay(Duration.ofHours(24).toMillis())
                } while (true)
            }
            // schedule Epic Games Store notifications
            launch {
                delay(countDelayTo(18, tag = "egs"))
                do {
                    val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
                    val guildsWithRegions = gamesGuildsAndChannels.map { it.first to (guildsManager.getGuildRegion(it.first) ?: "en") }
                    val egsGames = epicGamesService.load(guildsWithRegions.map { it.second }.distinct())
                    gamesGuildsAndChannels.forEach { (guildId, channelId) ->
                        val region = guildsWithRegions.first { it.first == guildId }.second
                        val egsGamesForRegion = egsGames?.get(region)
                        gamesDispatcher.showEgsGames(channelId, egsGamesForRegion)
                    }
                    delay(Duration.ofHours(24).toMillis())
                } while (true)
            }
        }
    }

    private fun countDelayTo(hour: Int, minutes: Int = 0, tag: String = ""): Long {
        return LocalDateTime.now()
            .withHour(hour)
            .withMinute(minutes)
            .withSecond(0)
            .let {
                val temp = if (LocalDateTime.now().hour >= hour) {
                    it.plusDays(1)
                } else {
                    it
                }
                println("Next notify is scheduled on $it ($tag)")
                return@let ChronoUnit.MILLIS.between(LocalDateTime.now(), temp).also { d ->
                    println("Delay is $d")
                }
            }
    }
}