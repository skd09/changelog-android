package com.sharvari.changelog.store.stats

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sharvari.changelog.model.stats.StatsDelta
import com.sharvari.changelog.service.device.DeviceService
import com.sharvari.changelog.data.response.ServerStats
import com.sharvari.changelog.utils.RatingManager
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
    private val TOTAL_UPVOTES         = intPreferencesKey("total_upvotes")
    private val TOTAL_DOWNVOTES       = intPreferencesKey("total_downvotes")
    private val CURRENT_STREAK        = intPreferencesKey("current_streak")
    private val LONGEST_STREAK        = intPreferencesKey("longest_streak")

    private val _totalReads      = MutableStateFlow(0)
    private val _totalSkips      = MutableStateFlow(0)
    private val _totalSessions   = MutableStateFlow(0)
    private val _avgReadTime     = MutableStateFlow(0)
    private val _avgSessionTime  = MutableStateFlow(0)
    private val _totalUpvotes    = MutableStateFlow(0)
    private val _totalDownvotes  = MutableStateFlow(0)
    private val _currentStreak   = MutableStateFlow(0)
    private val _longestStreak   = MutableStateFlow(0)

    val totalReads:     StateFlow<Int> = _totalReads.asStateFlow()
    val totalSkips:     StateFlow<Int> = _totalSkips.asStateFlow()
    val totalSessions:  StateFlow<Int> = _totalSessions.asStateFlow()
    val avgReadTime:    StateFlow<Int> = _avgReadTime.asStateFlow()
    val avgSessionTime: StateFlow<Int> = _avgSessionTime.asStateFlow()
    val totalUpvotes:   StateFlow<Int> = _totalUpvotes.asStateFlow()
    val totalDownvotes: StateFlow<Int> = _totalDownvotes.asStateFlow()
    val currentStreak:  StateFlow<Int> = _currentStreak.asStateFlow()
    val longestStreak:  StateFlow<Int> = _longestStreak.asStateFlow()

    private var pendingDelta = StatsDelta()
    private lateinit var appContext: Context
    private var isInitialized = false

    // ── Init — set context sync, load data async ────────────────────────────
    suspend fun init(context: Context) {
        appContext = context.applicationContext
        loadFromLocal()
        isInitialized = true
        startSyncTimer()
    }

    // ── Record events ─────────────────────────────────────────────────────────
    fun recordRead() {
        _totalReads.value++
        pendingDelta.reads++
        persistIfReady { it[TOTAL_READS] = _totalReads.value }
    }

    fun recordSkip() {
        _totalSkips.value++
        pendingDelta.skips++
        persistIfReady { it[TOTAL_SKIPS] = _totalSkips.value }
    }

    fun recordUpvote() {
        _totalUpvotes.value++
        persistIfReady { it[TOTAL_UPVOTES] = _totalUpvotes.value }
    }

    fun decrementUpvote() {
        if (_totalUpvotes.value > 0) _totalUpvotes.value--
        persistIfReady { it[TOTAL_UPVOTES] = _totalUpvotes.value }
    }

    fun recordDownvote() {
        _totalDownvotes.value++
        persistIfReady { it[TOTAL_DOWNVOTES] = _totalDownvotes.value }
    }

    fun decrementDownvote() {
        if (_totalDownvotes.value > 0) _totalDownvotes.value--
        persistIfReady { it[TOTAL_DOWNVOTES] = _totalDownvotes.value }
    }

    fun recordSession() {
        _totalSessions.value++
        pendingDelta.sessions++
        persistIfReady { it[TOTAL_SESSIONS] = _totalSessions.value }
    }

    fun recordReadTime(seconds: Int) {
        pendingDelta.readSeconds += seconds
        if (!isInitialized) return
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
        if (!isInitialized) return
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
        _totalUpvotes.value   = stats.totalUpvotes
        _totalDownvotes.value = stats.totalDownvotes
        val previousStreak = _currentStreak.value
        _currentStreak.value  = stats.currentStreak
        _longestStreak.value  = stats.longestStreak

        // Trigger rating prompt on streak milestone (3+ days, first time hitting it)
        if (stats.currentStreak >= 3 && previousStreak < 3) {
            appContext?.let { ctx ->
                (ctx as? android.app.Activity)?.let { RatingManager.recordAchievement(it) }
            }
        }

        persistIfReady {
            it[TOTAL_READS]    = stats.totalReads
            it[TOTAL_SKIPS]    = stats.totalSkips
            it[TOTAL_SESSIONS] = stats.totalSessions
            it[CURRENT_STREAK] = stats.currentStreak
            it[LONGEST_STREAK] = stats.longestStreak
        }
    }

    fun flushToServer() {
        val delta = pendingDelta
        if (delta.isEmpty) return
        pendingDelta = StatsDelta()
        DeviceService.shared.syncStats(delta)
    }

    // ── Local ─────────────────────────────────────────────────────────────────
    private suspend fun loadFromLocal() {
        val prefs = appContext.statsDataStore.data.first()
        // Only set if no in-flight increments happened before init
        if (_totalReads.value == 0)      _totalReads.value      = prefs[TOTAL_READS]      ?: 0
        if (_totalSkips.value == 0)      _totalSkips.value      = prefs[TOTAL_SKIPS]      ?: 0
        if (_totalSessions.value == 0)   _totalSessions.value   = prefs[TOTAL_SESSIONS]   ?: 0
        if (_totalUpvotes.value == 0)    _totalUpvotes.value    = prefs[TOTAL_UPVOTES]    ?: 0
        if (_totalDownvotes.value == 0)  _totalDownvotes.value  = prefs[TOTAL_DOWNVOTES]  ?: 0
        if (_currentStreak.value == 0)   _currentStreak.value   = prefs[CURRENT_STREAK]   ?: 0
        if (_longestStreak.value == 0)   _longestStreak.value   = prefs[LONGEST_STREAK]   ?: 0

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

    private fun persistIfReady(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        if (!isInitialized) return
        CoroutineScope(Dispatchers.IO).launch {
            appContext.statsDataStore.edit(block)
        }
    }
}
