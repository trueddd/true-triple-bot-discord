package services

import db.GuildsManager
import dev.kord.common.entity.DiscordPartialGuild
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.on
import dev.kord.rest.route.Position
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dispatchers.*
import utils.AppEnvironment
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MainBot(
    private val guildsManager: GuildsManager,
    private val epicGamesService: EpicGamesService,
    private val steamGamesService: SteamGamesService,
    private val gogGamesService: GogGamesService,
    private val crackedGamesService: CrackedGamesService,
    minecraftService: MinecraftService,
    client: Kord
) : BaseBot(client) {

    private val moviesDispatcher = MoviesDispatcher(guildsManager, client)

    private val gamesDispatcher = GamesDispatcher(guildsManager, epicGamesService, steamGamesService, gogGamesService, crackedGamesService, client)

    private val commonDispatcher = CommonDispatcher(guildsManager, client)

    private val roleGetterDispatcher = RoleGetterDispatcher(guildsManager, client)

    private val minecraftDispatcher = MinecraftDispatcher(guildsManager, minecraftService, client)

    private val addReactionListeners: Set<ReactionAddListener> by lazy {
        setOf(moviesDispatcher, roleGetterDispatcher)
    }

    private val removeReactionListeners: Set<ReactionRemoveListener> by lazy {
        setOf(roleGetterDispatcher)
    }

    private val botPrefixPattern = Regex("^${AppEnvironment.BOT_PREFIX}.*", RegexOption.DOT_MATCHES_ALL)
    private val gamesPattern = Regex("^${gamesDispatcher.getPrefix()}.*", RegexOption.DOT_MATCHES_ALL)
    private val moviesPattern = Regex("^${moviesDispatcher.getPrefix()}.*", RegexOption.DOT_MATCHES_ALL)
    private val minecraftPattern = Regex("^${minecraftDispatcher.getPrefix()}.*", RegexOption.DOT_MATCHES_ALL)

    override suspend fun attach() {
        client.rest.channel
        client.on<MemberLeaveEvent> {
            if (user.id.value == client.selfId.value) {
                guildsManager.removeGuild(guildId.asString)
            }
        }
        client.on<GuildDeleteEvent> {
            guildsManager.removeGuild(guildId.asString)
        }
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
            val messageText = message.content.removePrefix(AppEnvironment.BOT_PREFIX)
            val dispatcher = when {
                gamesPattern.matches(messageText) -> gamesDispatcher
                moviesPattern.matches(messageText) -> moviesDispatcher
                minecraftPattern.matches(messageText) -> minecraftDispatcher
                else -> commonDispatcher
            }
            val trimmedMessage = if (dispatcher is CommonDispatcher) {
                messageText
            } else {
                messageText.removePrefix(dispatcher.getPrefix()).removePrefix(BaseDispatcher.PREFIX_DELIMITER)
            }
            dispatcher.onMessageCreate(this, trimmedMessage)
        }

        if (AppEnvironment.isProdEnv()) {
            client.on<ReadyEvent> {
                // schedule cracked games notifications
//                launch {
//                    delay(countDelayTo(15, tag = "cracked"))
//                    do {
//                        val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
//                        val now = System.currentTimeMillis()
//                        val crackedGames = crackedGamesService.load()?.values?.firstOrNull()
//                            ?.filter { it.crackDate.time > now - Duration.ofDays(1).toMillis() }
//                        gamesGuildsAndChannels.forEach { (_, channelId, _) ->
//                            try {
//                                gamesDispatcher.showCrackedGames(Snowflake(channelId), crackedGames)
//                            } catch (e: Exception) {
//                                e.printStackTrace()
//                            }
//                        }
//
//                        delay(Duration.ofHours(24).toMillis())
//                    } while (true)
//                }
                // schedule GOG notifications
                launch {
                    delay(countDelayTo(16, tag = "gog"))
                    do {
                        val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
                        val gogGames = gogGamesService.load(gamesGuildsAndChannels.map { it.region }.distinct())
                        gamesGuildsAndChannels.forEach {
                            val gogGamesForRegion = gogGames?.get(it.region)
                            try {
                                gamesDispatcher.showGogGames(Snowflake(it.channelId), gogGamesForRegion)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        delay(Duration.ofHours(24).toMillis())
                    } while (true)
                }
                // schedule Steam notifications
                launch {
                    delay(countDelayTo(17, tag = "steam"))
                    do {
                        val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
                        val steamGames = steamGamesService.load(gamesGuildsAndChannels.map { it.region }.distinct())
                        gamesGuildsAndChannels.forEach {
                            val steamGamesForRegion = steamGames?.get(it.region)
                            try {
                                gamesDispatcher.showSteamGames(Snowflake(it.channelId), steamGamesForRegion)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        delay(Duration.ofHours(24).toMillis())
                    } while (true)
                }
                // schedule Epic Games Store notifications
                launch {
                    delay(countDelayTo(18, tag = "egs"))
                    do {
                        val gamesGuildsAndChannels = guildsManager.getGamesChannelsIds()
                        val egsGames = epicGamesService.load(gamesGuildsAndChannels.map { it.region }.distinct())
                        gamesGuildsAndChannels.forEach {
                            val egsGamesForRegion = egsGames?.get(it.region)
                            try {
                                gamesDispatcher.showEgsGames(Snowflake(it.channelId), egsGamesForRegion)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        delay(Duration.ofHours(24).toMillis())
                    } while (true)
                }
            }
        }
    }

    suspend fun checkGuilds() {
        val guilds = getAllGuilds()
        guildsManager.removeGuildsIgnore(guilds.map { it.id.asString })
    }

    private suspend fun getAllGuilds(): List<DiscordPartialGuild> {
        val guilds = mutableListOf<DiscordPartialGuild>()
        while (true) {
            val chunk = client.rest.user.getCurrentUserGuilds(if (guilds.isEmpty()) null else Position.After(guilds.last().id))
            if (chunk.isEmpty()) {
                break
            }
            guilds.addAll(chunk)
        }
        return guilds
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