package dispatchers.games

import data.gog.Product
import dev.kord.common.Color
import dev.kord.common.entity.optional.optional
import dev.kord.core.Kord
import dev.kord.rest.json.request.EmbedAuthorRequest
import dev.kord.rest.json.request.EmbedFieldRequest
import dev.kord.rest.json.request.EmbedRequest
import dispatchers.services.BaseGamesService
import dispatchers.services.GogGamesService

class GogDispatcher(kord: Kord) : BaseGameDispatcher<Product>(kord) {

    override val color: Color
        get() = Color(104, 0, 209)

    override val name: String
        get() = "GOG"

    override val service: BaseGamesService<Product> by lazy {
        GogGamesService()
    }

    override fun buildEmbed(data: List<Product>): EmbedRequest {
        val takeFirst = 15
        return EmbedRequest(
            color = color.optional(),
            author = EmbedAuthorRequest(
                name = name.optional(),
                url = "https://www.gog.com/games?sort=popularity&page=1&tab=on_sale".optional(),
                iconUrl = "https://dl2.macupdate.com/images/icons256/54428.png".optional(),
            ).optional(),
            fields = data.take(takeFirst).map { product ->
                EmbedFieldRequest(
                    name = product.title,
                    value = if (product.isPriceVisible && product.price != null && product.localPrice != null) {
                        buildString {
                            append("[")
                            if (product.price.isDiscounted) {
                                append("~~${product.localPrice?.base}~~ ")
                            }
                            append(product.localPrice?.final)
                            append("]")
                            append("(${product.urlFormatted})")
                        }
                    } else {
                        "[${product.developer}](${product.urlFormatted})"
                    },
                    inline = true.optional(),
                )
            }.optional(),
        )
    }
}
