package db

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction

class GuildsManager(
    private val database: Database
) {

    suspend fun syncGuilds(ids: List<String>) {
        suspendedTransactionAsync(Dispatchers.IO, database) {
            val saved = Guilds.selectAll().map { it[Guilds.id] }
            Guilds.deleteWhere { Guilds.id inList (saved - ids) }
            (ids - saved).forEach { guild ->
                Guilds.insert {
                    it[id] = guild
                }
            }
        }.await()
    }

    fun removeGuild(id: String) {
        return transaction(database) {
            Guilds.deleteWhere { Guilds.id eq id }
        }
    }

    private fun getString(guildId: String, column: Column<String?>): String? {
        return transaction(database) {
            Guilds.select { Guilds.id eq guildId }.singleOrNull()?.getOrNull(column)
        }
    }

    private fun setString(guildId: String, column: Column<String?>, property: String?): Boolean {
        return transaction(database) {
            if (Guilds.select { Guilds.id eq guildId }.singleOrNull() != null) {
                Guilds.update({ Guilds.id eq guildId }) {
                    it[column] = property
                } > 0
            } else {
                Guilds.insert {
                    it[id] = guildId
                    it[column] = property
                }.resultedValues?.size?.let { it > 0 } ?: false
            }
        }
    }

    fun setRoleGetterEmoji(messageId: String, roleId: String, emoji: String): Boolean {
        return transaction(database) {
            ReactiveRoles.insert {
                it[ReactiveRoles.messageId] = messageId
                it[ReactiveRoles.roleId] = roleId
                it[ReactiveRoles.emoji] = emoji
            }.resultedValues?.size?.let { it > 0 } ?: false
        }
    }

    fun unsetRoleGetter(messageId: String): Boolean {
        return transaction(database) {
            ReactiveRoles.deleteWhere {
                ReactiveRoles.messageId eq messageId
            } > 0
        }
    }

    fun getRoleIdByMessageAndEmoji(messageId: String, emoji: String): String? {
        return transaction(database) {
            ReactiveRoles.select {
                (ReactiveRoles.messageId eq messageId) and (ReactiveRoles.emoji eq emoji)
            }
                .singleOrNull()
                ?.getOrNull(ReactiveRoles.roleId)
        }
    }

    fun isRoleGetterMessage(messageId: String): Boolean {
        return transaction(database) {
            ReactiveRoles.select {
                ReactiveRoles.messageId eq messageId
            }.singleOrNull() != null
        }
    }

    fun getMoviesListChannel(guildId: String): String? {
        return getString(guildId, Guilds.moviesListChannelId)
    }

    fun setMoviesListChannel(guildId: String, channelId: String?): Boolean {
        return setString(guildId, Guilds.moviesListChannelId, channelId)
    }

    fun getWatchedMoviesChannelId(guildId: String): String? {
        return getString(guildId, Guilds.watchedMoviesChannelId)
    }

    fun setWatchedMoviesListChannel(guildId: String, channelId: String?): Boolean {
        return setString(guildId, Guilds.watchedMoviesChannelId, channelId)
    }

    fun getGamesChannelsIds(): List<GameChannel> {
        return transaction(database) {
            Guilds.selectAll().mapNotNull {
                val id = it.getOrNull(Guilds.id) ?: return@mapNotNull null
                val channelId = it.getOrNull(Guilds.gamesChannelId) ?: return@mapNotNull null
                val region = it.getOrNull(Guilds.region)
                GameChannel(id, channelId, region ?: "ru")
            }
        }
    }

    fun setGamesChannel(guildId: String, channelId: String?): Boolean {
        return setString(guildId, Guilds.gamesChannelId, channelId)
    }

    fun getGuildRegion(guildId: String): String? {
        return getString(guildId, Guilds.region)
    }

    fun setGuildRegion(guildId: String, newRegion: String?): Boolean {
        return setString(guildId, Guilds.region, newRegion)
    }

    fun getMoviesNotifyChannel(guildId: String): String? {
        return getString(guildId, Guilds.moviesNotifyChannelId)
    }

    fun setMoviesNotifyChannel(guildId: String, channelId: String?): Boolean {
        return setString(guildId, Guilds.moviesNotifyChannelId, channelId)
    }

    fun getMoviesRoleId(guildId: String): String? {
        return getString(guildId, Guilds.moviesRoleId)
    }

    fun setMoviesRoleId(guildId: String, roleId: String?): Boolean {
        return setString(guildId, Guilds.moviesRoleId, roleId)
    }
}