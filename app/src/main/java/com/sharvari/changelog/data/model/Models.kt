package com.sharvari.changelog.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Article ───────────────────────────────────────────────────────────────────

@Serializable
data class Article(
    val id: String,
    val title: String,
    val summary: String,
    @SerialName("original_url") val originalUrl: String,
    @SerialName("image_url")    val imageUrl: String? = null,
    @SerialName("source_name")  val sourceName: String? = null,
    @SerialName("read_count")   val readCount: Int = 0,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("created_at")   val createdAt: String = "",
    val category: ArticleCategory? = null,
)

@Serializable
data class ArticleCategory(
    val id: String,
    val name: String,
    val slug: String,
    val icon: String? = null,
    @SerialName("article_count") val articleCount: Int? = null,
)

// ── Responses ─────────────────────────────────────────────────────────────────

@Serializable
data class ArticlesResponse(
    val data: List<Article>,
    val meta: ResponseMeta,
)

@Serializable
data class CategoriesResponse(
    val data: List<ArticleCategory>,
)

@Serializable
data class ResponseMeta(
    val limit: Int,
    val offset: Int,
    val count: Int,
)

@Serializable
data class DeviceRegisterResponse(
    val token: String,
    val stats: ServerStats? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class ServerStats(
    @SerialName("total_reads")       val totalReads: Int = 0,
    @SerialName("total_skips")       val totalSkips: Int = 0,
    @SerialName("total_sessions")    val totalSessions: Int = 0,
    @SerialName("avg_read_time")     val avgReadTime: Int = 0,
    @SerialName("avg_session_time")  val avgSessionTime: Int = 0,
)

@Serializable
data class StatsResponse(val stats: ServerStats)

@Serializable
data class SuccessResponse(val success: Boolean = true)

// ── Requests ──────────────────────────────────────────────────────────────────

@Serializable
data class DeviceRegisterRequest(
    val platform: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("fcm_token")   val fcmToken: String? = null,
)

@Serializable
data class FCMTokenRequest(
    @SerialName("fcm_token") val fcmToken: String,
)

@Serializable
data class StatsSyncRequest(
    val reads: Int,
    val skips: Int,
    val sessions: Int,
    @SerialName("read_seconds")    val readSeconds: Int,
    @SerialName("session_seconds") val sessionSeconds: Int,
)

@Serializable
data class FeedbackRequest(
    val type: String,
    val message: String,
    val platform: String,
)

@Serializable
data class NotificationPreferencesRequest(
    @SerialName("frequency")    val frequency: String,
    @SerialName("time")         val time: String,
    @SerialName("category_ids") val categoryIds: List<String>?,
)

// ── Query params (not serialized — used to build URL) ─────────────────────────

data class ArticlesQuery(
    val category: String? = null,
    val exclude:  List<String> = emptyList(),
    val limit:    Int = 20,
    val offset:   Int = 0,
)

// ── Stats delta (local accumulator) ──────────────────────────────────────────

data class StatsDelta(
    var reads:          Int = 0,
    var skips:          Int = 0,
    var sessions:       Int = 0,
    var readSeconds:    Int = 0,
    var sessionSeconds: Int = 0,
) {
    val isEmpty: Boolean
        get() = reads == 0 && skips == 0 && sessions == 0 &&
                readSeconds == 0 && sessionSeconds == 0
}

// ── App Config ────────────────────────────────────────────────────────────────

@Serializable
data class AppConfig(
    val maintenance: MaintenanceConfig,
    @SerialName("force_update") val forceUpdate: ForceUpdateConfig,
)

@Serializable
data class MaintenanceConfig(
    val enabled: Boolean = false,
    val message: String = "We'll be back shortly.",
    val eta: String? = null,
)

@Serializable
data class ForceUpdateConfig(
    val enabled: Boolean = false,
    @SerialName("min_version")   val minVersion: String = "0.0.0",
    @SerialName("app_store_url") val appStoreUrl: String = "",
    val message: String = "Please update to continue.",
)

// ── All categories ────────────────────────────────────────────────────────────

val ALL_CATEGORIES = listOf(
    ArticleCategory("1",  "Technology",  "technology",   "memory"),
    ArticleCategory("2",  "AI",          "ai",           "smart_toy"),
    ArticleCategory("3",  "Security",    "security",     "lock"),
    ArticleCategory("4",  "Science",     "science",      "science"),
    ArticleCategory("5",  "Business",    "business",     "business_center"),
    ArticleCategory("6",  "Crypto",      "crypto",       "currency_bitcoin"),
    ArticleCategory("7",  "Gaming",      "gaming",       "sports_esports"),
    ArticleCategory("8",  "Space",       "space",        "rocket_launch"),
    ArticleCategory("9",  "Health",      "health",       "favorite"),
    ArticleCategory("10", "Open Source", "open-source",  "code"),
)