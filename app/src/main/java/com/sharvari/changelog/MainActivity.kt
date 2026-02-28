package com.sharvari.changelog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.sharvari.changelog.data.store.AppConfigService
import com.sharvari.changelog.data.store.CategoryStore
import com.sharvari.changelog.data.store.DeviceTokenStore
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()   // must be after super.onCreate, before setContent
        enableEdgeToEdge()

        setContent {
            ChangelogTheme {
                ChangelogApp()
            }
        }
    }
}

// ── Root app composable — mirrors iOS changelogApp body ───────────────────────
@Composable
fun ChangelogApp() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val appVersion = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    }
    val onboardingKey = "tc_onboarding_complete"
    val prefs = remember { context.getSharedPreferences("changelog", android.content.Context.MODE_PRIVATE) }

    var showSplash             by remember { mutableStateOf(true) }
    var hasCompletedOnboarding by remember { mutableStateOf(prefs.getBoolean(onboardingKey, false)) }
    var showSettings           by remember { mutableStateOf(false) }
    var cardsSeenSinceAd       by remember { mutableStateOf(0) }

    val appConfig   by AppConfigService.config.collectAsState()
    val isLoaded    by AppConfigService.isLoaded.collectAsState()
    val homeViewModel: HomeViewModel = viewModel()

    // Startup sequence — mirrors iOS .task { async let … }
    LaunchedEffect(Unit) {
        coroutineScope {
            val splashJob  = async { delay(3000) }
            val configJob  = async { AppConfigService.fetch() }
            val deviceJob  = async { DeviceTokenStore.registerIfNeeded(context, appVersion) }
            splashJob.await(); configJob.await(); deviceJob.await()
        }
        StatsStore.recordSession()
        // Pre-load ad
        AdManager.loadAd(context)
        showSplash = false
    }

    // Track ad frequency
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
            appConfig != null && AppConfigService.needsForceUpdate(appVersion) -> "forceUpdate"
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
                viewModel = homeViewModel,
            )
        }
    }
}