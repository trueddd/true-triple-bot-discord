package utils

import io.ktor.application.ApplicationCall
import io.ktor.request.receive
import java.text.SimpleDateFormat
import java.util.*

suspend inline fun <reified T : Any> ApplicationCall.receiveSafe(): T? {
    return try {
        receive<T>()
    } catch (e: Exception) {
        null
    }
}

fun String.egsDate(): Date {
    return try {
        val inputFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        inputFormatter.parse(this)
    } catch (e: Exception) {
        e.printStackTrace()
        Date()
    }
}