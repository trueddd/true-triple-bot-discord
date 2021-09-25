package dispatchers

import dev.kord.core.entity.interaction.Interaction
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent

interface MessageDeleteListener {
    suspend fun onMessageDelete(event: MessageDeleteEvent)
}

interface ReactionAddListener {
    suspend fun onReactionAdd(event: ReactionAddEvent)
}

interface ReactionRemoveListener {
    suspend fun onReactionRemove(event: ReactionRemoveEvent)
}

interface InteractionListener {
    suspend fun onInteractionReceived(interaction: Interaction)
}