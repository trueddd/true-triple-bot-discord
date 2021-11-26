package dispatchers.games

import data.egs.GiveAwayGame
import dev.kord.common.Color
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.rest.json.request.EmbedAuthorRequest
import dev.kord.rest.json.request.EmbedFieldRequest
import dev.kord.rest.json.request.EmbedRequest
import dispatchers.services.BaseGamesService
import dispatchers.services.EpicGamesService
import java.time.LocalDateTime

class EgsDispatcher(kord: Kord) : BaseGameDispatcher<GiveAwayGame>(kord) {

    override val color: Color
        get() = Color(12, 12, 12)

    override val name: String
        get() = "Epic Games Store"

    override val service: BaseGamesService<GiveAwayGame> by lazy {
        EpicGamesService()
    }

    override fun buildEmbed(data: List<GiveAwayGame>): EmbedRequest {
        val now = LocalDateTime.now()
        return EmbedRequest(
            color = color.optional(),
            author = EmbedAuthorRequest(
                name = name.optional(),
                url = "https://www.epicgames.com/store/en-US/free-games".optional(),
                iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/31/Epic_Games_logo.svg/1200px-Epic_Games_logo.svg.png".optional(),
            ).optional(),
            fields = data.map { giveAwayGame ->
                EmbedFieldRequest(
                    giveAwayGame.title,
                    inline = true.optional(),
                    value = giveAwayGame.let { game ->
                        val date = when {
                            game.promotion == null -> "Free"
                            now.isBefore(game.promotion.start) -> game.promotion.start.format()?.let { "с $it" } ?: "N/A"
                            now.isAfter(game.promotion.start) && now.isBefore(game.promotion.end) -> game.promotion.end.format()?.let { "до $it" } ?: "N/A"
                            else -> "Free"
                        }
                        val link = when {
                            game.productSlug.isNullOrEmpty() -> "https://www.epicgames.com/store/en-US/free-games"
                            else -> "https://www.epicgames.com/store/en-US/product/${game.productSlug}"
                        }
                        "[$date]($link)"
                    }
                )
            }.optional()
        )
    }
}
