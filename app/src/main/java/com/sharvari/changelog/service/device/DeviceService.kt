package com.sharvari.changelog.service.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.sharvari.changelog.data.other.APIService
import com.sharvari.changelog.model.stats.StatsDelta
import com.sharvari.changelog.store.device.DeviceTokenStore
import com.sharvari.changelog.store.device.FcmTokenStore
import com.sharvari.changelog.store.stats.StatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class DeviceService(
    private val apiService:    APIService       = APIService.shared,
    private val tokenStore:    DeviceTokenStore = DeviceTokenStore,
    private val fcmTokenStore: FcmTokenStore    = FcmTokenStore,
) {

    companion object {
        val shared = DeviceService()
    }

    // ── Registration ──────────────────────────────────────────────────────────

    suspend fun registerIfNeeded(appVersion: String) {
        Timber.d("registerIfNeeded — isRegistered=%s token=%s", tokenStore.isRegistered, tokenStore.token?.take(10) ?: "NULL")
        if (tokenStore.isRegistered) {
            Timber.d("Already registered — skipping register, checking FCM")
            syncFCMIfNeeded()
            return
        }
        Timber.d("Not registered — calling register()")
        register(appVersion)
    }

    suspend fun register(appVersion: String) {
        try {
            val fcmToken = fcmTokenStore.token
            Timber.d("Calling /devices/register — fcmToken=%s", fcmToken?.take(10) ?: "NULL")
            val response = apiService.registerDevice(
                appVersion = appVersion,
                fcmToken   = fcmToken,
            )
            tokenStore.saveToken(response.token)
            Timber.d("Device registered: %s...", response.token.take(10))
            response.stats?.let { StatsStore.applyFromServer(it) }

            if (fcmToken != null) {
                fcmTokenStore.markSynced()
            } else {
                syncFCMIfNeeded()
            }

        } catch (e: Exception) {
            Timber.e("Device registration failed: %s", e)
        }
    }

    // ── FCM Token ─────────────────────────────────────────────────────────────

    fun onFCMTokenReceived(newToken: String) {
        Timber.d("onFCMTokenReceived: %s...", newToken.take(10))
        fcmTokenStore.saveToken(newToken)

        if (!tokenStore.isRegistered) {
            Timber.d("Not registered yet — FCM will sync after registration")
            return
        }
        CoroutineScope(Dispatchers.IO).launch { sendFCMToken(newToken) }
    }

    suspend fun syncFCMIfNeeded() {
        Timber.d("syncFCMIfNeeded — isSynced=%s token=%s", fcmTokenStore.isSynced, fcmTokenStore.token?.take(10) ?: "NULL")
        if (fcmTokenStore.isSynced) {
            Timber.d("FCM already synced — skipping")
            return
        }

        val token = fcmTokenStore.token ?: fetchFCMTokenFromFirebase()?.also {
            fcmTokenStore.saveToken(it)
        } ?: run {
            Timber.d("No FCM token available yet")
            return
        }

        sendFCMToken(token)
    }

    private suspend fun fetchFCMTokenFromFirebase(): String? = withContext(Dispatchers.IO) {
        try {
            Tasks.await(FirebaseMessaging.getInstance().token)
        } catch (e: Exception) {
            Timber.w("Could not fetch FCM token from Firebase: %s", e)
            null
        }
    }

    private suspend fun sendFCMToken(fcmToken: String) {
        try {
            Timber.d("Calling /devices/fcm-token: %s...", fcmToken.take(10))
            apiService.updateFCMToken(fcmToken)
            fcmTokenStore.markSynced()
            Timber.d("FCM token synced")
        } catch (e: Exception) {
            Timber.w("FCM token sync failed: %s", e)
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    suspend fun fetchStats() {
        try {
            val response = apiService.fetchStats()
            StatsStore.applyFromServer(response.stats)
        } catch (e: Exception) {
            Timber.w("Stats fetch failed: %s", e)
        }
    }

    fun syncStats(delta: StatsDelta) {
        if (delta.isEmpty) return
        CoroutineScope(Dispatchers.IO).launch {
            try { apiService.syncStats(delta) }
            catch (e: Exception) { Timber.w("Stats sync failed: %s", e) }
        }
    }

    // ── Push notification permission (Android 13+) ────────────────────────────

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}