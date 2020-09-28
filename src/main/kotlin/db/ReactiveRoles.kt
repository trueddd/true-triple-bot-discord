package db

import org.jetbrains.exposed.sql.Table

object ReactiveRoles : Table() {
    val roleId = varchar("role_id", 64)
    val emoji = varchar("emoji", 64)
    val messageId = varchar("message_id", 64)

    override val tableName = "reactive_roles"

    override val primaryKey = PrimaryKey(roleId, emoji, name = "reactive_roles_pk")
}