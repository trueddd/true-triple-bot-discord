package dispatchers.games

import data.cracked.Game
import dev.kord.common.Color
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.rest.json.request.EmbedAuthorRequest
import dev.kord.rest.json.request.EmbedFieldRequest
import dev.kord.rest.json.request.EmbedFooterRequest
import dev.kord.rest.json.request.EmbedRequest
import dispatchers.services.BaseGamesService
import dispatchers.services.CrackedGamesService

class CrackedDispatcher(kord: Kord) : BaseGameDispatcher<Game>(kord) {

    override val color: Color
        get() = Color(66, 182, 29)

    override val name: String
        get() = "Cracked games"

    override val service: BaseGamesService<Game> by lazy {
        CrackedGamesService()
    }

    override fun buildEmbed(data: List<Game>): EmbedRequest {
        return EmbedRequest(
            color = color.optional(),
            author = EmbedAuthorRequest(
                name = name.optional(),
                url = "https://gamestatus.info/crackedgames".optional(),
                iconUrl = "https://gamestatus.info/favicon.ico".optional(),
            ).optional(),
            fields = data.map { game ->
                EmbedFieldRequest(
                    name = game.title,
                    value = "[Взломано ${game.crackDate!!.formatContext()}](https://gamestatus.info/${game.slug})",
                    inline = true.optional(),
                )
            }.optional(),
            footer = EmbedFooterRequest("Powered by GameStatus.info").optional(),
        )
    }
}
