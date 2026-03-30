package com.sharvari.changelog.store.bookmark

import android.content.Context
import com.sharvari.changelog.model.article.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * BookmarkStore — mirrors iOS BookmarkStore
 *
 * Persists bookmarked articles to SharedPreferences as JSON.
 * Exposes a StateFlow<List<Article>> so Compose can observe changes reactively.
 */
object BookmarkStore {

    private const val PREFS_NAME = "changelog_bookmarks"
    private const val KEY        = "bookmarked_articles"

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val _bookmarks = MutableStateFlow<List<Article>>(emptyList())
    val bookmarks: StateFlow<List<Article>> = _bookmarks.asStateFlow()

    // Must be called once at app start (e.g. in Application.onCreate or MainActivity)
    fun init(context: Context) {
        _bookmarks.value = load(context)
    }

    fun isBookmarked(articleId: String): Boolean =
        _bookmarks.value.any { it.id == articleId }

    fun toggle(context: Context, article: Article) {
        val current = _bookmarks.value.toMutableList()
        if (current.any { it.id == article.id }) {
            current.removeAll { it.id == article.id }
        } else {
            current.add(0, article) // newest first
        }
        _bookmarks.value = current
        save(context, current)
    }

    fun add(context: Context, article: Article) {
        if (isBookmarked(article.id)) return
        val updated = listOf(article) + _bookmarks.value
        _bookmarks.value = updated
        save(context, updated)
    }

    fun remove(context: Context, articleId: String) {
        val updated = _bookmarks.value.filter { it.id != articleId }
        _bookmarks.value = updated
        save(context, updated)
    }

    fun clear(context: Context) {
        _bookmarks.value = emptyList()
        save(context, emptyList())
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun save(context: Context, articles: List<Article>) {
        try {
            prefs(context).edit()
                .putString(KEY, json.encodeToString(articles))
                .apply()
        } catch (e: Exception) {
            Timber.e("BookmarkStore: save error — %s", e.message)
        }
    }

    private fun load(context: Context): List<Article> {
        return try {
            val raw = prefs(context).getString(KEY, null) ?: return emptyList()
            json.decodeFromString(raw)
        } catch (e: Exception) {
            Timber.e("BookmarkStore: load error — %s", e.message)
            emptyList()
        }
    }
}