package com.sharvari.changelog

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sharvari.changelog.data.service.DeviceService
import com.sharvari.changelog.data.store.AppConfigService
import com.sharvari.changelog.data.store.StatsStore
import com.sharvari.changelog.ui.components.AdManager
import com.sharvari.changelog.ui.home.HomeScreen
import com.sharvari.changelog.ui.home.HomeViewModel
import com.sharvari.changelog.ui.onboarding.OnboardingScreen
import com.sharvari.changelog.ui.others.ForceUpdateScreen
import com.sharvari.changelog.ui.others.MaintenanceScreen
import com.sharvari.changelog.ui.settings.SettingsScreen
import com.sharvari.changelog.ui.splash.SplashScreen
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.ChangelogTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        // Run once per process start — not tied to Compose lifecycle.
        // savedInstanceState != null means activity is being recreated (rotation etc) — skip.
        if (savedInstanceState == null) {
            val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
            CoroutineScope(Dispatchers.IO).launch {
                AppConfigService.fetch()
                DeviceService.shared.registerIfNeeded(appVersion)
            }
        }

        setContent { ChangelogTheme { ChangelogApp() } }
    }
}

@Composable
fun ChangelogApp() {
    val context  = LocalContext.current
    val activity = context as? ComponentActivity

    val onboardingKey = "tc_onboarding_complete"
    val prefs = remember { context.getSharedPreferences("changelog", android.content.Context.MODE_PRIVATE) }

    var showSplash             by remember { mutableStateOf(true) }
    var hasCompletedOnboarding by remember { mutableStateOf(prefs.getBoolean(onboardingKey, false)) }
    var showSettings           by remember { mutableStateOf(false) }
    var cardsSeenSinceAd       by remember { mutableStateOf(0) }

    val appConfig by AppConfigService.config.collectAsState()
    val isLoaded  by AppConfigService.isLoaded.collectAsState()

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory())

    // Permission granted → fetch FCM token from Firebase and call /devices/fcm-token
    // isSynced flag ensures we only do this once per token lifetime
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            println("✅ Notification permission granted — syncing FCM token")
            CoroutineScope(Dispatchers.IO).launch {
                DeviceService.shared.syncFCMIfNeeded()
            }
        } else {
            println("ℹ️ Notification permission denied")
        }
    }

    // Splash timer only — registration is handled in MainActivity.onCreate above
    LaunchedEffect(Unit) {
        coroutineScope {
            val splashJob  = async { delay(3000) }
            // Wait for config and registration to finish before hiding splash
            val configDone = async { while (!AppConfigService.isLoaded.value) delay(100) }
            splashJob.await(); configDone.await()
        }

        // Request notification permission after splash.
        // If already granted, sync FCM directly (isSynced flag prevents duplicate calls).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!DeviceService.shared.hasNotificationPermission(context)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    DeviceService.shared.syncFCMIfNeeded()
                }
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                DeviceService.shared.syncFCMIfNeeded()
            }
        }

        StatsStore.recordSession()
        AdManager.loadAd(context)
        showSplash = false
    }

    fun trackCard() {
        cardsSeenSinceAd++
        if (cardsSeenSinceAd >= 5) {
            cardsSeenSinceAd = 0
            activity?.let { AdManager.showAd(it) }
        }
    }

    AnimatedContent(
        targetState = when {
            showSplash -> "splash"
            !isLoaded  -> "loading"
            appConfig?.maintenance?.enabled == true -> "maintenance"
            appConfig != null && AppConfigService.needsForceUpdate(
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
            ) -> "forceUpdate"
            !hasCompletedOnboarding -> "onboarding"
            showSettings -> "settings"
            else -> "home"
        },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "rootNav",
        modifier = Modifier.fillMaxSize().background(AppColors.background),
    ) { screen ->
        when (screen) {
            "splash"      -> SplashScreen()
            "loading"     -> SplashScreen()
            "maintenance" -> MaintenanceScreen(appConfig!!.maintenance)
            "forceUpdate" -> ForceUpdateScreen(appConfig!!.forceUpdate)
            "onboarding"  -> OnboardingScreen {
                prefs.edit().putBoolean(onboardingKey, true).apply()
                hasCompletedOnboarding = true
            }
            "settings"    -> SettingsScreen { showSettings = false }
            else          -> HomeScreen(
                onOpenSettings = { showSettings = true },
                viewModel      = homeViewModel,
            )
        }
    }
}