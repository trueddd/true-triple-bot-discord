package db

import org.jetbrains.exposed.sql.Table
import utils.environmentDependentTableName

object Guilds : Table() {
    val id = varchar("id", 64)
    val gamesChannelId = varchar("games_channel_id", 64).nullable()
    val region = varchar("region", 8).nullable()

    private const val NAME = "guilds"

    override val tableName: String = NAME.environmentDependentTableName()

    override val primaryKey = PrimaryKey(id, name = "${tableName}_pk")
}