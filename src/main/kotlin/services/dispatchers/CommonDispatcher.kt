package services.dispatchers

import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import db.GuildsManager
import utils.Commands
import utils.commandRegex
import java.awt.Color

class CommonDispatcher(
    private val guildsManager: GuildsManager,
    client: Kord
) : BaseDispatcher(client), MessageCreateListener {

    override val dispatcherPrefix: String
        get() = ""

    override fun getPrefix(): String {
        return dispatcherPrefix
    }

    private val help = Commands.Common.HELP.commandRegex()
    private val pick = Commands.Common.PICK.commandRegex(false, RegexOption.DOT_MATCHES_ALL)
    private val locale = Commands.Common.LOCALE.commandRegex(singleWordCommand = false)

    override suspend fun onMessageCreate(event: MessageCreateEvent, trimmedMessage: String) {
        when {
            help.matches(trimmedMessage) -> {
                showHelp(event.message.channel)
            }
            pick.matches(trimmedMessage) -> {
                pickRandom(trimmedMessage.removePrefix(Commands.Common.PICK).trim(), event.message)
            }
            locale.matches(trimmedMessage) -> {
                setLocale(trimmedMessage.removePrefix(Commands.Common.LOCALE).trim(), event.message)
            }
        }
    }

    private suspend fun showHelp(channel: MessageChannelBehavior) {
        channel.createEmbed {
            color = Color.MAGENTA
            field {
                name = "Фильмы"
                value = getCommand(Commands.Movies.HELP, customPrefix = "movies")
                inline = true
            }
            field {
                name = "Игры"
                value = getCommand(Commands.Games.HELP, customPrefix = "games")
                inline = true
            }
            field {
                name = getCommand(Commands.Common.PICK)
                value = "Выбирает случайный вариант из написанных в команде. Название команды и варианты надо отделять новой строкой (`Shift` + `Enter`)."
                inline = true
            }
            field {
                name = getCommand(Commands.Common.LOCALE)
                value = "Установка языка сервера. Язык используется в других командах. Пример: `${getCommand(Commands.Common.LOCALE, format = false)} ru`"
                inline = true
            }
        }
    }

    private suspend fun pickRandom(messageText: String, message: Message) {
        println(messageText)
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

    private suspend fun setLocale(localeString: String, message: Message) {
        val success = guildsManager.setGuildRegion(message.getGuild().id.value, localeString)
        respondWithReaction(message, success)
    }
}