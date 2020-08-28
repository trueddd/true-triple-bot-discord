package utils

object AppEnvironment {

    private const val PORT = "PORT"
    private const val BOT_SECRET = "BOT_SECRET"
    private const val DATABASE_URL = "DATABASE_URL"

    fun getPort() = System.getenv(PORT)?.toInt() ?: 8080
    fun getBotSecret(): String = System.getenv(BOT_SECRET)
    fun getDatabaseUrl(): String = System.getenv(DATABASE_URL)
}