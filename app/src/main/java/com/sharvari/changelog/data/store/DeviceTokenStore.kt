package com.sharvari.changelog.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sharvari.changelog.data.api.APIService
import com.sharvari.changelog.data.model.StatsDelta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "device_prefs")

object DeviceTokenStore {

    private val TOKEN_KEY = stringPreferencesKey("tc_device_token")
    private var _token: String? = null

    val token: String? get() = _token
    val isRegistered: Boolean get() = _token != null

    // ── Init — call once from Application ──────────────────────────────────
    suspend fun init(context: Context) {
        val prefs = context.dataStore.data.first()
        _token = prefs[TOKEN_KEY]
    }

    // ── Registration ────────────────────────────────────────────────────────
    suspend fun registerIfNeeded(context: Context, appVersion: String) {
        if (isRegistered) return
        register(context, appVersion)
    }

    suspend fun register(context: Context, appVersion: String) {
        try {
            val response = APIService.registerDevice(appVersion)
            _token = response.token
            context.dataStore.edit { it[TOKEN_KEY] = response.token }

            // Apply server stats if returned
            response.stats?.let { StatsStore.applyFromServer(it) }

            println("✅ Device registered: ${response.token.take(10)}...")
        } catch (e: Exception) {
            println("❌ Device registration failed: $e")
        }
    }

    // ── Stats ───────────────────────────────────────────────────────────────
    suspend fun fetchStats() {
        val t = _token ?: return
        try {
            val response = APIService.fetchStats(t)
            StatsStore.applyFromServer(response.stats)
        } catch (e: Exception) {
            println("⚠️ Stats fetch failed: $e")
        }
    }

    fun syncStats(delta: StatsDelta) {
        val t = _token ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try { APIService.syncStats(t, delta) }
            catch (e: Exception) { println("⚠️ Stats sync failed: $e") }
        }
    }

    fun clearToken() { _token = null }
}
