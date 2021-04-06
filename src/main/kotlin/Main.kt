import db.GuildsManager
import dev.kord.core.Kord
import io.ktor.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import services.*
import utils.AppEnvironment
import utils.Commands
import utils.provideDatabase

fun main(args: Array<String>) {
    embeddedServer(Netty, port = AppEnvironment.getPort(), module = Application::module).start(wait = true)
}

fun Application.module() {

    val database = provideDatabase()
    val guildsManager = GuildsManager(database)
    val epicGamesService = EpicGamesService(database)
    val steamGamesService = SteamGamesService(database)
    val gogGamesService = GogGamesService(database)
    val crackedGamesService = CrackedGamesService(database)
    val minecraftService = MinecraftService(database)

    GlobalScope.launch {
        val client = Kord(AppEnvironment.getBotSecret())

        val bot = MainBot(guildsManager, epicGamesService, steamGamesService, gogGamesService, crackedGamesService, minecraftService, client)
        bot.checkGuilds()
        bot.attach()

        client.login {
            listening("${AppEnvironment.BOT_PREFIX}${Commands.Common.HELP}")
        }
    }
}