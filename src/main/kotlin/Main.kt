import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createMessage
import com.gitlab.kordlib.core.event.Event
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.core.on
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
import io.ktor.util.InternalAPI
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.event.Level
import utils.AppEnvironment
import java.awt.Color

@ExperimentalStdlibApi
@InternalAPI
@KtorExperimentalAPI
fun main(args: Array<String>) {
    embeddedServer(Netty, port = AppEnvironment.getPort(), module = Application::module).start(wait = true)
}

@ExperimentalStdlibApi
@InternalAPI
@KtorExperimentalAPI
fun Application.module() {

    GlobalScope.launch {
        val botToken = "NzMxMjgzNTUxNjQyOTc2MjU2.XwjzIQ.NywBGhB4wcRy_PiSal04mQtrF3c"
        val channelId = "719221812424081479"
        val watchedChannelId = "731480303658598461"
        val client = Kord(botToken)

        client.on<ReactionAddEvent> {
            if (channel.id.value == channelId && emoji.name == "✅") {
                println("check mark detected")
                val movieMessage = message.asMessage()
                message.delete()
                client.rest.channel.createMessage(watchedChannelId) {
                    content = movieMessage.content.replace("✅", "").trim()
                }
            }
        }
        client.on<MessageCreateEvent> {
            when (message.content) {
                "!roll" -> {
                    val messages = client.rest.channel.getMessages(channelId, limit = 100)
                    messages
                        .groupBy { it.reactions?.firstOrNull { reaction -> reaction.emoji.name == "\uD83D\uDC4D" }?.count ?: 0 }
                        .maxBy { it.key }
                        ?.value?.randomOrNull()
                        ?.let {
                            message.channel.createBotMessage("**${it.content}** by <@!${it.author.id}> :trophy:")
                        } ?: message.channel.createBotMessage("Nothing to watch :sad_cat:")
                }
                "!roll -show" -> {
                    val messages = client.rest.channel.getMessages(channelId, limit = 100)
                    messages
                        .groupBy { it.reactions?.firstOrNull { reaction -> reaction.emoji.name == "\uD83D\uDC4D" }?.count ?: 0 }
                        .maxBy { it.key }
                        ?.let { entry ->
                            val newText = entry.value.mapIndexed { index, it ->
                                "${index + 1}. ${it.content} by <@!${it.author.id}>"
                            }.joinToString("\n")
                            val votesCount = entry.value.first().reactions?.firstOrNull { it.emoji.name == "\uD83D\uDC4D" }?.count ?: 0
                            message.channel.createBotMessage("Most voted movies ($votesCount votes each):\n$newText")
                        } ?: message.channel.createBotMessage("Nothing to watch :sad_cat:")
                }
            }
        }

        client.on<Event> { println("Received event: $this") }

        client.login()
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

private suspend fun MessageChannelBehavior.createBotMessage(message: String, embedColor: Color = Color.MAGENTA) {
    createMessage {
        content = ""
        embed {
            description = message
            color = embedColor
        }
    }
}