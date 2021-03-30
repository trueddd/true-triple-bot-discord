package dispatchers

import db.GuildsManager
import dev.kord.common.Color
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import services.BaseBot
import utils.Commands
import utils.commandRegex
import utils.isSentByAdmin
import utils.replaceIfMatches
import java.net.URL

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
    private val roleSet = Regex("^${Commands.Common.ROLE_GETTER}\\s+<@&(\\d+)>\\s+<:(.+):\\d+>.*$")
    private val poll = Commands.Common.POLL.commandRegex(false, RegexOption.DOT_MATCHES_ALL)

    override suspend fun onMessageCreate(event: MessageCreateEvent, trimmedMessage: String) {
        when {
            help.matches(trimmedMessage) -> {
                showHelp(event.message.channel)
            }
            pick.matches(trimmedMessage) -> {
                pickRandom(trimmedMessage.removePrefix(Commands.Common.PICK).trim(), event.message)
            }
            locale.matches(trimmedMessage) -> {
                if (!event.isSentByAdmin()) {
                    respondWithReaction(event.message, false)
                    return
                }
                setLocale(trimmedMessage.removePrefix(Commands.Common.LOCALE).trim(), event.message)
            }
            roleSet.matches(trimmedMessage) -> {
                if (!event.isSentByAdmin()) {
                    respondWithReaction(event.message, false)
                    return
                }
                val role = trimmedMessage.replaceIfMatches(roleSet, "$1")
                val emoji = trimmedMessage.replaceIfMatches(roleSet, "$2")
                val success = if (role != null && emoji != null) {
                    guildsManager.setRoleGetterEmoji(event.message.id.asString, role, emoji)
                } else false
                respondWithReaction(event.message, success)
            }
            poll.matches(trimmedMessage) -> {
                createPoll(trimmedMessage.removePrefix(Commands.Common.POLL).trim(), event.message)
            }
        }
    }

    private suspend fun showHelp(channel: MessageChannelBehavior) {
        channel.createEmbed {
            color = magentaColor
            field {
                name = getCommand(Commands.Movies.HELP, customPrefix = "movies")
                value = "Справка по командам для фильмов"
            }
            field {
                name = getCommand(Commands.Games.HELP, customPrefix = "games")
                value = "Справка по командам по играм"
            }
            field {
                name = getCommand(Commands.Common.PICK)
                value = "Выбирает случайный вариант из написанных в команде. Название команды и варианты надо отделять новой строкой (`Shift` + `Enter`)."
            }
            field {
                name = getCommand(Commands.Common.LOCALE)
                value = "Установка языка сервера. Язык используется в других командах. Пример: `${getCommand(Commands.Common.LOCALE, format = false)} ru`"
            }
            field {
                name = getCommand(Commands.Common.ROLE_GETTER)
                value = "Создаёт выдачу роли пользователям, которые поставят указанное эмодзи в реакцию под сообщением с этой командой. Пример: `${BaseBot.BOT_PREFIX}${Commands.Common.ROLE_GETTER} <выдаваемая роль> <эмодзи>`. Также можно снять роль, убрав свою реакцию."
            }
            field {
                name = getCommand(Commands.Common.POLL)
                value = "Создаёт голосование с вопросом, написанным после команды текстом."
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
            postMessage(message.channel, chosen, Color(250, 200, 0))
        }
    }

    private fun String.isUrl(): Boolean {
        return try {
            URL(this)
            true
        } catch (e: Exception) {
            false
        }
    }

    private val imageRegex = Regex("-link (?<link>\\S*)", RegexOption.DOT_MATCHES_ALL)

    private suspend fun createPoll(messageText: String, message: Message) {
        val urlText = imageRegex.find(messageText)?.groups?.get("link")?.value?.let { if (it.isUrl()) it else null }
        val text = if (urlText == null) messageText else messageText.replace(imageRegex, "")
        val newMessage = client.rest.channel.createMessage(message.channelId) {
            embed {
                message.author?.let {
                    author {
                        icon = it.avatar.url
                        name = it.username
                    }
                }
                urlText?.let {
                    thumbnail {
                        url = it
                    }
                }
                description = text
                color = Color(185, 185, 0)
            }
        }
        client.rest.channel.createReaction(newMessage.channelId, newMessage.id, "\uD83D\uDC4D")
        client.rest.channel.createReaction(newMessage.channelId, newMessage.id, "\uD83D\uDC4E")
        message.delete()
    }

    private suspend fun setLocale(localeString: String, message: Message) {
        val success = guildsManager.setGuildRegion(message.getGuild().id.asString, localeString)
        respondWithReaction(message, success)
    }
}