package com.sharvari.changelog.service.deeplink

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

/**
 * Receives deep link payloads from push notifications and broadcasts
 * them to any collector (HomeViewModel) via a SharedFlow.
 *
 * Flow:
 *   FCM push tapped
 *     → ChangelogFirebaseMessagingService.onMessageReceived
 *       → DeepLinkManager.handleNotification(data)
 *         → HomeViewModel receives articleId → surfaces card to top
 */
object DeepLinkManager {

    private val _articleDeepLink = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val articleDeepLink: SharedFlow<String> = _articleDeepLink.asSharedFlow()

    /**
     * Call this from ChangelogFirebaseMessagingService.onMessageReceived
     * Expected FCM payload: { "article_id": "uuid-string" }
     */
    fun handleNotification(data: Map<String, String>) {
        val articleId = data["article_id"] ?: return
        // Validate UUID format to prevent injection
        try { java.util.UUID.fromString(articleId) } catch (_: IllegalArgumentException) { return }
        Timber.d("Deep link: article %s", articleId.take(8))
        _articleDeepLink.tryEmit(articleId)
    }
}
