package dispatchers

import db.GuildsManager
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.CommandArgument
import dev.kord.common.entity.ComponentType
import dev.kord.common.entity.DiscordComponent
import dev.kord.common.entity.optional.Optional
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.Interaction
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.json.request.EmbedAuthorRequest
import dev.kord.rest.json.request.EmbedImageRequest
import dev.kord.rest.json.request.EmbedRequest
import utils.Commands
import utils.isSentByAdmin
import utils.issuedByAdmin
import utils.replaceIfMatches
import java.net.URL

class CommonDispatcher(
    private val guildsManager: GuildsManager,
    client: Kord,
) : BaseDispatcher(client), MessageCreateListener, InteractionListener {

    override val dispatcherPrefix: String
        get() = ""

    override fun getPrefix(): String {
        return dispatcherPrefix
    }

    private val roleSet = Regex("^${Commands.Common.ROLE_GETTER}\\s+<@&(\\d+)>\\s+<:(.+):\\d+>.*$")

    override suspend fun onInteractionReceived(interaction: Interaction) {
        val command = interaction.data.data.name.value
        when (command) {
            Commands.Common.LOCALE -> setLocale(interaction)
            Commands.Common.POLL -> createPoll(interaction)
        }
    }

    override suspend fun onMessageCreate(event: MessageCreateEvent, trimmedMessage: String) {
        when {
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

    private suspend fun createPoll(interaction: Interaction) {
        val text = interaction.data.data.options.value
            ?.firstOrNull { it.name == "text" }?.value?.value
            ?.let { it as? CommandArgument.StringArgument }?.value
            ?: run {
                createTextResponse(interaction, "Error occurred while creating poll", onlyForUser = true)
                return
            }
        val link = interaction.data.data.options.value
            ?.firstOrNull { it.name == "url" }?.value?.value
            ?.let { it as? CommandArgument.StringArgument }?.value
            ?.let { if (it.isUrl()) it else null }
        val options = interaction.data.data.options.value
            ?.firstOrNull { it.name == "options" }?.value?.value
            ?.let { it as? CommandArgument.IntegerArgument }?.value ?: 2
        val author = interaction.user.asUser()

        // todo: make a followup message
        createEmbedResponse(
            interaction,
            EmbedRequest(
                author = EmbedAuthorRequest(
                    iconUrl = author.avatar.url.optional(),
                    name = author.username.optional(),
                ).optional(),
                color = Color(185, 185, 0).optional(),
                description = text.optional(),
                image = link?.let { EmbedImageRequest(it) }?.optional() ?: Optional.Missing(),
            ),
            components = (1..options).map {
                DiscordComponent(
                    ComponentType.Button,
                    style = ButtonStyle.Primary.optional(),
                    label = "$it".optional(),
                    customId = "$it".optional(),
                )
            }
        )
//        client.rest.channel.createReaction(newMessage.channelId, newMessage.id, "\uD83D\uDC4D")
//        client.rest.channel.createReaction(newMessage.channelId, newMessage.id, "\uD83D\uDC4E")
//        message.delete()
    }

    private suspend fun setLocale(interaction: Interaction) {
        if (!interaction.issuedByAdmin) {
            createTextResponse(
                interaction,
                "You are not allowed to use this command.",
                onlyForUser = true,
            )
            return
        }
        val region = interaction.data.data.options.value
            ?.also { println("options: $it") }
            ?.firstOrNull { it.name == "region" }
            ?.value?.value
            ?.let { it as? CommandArgument.StringArgument }?.value
        when {
            guildsManager.setGuildRegion(interaction.data.guildId.value!!.asString, region) -> {
                createTextResponse(
                    interaction,
                    "Guild region was set to **$region**",
                    onlyForUser = true,
                )
            }
            else -> {
                createTextResponse(
                    interaction,
                    "Error occurred while setting guild locale.",
                    onlyForUser = true,
                )
            }
        }
    }
}