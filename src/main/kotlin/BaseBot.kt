import dev.kord.core.Kord

abstract class BaseBot(
    protected val client: Kord,
) {

    abstract suspend fun attach()
}