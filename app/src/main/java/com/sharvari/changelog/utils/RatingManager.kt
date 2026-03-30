package com.sharvari.changelog.utils

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import timber.log.Timber

/**
 * Manages in-app review prompts with strict limits:
 *
 * 1. First install → show after 8 swipes
 * 2. If not rated → show every 3rd app open
 * 3. Maximum 3 prompts total
 * 4. After 3 prompts or rating → never ask again
 */
object RatingManager {

    private const val PREFS_NAME           = "tc_rating_prefs"
    private const val KEY_ASKED_COUNT      = "rating_asked_count"
    private const val KEY_HAS_RATED        = "rating_has_rated"
    private const val KEY_TOTAL_SWIPES     = "rating_total_swipes"
    private const val KEY_OPENS_SINCE_ASK  = "rating_opens_since_ask"
    private const val KEY_FIRST_PROMPT_DONE = "rating_first_prompt_done"

    private const val MAX_ASK_COUNT       = 3
    private const val FIRST_SWIPE_TRIGGER = 8
    private const val OPENS_BETWEEN_ASKS  = 3

    private lateinit var prefs: android.content.SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Call on every card swipe (left or right). */
    fun recordSwipe(activity: Activity) {
        if (shouldNeverAsk()) return

        val swipes = prefs.getInt(KEY_TOTAL_SWIPES, 0) + 1
        prefs.edit().putInt(KEY_TOTAL_SWIPES, swipes).apply()

        // First-install trigger: show after 8 swipes (only if first prompt hasn't fired yet)
        if (!prefs.getBoolean(KEY_FIRST_PROMPT_DONE, false) && swipes >= FIRST_SWIPE_TRIGGER) {
            showReview(activity)
        }
    }

    /** Call on every app open (when main screen appears). */
    fun recordAppOpen(activity: Activity) {
        if (shouldNeverAsk()) return
        if (!prefs.getBoolean(KEY_FIRST_PROMPT_DONE, false)) return // wait for swipe trigger first

        val opens = prefs.getInt(KEY_OPENS_SINCE_ASK, 0) + 1
        prefs.edit().putInt(KEY_OPENS_SINCE_ASK, opens).apply()

        if (opens >= OPENS_BETWEEN_ASKS) {
            showReview(activity)
        }
    }

    /** Call when user taps "Rate on Play Store" in settings. */
    fun markAsRated() {
        prefs.edit().putBoolean(KEY_HAS_RATED, true).apply()
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

        Timber.d("Showing in-app review (attempt %d/%d)", askedCount + 1, MAX_ASK_COUNT)

        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
            } else {
                Timber.w("In-app review request failed: %s", task.exception?.message)
            }
        }
    }
}
