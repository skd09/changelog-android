package com.sharvari.changelog.data.store

import com.sharvari.changelog.data.api.APIService
import com.sharvari.changelog.data.model.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppConfigService {

    private val _config   = MutableStateFlow<AppConfig?>(null)
    private val _isLoaded = MutableStateFlow(false)

    val config:   StateFlow<AppConfig?> = _config.asStateFlow()
    val isLoaded: StateFlow<Boolean>    = _isLoaded.asStateFlow()

    val isUnderMaintenance: Boolean
        get() = _config.value?.maintenance?.enabled == true

    fun needsForceUpdate(currentVersion: String): Boolean {
        val min = _config.value?.forceUpdate?.minVersion ?: return false
        if (!_config.value!!.forceUpdate.enabled) return false
        return compareVersions(currentVersion, min) < 0
    }

    suspend fun fetch() {
        try {
            _config.value = APIService.fetchConfig()
        } catch (e: Exception) {
            println("⚠️ AppConfig fetch failed: $e")
            // Non-fatal — app proceeds without remote config
        } finally {
            _isLoaded.value = true
        }
    }

    // "1.2.3" vs "1.2.4" → -1, 0, 1
    private fun compareVersions(a: String, b: String): Int {
        val pa = a.split(".").map { it.toIntOrNull() ?: 0 }
        val pb = b.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(pa.size, pb.size)
        for (i in 0 until len) {
            val diff = (pa.getOrElse(i) { 0 }) - (pb.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }
}
