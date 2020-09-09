package services.dispatchers

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent

interface MessageCreateListener {
    fun getPrefix(): String
    suspend fun onMessageCreate(event: MessageCreateEvent, trimmedMessage: String)
}

interface ReactionAddListener {
    suspend fun onReactionAdd(event: ReactionAddEvent)
}