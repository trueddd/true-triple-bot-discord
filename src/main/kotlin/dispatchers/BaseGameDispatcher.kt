package dispatchers

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.Interaction
import dev.kord.rest.json.request.EmbedRequest
import dev.kord.rest.json.request.MessageCreateRequest
import dev.kord.rest.json.request.MultipartMessageCreateRequest
import services.BaseGamesService

// todo: refactor game dispatchers to inherit this class
abstract class BaseGameDispatcher<T>(
    client: Kord,
) : BaseDispatcher(client) {

    abstract val color: Color

    abstract val name: String

    abstract fun buildEmbed(data: List<T>): EmbedRequest

    abstract val service: BaseGamesService<T>

    private fun getErrorMessage() = "Ошибка при попытке получения данных из $name"

    suspend fun postGamesEmbed(channelId: Snowflake, data: List<T>?) {
        if (data.isNullOrEmpty()) {
            postErrorMessage(channelId, getErrorMessage(), color)
            return
        }
        client.rest.channel.createMessage(
            channelId,
            MultipartMessageCreateRequest(
                MessageCreateRequest(
                    embeds = listOf(buildEmbed(data)).optional(),
                )
            )
        )
    }

    suspend fun postInteractionResponse(interaction: Interaction, data: List<T>?) {
        if (data.isNullOrEmpty()) {
            postErrorMessage(interaction, getErrorMessage())
        } else {
            createEmbedResponse(interaction, buildEmbed(data))
        }
    }
}
