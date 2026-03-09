package com.sharvari.changelog.data.api

import com.sharvari.changelog.data.model.AppConfig
import com.sharvari.changelog.data.model.ArticlesQuery
import com.sharvari.changelog.data.model.ArticlesResponse
import com.sharvari.changelog.data.model.CategoriesResponse
import com.sharvari.changelog.data.model.DeviceRegisterRequest
import com.sharvari.changelog.data.model.DeviceRegisterResponse
import com.sharvari.changelog.data.model.FCMTokenRequest
import com.sharvari.changelog.data.model.FeedbackRequest
import com.sharvari.changelog.data.model.StatsDelta
import com.sharvari.changelog.data.model.StatsSyncRequest
import com.sharvari.changelog.data.model.StatsResponse
import com.sharvari.changelog.data.model.SuccessResponse
import kotlinx.serialization.json.Json

// Thin façade — one public method per endpoint.
// All business logic lives in DeviceService / StatsStore.
// This class owns NO state and performs NO side effects beyond HTTP.

class APIService(
    private val client: APIClientProtocol = APIClient.shared,
) {

    companion object {
        val shared = APIService()

        private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

        private inline fun <reified T> deserializer(): suspend (ByteArray) -> T = { bytes ->
            json.decodeFromString(bytes.decodeToString())
        }
    }

    // ── Articles ──────────────────────────────────────────────────────────────

    suspend fun fetchArticles(
        category: String? = null,
        exclude:  List<String> = emptyList(),
        limit:    Int = 20,
        offset:   Int = 0,
    ): ArticlesResponse = client.request(
        APIRouter.Articles(ArticlesQuery(category, exclude, limit, offset)),
        deserializer()
    )

    // ── Categories ────────────────────────────────────────────────────────────

    suspend fun fetchCategories(): CategoriesResponse =
        client.request(APIRouter.Categories, deserializer())

    // ── Device Registration ───────────────────────────────────────────────────

    suspend fun registerDevice(
        appVersion: String,
        fcmToken:   String?,
    ): DeviceRegisterResponse = client.request(
        APIRouter.RegisterDevice(
            DeviceRegisterRequest(
                platform   = "android",
                appVersion = appVersion,
                fcmToken   = fcmToken,
            )
        ),
        deserializer()
    )

    // ── FCM Token Update ──────────────────────────────────────────────────────

    suspend fun updateFCMToken(fcmToken: String): SuccessResponse = client.request(
        APIRouter.UpdateFCMToken(FCMTokenRequest(fcmToken = fcmToken)),
        deserializer()
    )

    // ── Stats ─────────────────────────────────────────────────────────────────

    suspend fun fetchStats(): StatsResponse =
        client.request(APIRouter.FetchStats, deserializer())

    suspend fun syncStats(delta: StatsDelta): Unit = client.request(
        APIRouter.SyncStats(
            StatsSyncRequest(
                reads          = delta.reads,
                skips          = delta.skips,
                sessions       = delta.sessions,
                readSeconds    = delta.readSeconds,
                sessionSeconds = delta.sessionSeconds,
            )
        ),
        deserializer<SuccessResponse>()  // response discarded
    ).let {}

    // ── Feedback ──────────────────────────────────────────────────────────────

    suspend fun submitFeedback(type: String, message: String): Unit {
        val slug = type.lowercase().replace(" report", "")
        client.request(
            APIRouter.SubmitFeedback(FeedbackRequest(type = slug, message = message, platform = "android")),
            deserializer<SuccessResponse>()
        )
    }

    // ── App Config ────────────────────────────────────────────────────────────

    suspend fun fetchConfig(): AppConfig =
        client.request(APIRouter.AppConfig, deserializer())
}