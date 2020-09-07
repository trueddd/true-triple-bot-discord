package services

import Dispatcher
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import db.GuildsManager
import io.ktor.util.*
import utils.createBotMessage
import java.awt.Color
import java.util.*

@ExperimentalStdlibApi
@KtorExperimentalAPI
@InternalAPI
class TestBot(
    guildsManager: GuildsManager,
    epicGamesService: EpicGamesService,
    steamGamesService: SteamGamesService,
    client: Kord,
    dispatcher: Dispatcher
) : BaseBot(guildsManager, epicGamesService, steamGamesService, client, dispatcher) {

    override suspend fun attach() {
        client.on<MessageCreateEvent> {
            if (message.content.trim().toLowerCase(Locale("ru")).startsWith("жыве беларусь")) {
                message.channel.createBotMessage("<:flag:752634825818636407><:flag:752634825818636407> ЖЫВЕ! <:flag:752634825818636407><:flag:752634825818636407>", embedColor = Color.RED)
            }

            if (!message.content.startsWith(BOT_PREFIX)) {
                return@on
            }

            val command = message.content.removePrefix(BOT_PREFIX).trim()
            if (command.isEmpty()) {
                return@on
            }

            if (command.startsWith("pick")) {
                val options = command.removePrefix("pick").trim()
                    .split("\n")
                    .mapNotNull { if (it.isEmpty()) null else it }
                if (options.isEmpty()) {
                    dispatcher.markRequest(message, false)
                    return@on
                } else {
                    val chosen = options.randomOrNull() ?: run {
                        dispatcher.markRequest(message, false)
                        return@on
                    }
                    message.channel.createBotMessage(chosen, embedColor = Color.ORANGE)
                }
            }
        }
    }
}