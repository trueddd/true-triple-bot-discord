package dispatchers.games

import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.core.entity.interaction.Interaction
import dev.kord.rest.json.request.EmbedRequest
import dev.kord.rest.json.request.MessageCreateRequest
import dev.kord.rest.json.request.MultipartMessageCreateRequest
import dispatchers.BaseDispatcher
import dispatchers.services.BaseGamesService
import io.ktor.util.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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

    protected fun LocalDateTime.format(pattern: String = "d MMMM", locale: Locale = Locale("ru")): String? {
        return try {
            val outFormatter = DateTimeFormatter.ofPattern(pattern, locale)
            outFormatter.format(this)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    protected fun Date.formatContext(pattern: String = "d MMMM", locale: Locale = Locale("ru")): String? {
        val todayDays = Date().days()
        return when (this.days()) {
            todayDays -> "сегодня"
            todayDays - 1 -> "вчера"
            else -> this.toLocalDateTime().format(pattern, locale)
        }
    }

    protected fun Date.days(): Long {
        return this.time / 86_400_000
    }
}
