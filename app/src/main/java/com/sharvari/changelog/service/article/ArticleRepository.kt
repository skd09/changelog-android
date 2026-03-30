package com.sharvari.changelog.service.article

import com.sharvari.changelog.data.other.APIService
import com.sharvari.changelog.model.article.Article

interface ArticleRepository {
    suspend fun fetchArticles(
        category: String?,
        exclude:  List<String>,
    ): Result<List<Article>>
}

class DefaultArticleRepository(
    private val apiService: APIService = APIService.shared,
) : ArticleRepository {
    override suspend fun fetchArticles(category: String?, exclude: List<String>): Result<List<Article>> =
        runCatching {
            apiService.fetchArticles(category = category, exclude = exclude).data
        }
}
