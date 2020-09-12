package db

import org.jetbrains.exposed.sql.Table

object Guilds : Table() {
    val id = varchar("id", 64)
    val moviesListChannelId = varchar("movies_list_channel_id", 64).nullable()
    val watchedMoviesChannelId = varchar("watched_movies_channel_id", 64).nullable()
    val gamesChannelId = varchar("games_channel_id", 64).nullable()
    val region = varchar("region", 8).nullable()
    val moviesNotifyChannelId = varchar("movies_notify_channel_id", 64).nullable()
    val moviesRoleId = varchar("movie_role_id", 64).nullable()

    override val tableName: String = "guilds"

    override val primaryKey = PrimaryKey(id, name = "guilds_pk")
}