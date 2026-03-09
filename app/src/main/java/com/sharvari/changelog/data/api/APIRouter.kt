package com.sharvari.changelog.data.api

import com.sharvari.changelog.data.model.ArticlesQuery
import com.sharvari.changelog.data.model.DeviceRegisterRequest
import com.sharvari.changelog.data.model.FCMTokenRequest
import com.sharvari.changelog.data.model.FeedbackRequest
import com.sharvari.changelog.data.model.StatsSyncRequest

// Single source of truth for every endpoint in the app.
// Add a new case here when you add a new API call — nowhere else.

sealed class APIRouter {

    // ── Public (no auth) ──────────────────────────────────────────────────────
    data class RegisterDevice(val body: DeviceRegisterRequest) : APIRouter()
    object AppConfig : APIRouter()

    // ── Protected (X-Device-Token required) ──────────────────────────────────
    data class Articles(val query: ArticlesQuery) : APIRouter()
    object Categories : APIRouter()
    data class UpdateFCMToken(val body: FCMTokenRequest) : APIRouter()
    object FetchStats : APIRouter()
    data class SyncStats(val body: StatsSyncRequest) : APIRouter()
    data class SubmitFeedback(val body: FeedbackRequest) : APIRouter()
}

// ── Route properties ──────────────────────────────────────────────────────────

val APIRouter.method: HttpMethod get() = when (this) {
    is APIRouter.RegisterDevice,
    is APIRouter.UpdateFCMToken,
    is APIRouter.SyncStats,
    is APIRouter.SubmitFeedback -> HttpMethod.POST
    else                        -> HttpMethod.GET
}

val APIRouter.path: String get() = when (this) {
    is APIRouter.RegisterDevice  -> "/devices/register"
    is APIRouter.AppConfig       -> "/config"
    is APIRouter.Articles        -> "/articles"
    is APIRouter.Categories      -> "/categories"
    is APIRouter.UpdateFCMToken  -> "/devices/fcm-token"
    is APIRouter.FetchStats      -> "/devices/stats"
    is APIRouter.SyncStats       -> "/devices/stats/sync"
    is APIRouter.SubmitFeedback  -> "/feedback"
}

val APIRouter.requiresAuth: Boolean get() = when (this) {
    is APIRouter.RegisterDevice,
    is APIRouter.AppConfig -> false
    else                   -> true
}

// BASE_URL in BuildConfig already ends with /v1 — no prefix needed.
// AppConfig is the only route served from the root /api path (no /v1).
val APIRouter.isVersioned: Boolean get() = false

val APIRouter.queryItems: Map<String, String>? get() = when (this) {
    is APIRouter.Articles -> buildMap {
        put("limit",  query.limit.toString())
        put("offset", query.offset.toString())
        query.category?.let { put("category", it) }
        if (query.exclude.isNotEmpty()) put("exclude", query.exclude.joinToString(","))
    }
    else -> null
}

val APIRouter.body: Any? get() = when (this) {
    is APIRouter.RegisterDevice -> body
    is APIRouter.UpdateFCMToken -> body
    is APIRouter.SyncStats      -> body
    is APIRouter.SubmitFeedback -> body
    else                        -> null
}

enum class HttpMethod { GET, POST, PUT, DELETE }