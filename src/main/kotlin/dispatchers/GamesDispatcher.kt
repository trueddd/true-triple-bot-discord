package dispatchers

import data.cracked.Game
import data.egs.GiveAwayGame
import data.gog.Product
import data.steam.SteamGame
import db.GuildsManager
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.interaction.Interaction
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.json.request.*
import io.ktor.util.*
import utils.Commands
import utils.commandRegex
import utils.isSentByAdmin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class GamesDispatcher(
    private val guildsManager: GuildsManager,
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
        }
    }

    suspend fun showGogGames(interaction: Interaction, gogGames: List<Product>) {
        createEmbedResponse(
            interaction,
            buildGogGamesEmbed(gogGames),
        )
    }
    suspend fun showSteamGames(interaction: Interaction, elements: List<SteamGame>) {
        createEmbedResponse(
            interaction,
            buildSteamGamesEmbed(elements),
        )
    }
    fun buildEgsGamesEmbed(elements: List<GiveAwayGame>): EmbedRequest {
        val now = LocalDateTime.now()
        return EmbedRequest(
            color = Color(12, 12, 12).optional(),
            author = EmbedAuthorRequest(
                name = "Epic Games Store".optional(),
                url = "https://www.epicgames.com/store/en-US/free-games".optional(),
                iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/Epic_Games_logo.svg/1200px-Epic_Games_logo.svg.png".optional(),
            ).optional(),
            fields = elements.map { giveAwayGame ->
                EmbedFieldRequest(
                    giveAwayGame.title,
                    inline = true.optional(),
                    value = giveAwayGame.let { game ->
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
                        "[$date]($link)"
                    }
                )
            }.optional()
        )
    }
    suspend fun showEgsGames(interaction: Interaction, elements: List<GiveAwayGame>) {
        createEmbedResponse(
            interaction,
            buildEgsGamesEmbed(elements),
        )
    }
    suspend fun showEgsGames(channelId: Snowflake, elements: List<GiveAwayGame>) {
        client.rest.channel.createMessage(
            channelId,
            MultipartMessageCreateRequest(
                MessageCreateRequest(
                    embeds = listOf(buildEgsGamesEmbed(elements)).optional(),
                )
            )
        )
    }

    private fun buildSteamGamesEmbed(elements: List<SteamGame>): EmbedRequest {
        return EmbedRequest(
            color = Color(27, 40, 56).optional(),
            author = EmbedAuthorRequest(
                name = "Steam".optional(),
                iconUrl = "https://upload.wikimedia.org/wikipedia/commons/c/c1/Steam_Logo.png".optional(),
                url = "https://store.steampowered.com/specials#p=0&tab=TopSellers".optional(),
            ).optional(),
            fields = elements.map { steamGame ->
                EmbedFieldRequest(
                    name = steamGame.name.let { if (it.length <= 64) it else "${it.take(64)}..." },
                    value = if (steamGame.price.originalPrice != null && steamGame.price.currentPrice != null) {
                        buildString {
                            append("[")
                            if (steamGame.price.originalPrice.isNotEmpty()) {
                                append("~~${steamGame.price.originalPrice}~~ ")
                            }
                            append(steamGame.price.currentPrice)
                            append("]")
                            append("(${steamGame.url})")
                        }
                    } else {
                        "[Bundle ${steamGame.price.discount}](${steamGame.url})"
                    },
                    inline = true.optional()
                )
            }.optional()
        )
    }
    suspend fun showSteamGames(channelId: Snowflake, elements: List<SteamGame>) {
        client.rest.channel.createMessage(
            channelId,
            MultipartMessageCreateRequest(
                MessageCreateRequest(
                    embeds = listOf(buildSteamGamesEmbed(elements)).optional(),
                )
            ),
        )
    }

    private fun buildGogGamesEmbed(elements: List<Product>): EmbedRequest {
        val takeFirst = 15
        return EmbedRequest(
            color = Color(104, 0, 209).optional(),
            author = EmbedAuthorRequest(
                name = "GOG".optional(),
                url = "https://www.gog.com/games?sort=popularity&page=1&tab=on_sale".optional(),
                iconUrl = "https://dl2.macupdate.com/images/icons256/54428.png".optional(),
            ).optional(),
            fields = elements.take(takeFirst).map { product ->
                EmbedFieldRequest(
                    name = product.title,
                    value = if (product.isPriceVisible && product.price != null && product.localPrice != null) {
                        buildString {
                            append("[")
                            if (product.price.isDiscounted) {
                                append("~~${product.localPrice?.base}~~ ")
                            }
                            append(product.localPrice?.final)
                            append("]")
                            append("(${product.urlFormatted})")
                        }
                    } else {
                        "[${product.developer}](${product.urlFormatted})"
                    },
                    inline = true.optional(),
                )
            }.optional(),
        )
    }
    suspend fun showGogGames(channelId: Snowflake, elements: List<Product>?) {
        val messageColor = Color(104, 0, 209)
        if (elements == null || elements.isEmpty()) {
            postErrorMessage(channelId, "Не получилось с GOG\'ом", messageColor)
            return
        }
        client.rest.channel.createMessage(
            channelId,
            MultipartMessageCreateRequest(
                MessageCreateRequest(
                    embeds = listOf(buildGogGamesEmbed(elements)).optional(),
                )
            )
        )
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