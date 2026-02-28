package com.sharvari.changelog.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.readDataStore by preferencesDataStore(name = "read_articles")

object ReadArticlesStore {

    private val READ_IDS_KEY = stringSetPreferencesKey("read_article_ids")
    private val _readIds = MutableStateFlow<Set<String>>(emptySet())

    private lateinit var appContext: Context

    suspend fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = appContext.readDataStore.data.first()
        _readIds.value = prefs[READ_IDS_KEY] ?: emptySet()
    }

    fun markAsRead(id: String) {
        val updated = _readIds.value + id
        _readIds.value = updated
        CoroutineScope(Dispatchers.IO).launch {
            appContext.readDataStore.edit { it[READ_IDS_KEY] = updated }
        }
    }

    fun clear() {
        _readIds.value = emptySet()
        CoroutineScope(Dispatchers.IO).launch {
            appContext.readDataStore.edit { it[READ_IDS_KEY] = emptySet() }
        }
    }

    val excludeList: List<String> get() = _readIds.value.toList()
}
