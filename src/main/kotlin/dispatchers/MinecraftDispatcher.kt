package dispatchers

import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import data.minecraft.StatusResponse
import db.GuildsManager
import services.MinecraftService
import utils.Commands
import utils.commandRegex
import utils.isSentByAdmin
import java.awt.Color

class MinecraftDispatcher(
    private val guildsManager: GuildsManager,
    private val minecraftService: MinecraftService,
    client: Kord,
) : BaseDispatcher(client), MessageCreateListener {

    override val dispatcherPrefix: String
        get() = "minecraft"

    override fun getPrefix(): String {
        return dispatcherPrefix
    }

    private val set = Commands.Minecraft.SET.commandRegex(singleWordCommand = false)

    override suspend fun onMessageCreate(event: MessageCreateEvent, trimmedMessage: String) {
        println("entered dispatcher")
        val guildId = event.message.getGuild().id.value
        val channelId = event.message.channelId.value
        when {
            set.matches(trimmedMessage) -> {
                println("set")
                if (!event.isSentByAdmin()) {
                    respondWithReaction(event.message, false)
                    return
                }
                val newIp = trimmedMessage.removePrefix(Commands.Minecraft.SET).trim()
                println("new IP: $newIp")
                val changed = guildsManager.setMinecraftServerIp(guildId, newIp)
                respondWithReaction(event.message, changed)
            }
            else -> {
                println("else")
                val serverIp = guildsManager.getMinecraftServerIp(guildId) ?: run {
                    respondWithReaction(event.message, false)
                    return
                }
                val status = minecraftService.load(serverIp)
                showMinecraftServerStatus(channelId, status)
            }
        }
    }

    private suspend fun showMinecraftServerStatus(channelId: String, statusResponse: StatusResponse?) {
        val messageColor = Color(48, 116, 26)
        if (statusResponse == null) {
            postErrorMessage(channelId, "Не смог получить информацию о сервере", messageColor)
            return
        }
        client.rest.channel.createMessage(channelId) {
            embed {
                color = messageColor
                if (statusResponse.online) {
                    title = "Сервер Online \uD83D\uDFE2"
                    description = "Кубоёбов на сервере: ${statusResponse.players?.count ?: 0}"
                } else {
                    title = "Сервер Offline \uD83D\uDD34"
                }
                statusResponse.hostname?.let {
                    field {
                        name = "Адрес сервера"
                        value = "${statusResponse.hostname}"
                    }
                }
            }
        }
    }
}