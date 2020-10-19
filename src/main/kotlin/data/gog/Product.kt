package data.gog

data class Product(
    val developer: String,
    val genres: List<String>,
    val globalReleaseDate: Int?,
    val id: Int,
    val image: String?,
    val isDiscounted: Boolean,
    val isGame: Boolean,
    val isInDevelopment: Boolean,
    val isPriceVisible: Boolean,
    val price: Price?,
    val slug: String,
    val title: String,
    val url: String,
) {

    val urlFormatted: String
        get() = "https://www.gog.com/$url"
}