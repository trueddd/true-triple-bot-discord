package dispatchers

import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.common.entity.optional.Optional
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.Interaction
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.json.request.EmbedAuthorRequest
import dev.kord.rest.json.request.EmbedRequest
import dev.kord.rest.json.request.InteractionApplicationCommandCallbackData
import dev.kord.rest.json.request.InteractionResponseCreateRequest

abstract class BaseDispatcher(protected val client: Kord) {

    private val magentaColor = Color(255, 0, 255)

    private suspend fun respondToInteraction(
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

    private fun createFlags(onlyForUser: Boolean): MessageFlags {
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
        components: List<DiscordComponent>? = null,
        onlyForUser: Boolean = false,
    ) {
        respondToInteraction(
            integration,
            InteractionApplicationCommandCallbackData(
                embeds = listOf(embedRequest).optional(),
                flags = createFlags(onlyForUser).optional(),
                components = components?.let { elements ->
                    listOf(
                        DiscordComponent(
                            ComponentType.ActionRow,
                            components = elements.optional()
                        )
                    ).optional()
                } ?: Optional.Missing(),
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
}