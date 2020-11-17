package services

import org.jetbrains.exposed.sql.Database

abstract class BaseGamesService<T>(database: Database) : BaseService(database) {

    abstract suspend fun load(regions: List<String> = listOf("ru")): Map<String, List<T>>?
}