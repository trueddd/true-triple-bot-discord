package services

import Dispatcher
import com.gitlab.kordlib.core.Kord
import db.GuildsManager
import io.ktor.util.*

@KtorExperimentalAPI
@InternalAPI
abstract class BaseBot(
    protected val guildsManager: GuildsManager,
    protected val epicGamesService: EpicGamesService,
    protected val steamGamesService: SteamGamesService,
    protected val client: Kord,
    protected val dispatcher: Dispatcher
) {

    abstract suspend fun attach()

    companion object {
        const val BOT_PREFIX = "ttb!"
    }
}