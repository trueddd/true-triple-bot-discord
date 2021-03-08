package dispatchers

import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent

interface MessageCreateListener {
    fun getPrefix(): String
    suspend fun onMessageCreate(event: MessageCreateEvent, trimmedMessage: String)
}
interface MessageDeleteListener {
    suspend fun onMessageDelete(event: MessageDeleteEvent)
}

interface ReactionAddListener {
    suspend fun onReactionAdd(event: ReactionAddEvent)
}
interface ReactionRemoveListener {
    suspend fun onReactionRemove(event: ReactionRemoveEvent)
}