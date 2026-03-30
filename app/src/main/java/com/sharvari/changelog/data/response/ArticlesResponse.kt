package com.sharvari.changelog.data.response

import com.sharvari.changelog.model.article.Article
import kotlinx.serialization.Serializable

@Serializable
data class ArticlesResponse(
    val data: List<Article>,
    val meta: ResponseMeta,
)
