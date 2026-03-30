package com.sharvari.changelog.model.article

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArticleCategory(
    val id: String,
    val name: String,
    val slug: String,
    val icon: String? = null,
    @SerialName("article_count") val articleCount: Int? = null,
)

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
