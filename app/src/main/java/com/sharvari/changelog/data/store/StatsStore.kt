package com.sharvari.changelog.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sharvari.changelog.data.model.ServerStats
import com.sharvari.changelog.data.model.StatsDelta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Context.statsDataStore by preferencesDataStore(name = "stats_prefs")

object StatsStore {

    private val TOTAL_READS           = intPreferencesKey("total_reads")
    private val TOTAL_SKIPS           = intPreferencesKey("total_skips")
    private val TOTAL_SESSIONS        = intPreferencesKey("total_sessions")
    private val TOTAL_READ_SECONDS    = intPreferencesKey("total_read_seconds")
    private val TOTAL_SESSION_SECONDS = intPreferencesKey("total_session_seconds")

    private val _totalReads      = MutableStateFlow(0)
    private val _totalSkips      = MutableStateFlow(0)
    private val _totalSessions   = MutableStateFlow(0)
    private val _avgReadTime     = MutableStateFlow(0)
    private val _avgSessionTime  = MutableStateFlow(0)

    val totalReads:     StateFlow<Int> = _totalReads.asStateFlow()
    val totalSkips:     StateFlow<Int> = _totalSkips.asStateFlow()
    val totalSessions:  StateFlow<Int> = _totalSessions.asStateFlow()
    val avgReadTime:    StateFlow<Int> = _avgReadTime.asStateFlow()
    val avgSessionTime: StateFlow<Int> = _avgSessionTime.asStateFlow()

    private var pendingDelta = StatsDelta()
    private lateinit var appContext: Context

    // ── Init ─────────────────────────────────────────────────────────────────
    suspend fun init(context: Context) {
        appContext = context.applicationContext
        loadFromLocal()
        startSyncTimer()
    }

    // ── Record events ─────────────────────────────────────────────────────────
    fun recordRead() {
        _totalReads.value++
        pendingDelta.reads++
        persist { it[TOTAL_READS] = _totalReads.value }
    }

    fun recordSkip() {
        _totalSkips.value++
        pendingDelta.skips++
        persist { it[TOTAL_SKIPS] = _totalSkips.value }
    }

    fun recordSession() {
        _totalSessions.value++
        pendingDelta.sessions++
        persist { it[TOTAL_SESSIONS] = _totalSessions.value }
    }

    fun recordReadTime(seconds: Int) {
        pendingDelta.readSeconds += seconds
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = appContext.statsDataStore.data.first()
            val total = (prefs[TOTAL_READ_SECONDS] ?: 0) + seconds
            appContext.statsDataStore.edit { it[TOTAL_READ_SECONDS] = total }
            withContext(Dispatchers.Main) {
                _avgReadTime.value = if (_totalReads.value > 0) total / _totalReads.value else 0
            }
        }
    }

    fun recordSessionTime(seconds: Int) {
        pendingDelta.sessionSeconds += seconds
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = appContext.statsDataStore.data.first()
            val total = (prefs[TOTAL_SESSION_SECONDS] ?: 0) + seconds
            appContext.statsDataStore.edit { it[TOTAL_SESSION_SECONDS] = total }
            withContext(Dispatchers.Main) {
                _avgSessionTime.value = if (_totalSessions.value > 0) total / _totalSessions.value else 0
            }
        }
    }

    // ── Server sync ───────────────────────────────────────────────────────────
    fun applyFromServer(stats: ServerStats) {
        _totalReads.value     = stats.totalReads
        _totalSkips.value     = stats.totalSkips
        _totalSessions.value  = stats.totalSessions
        _avgReadTime.value    = stats.avgReadTime
        _avgSessionTime.value = stats.avgSessionTime

        persist {
            it[TOTAL_READS]    = stats.totalReads
            it[TOTAL_SKIPS]    = stats.totalSkips
            it[TOTAL_SESSIONS] = stats.totalSessions
        }
    }

    fun flushToServer() {
        val delta = pendingDelta
        if (delta.reads == 0 && delta.skips == 0 && delta.sessions == 0 &&
            delta.readSeconds == 0 && delta.sessionSeconds == 0) return
        pendingDelta = StatsDelta()
        DeviceTokenStore.syncStats(delta)
    }

    // ── Local load ────────────────────────────────────────────────────────────
    private suspend fun loadFromLocal() {
        val prefs = appContext.statsDataStore.data.first()
        _totalReads.value    = prefs[TOTAL_READS]    ?: 0
        _totalSkips.value    = prefs[TOTAL_SKIPS]    ?: 0
        _totalSessions.value = prefs[TOTAL_SESSIONS] ?: 0

        val readSecs = prefs[TOTAL_READ_SECONDS]    ?: 0
        val sessSecs = prefs[TOTAL_SESSION_SECONDS] ?: 0
        _avgReadTime.value    = if (_totalReads.value > 0) readSecs / _totalReads.value else 0
        _avgSessionTime.value = if (_totalSessions.value > 0) sessSecs / _totalSessions.value else 0
    }

    private fun startSyncTimer() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                kotlinx.coroutines.delay(30_000)
                flushToServer()
            }
        }
    }

    private fun persist(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            appContext.statsDataStore.edit(block)
        }
    }
}
