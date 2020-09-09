package utils

import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createMessage
import io.ktor.application.ApplicationCall
import io.ktor.request.receive
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

suspend inline fun <reified T : Any> ApplicationCall.receiveSafe(): T? {
    return try {
        receive<T>()
    } catch (e: Exception) {
        null
    }
}

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

fun String.commandRegex(singleWordCommand: Boolean = true): Regex {
    return Pattern.compile("^$this${if (singleWordCommand) "$" else ""}").toRegex()
}