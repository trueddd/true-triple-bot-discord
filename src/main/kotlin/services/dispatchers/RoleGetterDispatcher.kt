package services.dispatchers

import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageDeleteEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.core.event.message.ReactionRemoveEvent
import db.GuildsManager

class RoleGetterDispatcher(
    private val guildsManager: GuildsManager,
    client: Kord
) : BaseDispatcher(client), ReactionAddListener, ReactionRemoveListener, MessageDeleteListener {

    override val dispatcherPrefix: String
        get() = ""

    override suspend fun onReactionAdd(event: ReactionAddEvent) {
        val guildId = event.guildId?.value ?: ""
        val messageId = event.message.id.value
        val emoji = event.emoji.name
        val roleId = guildsManager.getRoleIdByMessageAndEmoji(messageId, emoji) ?: return
        event.user.asMember(Snowflake(guildId)).addRole(Snowflake(roleId))
    }

    override suspend fun onReactionRemove(event: ReactionRemoveEvent) {
        val guildId = event.guildId?.value ?: ""
        val messageId = event.message.id.value
        val emoji = event.emoji.name
        val roleId = guildsManager.getRoleIdByMessageAndEmoji(messageId, emoji) ?: return
        event.user.asMember(Snowflake(guildId)).removeRole(Snowflake(roleId))
    }

    override suspend fun onMessageDelete(event: MessageDeleteEvent) {
        val messageId = event.messageId.value
        if (guildsManager.isRoleGetterMessage(messageId)) {
            guildsManager.unsetRoleGetter(messageId)
        }
    }
}