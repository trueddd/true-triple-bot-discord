package utils

import dev.kord.common.entity.Permission
import dev.kord.core.entity.interaction.Interaction
import java.text.SimpleDateFormat
import java.util.*

fun String.egsDate(): Date {
    return try {
        val inputFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        inputFormatter.parse(this)
    } catch (e: Exception) {
        e.printStackTrace()
        Date()
    }
}

val Interaction.issuedByAdmin: Boolean
    get() = data.permissions.value?.contains(Permission.Administrator) == true

fun String.environmentDependentTableName(): String {
    return if (AppEnvironment.isTestEnv()) {
        "$this-test"
    } else {
        this
    }
}
