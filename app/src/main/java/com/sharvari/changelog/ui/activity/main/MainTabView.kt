package com.sharvari.changelog.ui.activity.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharvari.changelog.model.article.Article
import com.sharvari.changelog.service.analytics.AnalyticsManager
import com.sharvari.changelog.service.analytics.SwipeDirection
import com.sharvari.changelog.ui.screen.article.ArticleReaderScreen
import com.sharvari.changelog.ui.screen.discover.DiscoverScreen
import com.sharvari.changelog.ui.screen.home.HomeScreen
import com.sharvari.changelog.store.home.HomeViewModel
import com.sharvari.changelog.ui.screen.settings.SettingsScreen
import com.sharvari.changelog.ui.theme.AppColors

@Composable
fun MainTabView(
    homeViewModel: HomeViewModel,
    onTrackCard:   () -> Unit,
) {
    var selectedTab    by remember { mutableStateOf(Tab.FEED) }
    var showSettings   by remember { mutableStateOf(false) }
    var readerArticle  by remember { mutableStateOf<Article?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            Tab.FEED     -> HomeScreen(
                onOpenSettings    = { showSettings = true },
                viewModel         = homeViewModel,
                onOpenReader      = { article ->
                    AnalyticsManager.fullArticleTapped(article)
                    readerArticle = article
                },
                onSwipeLeft       = { article ->
                    AnalyticsManager.articleSwiped(article, SwipeDirection.LEFT)
                    homeViewModel.dismissArticle(article)
                    onTrackCard()
                },
                onSwipeRight      = { article ->
                    AnalyticsManager.articleSwiped(article, SwipeDirection.RIGHT)
                    homeViewModel.openArticle(article)
                    onTrackCard()
                    readerArticle = article
                },
            )
            Tab.DISCOVER -> DiscoverScreen()
            Tab.SETTINGS -> { /* Settings handled via overlay */ }
        }

        if (showSettings) {
            SettingsScreen(onDismiss = { showSettings = false })
        }

        readerArticle?.let { article ->
            ArticleReaderScreen(
                article   = article,
                onDismiss = { readerArticle = null },
            )
        }

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
        HorizontalDivider(
            color     = AppColors.divider,
            thickness = 0.5.dp,
            modifier  = Modifier.align(Alignment.TopCenter),
        )

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
                    animationSpec = tween(200), label = "icon_${tab.name}",
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
