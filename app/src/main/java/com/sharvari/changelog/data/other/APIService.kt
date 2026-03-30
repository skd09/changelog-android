package com.sharvari.changelog.data.other

import com.sharvari.changelog.model.config.AppConfig
import com.sharvari.changelog.model.article.ArticlesQuery
import com.sharvari.changelog.model.stats.StatsDelta
import com.sharvari.changelog.data.request.DeviceRegisterRequest
import com.sharvari.changelog.data.request.VoteRequest
import com.sharvari.changelog.data.request.FCMTokenRequest
import com.sharvari.changelog.data.request.FeedbackRequest
import com.sharvari.changelog.data.request.NotificationPreferencesRequest
import com.sharvari.changelog.data.request.StatsSyncRequest
import com.sharvari.changelog.data.response.ArticlesResponse
import com.sharvari.changelog.data.response.CategoriesResponse
import com.sharvari.changelog.data.response.DeviceRegisterResponse
import com.sharvari.changelog.data.response.StatsResponse
import com.sharvari.changelog.data.response.SuccessResponse
import kotlinx.serialization.json.Json
import timber.log.Timber

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

    // ── View Article (increments read_count on server) ────────────────────────

    suspend fun recordView(id: String) {
        // POST /articles/{id}/view — increments read_count, deduped per device per 24h
        try { client.request(APIRouter.RecordView(id), deserializer<SuccessResponse>()) }
        catch (_: Exception) { /* fire-and-forget */ }
    }

    // ── Vote ────────────────────────────────────────────────────────────────

    suspend fun voteArticle(id: String, direction: String) {
        // POST /articles/{id}/vote — upvote or downvote
        try {
            client.request(APIRouter.VoteArticle(id, VoteRequest(direction)), deserializer<SuccessResponse>())
            Timber.d("Vote recorded: %s -> %s", id.take(8), direction)
        } catch (e: Exception) {
            Timber.e(e, "Vote failed for article %s", id.take(8))
        }
    }

    // ── Trending ──────────────────────────────────────────────────────────────

    suspend fun fetchTrending(): ArticlesResponse =
        client.request(APIRouter.Trending, deserializer())

    // ── Search ────────────────────────────────────────────────────────────────

    suspend fun searchArticles(query: String): ArticlesResponse =
        client.request(APIRouter.Search(query), deserializer())

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

    // ── Notification Preferences ──────────────────────────────────────────────

    suspend fun updateNotificationPreferences(
        frequency:   String,
        time:        String,
        categoryIds: List<String>?,
    ): SuccessResponse = client.request(
        APIRouter.UpdateNotificationPreferences(
            NotificationPreferencesRequest(
                frequency   = frequency,
                time        = time,
                categoryIds = categoryIds?.ifEmpty { null },
            )
        ),
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