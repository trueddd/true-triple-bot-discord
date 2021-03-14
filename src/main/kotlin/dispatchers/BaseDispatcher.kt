package dispatchers

import dev.kord.common.Color
import dev.kord.common.entity.DiscordUser
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import services.BaseBot

abstract class BaseDispatcher(protected val client: Kord) {

    protected val magentaColor = Color(255, 0, 255)

    private val okEmoji: ReactionEmoji by lazy {
        ReactionEmoji.Unicode(OK_EMOJI_SYMBOL)
    }

    private val errorEmoji: ReactionEmoji by lazy {
        ReactionEmoji.Unicode(ERROR_EMOJI_SYMBOL)
    }

    abstract val dispatcherPrefix: String

    suspend fun postMessage(channelId: Snowflake, message: String, messageColor: Color = magentaColor) {
        client.rest.channel.createMessage(channelId) {
            content = ""
            embed {
                description = message
                color = messageColor
            }
        }
    }

    suspend fun postMessage(channel: MessageChannelBehavior, message: String, messageColor: Color = magentaColor) {
        postMessage(channel.id, message, messageColor)
    }

    suspend fun postErrorMessage(channelId: Snowflake, message: String, messageColor: Color = magentaColor) {
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

    suspend fun postErrorMessage(channel: MessageChannelBehavior, message: String, messageColor: Color = magentaColor) {
        postErrorMessage(channel.id, message, messageColor)
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

    protected fun getMention(user: DiscordUser) = "<@!${user.id.asString}>"

    protected fun getChannelMention(channelId: String) = "<#${channelId}>"

    fun getCommand(commandName: String, customPrefix: String? = null, format: Boolean = true): String {
        val commandPrefix = customPrefix ?: dispatcherPrefix
        val commandText = "${BaseBot.BOT_PREFIX}${commandPrefix}${if (commandPrefix.isEmpty()) "" else PREFIX_DELIMITER}${commandName}"
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