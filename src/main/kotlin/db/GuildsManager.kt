package db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class GuildsManager(
    private val database: Database
) {

    fun getMoviesListChannel(guildId: String): String? {
        return transaction(database) {
            Guilds.select { Guilds.id eq guildId }.singleOrNull()?.getOrNull(Guilds.moviesListChannelId)
        }
    }

    fun setMoviesListChannel(guildId: String, channelId: String?): Boolean {
        return transaction(database) {
            if (Guilds.select { Guilds.id eq guildId }.singleOrNull() != null) {
                Guilds.update({ Guilds.id eq guildId }) {
                    it[moviesListChannelId] = channelId
                } > 0
            } else {
                Guilds.insert {
                    it[id] = guildId
                    it[moviesListChannelId] = channelId
                }.resultedValues?.size?.let { it > 0 } ?: false
            }
        }
    }

    fun getWatchedMoviesChannelId(guildId: String): String? {
        return transaction(database) {
            Guilds.select { Guilds.id eq guildId }.singleOrNull()?.getOrNull(Guilds.watchedMoviesChannelId)
        }
    }

    fun setWatchedMoviesListChannel(guildId: String, channelId: String?): Boolean {
        return transaction(database) {
            if (Guilds.select { Guilds.id eq guildId }.singleOrNull() != null) {
                Guilds.update({ Guilds.id eq guildId }) {
                    it[watchedMoviesChannelId] = channelId
                } > 0
            } else {
                Guilds.insert {
                    it[id] = guildId
                    it[watchedMoviesChannelId] = channelId
                }.resultedValues?.size?.let { it > 0 } ?: false
            }
        }
    }

    fun getGamesChannelId(guildId: String): String? {
        return transaction(database) {
            Guilds.select { Guilds.id eq guildId }.singleOrNull()?.getOrNull(Guilds.gamesChannelId)
        }
    }

//    TODO: add scheduler
//    fun getGamesChannelsIds(): List<String> {
//        return transaction(database) {
//            Guilds.selectAll().mapNotNull { it.getOrNull(Guilds.gamesChannelId) }
//        }
//    }

    fun setGamesChannel(guildId: String, channelId: String?): Boolean {
        return transaction(database) {
            if (Guilds.select { Guilds.id eq guildId }.singleOrNull() != null) {
                Guilds.update({ Guilds.id eq guildId }) {
                    it[gamesChannelId] = channelId
                } > 0
            } else {
                Guilds.insert {
                    it[id] = guildId
                    it[gamesChannelId] = channelId
                }.resultedValues?.size?.let { it > 0 } ?: false
            }
        }
    }
}