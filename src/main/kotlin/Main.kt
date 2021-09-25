import db.GuildsManager
import dev.kord.common.annotation.KordPreview
import dev.kord.core.Kord
import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import services.*
import utils.AppEnvironment
import utils.provideDatabase

fun main(args: Array<String>) {
    embeddedServer(Netty, port = AppEnvironment.getPort(), module = Application::module).start(wait = true)
}

@OptIn(KordPreview::class, DelicateCoroutinesApi::class)
fun Application.module() {

    val database = provideDatabase()
    val guildsManager = GuildsManager(database)
    val epicGamesService = EpicGamesService(database)
    val steamGamesService = SteamGamesService(database)
    val gogGamesService = GogGamesService(database)
    val crackedGamesService = CrackedGamesService(database)

    GlobalScope.launch {
        val client = Kord(AppEnvironment.getBotSecret())

        val bot = MainBot(guildsManager, epicGamesService, steamGamesService, gogGamesService, crackedGamesService, client)
        bot.checkGuilds()
        bot.attach()

        client.login {
            listening("slash commands \uD83D\uDE0E")
        }
    }
}