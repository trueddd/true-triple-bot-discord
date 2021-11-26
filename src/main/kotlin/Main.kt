import db.GuildsManager
import dev.kord.common.annotation.KordPreview
import dev.kord.core.Kord
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import utils.AppEnvironment
import utils.provideDatabase

@Suppress("UNUSED_PARAMETER")
fun main(args: Array<String>) {
    embeddedServer(Netty, port = AppEnvironment.getPort(), module = { module() }).start(wait = true)
}

@OptIn(KordPreview::class, DelicateCoroutinesApi::class)
fun module() {

    val database = provideDatabase()
    val guildsManager = GuildsManager(database)

    GlobalScope.launch {
        val client = Kord(AppEnvironment.getBotSecret())

        val bot = MainBot(guildsManager, client)
        bot.checkGuilds()
        bot.attach()

        client.login {
            listening("slash commands \uD83D\uDE0E")
        }
    }
}