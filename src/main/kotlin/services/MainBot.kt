package services

import Scheduler
import db.GuildsManager
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.DiscordPartialGuild
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.interaction.role
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.subCommand
import dev.kord.rest.route.Position
import dispatchers.*
import utils.AppEnvironment
import utils.Commands

@KordPreview
class MainBot(
    private val guildsManager: GuildsManager,
    private val epicGamesService: EpicGamesService,
    private val steamGamesService: SteamGamesService,
    private val gogGamesService: GogGamesService,
    private val crackedGamesService: CrackedGamesService,
    client: Kord
) : BaseBot(client) {

    private val gamesDispatcher = GamesDispatcher(
        guildsManager,
        epicGamesService,
        steamGamesService,
        gogGamesService,
        crackedGamesService,
        client,
    )

    private val commonDispatcher = CommonDispatcher(guildsManager, client)

    private val roleGetterDispatcher = RoleGetterDispatcher(guildsManager, client)

    private val addReactionListeners: Set<ReactionAddListener> by lazy {
        setOf(roleGetterDispatcher)
    }

    private val removeReactionListeners: Set<ReactionRemoveListener> by lazy {
        setOf(roleGetterDispatcher)
    }

    private val scheduler: Scheduler by lazy {
        Scheduler(guildsManager, epicGamesService, steamGamesService, gogGamesService, crackedGamesService, client)
    }

    override suspend fun attach() {
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
        client.on<InteractionCreateEvent> {
            when (interaction.data.data.name.value) {
                "games" -> gamesDispatcher.onInteractionReceived(interaction)
                else -> commonDispatcher.onInteractionReceived(interaction)
            }
        }

        if (AppEnvironment.isProdEnv()) {
            client.on<ReadyEvent> {
                scheduler.scheduleCracked()
                scheduler.scheduleGog()
                scheduler.scheduleSteam()
                scheduler.scheduleEgs()
            }
        }

        createSlashCommands()
    }

    private suspend fun createSlashCommands() {
        if (AppEnvironment.isProdEnv()) {
            client.createGlobalApplicationCommands {
                input(Commands.GAMES, "Games related commands") {
                    subCommand(Commands.Games.EGS, "List all free games promotions from EGS")
                    subCommand(Commands.Games.STEAM, "List top selling games from Steam")
                    subCommand(Commands.Games.GOG, "List currently the most popular games from GOG with discounts")
                    subCommand(Commands.Games.CRACKED, "List of last cracked games")
                    subCommand(Commands.Games.SET, "Schedule game notifications in selected channel") {
                        channel("channel", "Channel where to send game notifications") {
                            required = true
                        }
                    }
                    subCommand(Commands.Games.UNSET, "Cancel game notifications")
                }
                input(Commands.Common.LOCALE, "Sets guild locale to help bot send game notifications with proper currency prices") {
                    string("region", "Region abbreviation (e.g. `ru`, `ua` or `en`). For more info, check this standard ISO 639.") {
                        required = true
                    }
                }
                input(Commands.Common.POLL, "Create a poll") {
                    string("text", "Poll subject") {
                        required = true
                    }
                    string("url", "URL of picture that will be attached to poll") {
                        required = false
                    }
                }
                input(Commands.Common.ROLE_GETTER, "Bind role receiving to message reaction") {
                    role("role", "Role to give") {
                        required = true
                    }
                    string("emote", "Role will be given when user reacted to message with this emote (enter emote, not it\'s name)") {
                        required = true
                    }
                    string("message", "ID of message which reactions should be observed") {
                        required = true
                    }
                }
            }
        } else {
            client.createGuildChatInputCommand(
                Snowflake("884176842783879189"),
                Commands.Games.CRACKED,
                "List of last cracked games"
            )
        }
    }

    suspend fun checkGuilds() {
        val guilds = getAllGuilds()
        println(guilds.map { it.name })
        guildsManager.syncGuilds(guilds.map { it.id.asString })
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