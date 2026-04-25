package com.sharvari.changelog.utils

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.testing.FakeReviewManager
import com.sharvari.changelog.BuildConfig
import timber.log.Timber

/**
 * Manages in-app review prompts with engagement-based triggers:
 *
 * 1. First prompt: after 3rd session AND 15+ articles read (engaged user)
 * 2. Achievement trigger: on first streak (3+ days) or rank-up
 * 3. Recurring: every 5th app open after first prompt
 * 4. Maximum 3 prompts total, then never again
 */
object RatingManager {

    private const val PREFS_NAME            = "tc_rating_prefs"
    private const val KEY_ASKED_COUNT       = "rating_asked_count"
    private const val KEY_HAS_RATED         = "rating_has_rated"
    private const val KEY_TOTAL_SWIPES      = "rating_total_swipes"
    private const val KEY_SESSION_COUNT     = "rating_session_count"
    private const val KEY_OPENS_SINCE_ASK   = "rating_opens_since_ask"
    private const val KEY_FIRST_PROMPT_DONE = "rating_first_prompt_done"

    private const val MAX_ASK_COUNT         = 3
    private const val MIN_SWIPES_FOR_FIRST  = 15
    private const val MIN_SESSIONS_FOR_FIRST = 3
    private const val OPENS_BETWEEN_ASKS    = 5

    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Timber.d("RatingManager init — swipes=%d, sessions=%d, asked=%d, firstDone=%b, rated=%b",
            prefs.getInt(KEY_TOTAL_SWIPES, 0),
            prefs.getInt(KEY_SESSION_COUNT, 0),
            prefs.getInt(KEY_ASKED_COUNT, 0),
            prefs.getBoolean(KEY_FIRST_PROMPT_DONE, false),
            prefs.getBoolean(KEY_HAS_RATED, false))
    }

    /** Call on every card swipe (left or right). */
    fun recordSwipe(activity: Activity) {
        val swipes = prefs.getInt(KEY_TOTAL_SWIPES, 0) + 1
        prefs.edit().putInt(KEY_TOTAL_SWIPES, swipes).apply()
        checkFirstPrompt(activity)
    }

    /** Call on every app open (when main screen appears). */
    fun recordAppOpen(activity: Activity) {
        if (shouldNeverAsk()) return

        val sessions = prefs.getInt(KEY_SESSION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_SESSION_COUNT, sessions).apply()

        // First prompt check (session + swipe threshold)
        if (!prefs.getBoolean(KEY_FIRST_PROMPT_DONE, false)) {
            checkFirstPrompt(activity)
            return
        }

        // Recurring prompt
        val opens = prefs.getInt(KEY_OPENS_SINCE_ASK, 0) + 1
        prefs.edit().putInt(KEY_OPENS_SINCE_ASK, opens).apply()
        if (opens >= OPENS_BETWEEN_ASKS) {
            showReview(activity)
        }
    }

    /** Call when user hits a reading streak of 3+ days or ranks up.
     *  High-satisfaction moments — best time to ask. */
    fun recordAchievement(activity: Activity) {
        if (shouldNeverAsk()) return
        if (!prefs.getBoolean(KEY_FIRST_PROMPT_DONE, false)) return

        val opens = prefs.getInt(KEY_OPENS_SINCE_ASK, 0)
        if (opens >= 2) {
            showReview(activity)
        }
    }

    /** Call when user taps "Rate on Play Store" in settings. */
    fun markAsRated() {
        prefs.edit().putBoolean(KEY_HAS_RATED, true).apply()
        Timber.d("RatingManager: marked as rated")
    }

    /** Reset all state — useful for testing. */
    fun reset() {
        prefs.edit().clear().apply()
        Timber.d("RatingManager: reset all state")
    }

    // ── Private ─────────────────────────────────────────────────────────────

    private fun checkFirstPrompt(activity: Activity) {
        if (shouldNeverAsk()) return
        if (prefs.getBoolean(KEY_FIRST_PROMPT_DONE, false)) return

        val swipes   = prefs.getInt(KEY_TOTAL_SWIPES, 0)
        val sessions = prefs.getInt(KEY_SESSION_COUNT, 0)

        if (swipes >= MIN_SWIPES_FOR_FIRST && sessions >= MIN_SESSIONS_FOR_FIRST) {
            showReview(activity)
        }
    }

    private fun shouldNeverAsk(): Boolean {
        return prefs.getBoolean(KEY_HAS_RATED, false) ||
               prefs.getInt(KEY_ASKED_COUNT, 0) >= MAX_ASK_COUNT
    }

    private fun showReview(activity: Activity) {
        val askedCount = prefs.getInt(KEY_ASKED_COUNT, 0)
        if (askedCount >= MAX_ASK_COUNT) return

        prefs.edit()
            .putInt(KEY_ASKED_COUNT, askedCount + 1)
            .putBoolean(KEY_FIRST_PROMPT_DONE, true)
            .putInt(KEY_OPENS_SINCE_ASK, 0)
            .apply()

        Timber.d("RatingManager: showing review (attempt %d/%d)", askedCount + 1, MAX_ASK_COUNT)

        val manager = if (BuildConfig.DEBUG) {
            FakeReviewManager(activity)
        } else {
            ReviewManagerFactory.create(activity)
        }

        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Timber.d("RatingManager: review flow ready, launching")
                manager.launchReviewFlow(activity, task.result)
            } else {
                Timber.w("RatingManager: review request failed: %s", task.exception?.message)
            }
        }
    }
}
