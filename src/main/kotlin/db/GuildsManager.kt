package db

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.AppEnvironment
import java.net.URI

class GuildsManager {

    private val database: Database by lazy {
        val uri = URI(AppEnvironment.getDatabaseUrl())
        val username = uri.userInfo.split(":")[0]
        val password = uri.userInfo.split(":")[1]
        val dbUrl = "jdbc:postgresql://${uri.host}:${uri.port}${uri.path}?sslmode=require"
        Database.connect(dbUrl, "org.postgresql.Driver", username, password).also {
            println("Connecting DB at ${it.url}")
            transaction(it) {
                println("Creating tables")
                SchemaUtils.create(Guilds)
            }
        }
    }

    fun getMoviesListChannel(guildId: String): String? {
        return transaction(database) {
            Guilds.select { Guilds.id eq guildId }.singleOrNull()?.getOrNull(Guilds.moviesListChannelId)
        }
    }

    fun setMoviesListChannel(guildId: String, channelId: String) {
        return transaction(database) {
            if (Guilds.select { Guilds.id eq guildId }.singleOrNull() != null) {
                Guilds.update({ Guilds.id eq guildId }) {
                    it[moviesListChannelId] = channelId
                }
            } else {
                Guilds.insert {
                    it[id] = guildId
                    it[moviesListChannelId] = channelId
                }
            }
        }
    }

    fun getWatchedMoviesChannelId(guildId: String): String? {
        return transaction(database) {
            Guilds.select { Guilds.id eq guildId }.singleOrNull()?.getOrNull(Guilds.watchedMoviesChannelId)
        }
    }

    fun setWatchedMoviesListChannel(guildId: String, channelId: String) {
        return transaction(database) {
            if (Guilds.select { Guilds.id eq guildId }.singleOrNull() != null) {
                Guilds.update({ Guilds.id eq guildId }) {
                    it[watchedMoviesChannelId] = channelId
                }
            } else {
                Guilds.insert {
                    it[id] = guildId
                    it[watchedMoviesChannelId] = channelId
                }
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

    fun setGamesChannel(guildId: String, channelId: String) {
        return transaction(database) {
            if (Guilds.select { Guilds.id eq guildId }.singleOrNull() != null) {
                Guilds.update({ Guilds.id eq guildId }) {
                    it[gamesChannelId] = channelId
                }
            } else {
                Guilds.insert {
                    it[id] = guildId
                    it[gamesChannelId] = channelId
                }
            }
        }
    }
}