package com.sharvari.changelog.store.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sharvari.changelog.data.other.APIError
import com.sharvari.changelog.model.article.Article
import com.sharvari.changelog.service.article.ArticleRepository
import com.sharvari.changelog.service.article.DefaultArticleRepository
import com.sharvari.changelog.service.deeplink.DeepLinkManager
import com.sharvari.changelog.store.category.CategoryStore
import com.sharvari.changelog.store.device.DeviceTokenStore
import com.sharvari.changelog.store.article.ReadArticlesStore
import com.sharvari.changelog.store.stats.StatsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val articles: List<Article>,
        val isRefreshing: Boolean = false,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
    data object Empty : HomeUiState
}

class HomeViewModel(
    private val repository:    ArticleRepository = DefaultArticleRepository(),
    private val categoryStore: CategoryStore     = CategoryStore,
    private val readStore:     ReadArticlesStore = ReadArticlesStore,
    private val statsStore:    StatsStore        = StatsStore,
    private val tokenStore:    DeviceTokenStore  = DeviceTokenStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var isLoadingInternal = false
    private var articles = mutableListOf<Article>()

    init {
        loadArticles()
        listenForDeepLinks()
    }

    // ── Deep link handling ───────────────────────────────────────────────────

    private fun listenForDeepLinks() {
        viewModelScope.launch {
            DeepLinkManager.articleDeepLink.collect { articleId ->
                surfaceArticle(articleId)
            }
        }
    }

    private suspend fun surfaceArticle(id: String) {
        val existing = articles.firstOrNull { it.id == id }
        if (existing != null) {
            articles = (listOf(existing) + articles.filter { it.id != id }).toMutableList()
            _uiState.value = HomeUiState.Success(articles = articles.toList())
            return
        }

        try {
            val response = repository.fetchArticles(category = null, exclude = emptyList())
            val target = response.getOrThrow().firstOrNull { it.id == id }
            if (target != null) {
                articles = (listOf(target) + articles.filter { it.id != id }).toMutableList()
                _uiState.value = HomeUiState.Success(articles = articles.toList())
                Timber.d("Surfaced article from deep link: %s", target.title.take(50))
            } else {
                Timber.w("Deep link article not found in feed: %s", id)
            }
        } catch (e: Exception) {
            Timber.e("surfaceArticle error: %s", e)
        }
    }

    // ── Article loading ──────────────────────────────────────────────────────

    fun loadArticles(refresh: Boolean = false) {
        if (isLoadingInternal) return
        viewModelScope.launch {
            isLoadingInternal = true
            if (refresh) {
                articles.clear()
                _uiState.value = HomeUiState.Loading
            } else if (articles.isNotEmpty()) {
                _uiState.value = HomeUiState.Success(articles = articles.toList(), isRefreshing = true)
            }

            var waited = 0
            while (tokenStore.token == null && waited < 10) {
                delay(500); waited++
            }

            if (tokenStore.token == null) {
                _uiState.value = HomeUiState.Error("Could not connect. Please try again.")
                isLoadingInternal = false
                return@launch
            }

            try {
                val selectedSlugs = categoryStore.selectedSlugs.value
                val currentIds    = articles.map { it.id }
                val excludeIds    = (readStore.excludeList + currentIds).distinct()

                val result = repository.fetchArticles(
                    category = null,
                    exclude  = excludeIds,
                )

                result.fold(
                    onSuccess = { newArticles ->
                        val filtered = newArticles.filter { article ->
                            val slug = article.category?.slug ?: return@filter true
                            selectedSlugs.contains(slug)
                        }
                        articles.addAll(filtered)
                        _uiState.value = if (articles.isEmpty()) {
                            HomeUiState.Empty
                        } else {
                            HomeUiState.Success(articles = articles.toList())
                        }
                    },
                    onFailure = { e ->
                        when (e) {
                            is APIError.Unauthorized -> {
                                _uiState.value = if (articles.isEmpty()) {
                                    HomeUiState.Error("Reconnecting... please try again.")
                                } else {
                                    HomeUiState.Success(articles = articles.toList())
                                }
                            }
                            else -> {
                                Timber.e("loadArticles error: %s", e)
                                _uiState.value = if (articles.isEmpty()) {
                                    HomeUiState.Error("Could not load news. Check your connection.")
                                } else {
                                    HomeUiState.Success(articles = articles.toList())
                                }
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e("loadArticles error: %s", e)
                _uiState.value = if (articles.isEmpty()) {
                    HomeUiState.Error("Could not load news. Check your connection.")
                } else {
                    HomeUiState.Success(articles = articles.toList())
                }
            }

            isLoadingInternal = false
        }
    }

    fun dismissArticle(article: Article) {
        readStore.markAsRead(article.id)
        removeAndRefreshIfNeeded(article.id)
    }

    fun openArticle(article: Article) {
        readStore.markAsRead(article.id)
        removeAndRefreshIfNeeded(article.id)
    }

    fun refreshForCategoryChange() {
        articles.clear()
        loadArticles(refresh = true)
    }

    private fun removeAndRefreshIfNeeded(articleId: String) {
        articles = articles.filter { it.id != articleId }.toMutableList()
        _uiState.value = if (articles.isEmpty()) {
            HomeUiState.Empty
        } else {
            HomeUiState.Success(articles = articles.toList())
        }
        if (articles.size < 3) loadArticles()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel() as T
    }
}
