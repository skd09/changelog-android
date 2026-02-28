package com.sharvari.changelog.data.api

import com.sharvari.changelog.BuildConfig
import com.sharvari.changelog.data.model.AppConfig
import com.sharvari.changelog.data.model.ArticlesResponse
import com.sharvari.changelog.data.model.CategoriesResponse
import com.sharvari.changelog.data.model.DeviceRegisterResponse
import com.sharvari.changelog.data.model.ServerStats
import com.sharvari.changelog.data.model.StatsDelta
import com.sharvari.changelog.data.model.StatsResponse
import com.sharvari.changelog.data.store.DeviceTokenStore
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
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

sealed class APIError : Exception() {
    object NotRegistered  : APIError()
    object Unauthorized   : APIError()
    object InvalidURL     : APIError()
    data class ServerError(val code: Int) : APIError()
    data class NetworkError(override val cause: Throwable) : APIError()
    data class DecodingError(override val cause: Throwable) : APIError()
}

object APIService {

    private val baseURL = BuildConfig.BASE_URL

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            logger = object : Logger { override fun log(message: String) { println("🌐 $message") } }
            level = LogLevel.INFO
        }
        engine { connectTimeout = 15_000; socketTimeout = 30_000 }
    }

    // ── Fetch Articles ──────────────────────────────────────────────────────
    suspend fun fetchArticles(
        category: String? = null,
        exclude: List<String> = emptyList(),
        limit: Int = 20,
        offset: Int = 0,
    ): ArticlesResponse {
        val token = DeviceTokenStore.token ?: throw APIError.NotRegistered

        val response = client.get("$baseURL/articles") {
            headers { append("X-Device-Token", token); append("Accept", "application/json") }
            parameter("limit", limit)
            parameter("offset", offset)
            if (category != null) parameter("category", category)
            if (exclude.isNotEmpty()) parameter("exclude", exclude.joinToString(","))
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.Unauthorized -> {
                DeviceTokenStore.clearToken()
                throw APIError.Unauthorized
            }
            else -> throw APIError.ServerError(response.status.value)
        }
    }

    // ── Fetch Categories ────────────────────────────────────────────────────
    suspend fun fetchCategories(): CategoriesResponse {
        val token = DeviceTokenStore.token ?: throw APIError.NotRegistered
        val response = client.get("$baseURL/categories") {
            headers { append("X-Device-Token", token); append("Accept", "application/json") }
        }
        return response.body()
    }

    // ── Register Device ─────────────────────────────────────────────────────
    suspend fun registerDevice(appVersion: String): DeviceRegisterResponse {
        val response = client.post("$baseURL/devices/register") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("platform" to "android", "app_version" to appVersion))
        }
        return response.body()
    }

    // ── Fetch Stats ─────────────────────────────────────────────────────────
    suspend fun fetchStats(token: String): StatsResponse {
        val response = client.get("$baseURL/devices/stats") {
            headers { append("X-Device-Token", token) }
        }
        return response.body()
    }

    // ── Sync Stats ──────────────────────────────────────────────────────────
    suspend fun syncStats(token: String, delta: StatsDelta) {
        client.post("$baseURL/devices/stats/sync") {
            headers { append("X-Device-Token", token) }
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "reads"           to delta.reads,
                "skips"           to delta.skips,
                "sessions"        to delta.sessions,
                "read_seconds"    to delta.readSeconds,
                "session_seconds" to delta.sessionSeconds,
            ))
        }
    }

    // ── Submit Feedback ─────────────────────────────────────────────────────
    suspend fun submitFeedback(type: String, message: String) {
        val token = DeviceTokenStore.token ?: return
        val typeSlug = type.lowercase().replace(" report", "")
        client.post("$baseURL/feedback") {
            headers { append("X-Device-Token", token) }
            contentType(ContentType.Application.Json)
            setBody(mapOf("type" to typeSlug, "message" to message, "platform" to "android"))
        }
    }

    // ── App Config ──────────────────────────────────────────────────────────
    suspend fun fetchConfig(): AppConfig {
        val response = client.get("$baseURL/config") {
            headers { append("Accept", "application/json") }
        }
        return response.body()
    }
}
