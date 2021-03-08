package dispatchers

import db.GuildsManager
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent

class RoleGetterDispatcher(
    private val guildsManager: GuildsManager,
    client: Kord
) : BaseDispatcher(client), ReactionAddListener, ReactionRemoveListener, MessageDeleteListener {

    override val dispatcherPrefix: String
        get() = ""

    override suspend fun onReactionAdd(event: ReactionAddEvent) {
        val guildId = event.guildId?.asString ?: ""
        val messageId = event.message.id.asString
        val emoji = event.emoji.name
        val roleId = guildsManager.getRoleIdByMessageAndEmoji(messageId, emoji) ?: return
        event.user.asMember(Snowflake(guildId)).addRole(Snowflake(roleId))
    }

    override suspend fun onReactionRemove(event: ReactionRemoveEvent) {
        val guildId = event.guildId?.asString ?: ""
        val messageId = event.message.id.asString
        val emoji = event.emoji.name
        val roleId = guildsManager.getRoleIdByMessageAndEmoji(messageId, emoji) ?: return
        event.user.asMember(Snowflake(guildId)).removeRole(Snowflake(roleId))
    }

    override suspend fun onMessageDelete(event: MessageDeleteEvent) {
        val messageId = event.messageId.asString
        if (guildsManager.isRoleGetterMessage(messageId)) {
            guildsManager.unsetRoleGetter(messageId)
        }
    }
}