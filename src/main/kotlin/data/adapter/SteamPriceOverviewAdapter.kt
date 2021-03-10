package data.adapter

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import data.steam.PriceOverview
import java.lang.reflect.Type

class SteamPriceOverviewAdapter : JsonDeserializer<PriceOverview> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): PriceOverview? {
        return json?.let {
            when {
                it.isJsonObject -> {
                    val root = it.asJsonObject
                    val initial = root["initial_formatted"]?.asString ?: return null
                    val final = root["final_formatted"]?.asString
                    val discount = root["discount_percent"]?.asInt
                    PriceOverview(initial, final, discount)
                }
                else -> return null
            }
        }
    }
}