package dispatchers

import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.common.entity.optional.Optional
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.interaction.Interaction
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.json.request.*
import utils.AppEnvironment

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

    protected suspend fun respondToInteraction(
        integration: Interaction,
        data: Optional<InteractionApplicationCommandCallbackData>,
    ) {
        client.rest.interaction.createInteractionResponse(
            integration.id,
            integration.token,
            InteractionResponseCreateRequest(
                InteractionResponseType.ChannelMessageWithSource,
                data,
            )
        )
    }

    protected fun createFlags(
        onlyForUser: Boolean,
    ): MessageFlags {
        return MessageFlags.Builder()
            .apply {
                if (onlyForUser) {
                    + MessageFlag.Ephemeral
                }
            }
            .flags()
    }

    protected suspend fun createTextResponse(
        integration: Interaction,
        content: String,
        onlyForUser: Boolean = false,
    ) {
        respondToInteraction(
            integration,
            InteractionApplicationCommandCallbackData(
                content = content.optional(),
                flags = createFlags(onlyForUser).optional(),
            ).optional(),
        )
    }

    protected suspend fun createEmbedResponse(
        integration: Interaction,
        embedRequest: EmbedRequest,
        onlyForUser: Boolean = false,
    ) {
        respondToInteraction(
            integration,
            InteractionApplicationCommandCallbackData(
                embeds = listOf(embedRequest).optional(),
                flags = createFlags(onlyForUser).optional(),
            ).optional(),
        )
    }

    suspend fun postErrorMessage(integration: Interaction, message: String) {
        createEmbedResponse(
            integration,
            EmbedRequest(
                color = magentaColor.optional(),
                author = EmbedAuthorRequest(
                    name = message.optional(),
                    iconUrl = "https://cdn.discordapp.com/emojis/722871552290455563.png?v=1".optional(),
                ).optional(),
            )
        )
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
        val commandText = "${AppEnvironment.BOT_PREFIX}${commandPrefix}${if (commandPrefix.isEmpty()) "" else PREFIX_DELIMITER}${commandName}"
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