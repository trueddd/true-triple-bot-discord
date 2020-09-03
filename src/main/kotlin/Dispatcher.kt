import com.gitlab.kordlib.common.entity.DiscordMessage
import com.gitlab.kordlib.common.entity.DiscordUser
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.behavior.channel.createMessage
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.ReactionEmoji
import data.Movie
import data.egs.GiveAwayGame
import data.steam.SteamGame
import io.ktor.util.KtorExperimentalAPI
import java.awt.Color
import java.io.Closeable
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.Exception

@KtorExperimentalAPI
class Dispatcher(private val client: Kord) : Closeable {

    private val googleService by lazy {
        GoogleService()
    }

    private val moviesChannelId = "719221812424081479"

    private suspend fun getMovieList(): Map<Int, List<DiscordMessage>> {
        val messages = client.rest.channel.getMessages(moviesChannelId, limit = 100)
        return messages
            .groupBy { it.reactions?.firstOrNull { reaction -> reaction.emoji.name == "\uD83D\uDC4D" }?.count ?: 0 }
    }

    @ExperimentalStdlibApi
    suspend fun rollMovies(channel: MessageChannelBehavior, withSearch: Boolean = false) {
        getMovieList()
            .maxByOrNull { it.key }
            ?.value?.randomOrNull()
            ?.let {
                channel.createBotMessage("**${it.content}** от ${it.author.getMention()} :trophy:")
                if (withSearch) {
                    searchForMovie(channel, it.content)
                }
            } ?: channel.createErrorMessage("Нечего смотреть")
    }

    suspend fun showMoviesToRoll(channel: MessageChannelBehavior) {
        getMovieList()
            .maxByOrNull { it.key }
            ?.let { entry ->
                val newText = entry.value.mapIndexed { index, it ->
                    "${index + 1}. ${it.content} от ${it.author.getMention()}"
                }.joinToString("\n")
                val votesCount = entry.value.first().reactions?.firstOrNull { it.emoji.name == "\uD83D\uDC4D" }?.count ?: 0
                channel.createBotMessage("Фильмы с наибольшим количесвом лайков ($votesCount лайка у каждого):\n$newText")
            } ?: channel.createErrorMessage("Нечего смотреть")
    }

    suspend fun showMoviesHelp(channel: MessageChannelBehavior) {
        channel.createEmbed {
            color = Color.MAGENTA
            title = "Manual :notebook:"
            field {
                name = "Правила выбора фильмов"
                value = "В выборку попадают первые 100 фильмов из канала ${moviesChannelId.getChannelMention()}. Фильм выбирается по наибольшему количеству лайков ( :thumbsup: ) в реакциях."
            }
            field {
                name = "`ttb!top`"
                value = "Показывает список фильмов с максимальным количеством лайков."
                inline = true
            }
            field {
                name = "`ttb!search`"
                value = "Ищет фильм на Кинопоиске. Пример использования: `ttb!search Геи-ниггеры из далёкого космоса`."
                inline = true
            }
            field {
                name = "`ttb!roll`"
                value = "Выбирает случайный фильм из выборки, которую можно посмотреть по комманде `ttb!top`. Если ввести параметр `-s`, бот найдёт выбранный фильм на Кинопоиске."
                inline = true
            }
        }
    }

    suspend fun searchForMovie(channel: MessageChannelBehavior, movieName: String) {
        if (movieName.isBlank()) {
            channel.createErrorMessage("Не могу искать, если ты не скажешь, что искать")
            return
        }
        val (movieLink, movie) = getMovie(movieName) ?: run {
            channel.createErrorMessage("Не нашёл")
            return
        }
        channel.createEmbed {
            color = Color.MAGENTA
            title = movie?.name ?: movieName
            author {
                icon = "https://yt3.ggpht.com/a/AATXAJz7FyhOdugVDwiazqdVf0P-xD1GOlkj-qdwD7cPtg=s900-c-k-c0xffffffff-no-rj-mo"
                name = "Смотреть на Кинопоиске"
                url = movieLink
            }
            movie?.let {
                image = movie.image
                field {
                    name = "Актёры"
                    value = movie.actors
                    inline = true
                }
                field {
                    name = "Дата выхода"
                    value = movie.dateCreated.convertDate()
                    inline = true
                }
                field {
                    name = "Режиссёр"
                    value = movie.director
                    inline = true
                }
                field {
                    name = "Жанр"
                    value = movie.genre
                    inline = true
                }
                footer {
                    text = movie.description
                }
            }
        }
    }

    suspend fun showEgsGames(channelId: String, elements: List<GiveAwayGame>) {
        val messageColor = Color(12, 12, 12)
        if (elements.isEmpty()) {
            createErrorMessage(channelId, "Игры не раздают", messageColor)
            return
        }
        client.rest.channel.createMessage(channelId) {
            embed {
                color = messageColor
                author {
                    icon = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/Epic_Games_logo.svg/1200px-Epic_Games_logo.svg.png"
                    name = "Игрульки, которые можно сейчас забрать"
                    url = "https://www.epicgames.com/store/en-US/free-games"
                }
                elements.forEach {
                    field {
                        val now = LocalDateTime.now()
                        name = it.title
                        val date = when {
                            it.promotion == null -> "Free"
                            now.isBefore(it.promotion.start) -> "с ${it.promotion.start.format()}"
                            now.isAfter(it.promotion.start) && now.isBefore(it.promotion.end) -> "до ${it.promotion.end.format()}"
                            else -> "Free"
                        }
                        val link = when {
                            it.productSlug.isNullOrEmpty() -> "https://www.epicgames.com/store/en-US/free-games"
                            else -> "https://www.epicgames.com/store/en-US/product/${it.productSlug}"
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
            createErrorMessage(channelId, "Игры не раздают", messageColor)
            return
        }
        client.rest.channel.createMessage(channelId) {
            embed {
                color = messageColor
                author {
                    icon = "https://upload.wikimedia.org/wikipedia/commons/c/c1/Steam_Logo.png"
                    name = "Steam discounts"
                    url = "https://store.steampowered.com/specials#p=0&tab=TopSellers"
                }
                elements.forEach {
                    field {
                        name = it.name
                        value = if (it.originalPrice != null && it.currentPrice != null) {
                            "[~~${it.originalPrice}~~ ${it.currentPrice}](${it.url})"
                        } else {
                            "[See in the store](${it.url})"
                        }
                        inline = true
                    }
                }
            }
        }
    }

    private suspend fun getMovie(name: String): Pair<String, Movie?>? {
        return googleService.load(name)?.items?.firstOrNull {
            it.pageMap.movies?.firstOrNull() != null
        }?.let {
            val link = it.link
            val movie = it.pageMap.movies?.firstOrNull()
            link to movie
        }
    }

    private suspend fun MessageChannelBehavior.createBotMessage(message: String, embedColor: Color = Color.MAGENTA) {
        createMessage {
            content = ""
            embed {
                description = message
                color = embedColor
            }
        }
    }

    private suspend fun MessageChannelBehavior.createErrorMessage(message: String) {
        createEmbed {
            color = Color.MAGENTA
            author {
                icon = "https://cdn.discordapp.com/emojis/722871552290455563.png?v=1"
                name = message
            }
        }
    }

    suspend fun createErrorMessage(channelId: String, message: String, messageColor: Color = Color.MAGENTA) {
        client.rest.channel.createMessage(channelId) {
            embed {
                color = messageColor
                author {
                    icon = "https://cdn.discordapp.com/emojis/722871552290455563.png?v=1"
                    name = message
                }
            }
        }
    }

    suspend fun markRequest(message: Message, isSuccessful: Boolean) {
        if (isSuccessful) {
            message.deleteOwnReaction(ReactionEmoji.Unicode("❌"))
            message.addReaction(ReactionEmoji.Unicode("\uD83C\uDD97"))
        } else {
            message.deleteOwnReaction(ReactionEmoji.Unicode("\uD83C\uDD97"))
            message.addReaction(ReactionEmoji.Unicode("❌"))
        }
    }

    private fun LocalDateTime.format(pattern: String = "MMM d"): String {
        return try {
            val outFormatter = DateTimeFormatter.ofPattern(pattern, Locale.UK)
            outFormatter.format(this)
        } catch (e: Exception) {
            e.printStackTrace()
            "TBA"
        }
    }

    private fun String.convertDate(): String {
        return try {
            val inputFormatter = SimpleDateFormat("yyyy-MM-dd")
            val date = inputFormatter.parse(this)
            val outputFormatter = SimpleDateFormat("d LLLL yyyy", Locale.forLanguageTag("ru"))
            outputFormatter.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
            this
        }
    }

    override fun close() {
        googleService.close()
    }

    private fun DiscordUser.getMention() = "<@!$id>"

    private fun String.getChannelMention() = "<#$this>"
}