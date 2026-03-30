package com.sharvari.changelog.model.article

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
