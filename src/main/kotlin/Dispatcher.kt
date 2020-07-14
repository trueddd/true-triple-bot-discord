import com.gitlab.kordlib.common.entity.DiscordMessage
import com.gitlab.kordlib.common.entity.DiscordUser
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.behavior.channel.createMessage
import java.awt.Color

class Dispatcher(private val client: Kord) {

    private val moviesChannelId = "719221812424081479"

    private suspend fun getMovieList(): Map<Int, List<DiscordMessage>> {
        val messages = client.rest.channel.getMessages(moviesChannelId, limit = 100)
        return messages
            .groupBy { it.reactions?.firstOrNull { reaction -> reaction.emoji.name == "\uD83D\uDC4D" }?.count ?: 0 }
    }

    @ExperimentalStdlibApi
    suspend fun rollMovies(channel: MessageChannelBehavior) {
        getMovieList()
            .maxBy { it.key }
            ?.value?.randomOrNull()
            ?.let {
                channel.createBotMessage("**${it.content}** by ${it.author.getMention()} :trophy:")
            } ?: channel.createBotMessage("Nothing to watch :sad_cat:")
    }

    suspend fun showMoviesToRoll(channel: MessageChannelBehavior) {
        getMovieList()
            .maxBy { it.key }
            ?.let { entry ->
                val newText = entry.value.mapIndexed { index, it ->
                    "${index + 1}. ${it.content} by ${it.author.getMention()}"
                }.joinToString("\n")
                val votesCount = entry.value.first().reactions?.firstOrNull { it.emoji.name == "\uD83D\uDC4D" }?.count ?: 0
                channel.createBotMessage("Most voted movies ($votesCount votes each):\n$newText")
            } ?: channel.createBotMessage("Nothing to watch :sad_cat:")
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
                name = "`!roll -show`"
                value = "Показывает список фильмов с максимальным количеством лайков."
                inline = true
            }
            field {
                name = "`!roll`"
                value = "Выбирает случайный фильм из выборки, которую можно посмотреть по комманде `!roll -show`"
                inline = true
            }
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

    private fun DiscordUser.getMention() = "<@!$id>"

    private fun String.getChannelMention() = "<#$this>"
}