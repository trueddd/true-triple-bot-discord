import com.gitlab.kordlib.core.Kord
import db.GuildsManager
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.event.Level
import services.BaseBot.Companion.BOT_PREFIX
import services.EpicGamesService
import services.MainBot
import services.SteamGamesService
import services.TestBot
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

    GlobalScope.launch {
        val client = Kord(AppEnvironment.getBotSecret())

        if (AppEnvironment.isProdEnv()) {
            val bot = MainBot(guildsManager, epicGamesService, steamGamesService, client)
            bot.attach()
        }
        if (AppEnvironment.isTestEnv()) {
            val bot = TestBot(guildsManager, epicGamesService, steamGamesService, client)
            bot.attach()
        }

        client.login {
            listening("${BOT_PREFIX}${Commands.Common.HELP}")
        }
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        anyHost()
    }

    routing {

        get("/") {
            call.respond(HttpStatusCode.OK, "Server is up")
        }
    }
}