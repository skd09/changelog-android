package com.sharvari.changelog.store.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharvari.changelog.data.other.APIService
import com.sharvari.changelog.model.article.Article
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<Article>) : SearchUiState
    data object NoResults : SearchUiState
}

class SearchViewModel : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)

    val query: StateFlow<String> = _query.asStateFlow()
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.isBlank()) { _uiState.value = SearchUiState.Idle; return }
        searchJob = viewModelScope.launch {
            delay(350) // debounce 350ms — matches iOS
            _uiState.value = SearchUiState.Loading
            try {
                val response = APIService.shared.searchArticles(q.trim())
                _uiState.value = if (response.data.isEmpty()) {
                    SearchUiState.NoResults
                } else {
                    SearchUiState.Success(results = response.data)
                }
            } catch (_: Exception) {
                _uiState.value = SearchUiState.NoResults
            }
        }
    }

    fun clearQuery() {
        _query.value = ""
        _uiState.value = SearchUiState.Idle
    }
}
