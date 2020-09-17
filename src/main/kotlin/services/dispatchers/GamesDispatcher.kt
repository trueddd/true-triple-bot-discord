package services.dispatchers

import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import data.egs.GiveAwayGame
import data.steam.SteamGame
import db.GuildsManager
import services.EpicGamesService
import services.SteamGamesService
import utils.Commands
import utils.commandRegex
import java.awt.Color
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class GamesDispatcher(
    private val guildsManager: GuildsManager,
    private val epicGamesService: EpicGamesService,
    private val steamGamesService: SteamGamesService,
    client: Kord
) : BaseDispatcher(client), MessageCreateListener {

    override val dispatcherPrefix: String
        get() = "games"

    override fun getPrefix(): String {
        return dispatcherPrefix
    }

    private val help = Commands.Games.HELP.commandRegex()
    private val set = Commands.Games.SET.commandRegex()
    private val unset = Commands.Games.UNSET.commandRegex()
    private val egs = Commands.Games.EGS.commandRegex()
    private val steam = Commands.Games.STEAM.commandRegex()

    override suspend fun onMessageCreate(event: MessageCreateEvent, trimmedMessage: String) {
        val guildId = event.message.getGuild().id.value
        val channelId = event.message.channelId.value
        when {
            help.matches(trimmedMessage) -> {
                showHelp(event.message.channel)
            }
            set.matches(trimmedMessage) -> {
                val changed = guildsManager.setGamesChannel(guildId, channelId)
                respondWithReaction(event.message, changed)
            }
            unset.matches(trimmedMessage) -> {
                val changed = guildsManager.setGamesChannel(guildId, null)
                respondWithReaction(event.message, changed)
            }
            egs.matches(trimmedMessage) -> {
                val games = epicGamesService.load()
                showEgsGames(channelId, games)
            }
            steam.matches(trimmedMessage) -> {
                val region = guildsManager.getGuildRegion(guildId)
                val games = steamGamesService.loadGames(listOf(region ?: "en"))[region] ?: return
                showSteamGames(channelId, games)
            }
        }
    }

    suspend fun showHelp(channel: MessageChannelBehavior) {
        channel.createEmbed {
            color = Color(145, 71, 255)
            field {
                name = "Игры"
                value = "Следующие команды отвечают за уведомления о скидках, распродажах и раздачах игр."
            }
            field {
                name = getCommand(Commands.Games.SET)
                value = "Устанавливает канал, в который была отправлена команда, как канал куда бот будет присылать уведомления."
                inline = true
            }
            field {
                name = getCommand(Commands.Games.UNSET)
                value = "Отменяет предыдущую команду."
                inline = true
            }
            field {
                name = getCommand(Commands.Games.STEAM)
                value = "Показывает список текущих скидок в Steam."
                inline = true
            }
            field {
                name = getCommand(Commands.Games.EGS)
                value = "Показывает список текущих и будущих раздач в Epic Games Store."
                inline = true
            }
        }
    }

    suspend fun showEgsGames(channelId: String, elements: List<GiveAwayGame>) {
        val messageColor = Color(12, 12, 12)
        if (elements.isEmpty()) {
            postErrorMessage(channelId, "Игры не раздают", messageColor)
            return
        }
        client.rest.channel.createMessage(channelId) {
            embed {
                color = messageColor
                author {
                    icon = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/Epic_Games_logo.svg/1200px-Epic_Games_logo.svg.png"
                    name = "Epic Games Store"
                    url = "https://www.epicgames.com/store/en-US/free-games"
                }
                elements.forEach { game ->
                    field {
                        val now = LocalDateTime.now()
                        name = game.title
                        val date = when {
                            game.promotion == null -> "Free"
                            now.isBefore(game.promotion.start) -> game.promotion.start.format()?.let { "с $it" } ?: "N/A"
                            now.isAfter(game.promotion.start) && now.isBefore(game.promotion.end) -> game.promotion.end.format()?.let { "до $it" } ?: "N/A"
                            else -> "Free"
                        }
                        val link = when {
                            game.productSlug.isNullOrEmpty() -> "https://www.epicgames.com/store/en-US/free-games"
                            else -> "https://www.epicgames.com/store/en-US/product/${game.productSlug}"
                        }
                        value = "[$date]($link)"
                        inline = true
                    }
                }
            }
        }
    }

    suspend fun showSteamGames(channelId: String, elements: List<SteamGame>) {
        val messageColor = Color(27, 40, 56)
        if (elements.isEmpty()) {
            postErrorMessage(channelId, "Игры не раздают", messageColor)
            return
        }
        client.rest.channel.createMessage(channelId) {
            embed {
                color = messageColor
                author {
                    icon = "https://upload.wikimedia.org/wikipedia/commons/c/c1/Steam_Logo.png"
                    name = "Steam"
                    url = "https://store.steampowered.com/specials#p=0&tab=TopSellers"
                }
                elements.forEach {
                    field {
                        name = it.name
                        value = if (it.originalPrice != null && it.currentPrice != null) {
                            buildString {
                                append("[")
                                if (it.originalPrice.isNotEmpty()) {
                                    append("~~${it.originalPrice}~~ ")
                                }
                                append(it.currentPrice)
                                append("]")
                                append("(${it.url})")
                            }
                        } else {
                            "[See in the store](${it.url})"
                        }
                        inline = true
                    }
                }
            }
        }
    }

    private fun LocalDateTime.format(pattern: String = "d MMMM", locale: Locale = Locale("ru")): String? {
        return try {
            val outFormatter = DateTimeFormatter.ofPattern(pattern, locale)
            outFormatter.format(this)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}