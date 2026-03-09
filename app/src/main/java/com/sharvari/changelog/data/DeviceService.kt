package com.sharvari.changelog.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.sharvari.changelog.data.api.APIService
import com.sharvari.changelog.data.model.StatsDelta
import com.sharvari.changelog.data.store.DeviceTokenStore
import com.sharvari.changelog.data.store.FcmTokenStore
import com.sharvari.changelog.data.store.StatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        println("📲 registerIfNeeded — isRegistered=${tokenStore.isRegistered} token=${tokenStore.token?.take(10) ?: "NULL"}")
        if (tokenStore.isRegistered) {
            println("ℹ️ Already registered — skipping register, checking FCM")
            syncFCMIfNeeded()
            return
        }
        println("📲 Not registered — calling register()")
        register(appVersion)
    }

    suspend fun register(appVersion: String) {
        try {
            val fcmToken = fcmTokenStore.token
            println("📲 Calling /devices/register — fcmToken=${fcmToken?.take(10) ?: "NULL"}")
            val response = apiService.registerDevice(
                appVersion = appVersion,
                fcmToken   = fcmToken,
            )
            tokenStore.saveToken(response.token)
            println("✅ Device registered: ${response.token.take(10)}...")
            response.stats?.let { StatsStore.applyFromServer(it) }

            if (fcmToken != null) {
                fcmTokenStore.markSynced()
            } else {
                syncFCMIfNeeded()
            }

        } catch (e: Exception) {
            println("❌ Device registration failed: $e")
        }
    }

    // ── FCM Token ─────────────────────────────────────────────────────────────

    fun onFCMTokenReceived(newToken: String) {
        println("🔔 onFCMTokenReceived: ${newToken.take(10)}...")
        fcmTokenStore.saveToken(newToken)

        if (!tokenStore.isRegistered) {
            println("ℹ️ Not registered yet — FCM will sync after registration")
            return
        }
        CoroutineScope(Dispatchers.IO).launch { sendFCMToken(newToken) }
    }

    suspend fun syncFCMIfNeeded() {
        println("🔔 syncFCMIfNeeded — isSynced=${fcmTokenStore.isSynced} token=${fcmTokenStore.token?.take(10) ?: "NULL"}")
        if (fcmTokenStore.isSynced) {
            println("ℹ️ FCM already synced — skipping")
            return
        }

        val token = fcmTokenStore.token ?: fetchFCMTokenFromFirebase()?.also {
            fcmTokenStore.saveToken(it)
        } ?: run {
            println("ℹ️ No FCM token available yet")
            return
        }

        sendFCMToken(token)
    }

    private suspend fun fetchFCMTokenFromFirebase(): String? = withContext(Dispatchers.IO) {
        try {
            Tasks.await(FirebaseMessaging.getInstance().token)
        } catch (e: Exception) {
            println("⚠️ Could not fetch FCM token from Firebase: $e")
            null
        }
    }

    private suspend fun sendFCMToken(fcmToken: String) {
        try {
            println("🔔 Calling /devices/fcm-token: ${fcmToken.take(10)}...")
            apiService.updateFCMToken(fcmToken)
            fcmTokenStore.markSynced()
            println("✅ FCM token synced")
        } catch (e: Exception) {
            println("⚠️ FCM token sync failed: $e")
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    suspend fun fetchStats() {
        try {
            val response = apiService.fetchStats()
            StatsStore.applyFromServer(response.stats)
        } catch (e: Exception) {
            println("⚠️ Stats fetch failed: $e")
        }
    }

    fun syncStats(delta: StatsDelta) {
        if (delta.isEmpty) return
        CoroutineScope(Dispatchers.IO).launch {
            try { apiService.syncStats(delta) }
            catch (e: Exception) { println("⚠️ Stats sync failed: $e") }
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