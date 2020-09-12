package db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class GuildsManager(
    private val database: Database
) {

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

    fun getGamesChannelsIds(): List<Pair<String, String>> {
        return transaction(database) {
            Guilds.selectAll().mapNotNull {
                val id = it.getOrNull(Guilds.id) ?: return@mapNotNull null
                val channelId = it.getOrNull(Guilds.gamesChannelId) ?: return@mapNotNull null
                id to channelId
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