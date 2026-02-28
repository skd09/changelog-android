package com.sharvari.changelog.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

data class StatsDelta(
    var reads: Int = 0,
    var skips: Int = 0,
    var sessions: Int = 0,
    var readSeconds: Int = 0,
    var sessionSeconds: Int = 0,
)

// ── App Config (maintenance / force update) ────────────────────────────────

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
    @SerialName("min_version") val minVersion: String = "0.0.0",
    @SerialName("app_store_url") val appStoreUrl: String = "",
    val message: String = "Please update to continue.",
)

// ── All categories — mirrors iOS CategoryStore.allCategories ───────────────

val ALL_CATEGORIES = listOf(
    ArticleCategory("1",  "Technology", "technology",  "memory"),
    ArticleCategory("2",  "AI",         "ai",          "smart_toy"),
    ArticleCategory("3",  "Security",   "security",    "lock"),
    ArticleCategory("4",  "Science",    "science",     "science"),
    ArticleCategory("5",  "Business",   "business",    "business_center"),
    ArticleCategory("6",  "Crypto",     "crypto",      "currency_bitcoin"),
    ArticleCategory("7",  "Gaming",     "gaming",      "sports_esports"),
    ArticleCategory("8",  "Space",      "space",       "rocket_launch"),
    ArticleCategory("9",  "Health",     "health",      "favorite"),
    ArticleCategory("10", "Open Source","open-source", "code"),
)
