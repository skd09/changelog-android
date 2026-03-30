package com.sharvari.changelog.store.category

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sharvari.changelog.model.article.ALL_CATEGORIES
import com.sharvari.changelog.model.article.ArticleCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.categoryDataStore by preferencesDataStore(name = "category_prefs")

object CategoryStore {

    private val SELECTED_SLUGS_KEY = stringSetPreferencesKey("selected_category_slugs")

    private val defaultSlugs = ALL_CATEGORIES.map { it.slug }.toSet()

    private val _selectedSlugs = MutableStateFlow<Set<String>>(defaultSlugs)
    val selectedSlugs: StateFlow<Set<String>> = _selectedSlugs.asStateFlow()

    val allCategories: List<ArticleCategory> = ALL_CATEGORIES

    private lateinit var appContext: Context

    suspend fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = appContext.categoryDataStore.data.first()
        _selectedSlugs.value = prefs[SELECTED_SLUGS_KEY] ?: defaultSlugs
    }

    fun toggle(slug: String) {
        val current = _selectedSlugs.value.toMutableSet()
        if (current.contains(slug)) {
            if (current.size > 1) current.remove(slug) // keep at least 1
        } else {
            current.add(slug)
        }
        _selectedSlugs.value = current
        persist(current)
    }

    fun selectAll() {
        val all = allCategories.map { it.slug }.toSet()
        _selectedSlugs.value = all
        persist(all)
    }

    /** Deselects all categories except the first one (must keep at least 1 selected) */
    fun clearAll() {
        val first = setOf(allCategories.first().slug)
        _selectedSlugs.value = first
        persist(first)
    }

    private fun persist(slugs: Set<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            appContext.categoryDataStore.edit { it[SELECTED_SLUGS_KEY] = slugs }
        }
    }

    val selectedCategories: List<ArticleCategory>
        get() = allCategories.filter { _selectedSlugs.value.contains(it.slug) }
}