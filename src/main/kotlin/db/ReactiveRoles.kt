package db

import org.jetbrains.exposed.sql.Table
import utils.environmentDependentTableName

object ReactiveRoles : Table() {
    val roleId = varchar("role_id", 64)
    val emoji = varchar("emoji", 64)
    val messageId = varchar("message_id", 64)

    private const val NAME = "reactive_roles"

    override val tableName = NAME.environmentDependentTableName()

    override val primaryKey = PrimaryKey(roleId, emoji, name = "${tableName}_pk")
}