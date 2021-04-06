package utils

import dev.kord.common.entity.Permission
import dev.kord.core.event.message.MessageCreateEvent
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

fun String.commandRegex(singleWordCommand: Boolean = true, vararg flags: RegexOption): Regex {
    return Regex("^$this${if (singleWordCommand) "$" else ".*"}", flags.toSet())
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