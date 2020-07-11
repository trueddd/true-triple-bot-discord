package utils

import io.ktor.application.ApplicationCall
import io.ktor.request.receive

suspend inline fun <reified T : Any> ApplicationCall.receiveSafe(): T? {
    return try {
        receive<T>()
    } catch (e: Exception) {
        null
    }
}