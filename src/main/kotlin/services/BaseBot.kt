package services

import com.gitlab.kordlib.core.Kord
import db.GuildsManager

abstract class BaseBot(
    protected val client: Kord,
) {

    abstract suspend fun attach()

    companion object {
        const val BOT_PREFIX = "ttb!"
    }
}