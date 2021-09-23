package dispatchers

import data.cracked.Game
import data.egs.GiveAwayGame
import data.gog.Product
import data.steam.SteamGame
import db.GuildsManager
import dev.kord.common.Color
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.CommandArgument
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.Interaction
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.json.request.*
import io.ktor.util.*
import services.EpicGamesService
import services.GogGamesService
import services.SteamGamesService
import utils.Commands
import utils.issuedByAdmin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class GamesDispatcher(
    private val guildsManager: GuildsManager,
    private val epicGamesService: EpicGamesService,
    private val steamGamesService: SteamGamesService,
    private val gogGamesService: GogGamesService,
    client: Kord,
) : BaseDispatcher(client), InteractionListener {

    override val dispatcherPrefix: String
        get() = "games"

    override suspend fun onInteractionReceived(interaction: Interaction) {
        val subCommand = interaction.data.data.options.value?.firstOrNull()?.name
        when (subCommand) {
            Commands.Games.EGS -> {
                val region = guildsManager.getGuildRegion(interaction.data.guildId.value!!.asString) ?: "ru"
                val games = epicGamesService.load(listOf(region))?.get(region)
                if (games != null) {
                    createEmbedResponse(interaction, buildEgsGamesEmbed(games))
                } else {
                    postErrorMessage(interaction, "Игры не раздают")
                }
            }
            Commands.Games.GOG -> {
                val region = guildsManager.getGuildRegion(interaction.data.guildId.value!!.asString) ?: "ru"
                val games = gogGamesService.load(listOf(region))?.get(region)
                if (games != null) {
                    createEmbedResponse(interaction, buildGogGamesEmbed(games))
                } else {
                    postErrorMessage(interaction, "Не получилось со GOG'ом")
                }
            }
            Commands.Games.STEAM -> {
                val region = guildsManager.getGuildRegion(interaction.data.guildId.value!!.asString) ?: "ru"
                val games = steamGamesService.load(listOf(region))?.get(region)
                if (games != null) {
                    createEmbedResponse(interaction, buildSteamGamesEmbed(games))
                } else {
                    postErrorMessage(interaction, "Не получилось со Steam\'ом")
                }
            }
            Commands.Games.SET -> setGamesChannel(interaction)
            Commands.Games.UNSET -> unsetGamesChannel(interaction)
        }
    }

    private suspend fun unsetGamesChannel(interaction: Interaction) {
        if (!interaction.issuedByAdmin) {
            createTextResponse(
                interaction,
                "You are not allowed to use this command.",
                onlyForUser = true,
            )
            return
        }
        when {
            guildsManager.setGamesChannel(interaction.data.guildId.value!!.asString, null) -> {
                createTextResponse(
                    interaction,
                    "Successfully cancelled games notifications",
                    onlyForUser = true,
                )
            }
            else -> createTextResponse(
                interaction,
                "Error occurred while cancelling game notifications",
                onlyForUser = true,
            )
        }
    }

    private suspend fun setGamesChannel(interaction: Interaction) {
        if (!interaction.issuedByAdmin) {
            createTextResponse(
                interaction,
                "You are not allowed to use this command.",
                onlyForUser = true,
            )
            return
        }
        val channelId = interaction.data.data.options.value
            ?.firstOrNull { it.name == "set" }?.values?.value
            ?.filterIsInstance<CommandArgument.ChannelArgument>()
            ?.firstOrNull { it.name == "channel" }?.value
            ?: run {
                createTextResponse(
                    interaction,
                    "Couldn't get channel ID.",
                    onlyForUser = true,
                )
                return
            }
        val channel = try {
            client.rest.channel.getChannel(channelId)
        } catch (e: Exception) {
            null
        }
        when {
            channel == null -> createTextResponse(interaction, "Channel not found", onlyForUser = true)
            channel.type != ChannelType.GuildText -> createTextResponse(interaction, "You can only pass text channel", onlyForUser = true)
            guildsManager.setGamesChannel(interaction.data.guildId.value!!.asString, channelId.asString) -> {
                val channelName = channel.name.value ?: channelId
                createTextResponse(
                    interaction,
                    "Successfully set games channel to **$channelName**",
                    onlyForUser = true,
                )
            }
            else -> createTextResponse(
                interaction,
                "Error occurred while setting game channel",
                onlyForUser = true,
            )
        }
    }

    private fun buildEgsGamesEmbed(elements: List<GiveAwayGame>): EmbedRequest {
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