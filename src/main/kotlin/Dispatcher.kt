import com.gitlab.kordlib.common.entity.DiscordMessage
import com.gitlab.kordlib.common.entity.DiscordUser
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.behavior.channel.createMessage
import data.Movie
import io.ktor.util.KtorExperimentalAPI
import java.awt.Color
import java.io.Closeable
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

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
            .maxBy { it.key }
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
            .maxBy { it.key }
            ?.let { entry ->
                val newText = entry.value.mapIndexed { index, it ->
                    "${index + 1}. ${it.content} от ${it.author.getMention()}"
                }.joinToString("\n")
                val votesCount = entry.value.first().reactions?.firstOrNull { it.emoji.name == "\uD83D\uDC4D" }?.count ?: 0
                channel.createBotMessage("Фильмы с наибольшим количесвом лайков ($votesCount лайка у каждого):\n$newText")
            } ?: channel.createErrorMessage("Нечего смотреть")
    }

    suspend fun showHelp(channel: MessageChannelBehavior) {
        channel.createEmbed {
            color = Color.MAGENTA
            title = "Manual :notebook:"
            field {
                name = "Правила выбора фильмов"
                value = "В выборку попадают первые 100 фильмов из канала ${moviesChannelId.getChannelMention()}. Фильм выбирается по наибольшему количеству лайков ( :thumbsup: ) в реакциях."
            }
            field {
                name = "`!top`"
                value = "Показывает список фильмов с максимальным количеством лайков."
                inline = true
            }
            field {
                name = "`!search`"
                value = "Ищет фильм на Кинопоиске. Пример использования: `!search Геи-ниггеры из далёкого космоса`."
                inline = true
            }
            field {
                name = "`!roll`"
                value = "Выбирает случайный фильм из выборки, которую можно посмотреть по комманде `!top`. Если ввести параметр `-s`, бот найдёт выбранный фильм на Кинопоиске."
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

    private suspend fun getMovie(name: String): Pair<String, Movie?>? {
        return googleService.load(name)?.items?.firstOrNull {
            it.pageMap.movies?.firstOrNull() != null
        }?.let {
            val link = it.link
            val movie = it.pageMap.movies?.firstOrNull()
            link to movie
        }
    }

    suspend fun answerUnauthorized(channel: MessageChannelBehavior) {
        channel.createEmbed {
            color = Color.MAGENTA
            description = "[Click me](https://www.youtube.com/watch?v=koOqw-trpLw)"
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