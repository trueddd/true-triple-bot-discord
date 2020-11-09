package services

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.jetbrains.exposed.sql.Database
import kotlin.coroutines.CoroutineContext

abstract class BaseService<T>(
    protected val database: Database
) : CoroutineScope {

    protected val client by lazy {
        HttpClient(OkHttp) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + Job()

    abstract suspend fun load(regions: List<String> = listOf("en")): Map<String, List<T>>?

    open suspend fun loadResponse(regions: List<String> = listOf("en")): ServiceResponse<Map<String, List<T>>> {
        return ServiceResponse.Error.Unimplemented()
    }
}