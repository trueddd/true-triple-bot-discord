package dispatchers

import db.GuildsManager
import dev.kord.common.Color
import dev.kord.common.entity.CommandArgument
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.Optional
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.Interaction
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.json.request.EmbedAuthorRequest
import dev.kord.rest.json.request.EmbedImageRequest
import dev.kord.rest.json.request.EmbedRequest
import kotlinx.coroutines.flow.*
import utils.Commands
import utils.commandArgument
import utils.issuedByAdmin
import java.net.URL

class CommonDispatcher(
    private val guildsManager: GuildsManager,
    client: Kord,
) : BaseDispatcher(client), InteractionListener {

    private val emoteRegex = Regex("<:(?<id>.+):\\d+>")

    override suspend fun onInteractionReceived(interaction: Interaction) {
        when (interaction.data.data.name.value) {
            Commands.Common.LOCALE -> setLocale(interaction)
            Commands.Common.POLL -> createPoll(interaction)
            Commands.Common.ROLE_GETTER -> setupRoleGetter(interaction)
        }
    }

    private suspend fun setupRoleGetter(interaction: Interaction) {
        if (!interaction.issuedByAdmin) {
            createTextResponse(
                interaction,
                "You are not allowed to use this command.",
                onlyForUser = true,
            )
            return
        }
        val messageId = interaction.commandArgument<CommandArgument.StringArgument>("message")?.value
            ?.let { try { Snowflake(it) } catch (e: Exception) { null } }
            ?: run {
                createTextResponse(
                    interaction,
                    "Couldn\'t understand which message should be observed",
                    onlyForUser = true
                )
                return
            }
        val role = interaction.commandArgument<CommandArgument.RoleArgument>("role")?.value
            ?: run {
                createTextResponse(
                    interaction,
                    "Couldn\'t understand which role to give",
                    onlyForUser = true
                )
                return
            }
        val emote = interaction.commandArgument<CommandArgument.StringArgument>("emote")?.value
            ?.let { emoteRegex.matchEntire(it)?.groups?.get("id")?.value }
            ?: run {
                createTextResponse(
                    interaction,
                    "Couldn\'t parse emote",
                    onlyForUser = true
                )
                return
            }
        when {
            guildsManager.setRoleGetterEmoji(messageId.asString, role.asString, emote) -> {
                createTextResponse(
                    interaction,
                    "Role setter set up successfully",
                    onlyForUser = true,
                )
            }
            else -> {
                createTextResponse(
                    interaction,
                    "Something went wrong",
                    onlyForUser = true,
                )
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
        val author = interaction.user.asUser()

        client.events
            .filterIsInstance<MessageCreateEvent>()
            .filter { it.message.data.interaction.value?.id == interaction.id }
            .take(1)
            .onEach {
                client.rest.channel.createReaction(it.message.channelId, it.message.id, "\uD83D\uDC4D")
                client.rest.channel.createReaction(it.message.channelId, it.message.id, "\uD83D\uDC4E")
            }
            .launchIn(client)
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
        )
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