package com.sharvari.changelog.store.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharvari.changelog.data.other.APIService
import com.sharvari.changelog.model.article.Article
import com.sharvari.changelog.model.article.ArticleCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface TrendingUiState {
    data object Loading : TrendingUiState
    data class Success(
        val articles: List<Article>,
        val filtered: List<Article>,
        val categories: List<ArticleCategory>,
    ) : TrendingUiState
    data object Empty : TrendingUiState
}

class TrendingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<TrendingUiState>(TrendingUiState.Loading)
    val uiState: StateFlow<TrendingUiState> = _uiState.asStateFlow()

    private var allArticles: List<Article> = emptyList()
    private var previousRanks: Map<String, Int> = emptyMap()

    init { load() }

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            if (!refresh) _uiState.value = TrendingUiState.Loading
            val snapshot = allArticles.mapIndexed { i, a -> a.id to (i + 1) }.toMap()
            try {
                // Fetch trending articles and all categories in parallel
                val trendingDeferred  = async { APIService.shared.fetchTrending() }
                val categoriesDeferred = async { runCatching { APIService.shared.fetchCategories() } }

                val response = trendingDeferred.await()
                if (refresh) previousRanks = snapshot
                allArticles = response.data

                // Use the full categories list from the API, not just what's in trending
                val categories = categoriesDeferred.await().getOrNull()?.data
                    ?.sortedBy { it.name }
                    ?: response.data.mapNotNull { it.category }.distinctBy { it.slug }.sortedBy { it.name }

                _uiState.value = if (response.data.isEmpty()) {
                    TrendingUiState.Empty
                } else {
                    TrendingUiState.Success(
                        articles = response.data,
                        filtered = response.data,
                        categories = categories,
                    )
                }
            } catch (e: Exception) {
                Timber.e("TrendingViewModel: %s", e.message)
            }
        }
    }

    fun filter(slug: String?) {
        viewModelScope.launch {
            val current = _uiState.value
            if (current !is TrendingUiState.Success) return@launch
            try {
                val response = APIService.shared.fetchTrending(category = slug)
                _uiState.value = current.copy(filtered = response.data)
            } catch (_: Exception) {
                // Fallback to client-side filtering
                val filtered = if (slug == null) current.articles
                else current.articles.filter { it.category?.slug == slug }
                _uiState.value = current.copy(filtered = filtered)
            }
        }
    }

    fun rankChange(articleId: String): Int {
        val prev = previousRanks[articleId] ?: return 0
        val current = when (val state = _uiState.value) {
            is TrendingUiState.Success -> state.filtered.indexOfFirst { it.id == articleId } + 1
            else -> return 0
        }
        return prev - current
    }
}
