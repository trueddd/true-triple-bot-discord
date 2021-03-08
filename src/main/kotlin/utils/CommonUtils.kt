package utils

import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.event.message.MessageCreateEvent
import services.BaseBot
import java.text.SimpleDateFormat
import java.util.*

fun String.egsDate(): Date {
    return try {
        val inputFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        inputFormatter.parse(this)
    } catch (e: Exception) {
        e.printStackTrace()
        Date()
    }
}

suspend fun MessageChannelBehavior.createBotMessage(message: String, embedColor: Color = Color(255, 0, 255)) {
    createMessage {
        content = ""
        embed {
            description = message
            color = embedColor
        }
    }
}

fun String.commandRegex(singleWordCommand: Boolean = true, vararg flags: RegexOption): Regex {
    return Regex("^$this${if (singleWordCommand) "$" else ".*"}", flags.toSet())
}

suspend fun Kord.setDefaultStatus() {
    editPresence {
        listening("${BaseBot.BOT_PREFIX}${Commands.Common.HELP}")
    }
}

fun String.replaceIfMatches(regex: Regex, replacement: String): String? {
    return if (matches(regex)) {
        replace(regex, replacement)
    } else {
        null
    }
}

suspend fun MessageCreateEvent.isSentByAdmin(): Boolean {
    val guild = guildId ?: return false
    return message.author?.asMember(guild)?.getPermissions()?.contains(Permission.Administrator) == true
}