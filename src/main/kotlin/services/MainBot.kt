package services

import Scheduler
import db.GuildsManager
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.DiscordPartialGuild
import dev.kord.common.entity.InteractionResponseType
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.on
import dev.kord.gateway.Event
import dev.kord.gateway.InteractionCreate
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.json.request.InteractionApplicationCommandCallbackData
import dev.kord.rest.json.request.InteractionResponseCreateRequest
import dev.kord.rest.json.request.MultipartFollowupMessageCreateRequest
import dev.kord.rest.route.Position
import dispatchers.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import utils.AppEnvironment

@KordPreview
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

    private val scheduler: Scheduler by lazy {
        Scheduler(guildsManager, epicGamesService, steamGamesService, gogGamesService, crackedGamesService, client)
    }

    private val botPrefixPattern = Regex("^${AppEnvironment.BOT_PREFIX}.*", RegexOption.DOT_MATCHES_ALL)
    private val gamesPattern = Regex("^${gamesDispatcher.getPrefix()}.*", RegexOption.DOT_MATCHES_ALL)
    private val moviesPattern = Regex("^${moviesDispatcher.getPrefix()}.*", RegexOption.DOT_MATCHES_ALL)
    private val minecraftPattern = Regex("^${minecraftDispatcher.getPrefix()}.*", RegexOption.DOT_MATCHES_ALL)

    override suspend fun attach() {
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
            println("Message: ${message.content}. From ${message.getGuild().id.asString}")
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
        client.on<InteractionCreateEvent> {
            kord.rest.interaction.createInteractionResponse(
                interaction.id,
                interaction.token,
                InteractionResponseCreateRequest(
                    InteractionResponseType.ChannelMessageWithSource,
                    Optional.Value(
                        InteractionApplicationCommandCallbackData(
                            content = Optional.Value(
                                "Hello :)"
                            )
                        )
                    )
                )
            )
        }

        if (AppEnvironment.isProdEnv()) {
            client.on<ReadyEvent> {
//                scheduler.scheduleCracked()
                scheduler.scheduleGog()
                scheduler.scheduleSteam()
                scheduler.scheduleEgs()
            }
        }

        if (AppEnvironment.isTestEnv()) {
            println("Slash commands")
            createSlashCommands()
        }
    }

    private suspend fun createSlashCommands() {
//        client.on<InteractionCreateEvent> {
//            println("Interaction: ${this.interaction.command}")
//        }
        client.createGuildChatInputCommand(Snowflake("884176842783879189"), "test", "description")
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
}