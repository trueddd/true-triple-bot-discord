package dispatchers

import db.GuildsManager
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.CommandArgument
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.Interaction
import dispatchers.games.*
import utils.Commands
import utils.issuedByAdmin

class GamesDispatcher(
    private val guildsManager: GuildsManager,
    private val egsDispatcher: EgsDispatcher,
    private val steamDispatcher: SteamDispatcher,
    private val gogDispatcher: GogDispatcher,
    private val crackedDispatcher: CrackedDispatcher,
    private val nintendoDispatcher: NintendoDispatcher,
    client: Kord,
) : BaseDispatcher(client), InteractionListener {

    private suspend fun <T> postGamesInteraction(interaction: Interaction, dispatcher: BaseGameDispatcher<T>) {
        val region = guildsManager.getGuildRegion(interaction.data.guildId.value!!.asString) ?: "ru"
        val games = dispatcher.service.load(listOf(region))?.get(region)
        dispatcher.postInteractionResponse(interaction, games)
    }

    override suspend fun onInteractionReceived(interaction: Interaction) {
        when (interaction.data.data.options.value?.firstOrNull()?.name) {
            Commands.Games.EGS -> postGamesInteraction(interaction, egsDispatcher)
            Commands.Games.GOG -> postGamesInteraction(interaction, gogDispatcher)
            Commands.Games.STEAM -> postGamesInteraction(interaction, steamDispatcher)
            Commands.Games.CRACKED -> postGamesInteraction(interaction, crackedDispatcher)
            Commands.Games.NINTENDO -> postGamesInteraction(interaction, nintendoDispatcher)
            Commands.Games.SET -> setGamesChannel(interaction)
            Commands.Games.UNSET -> unsetGamesChannel(interaction)
        }
    }

    private suspend fun unsetGamesChannel(interaction: Interaction) {
        if (!interaction.issuedByAdmin) {
            createTextResponse(
                interaction,
                "You are not allowed to use this command.",
                onlyForUser = true,
            )
            return
        }
        when {
            guildsManager.setGamesChannel(interaction.data.guildId.value!!.asString, null) -> {
                createTextResponse(
                    interaction,
                    "Successfully cancelled games notifications",
                    onlyForUser = true,
                )
            }
            else -> createTextResponse(
                interaction,
                "Error occurred while cancelling game notifications",
                onlyForUser = true,
            )
        }
    }

    private suspend fun setGamesChannel(interaction: Interaction) {
        if (!interaction.issuedByAdmin) {
            createTextResponse(
                interaction,
                "You are not allowed to use this command.",
                onlyForUser = true,
            )
            return
        }
        val channelId = interaction.data.data.options.value
            ?.firstOrNull { it.name == "set" }?.values?.value
            ?.filterIsInstance<CommandArgument.ChannelArgument>()
            ?.firstOrNull { it.name == "channel" }?.value
            ?: run {
                createTextResponse(
                    interaction,
                    "Couldn't get channel ID.",
                    onlyForUser = true,
                )
                return
            }
        val channel = try {
            client.rest.channel.getChannel(channelId)
        } catch (e: Exception) {
            null
        }
        when {
            channel == null -> createTextResponse(interaction, "Channel not found", onlyForUser = true)
            channel.type != ChannelType.GuildText && channel.type != ChannelType.GuildNews -> {
                createTextResponse(interaction, "You can only pass text channel", onlyForUser = true)
            }
            guildsManager.setGamesChannel(interaction.data.guildId.value!!.asString, channelId.asString) -> {
                val channelName = channel.name.value ?: channelId
                createTextResponse(
                    interaction,
                    "Successfully set games channel to **$channelName**",
                    onlyForUser = true,
                )
            }
            else -> createTextResponse(
                interaction,
                "Error occurred while setting game channel",
                onlyForUser = true,
            )
        }
    }
}
