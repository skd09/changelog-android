package com.sharvari.changelog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sharvari.changelog.data.api.APIError
import com.sharvari.changelog.data.api.APIService
import com.sharvari.changelog.data.model.Article
import com.sharvari.changelog.data.store.CategoryStore
import com.sharvari.changelog.data.store.DeviceTokenStore
import com.sharvari.changelog.data.store.ReadArticlesStore
import com.sharvari.changelog.data.store.StatsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Repository interface (SRP + DIP) ─────────────────────────────────────────

interface ArticleRepository {
    suspend fun fetchArticles(
        category: String?,
        exclude:  List<String>,
    ): List<Article>
}

// ── Default implementation ────────────────────────────────────────────────────

class DefaultArticleRepository(
    private val apiService: APIService = APIService.shared,
) : ArticleRepository {
    override suspend fun fetchArticles(category: String?, exclude: List<String>): List<Article> =
        apiService.fetchArticles(category = category, exclude = exclude).data
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class HomeViewModel(
    private val repository:    ArticleRepository = DefaultArticleRepository(),
    private val categoryStore: CategoryStore     = CategoryStore,
    private val readStore:     ReadArticlesStore = ReadArticlesStore,
    private val statsStore:    StatsStore        = StatsStore,
    private val tokenStore:    DeviceTokenStore  = DeviceTokenStore,
) : ViewModel() {

    private val _articles      = MutableStateFlow<List<Article>>(emptyList())
    private val _isLoading     = MutableStateFlow(false)
    private val _hasLoadedOnce = MutableStateFlow(false)
    private val _errorMessage  = MutableStateFlow<String?>(null)

    val articles:      StateFlow<List<Article>> = _articles.asStateFlow()
    val isLoading:     StateFlow<Boolean>       = _isLoading.asStateFlow()
    val hasLoadedOnce: StateFlow<Boolean>       = _hasLoadedOnce.asStateFlow()
    val errorMessage:  StateFlow<String?>       = _errorMessage.asStateFlow()

    init { loadArticles() }

    fun loadArticles(refresh: Boolean = false) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value    = true
            _errorMessage.value = null
            if (refresh) _articles.value = emptyList()

            // Wait up to 5s for device registration to complete
            var waited = 0
            while (tokenStore.token == null && waited < 10) {
                delay(500); waited++
            }

            if (tokenStore.token == null) {
                _errorMessage.value  = "Could not connect. Please try again."
                _isLoading.value     = false
                _hasLoadedOnce.value = true
                return@launch
            }

            try {
                val selectedSlugs = categoryStore.selectedSlugs.value
                val currentIds    = _articles.value.map { it.id }
                val excludeIds    = (readStore.excludeList + currentIds).distinct()

                val newArticles = repository.fetchArticles(
                    category = null,
                    exclude  = excludeIds,
                )

                val filtered = newArticles.filter { article ->
                    val slug = article.category?.slug ?: return@filter true
                    selectedSlugs.contains(slug)
                }
                _articles.value = _articles.value + filtered

            } catch (e: APIError.Unauthorized) {
                _errorMessage.value = "Reconnecting... please try again."
            } catch (e: Exception) {
                println("❌ loadArticles error: $e")
                _errorMessage.value = "Could not load news. Check your connection."
            }

            _isLoading.value     = false
            _hasLoadedOnce.value = true
        }
    }

    fun dismissArticle(article: Article) {
        readStore.markAsRead(article.id)
        statsStore.recordSkip()
        removeAndRefreshIfNeeded(article.id)
    }

    fun openArticle(article: Article) {
        readStore.markAsRead(article.id)
        statsStore.recordRead()
        removeAndRefreshIfNeeded(article.id)
    }

    fun refreshForCategoryChange() {
        _articles.value = emptyList()
        loadArticles(refresh = true)
    }

    private fun removeAndRefreshIfNeeded(articleId: String) {
        _articles.value = _articles.value.filter { it.id != articleId }
        if (_articles.value.size < 3) loadArticles()
    }

    // ── Factory for DI without Hilt ───────────────────────────────────────────
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel() as T
    }
}