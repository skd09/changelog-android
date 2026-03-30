package com.sharvari.changelog.store.device

import android.content.Context
import com.sharvari.changelog.data.other.TokenProvider
import timber.log.Timber

object DeviceTokenStore : TokenProvider {

    private const val PREFS_NAME = "device_prefs"
    private const val TOKEN_KEY  = "tc_device_token"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override val token: String?
        get() = prefs()?.getString(TOKEN_KEY, null)

    val isRegistered: Boolean
        get() {
            val ctx = appContext
            val token = prefs()?.getString(TOKEN_KEY, null)
            Timber.d("isRegistered check — appContext=%s, token=%s", ctx != null, token?.take(10) ?: "NULL")
            return token != null
        }

    fun saveToken(token: String) {
        prefs()?.edit()?.putString(TOKEN_KEY, token)?.apply()
        Timber.d("Device token saved: %s...", token.take(10))
    }

    override fun clearToken() {
        prefs()?.edit()?.remove(TOKEN_KEY)?.apply()
        Timber.d("Device token cleared")
    }
}