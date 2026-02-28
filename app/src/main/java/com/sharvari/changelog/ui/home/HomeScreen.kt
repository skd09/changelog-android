package com.sharvari.changelog.ui.home

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sharvari.changelog.ui.article.ArticleCardView
import com.sharvari.changelog.ui.components.CyberBackground
import com.sharvari.changelog.ui.components.CyberLoader
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val articles      by viewModel.articles.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val hasLoadedOnce by viewModel.hasLoadedOnce.collectAsState()
    val errorMessage  by viewModel.errorMessage.collectAsState()

    var isOnline by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Network monitor — matches iOS NWPathMonitor
    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isOnline = true }
            override fun onLost(network: Network) { isOnline = false }
        }
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(req, cb)
        onDispose { cm.unregisterNetworkCallback(cb) }
    }

    CyberBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text  = "The Changelog",
                            style = AppTypography.mono12,
                            color = AppColors.textPrimary,
                            letterSpacing = 0.10.sp,
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AppColors.textSecondary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppColors.background,
                        scrolledContainerColor = AppColors.background,
                    ),
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    !isOnline && articles.isEmpty() -> NoInternetView { viewModel.loadArticles(true) }
                    !hasLoadedOnce || (isLoading && articles.isEmpty()) -> LoadingView()
                    errorMessage != null && articles.isEmpty() -> ErrorView(errorMessage!!) { viewModel.loadArticles(true) }
                    hasLoadedOnce && articles.isEmpty() -> EmptyView { viewModel.loadArticles(true) }
                    else -> {
                        CardStack(
                            articles    = articles.take(3),
                            onSwipeLeft = { viewModel.dismissArticle(it) },
                            onSwipeRight = { viewModel.openArticle(it) },
                        )

                        // Loading banner on top of cards
                        AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
                            LoadingBanner()
                        }
                    }
                }
            }
        }
    }
}

// ── Card Stack ────────────────────────────────────────────────────────────────
@Composable
private fun CardStack(
    articles: List<com.sharvari.changelog.data.model.Article>,
    onSwipeLeft: (com.sharvari.changelog.data.model.Article) -> Unit,
    onSwipeRight: (com.sharvari.changelog.data.model.Article) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
    ) {
        // Render back-to-front so top card (index 0) draws on top
        articles.asReversed().forEachIndexed { revIdx, article ->
            val idx   = articles.size - 1 - revIdx
            val scale = 1f - idx * 0.04f
            val yOff  = idx * 28f          // px offset for stacked peek
            val isTop = idx == 0

            key(article.id) {              // fresh offsetX state per unique article
                ArticleCardView(
                    article      = article,
                    isTop        = isTop,
                    onSwipeLeft  = { onSwipeLeft(article) },
                    onSwipeRight = { onSwipeRight(article) },
                    modifier     = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX       = scale
                            scaleY       = scale
                            translationY = yOff
                        }
                )
            }
        }
    }
}

// ── Loading banner ────────────────────────────────────────────────────────────
@Composable
private fun LoadingBanner() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .padding(top = AppSpacing.sm)
                .background(AppColors.surface.copy(alpha = 0.95f), RoundedCornerShape(AppRadius.pill))
                .border(1.dp, AppColors.neon.copy(alpha = 0.3f), RoundedCornerShape(AppRadius.pill))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CyberLoader(size = 14.dp, strokeWidth = 1.5.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text  = "FETCHING MORE...",
                style = AppTypography.mono10,
                color = AppColors.neon,
                letterSpacing = AppTypography.trackingWide,
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

// ── Full-screen loading ───────────────────────────────────────────────────────
@Composable
private fun LoadingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CyberLoader(size = 64.dp)
            Spacer(Modifier.height(AppSpacing.lg))
            Text("LOADING FEED", style = AppTypography.label, color = AppColors.neon, letterSpacing = AppTypography.trackingXWide)
            Spacer(Modifier.height(6.dp))
            Text("Fetching the latest stories...", style = AppTypography.footnote, color = AppColors.textMuted)
        }
    }
}

// ── No internet ───────────────────────────────────────────────────────────────
@Composable
private fun NoInternetView(onRetry: () -> Unit) {
    StateView(
        icon    = Icons.Default.SignalWifiOff,
        title   = "NO CONNECTION",
        message = "Check your internet and try again",
        color   = AppColors.magenta,
        onRetry = onRetry,
    )
}

// ── Error ─────────────────────────────────────────────────────────────────────
@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    StateView(
        icon    = Icons.Default.Warning,
        title   = "SOMETHING WENT WRONG",
        message = message,
        color   = AppColors.magenta,
        onRetry = onRetry,
    )
}

// ── Empty ─────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyView(onRetry: () -> Unit) {
    StateView(
        icon    = Icons.Default.Refresh,
        title   = "ALL CAUGHT UP",
        message = "You've read everything. Check back later.",
        color   = AppColors.neon,
        onRetry = onRetry,
        retryLabel = "REFRESH",
    )
}

// ── Shared state view ─────────────────────────────────────────────────────────
@Composable
private fun StateView(
    icon: ImageVector,
    title: String,
    message: String,
    color: Color,
    onRetry: () -> Unit,
    retryLabel: String = "TRY AGAIN",
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
            modifier = Modifier.padding(AppSpacing.xl),
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(color.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(44.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = AppTypography.label, color = color, letterSpacing = AppTypography.trackingXWide)
                Spacer(Modifier.height(AppSpacing.sm))
                Text(message, style = AppTypography.footnote, color = AppColors.textSecondary)
            }

            TextButton(
                onClick   = onRetry,
                modifier  = Modifier
                    .background(AppColors.neon.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .border(1.dp, AppColors.neon.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
            ) {
                Text(retryLabel, style = AppTypography.mono12, color = AppColors.neon, letterSpacing = AppTypography.trackingWide)
            }
        }
    }
}