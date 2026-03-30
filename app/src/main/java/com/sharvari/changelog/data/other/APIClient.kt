package com.sharvari.changelog.data.other

import com.sharvari.changelog.BuildConfig
import com.sharvari.changelog.store.device.DeviceTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber

// ── Errors ────────────────────────────────────────────────────────────────────

sealed class APIError : Exception() {
    object NotRegistered                                   : APIError()
    object Unauthorized                                    : APIError()
    object InvalidURL                                      : APIError()
    data class ClientError(val code: Int)                  : APIError()
    data class ServerError(val code: Int)                  : APIError()
    data class NetworkError(override val cause: Throwable) : APIError()
    data class DecodingError(override val cause: Throwable): APIError()
}

// ── Protocol — allows mocking in tests ───────────────────────────────────────

interface APIClientProtocol {
    suspend fun <T> request(route: APIRouter, deserialize: suspend (ByteArray) -> T): T
}

// ── Token provider interface (allows mocking) ─────────────────────────────────

interface TokenProvider {
    val token: String?
    fun clearToken()
}

// ── Concrete implementation ───────────────────────────────────────────────────

class APIClient(
    private val tokenProvider: TokenProvider = DeviceTokenStore,
) : APIClientProtocol {

    companion object {
        val shared = APIClient()

        private val baseURL    = BuildConfig.BASE_URL
        private val baseURLRaw = baseURL.removeSuffix("/v1")

        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        val httpClient = HttpClient(Android) {
            install(ContentNegotiation) { json(json) }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) { Timber.d(message) }
                }
                level = LogLevel.INFO
            }
            engine { connectTimeout = 15_000; socketTimeout = 30_000 }
        }
    }

    private var isRetryingAfter401 = false

    override suspend fun <T> request(route: APIRouter, deserialize: suspend (ByteArray) -> T): T {
        val base      = if (route is APIRouter.AppConfig) baseURLRaw else baseURL
        val urlString = base + route.path

        Timber.d("[%s] %s", route.method, urlString)

        val response = when (route.method) {
            HttpMethod.POST -> httpClient.post(urlString) {
                if (route.requiresAuth) {
                    val token = tokenProvider.token ?: throw APIError.NotRegistered
                    headers { append("X-Device-Token", token) }
                }
                headers { append("Accept", "application/json") }
                route.body?.let { contentType(ContentType.Application.Json); setBody(it) }
            }
            HttpMethod.PUT -> httpClient.put(urlString) {
                if (route.requiresAuth) {
                    val token = tokenProvider.token ?: throw APIError.NotRegistered
                    headers { append("X-Device-Token", token) }
                }
                headers { append("Accept", "application/json") }
                route.body?.let { contentType(ContentType.Application.Json); setBody(it) }
            }
            HttpMethod.GET -> httpClient.get(urlString) {
                if (route.requiresAuth) {
                    val token = tokenProvider.token ?: throw APIError.NotRegistered
                    headers { append("X-Device-Token", token) }
                }
                headers { append("Accept", "application/json") }
                route.queryItems?.forEach { (k, v) -> parameter(k, v) }
            }
            else -> throw APIError.InvalidURL
        }

        Timber.d("-> %s", response.status.value)

        return when {
            response.status.value in 200..299 -> deserialize(response.body())

            response.status == HttpStatusCode.Unauthorized -> {
                if (!isRetryingAfter401) {
                    isRetryingAfter401 = true
                    Timber.w("401 — clearing token, needs re-registration")
                    tokenProvider.clearToken()
                    isRetryingAfter401 = false
                }
                throw APIError.Unauthorized
            }

            else -> if (response.status.value in 400..499)
                throw APIError.ClientError(response.status.value)
            else
                throw APIError.ServerError(response.status.value)
        }
    }
}