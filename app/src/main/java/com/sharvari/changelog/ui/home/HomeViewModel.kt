package com.sharvari.changelog.ui.home

import androidx.lifecycle.ViewModel
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

class HomeViewModel : ViewModel() {

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

            // Wait for token — max 5 seconds (matches iOS)
            var waited = 0
            while (DeviceTokenStore.token == null && waited < 10) {
                delay(500)
                waited++
            }

            if (DeviceTokenStore.token == null) {
                _errorMessage.value = "Could not connect. Please try again."
                _isLoading.value    = false
                _hasLoadedOnce.value = true
                return@launch
            }

            try {
                val selectedSlugs = CategoryStore.selectedSlugs.value
                val response = APIService.fetchArticles(
                    exclude = ReadArticlesStore.excludeList,
                )
                val filtered = response.data.filter { article ->
                    val slug = article.category?.slug ?: return@filter true
                    selectedSlugs.contains(slug)
                }
                // Debug — remove once images confirmed working
                filtered.take(3).forEach { a ->
                    android.util.Log.d("CHANGELOG_IMG", "article: ${a.title.take(30)} | imageUrl: ${a.imageUrl}")
                }
                _articles.value = filtered
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
        ReadArticlesStore.markAsRead(article.id)
        StatsStore.recordSkip()
        _articles.value = _articles.value.filter { it.id != article.id }
        if (_articles.value.size < 3) loadArticles()
    }

    fun openArticle(article: Article) {
        ReadArticlesStore.markAsRead(article.id)
        StatsStore.recordRead()
        _articles.value = _articles.value.filter { it.id != article.id }
        if (_articles.value.size < 3) loadArticles()
    }

    fun refreshForCategoryChange() {
        _articles.value = emptyList()
        loadArticles(refresh = true)
    }
}