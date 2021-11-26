package data.nintendo

data class Response(
    val response: GamesContainer,
)

data class GamesContainer(
    val docs: List<Game>,
)
