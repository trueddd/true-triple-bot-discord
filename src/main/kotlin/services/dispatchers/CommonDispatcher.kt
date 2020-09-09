package services.dispatchers

import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import java.awt.Color

// TODO: migrate to regexp
class CommonDispatcher(client: Kord) : BaseDispatcher(client), MessageCreateListener {

    override val dispatcherPrefix: String
        get() = ""

    override fun getPrefix(): String {
        return dispatcherPrefix
    }

    override suspend fun onMessageCreate(event: MessageCreateEvent, trimmedMessage: String) {
        when {
            trimmedMessage == "help" -> {
                showHelp(event.message.channel)
            }
            trimmedMessage.startsWith("pick") -> {
                pickRandom(trimmedMessage.removePrefix("pick").trim(), event.message)
            }
        }
    }

    private suspend fun showHelp(channel: MessageChannelBehavior) {
        channel.createEmbed {
            color = Color.MAGENTA
            field {
                name = "Фильмы"
                value = "`ttb!movies/help`"
                inline = true
            }
            field {
                name = "Игры"
                value = "`ttb!games/help`"
                inline = true
            }
            field {
                name = "`ttb!pick`"
                value = "Выбирает случайный вариант из написанных в команде. Название команды и варианты надо отделять новой строкой (`Shift` + `Enter`)."
                inline = true
            }
        }
    }

    private suspend fun pickRandom(messageText: String, message: Message) {
        val options = messageText
            .split("\n")
            .mapNotNull { if (it.isEmpty()) null else it }
        if (options.isEmpty()) {
            respondWithReaction(message, false)
            return
        } else {
            val chosen = options.randomOrNull() ?: run {
                respondWithReaction(message, false)
                return
            }
            postMessage(message.channel, chosen, Color.ORANGE)
        }
    }
}