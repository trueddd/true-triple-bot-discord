package services.dispatchers

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.MessageDeleteEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.core.event.message.ReactionRemoveEvent

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