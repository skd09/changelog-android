package com.sharvari.changelog.ui.activity.main

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sharvari.changelog.service.analytics.AnalyticsManager
import com.sharvari.changelog.service.device.DeviceService
import com.sharvari.changelog.store.config.AppConfigService
import com.sharvari.changelog.utils.AdManager
import com.sharvari.changelog.utils.RatingManager
import com.sharvari.changelog.store.home.HomeViewModel
import com.sharvari.changelog.ui.screen.onboarding.OnboardingScreen
import com.sharvari.changelog.ui.screen.others.ForceUpdateScreen
import com.sharvari.changelog.ui.screen.others.MaintenanceScreen
import com.sharvari.changelog.ui.screen.splash.SplashScreen
import com.sharvari.changelog.ui.theme.AppColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChangelogApp() {
    val context  = LocalContext.current
    val activity = context as? ComponentActivity

    val onboardingKey = "tc_onboarding_complete"
    val prefs = remember {
        context.getSharedPreferences("changelog", android.content.Context.MODE_PRIVATE)
    }

    var showSplash             by remember { mutableStateOf(true) }
    var hasCompletedOnboarding by remember { mutableStateOf(prefs.getBoolean(onboardingKey, false)) }
    var cardsSeenSinceAd       by remember { mutableStateOf(0) }

    val appConfig by AppConfigService.config.collectAsStateWithLifecycle()
    val isLoaded  by AppConfigService.isLoaded.collectAsStateWithLifecycle()

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory())

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            CoroutineScope(Dispatchers.IO).launch {
                DeviceService.shared.syncFCMIfNeeded()
            }
        }
    }

    // ── Session analytics — matches iOS onAppear/willResignActive ────────
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START  -> AnalyticsManager.sessionStarted()
                Lifecycle.Event.ON_STOP   -> AnalyticsManager.sessionEnded()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        coroutineScope {
            val splashJob  = async { delay(3000) }
            val configDone = async { while (!AppConfigService.isLoaded.value) delay(100) }
            splashJob.await(); configDone.await()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!DeviceService.shared.hasNotificationPermission(context)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                CoroutineScope(Dispatchers.IO).launch { DeviceService.shared.syncFCMIfNeeded() }
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch { DeviceService.shared.syncFCMIfNeeded() }
        }

        AdManager.loadAd(context)
        showSplash = false
    }

    fun trackCard() {
        cardsSeenSinceAd++
        activity?.let { RatingManager.recordSwipe(it) }
        if (cardsSeenSinceAd >= 9) {
            cardsSeenSinceAd = 0
            activity?.let { AdManager.showAd(it) }
        }
    }

    val rootState = when {
        showSplash -> "splash"
        !isLoaded  -> "loading"
        appConfig?.maintenance?.enabled == true -> "maintenance"
        appConfig != null && AppConfigService.needsForceUpdate(
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        ) -> "forceUpdate"
        !hasCompletedOnboarding -> "onboarding"
        else -> "main"
    }

    AnimatedContent(
        targetState    = rootState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label          = "rootNav",
        modifier       = Modifier.fillMaxSize().background(AppColors.background),
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
            else -> {
                activity?.let { RatingManager.recordAppOpen(it) }
                MainTabView(
                    homeViewModel = homeViewModel,
                    onTrackCard   = ::trackCard,
                )
            }
        }
    }
}
