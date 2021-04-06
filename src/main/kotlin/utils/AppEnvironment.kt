package utils

object AppEnvironment {

    private const val PORT = "PORT"
    private const val BOT_SECRET = "BOT_SECRET"
    private const val DATABASE_URL = "DATABASE_URL"
    private const val ENV = "ENV"
    private const val ENV_TEST = "TEST"
    private const val ENV_PROD = "PROD"
    private const val GOOGLE_KEY = "GOOGLE_KEY"
    private const val SEARCH_ENGINE = "SEARCH_ENGINE"

    val BOT_PREFIX = if (isTestEnv()) "test-ttb!" else "ttb!"

    fun getPort() = System.getenv(PORT)?.toInt() ?: 8080
    fun getBotSecret(): String = System.getenv(BOT_SECRET)
    fun getDatabaseUrl(): String = System.getenv(DATABASE_URL)
    fun getGoogleKey(): String = System.getenv(GOOGLE_KEY)
    fun getSearchEngine(): String = System.getenv(SEARCH_ENGINE)
    fun isTestEnv(): Boolean = System.getenv(ENV) == ENV_TEST
    fun isProdEnv(): Boolean = System.getenv(ENV) == ENV_PROD
}