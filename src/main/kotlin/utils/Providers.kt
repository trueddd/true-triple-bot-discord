package utils

import data.egs.GiveAwayGames
import db.Guilds
import db.ReactiveRoles
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

fun provideDatabase(): Database {
    val uri = URI(AppEnvironment.getDatabaseUrl())
    val username = uri.userInfo.split(":")[0]
    val password = uri.userInfo.split(":")[1]
    val dbUrl = "jdbc:postgresql://${uri.host}:${uri.port}${uri.path}?sslmode=require"
    return Database.connect(dbUrl, "org.postgresql.Driver", username, password).also {
        println("Connecting DB at ${it.url}")
        transaction(it) {
            println("Creating tables")
            SchemaUtils.create(Guilds, GiveAwayGames, ReactiveRoles)
        }
    }
}