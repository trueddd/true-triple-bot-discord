package dispatchers

import data.nintendo.Game
import dev.kord.common.Color
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.rest.json.request.EmbedAuthorRequest
import dev.kord.rest.json.request.EmbedFieldRequest
import dev.kord.rest.json.request.EmbedRequest
import services.BaseGamesService
import services.NintendoGamesService

class NintendoDispatcher(kord: Kord) : BaseGameDispatcher<Game>(kord) {

    override val color: Color
        get() = Color(230, 7, 18)

    override val name: String
        get() = "Nintendo Store"

    override val service: BaseGamesService<Game> by lazy {
        NintendoGamesService()
    }

    override fun buildEmbed(data: List<Game>): EmbedRequest {
        return EmbedRequest(
            color = color.optional(),
            author = EmbedAuthorRequest(
                name = name.optional(),
                url = "https://www.nintendo.ru/-/--299117.html?f=147394-5-82-6955".optional(),
                iconUrl = "https://www.pngitem.com/pimgs/m/110-1107244_clip-art-for-free-download-nintendo-switch-famicom.png".optional(),
            ).optional(),
            fields = data.take(15).map { game ->
                EmbedFieldRequest(
                    name = game.title,
                    value = "[~~${game.regularPrice} ₽~~ ${game.lowestPrice} ₽](https://www.nintendo.ru${game.url})",
                    inline = true.optional(),
                )
            }.optional()
        )
    }
}
