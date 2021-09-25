package utils

import dev.kord.common.entity.CommandArgument
import dev.kord.core.entity.interaction.Interaction

inline fun <reified T : CommandArgument<*>> Interaction.commandArgument(name: String): T? {
    return data.data.options.value
        ?.firstOrNull { it.name == name }?.value?.value
        ?.let { it as? T }
}