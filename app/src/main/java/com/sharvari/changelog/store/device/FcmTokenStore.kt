package com.sharvari.changelog.store.device

import android.content.Context
import timber.log.Timber

object FcmTokenStore {

    private const val PREFS_NAME  = "fcm_prefs"
    private const val TOKEN_KEY   = "tc_fcm_token"
    private const val SYNCED_KEY  = "tc_fcm_token_synced"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val token: String?
        get() = prefs()?.getString(TOKEN_KEY, null)

    // True only after a successful /devices/fcm-token response
    val isSynced: Boolean
        get() = prefs()?.getBoolean(SYNCED_KEY, false) == true

    fun saveToken(token: String) {
        // Saving a new token clears the synced flag — backend needs to be updated
        prefs()?.edit()
            ?.putString(TOKEN_KEY, token)
            ?.putBoolean(SYNCED_KEY, false)  // ← new token = not yet synced
            ?.apply()
        Timber.d("FCM token saved: %s...", token.take(10))
    }

    fun markSynced() {
        prefs()?.edit()?.putBoolean(SYNCED_KEY, true)?.apply()
    }

    fun clearToken() {
        prefs()?.edit()
            ?.remove(TOKEN_KEY)
            ?.putBoolean(SYNCED_KEY, false)
            ?.apply()
    }
}