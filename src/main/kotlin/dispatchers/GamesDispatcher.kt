package dispatchers

import data.cracked.Game
import data.egs.GiveAwayGame
import data.gog.Product
import data.steam.SteamGame
import db.GuildsManager
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.embed
import io.ktor.util.*
import services.CrackedGamesService
import services.EpicGamesService
import services.GogGamesService
import services.SteamGamesService
import utils.Commands
import utils.commandRegex
import utils.isSentByAdmin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class GamesDispatcher(
    private val guildsManager: GuildsManager,
    private val epicGamesService: EpicGamesService,
    private val steamGamesService: SteamGamesService,
    private val gogGamesService: GogGamesService,
    private val crackedGamesService: CrackedGamesService,
    client: Kord
) : BaseDispatcher(client),
    MessageCreateListener
{

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
    private val gog = Commands.Games.GOG.commandRegex()
    private val cracked = Commands.Games.CRACKED.commandRegex()

    override suspend fun onMessageCreate(event: MessageCreateEvent, trimmedMessage: String) {
        val guildId = event.message.getGuild().id.asString
        val channelId = event.message.channelId
        when {
            help.matches(trimmedMessage) -> {
                showHelp(event.message.channel)
            }
            set.matches(trimmedMessage) -> {
                if (!event.isSentByAdmin()) {
                    respondWithReaction(event.message, false)
                    return
                }
                val changed = guildsManager.setGamesChannel(guildId, channelId.asString)
                respondWithReaction(event.message, changed)
            }
            unset.matches(trimmedMessage) -> {
                if (!event.isSentByAdmin()) {
                    respondWithReaction(event.message, false)
                    return
                }
                val changed = guildsManager.setGamesChannel(guildId, null)
                respondWithReaction(event.message, changed)
            }
            egs.matches(trimmedMessage) -> {
                val region = guildsManager.getGuildRegion(guildId) ?: "ru"
                val games = epicGamesService.load(listOf(region))?.get(region) ?: return
                showEgsGames(channelId, games)
            }
            steam.matches(trimmedMessage) -> {
                val region = guildsManager.getGuildRegion(guildId) ?: "ru"
                val games = steamGamesService.load(listOf(region))?.get(region) ?: return
                showSteamGames(channelId, games)
            }
            gog.matches(trimmedMessage) -> {
                val region = guildsManager.getGuildRegion(guildId) ?: "ru"
                val games = gogGamesService.load(listOf(region))?.get(region) ?: return
                showGogGames(channelId, games)
            }
            cracked.matches(trimmedMessage) -> {
                showCrackedPlaceholder(channelId)
//                val cracked = crackedGamesService.load()
//                showCrackedGames(channelId, cracked?.values?.firstOrNull())
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
            field {
                name = getCommand(Commands.Games.GOG)
                value = "Показывает список игр из магазина GOG со вкладки *Со скидкой*."
                inline = true
            }
            field {
                name = getCommand(Commands.Games.CRACKED)
                value = "Показывает список недавно взломанных игр с портала CrackWatch."
                inline = true
            }
        }
    }

    suspend fun showEgsGames(channelId: Snowflake, elements: List<GiveAwayGame>?) {
        val messageColor = Color(12, 12, 12)
        if (elements == null || elements.isEmpty()) {
            postErrorMessage(channelId, "Игры не раздают", messageColor)
            return
        }
        client.rest.channel.createMessage(channelId) {
            val now = LocalDateTime.now()
            embed {
                color = messageColor
                author {
                    icon = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/Epic_Games_logo.svg/1200px-Epic_Games_logo.svg.png"
                    name = "Epic Games Store"
                    url = "https://www.epicgames.com/store/en-US/free-games"
                }
                elements.forEach { game ->
                    field {
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

    suspend fun showSteamGames(channelId: Snowflake, elements: List<SteamGame>?) {
        val messageColor = Color(27, 40, 56)
        if (elements == null || elements.isEmpty()) {
            postErrorMessage(channelId, "Не получилось со Steam\'ом", messageColor)
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
                        name = it.name.let {
                            if (it.length <= 64) it else "${it.take(64)}..."
                        }
                        value = if (it.price.originalPrice != null && it.price.currentPrice != null) {
                            buildString {
                                append("[")
                                if (it.price.originalPrice.isNotEmpty()) {
                                    append("~~${it.price.originalPrice}~~ ")
                                }
                                append(it.price.currentPrice)
                                append("]")
                                append("(${it.url})")
                            }
                        } else {
                            "[Bundle ${it.price.discount}](${it.url})"
                        }
                        inline = true
                    }
                }
            }
        }
    }

    suspend fun showGogGames(channelId: Snowflake, elements: List<Product>?) {
        val messageColor = Color(104, 0, 209)
        if (elements == null || elements.isEmpty()) {
            postErrorMessage(channelId, "Не получилось с GOG\'ом", messageColor)
            return
        }
        client.rest.channel.createMessage(channelId) {
            embed {
                color = messageColor
                author {
                    icon = "https://dl2.macupdate.com/images/icons256/54428.png"
                    name = "GOG"
                    url = "https://www.gog.com/games?sort=popularity&page=1&tab=on_sale"
                }
                val takeFirst = 15
                elements.take(takeFirst).forEach {
                    field {
                        name = it.title
                        value = if (it.isPriceVisible && it.price != null && it.localPrice != null) {
                            buildString {
                                append("[")
                                if (it.price.isDiscounted) {
                                    append("~~${it.localPrice?.base}~~ ")
                                }
                                append(it.localPrice?.final)
                                append("]")
                                append("(${it.urlFormatted})")
                            }
                        } else {
                            "[${it.developer}](${it.urlFormatted})"
                        }
                        inline = true
                    }
                }
                if (elements.count() - takeFirst > 0) {
                    field {
                        name = "More here :point_down:"
                        value = "[click](https://www.gog.com/games?sort=popularity&page=1&tab=on_sale)"
                    }
                }
            }
        }
    }

    private suspend fun showCrackedPlaceholder(channelId: Snowflake) {
        val messageColor = Color(237, 28, 35)
        client.rest.channel.createMessage(channelId) {
            embed {
                color = messageColor
                author {
                    icon = "https://img7.androidappsapk.co/OurDjLyAKt2YTXvtMPQfHkQd07NmdEOpAwfqy1_cy9pxG3CX6vOu88mVh20TJa30ZdQ=s300"
                    name = "Последние взломанные игры"
                    url = "https://crackwatch.com/games"
                }
                description = "Работа сервиса crackwatch.com временно [приостановлена](https://crackwatch.com)."
            }
        }
    }

    suspend fun showCrackedGames(channelId: Snowflake, elements: List<Game>?) {
        val messageColor = Color(237, 28, 35)
        if (elements == null) {
            postErrorMessage(channelId, "Не получилось с кряками", messageColor)
            return
        }
        if (elements.isEmpty()) {
            return
        }
        client.rest.channel.createMessage(channelId) {
            embed {
                color = messageColor
                author {
                    icon = "https://img7.androidappsapk.co/OurDjLyAKt2YTXvtMPQfHkQd07NmdEOpAwfqy1_cy9pxG3CX6vOu88mVh20TJa30ZdQ=s300"
                    name = "Последние взломанные игры"
                    url = "https://crackwatch.com/games"
                }
                val today = Date().days()
                val todayCracks = elements.count { it.crackDate.days() == today }
                val takeFirst = todayCracks + (if (9 - todayCracks <= 0) 0 else 9 - todayCracks)
                elements.take(takeFirst).forEach {
                    field {
                        name = it.title
                        value = "[Взломано ${it.crackDate.formatContext()}](https://crackwatch.com/game/${it.slug})"
                        inline = true
                    }
                }
                footer {
                    icon = "https://img7.androidappsapk.co/OurDjLyAKt2YTXvtMPQfHkQd07NmdEOpAwfqy1_cy9pxG3CX6vOu88mVh20TJa30ZdQ=s300"
                    text = "Powered by CrackWatch"
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

    private fun Date.formatContext(pattern: String = "d MMMM", locale: Locale = Locale("ru")): String? {
        val todayDays = Date().days()
        return when (this.days()) {
            todayDays -> "сегодня"
            todayDays - 1 -> "вчера"
            else -> this.toLocalDateTime().format(pattern, locale)
        }
    }

    private fun Date.days(): Long {
        return this.time / 86_400_000
    }
}