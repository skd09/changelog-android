package com.sharvari.changelog.data.response

import com.sharvari.changelog.model.article.Article
import kotlinx.serialization.Serializable

@Serializable
data class BookmarksResponse(
    val data: List<Article>,
)
