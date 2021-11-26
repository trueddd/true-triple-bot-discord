package services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*

abstract class BaseService {

    protected val gson: Gson by lazy {
        GsonBuilder().apply {
            configGson()
        }.create()
    }

    protected val client by lazy {
        HttpClient(OkHttp) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.BODY
            }
        }
    }

    protected open fun GsonBuilder.configGson() {}
}