package com.sharvari.changelog.data.response

import com.sharvari.changelog.model.article.ArticleCategory
import kotlinx.serialization.Serializable

@Serializable
data class CategoriesResponse(
    val data: List<ArticleCategory>,
)
