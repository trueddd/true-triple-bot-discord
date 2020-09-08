package services

import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import db.GuildsManager

class TestBot(
    guildsManager: GuildsManager,
    epicGamesService: EpicGamesService,
    steamGamesService: SteamGamesService,
    client: Kord
) : BaseBot(guildsManager, epicGamesService, steamGamesService, client) {

    override suspend fun attach() {
        client.on<MessageCreateEvent> {
        }
    }
}