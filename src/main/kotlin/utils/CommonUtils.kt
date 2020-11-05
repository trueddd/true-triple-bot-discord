package utils

import com.gitlab.kordlib.common.entity.Permission
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createMessage
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import services.BaseBot
import java.awt.Color
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

suspend fun MessageChannelBehavior.createBotMessage(message: String, embedColor: Color = Color.MAGENTA) {
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