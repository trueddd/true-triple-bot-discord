package dispatchers.games

import data.steam.SteamGame
import dev.kord.common.Color
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.rest.json.request.EmbedAuthorRequest
import dev.kord.rest.json.request.EmbedFieldRequest
import dev.kord.rest.json.request.EmbedRequest
import dispatchers.services.BaseGamesService
import dispatchers.services.SteamGamesService

class SteamDispatcher(kord: Kord) : BaseGameDispatcher<SteamGame>(kord) {

    override val color: Color
        get() = Color(27, 40, 56)

    override val name: String
        get() = "Steam"

    override val service: BaseGamesService<SteamGame> by lazy {
        SteamGamesService()
    }

    override fun buildEmbed(data: List<SteamGame>): EmbedRequest {
        return EmbedRequest(
            color = color.optional(),
            author = EmbedAuthorRequest(
                name = name.optional(),
                iconUrl = "https://upload.wikimedia.org/wikipedia/commons/c/c1/Steam_Logo.png".optional(),
                url = "https://store.steampowered.com/specials#p=0&tab=TopSellers".optional(),
            ).optional(),
            fields = data.map { steamGame ->
                EmbedFieldRequest(
                    name = steamGame.name.let { if (it.length <= 64) it else "${it.take(64)}..." },
                    value = if (steamGame.price.originalPrice != null && steamGame.price.currentPrice != null) {
                        buildString {
                            append("[")
                            if (steamGame.price.originalPrice.isNotEmpty()) {
                                append("~~${steamGame.price.originalPrice}~~ ")
                            }
                            append(steamGame.price.currentPrice)
                            append("]")
                            append("(${steamGame.url})")
                        }
                    } else {
                        "[Bundle ${steamGame.price.discount}](${steamGame.url})"
                    },
                    inline = true.optional()
                )
            }.optional()
        )
    }
}
