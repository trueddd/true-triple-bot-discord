package services

abstract class BaseGamesService<T> : BaseService() {

    abstract suspend fun load(regions: List<String> = listOf("ru")): Map<String, List<T>>?
}