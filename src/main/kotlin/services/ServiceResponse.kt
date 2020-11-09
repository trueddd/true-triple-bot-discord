package services

sealed class ServiceResponse <T> {

    data class Success<T>(val data: T) : ServiceResponse<T>()

    sealed class Error<T>(open val cause: Throwable?) : ServiceResponse<T>() {

        class Unimplemented<T> : Error<T>(IllegalStateException("Not implemented"))

        data class HttpRequest<T>(override val cause: Throwable? = null) : Error<T>(cause)
    }
}

fun <T> T.success() = ServiceResponse.Success(this)

fun <T> Exception.httpError() = ServiceResponse.Error.HttpRequest<T>(this.cause)