package com.sharvari.changelog.service.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.sharvari.changelog.model.article.Article
import com.sharvari.changelog.store.stats.StatsStore

enum class SwipeDirection { LEFT, RIGHT }

object AnalyticsManager {

    private var analytics: FirebaseAnalytics? = null

    private var sessionStart: Long? = null
    private var articleOpenTime: Long? = null
    private var currentArticleId: String? = null

    fun init(analytics: FirebaseAnalytics) {
        this.analytics = analytics
    }

    // ── Session ─────────────────────────────────────────────────────────────

    fun sessionStarted() {
        sessionStart = System.currentTimeMillis()
        StatsStore.recordSession()
        analytics?.logEvent("app_session_start") {}
    }

    fun sessionEnded() {
        val start = sessionStart ?: return
        val duration = ((System.currentTimeMillis() - start) / 1000).toInt()
        StatsStore.recordSessionTime(duration)
        StatsStore.flushToServer()
        analytics?.logEvent("app_session_end") {
            param("duration_seconds", duration.toLong())
        }
        sessionStart = null
    }

    // ── Article events ──────────────────────────────────────────────────────

    fun articleSwiped(article: Article, direction: SwipeDirection) {
        if (direction == SwipeDirection.RIGHT) {
            StatsStore.recordRead()
        } else {
            StatsStore.recordSkip()
        }
        val event = if (direction == SwipeDirection.RIGHT) "article_read" else "article_skipped"
        analytics?.logEvent(event) {
            param("article_id", article.id)
            param("source", article.sourceName ?: "unknown")
            param("category", article.category?.slug ?: "unknown")
        }
    }

    fun articleOpened(article: Article) {
        articleOpenTime = System.currentTimeMillis()
        currentArticleId = article.id
        analytics?.logEvent("article_opened") {
            param("article_id", article.id)
            param("source", article.sourceName ?: "unknown")
        }
    }

    fun articleClosed() {
        val openTime = articleOpenTime ?: return
        val articleId = currentArticleId ?: return
        val readTime = ((System.currentTimeMillis() - openTime) / 1000).toInt()
        StatsStore.recordReadTime(readTime)
        analytics?.logEvent("article_read_time") {
            param("article_id", articleId)
            param("seconds_spent", readTime.toLong())
        }
        articleOpenTime = null
        currentArticleId = null
    }

    fun fullArticleTapped(article: Article) {
        analytics?.logEvent("full_article_tapped") {
            param("article_id", article.id)
            param("source", article.sourceName ?: "unknown")
        }
    }

    fun appRated()     { analytics?.logEvent("app_rated") {} }
    fun appShared()    { analytics?.logEvent("app_shared") {} }
    fun feedbackSent() { analytics?.logEvent("feedback_sent") {} }
    fun categoryChanged(slug: String) {
        analytics?.logEvent("category_changed") {
            param("slug", slug)
        }
    }

    // ── Screen & click tracking ────────────────────────────────────────────

    fun trackScreen(name: String) {
        analytics?.logEvent("screen_view") {
            param("screen_name", name)
        }
    }

    fun trackClick(name: String, screen: String) {
        analytics?.logEvent("click_event") {
            param("click_name", name)
            param("screen_name", screen)
        }
    }
}
