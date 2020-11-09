package services

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import org.jetbrains.exposed.sql.Database

abstract class BaseService(
    protected val database: Database
) {

    protected val client by lazy {
        HttpClient(OkHttp) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
    }
}