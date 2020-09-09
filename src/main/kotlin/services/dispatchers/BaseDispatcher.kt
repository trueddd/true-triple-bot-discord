package services.dispatchers

import com.gitlab.kordlib.common.entity.DiscordChannel
import com.gitlab.kordlib.common.entity.DiscordUser
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.ReactionEmoji
import services.BaseBot
import java.awt.Color

abstract class BaseDispatcher(protected val client: Kord) {

    private val okEmoji: ReactionEmoji by lazy {
        ReactionEmoji.Unicode(OK_EMOJI_SYMBOL)
    }

    private val errorEmoji: ReactionEmoji by lazy {
        ReactionEmoji.Unicode(ERROR_EMOJI_SYMBOL)
    }

    abstract val dispatcherPrefix: String

    suspend fun postMessage(channelId: String, message: String, messageColor: Color = Color.MAGENTA) {
        client.rest.channel.createMessage(channelId) {
            content = ""
            embed {
                description = message
                color = messageColor
            }
        }
    }

    suspend fun postMessage(channel: MessageChannelBehavior, message: String, messageColor: Color = Color.MAGENTA) {
        postMessage(channel.id.value, message, messageColor)
    }

    suspend fun postErrorMessage(channelId: String, message: String, messageColor: Color = Color.MAGENTA) {
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

    suspend fun postErrorMessage(channel: MessageChannelBehavior, message: String, messageColor: Color = Color.MAGENTA) {
        postErrorMessage(channel.id.value, message, messageColor)
    }

    suspend fun respondWithReaction(message: Message, success: Boolean) {
        if (success) {
            message.deleteOwnReaction(errorEmoji)
            message.addReaction(okEmoji)
        } else {
            message.deleteOwnReaction(okEmoji)
            message.addReaction(errorEmoji)
        }
    }

    protected fun getMention(user: DiscordUser) = "<@!${user.id}>"

    protected fun getChannelMention(channel: DiscordChannel) = "<#${channel.id}>"

    protected fun getChannelMention(channelId: String) = "<#${channelId}>"

    fun getCommand(commandName: String, format: Boolean = true): String {
        val commandText = "${BaseBot.BOT_PREFIX}${dispatcherPrefix}${PREFIX_DELIMITER}${commandName}"
        return if (format) {
            "`$commandText`"
        } else {
            commandText
        }
    }

    companion object {
        const val OK_EMOJI_SYMBOL = "\uD83C\uDD97"
        const val ERROR_EMOJI_SYMBOL = "❌"

        const val PREFIX_DELIMITER = "/"
    }
}