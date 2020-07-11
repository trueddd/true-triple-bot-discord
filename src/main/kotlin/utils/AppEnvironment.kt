package utils

object AppEnvironment {

    private const val DATABASE_URL = "DATABASE_URL"
    private const val PORT = "PORT"

    fun getDatabaseUrl() = System.getenv(DATABASE_URL)?.toString() ?: ""
    fun getPort() = System.getenv(PORT)?.toInt() ?: 8080

    object JWT {

        private const val JWT_ISSUER = "JWT_ISSUER"
        private const val JWT_SECRET = "JWT_SECRET"
        private const val JWT_REALM = "JWT_REALM"

        fun secret() = System.getenv(JWT_SECRET)?.toString() ?: ""
        fun issuer() = System.getenv(JWT_ISSUER)?.toString() ?: ""
        fun realm() = System.getenv(JWT_REALM)?.toString() ?: ""
    }
}