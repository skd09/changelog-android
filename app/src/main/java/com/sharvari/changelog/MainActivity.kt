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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sharvari.changelog.data.service.DeviceService
import com.sharvari.changelog.data.store.AppConfigService
import com.sharvari.changelog.data.store.BookmarkStore
import com.sharvari.changelog.data.store.StatsStore
import com.sharvari.changelog.ui.components.AdManager
import com.sharvari.changelog.ui.discover.DiscoverScreen
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

        if (savedInstanceState == null) {
            val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
            CoroutineScope(Dispatchers.IO).launch {
                AppConfigService.fetch()
                DeviceService.shared.registerIfNeeded(appVersion)
            }
            // Init BookmarkStore from disk
            BookmarkStore.init(this)
        }

        setContent { ChangelogTheme { ChangelogApp() } }
    }
}

// ── Tab definition ────────────────────────────────────────────────────────────

enum class Tab(
    val label:      String,
    val icon:       ImageVector,
    val activeIcon: ImageVector,
) {
    FEED("FEED",         Icons.Outlined.Newspaper,  Icons.Filled.Newspaper),
    DISCOVER("DISCOVER", Icons.Outlined.AutoAwesome,    Icons.Default.AutoAwesome),
    SETTINGS("SETTINGS", Icons.Outlined.Settings,   Icons.Filled.Settings),
}

// ─────────────────────────────────────────────────────────────────────────────
// ChangelogApp
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ChangelogApp() {
    val context  = LocalContext.current
    val activity = context as? ComponentActivity

    val onboardingKey = "tc_onboarding_complete"
    val prefs = remember { context.getSharedPreferences("changelog", android.content.Context.MODE_PRIVATE) }

    var showSplash             by remember { mutableStateOf(true) }
    var hasCompletedOnboarding by remember { mutableStateOf(prefs.getBoolean(onboardingKey, false)) }
    var cardsSeenSinceAd       by remember { mutableStateOf(0) }

    val appConfig by AppConfigService.config.collectAsState()
    val isLoaded  by AppConfigService.isLoaded.collectAsState()

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
        targetState = rootState,
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
            else -> MainTabView(
                homeViewModel = homeViewModel,
                onTrackCard   = ::trackCard,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MainTabView — mirrors iOS MainTabView with custom bottom tab bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MainTabView(
    homeViewModel: HomeViewModel,
    onTrackCard:   () -> Unit,
) {
    var selectedTab  by remember { mutableStateOf(Tab.FEED) }
    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Tab content
        when (selectedTab) {
            Tab.FEED     -> HomeScreen(
                onOpenSettings = { showSettings = true },
                viewModel      = homeViewModel,
            )
            Tab.DISCOVER -> DiscoverScreen()
            Tab.SETTINGS -> {
                // Settings handled via overlay — snap back to FEED if user taps Settings tab
                // then closes. Keep FEED selected, show Settings as overlay.
            }
        }

        // Settings overlay
        if (showSettings) {
            SettingsScreen(onDismiss = { showSettings = false })
        }

        // Custom bottom tab bar — matches iOS design
        CustomTabBar(
            selectedTab = selectedTab,
            modifier    = Modifier.align(Alignment.BottomCenter),
            onTabSelected = { tab ->
                if (tab == Tab.SETTINGS) {
                    showSettings = true
                } else {
                    selectedTab  = tab
                    showSettings = false
                }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CustomTabBar — matches iOS custom tab bar with animated indicator
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CustomTabBar(
    selectedTab:   Tab,
    modifier:      Modifier = Modifier,
    onTabSelected: (Tab) -> Unit,
) {
    val tabs = Tab.entries

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .navigationBarsPadding(),
    ) {
        // Top divider
        HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp, modifier = Modifier.align(Alignment.TopCenter))

        // Animated neon indicator at top — one box per tab, fade in/out
        val selectedIndex = tabs.indexOf(selectedTab)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter),
        ) {
            tabs.forEachIndexed { i, _ ->
                val indicatorAlpha by animateFloatAsState(
                    targetValue   = if (i == selectedIndex) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                    label         = "indicator_$i",
                )
                Box(
                    modifier         = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .fillMaxHeight()
                            .alpha(indicatorAlpha)
                            .background(
                                Brush.horizontalGradient(listOf(AppColors.neon, AppColors.magenta)),
                                RoundedCornerShape(1.dp),
                            ),
                    )
                }
            }
        }

        // Tab items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 16.dp),
        ) {
            tabs.forEach { tab ->
                val isActive = selectedTab == tab
                val iconColor by animateColorAsState(
                    if (isActive) AppColors.neon else AppColors.textMuted,
                    animationSpec = tween(200), label = "icon_${tab.name}"
                )

                Column(
                    modifier             = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment  = Alignment.CenterHorizontally,
                    verticalArrangement  = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector        = if (isActive) tab.activeIcon else tab.icon,
                        contentDescription = tab.label,
                        tint               = iconColor,
                        modifier           = Modifier.size(22.dp),
                    )
                    Text(
                        text          = tab.label,
                        fontFamily    = FontFamily.Monospace,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 9.sp,
                        letterSpacing = 1.sp,
                        color         = iconColor,
                    )
                }
            }
        }
    }
}