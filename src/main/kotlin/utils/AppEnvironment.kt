package utils

object AppEnvironment {

    private const val PORT = "PORT"

    fun getPort() = System.getenv(PORT)?.toInt() ?: 8080
}