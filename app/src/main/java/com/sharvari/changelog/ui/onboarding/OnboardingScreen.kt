package com.sharvari.changelog.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharvari.changelog.data.model.ArticleCategory
import com.sharvari.changelog.data.store.CategoryStore
import com.sharvari.changelog.ui.components.CyberBackground
import com.sharvari.changelog.ui.components.CyberBadge
import com.sharvari.changelog.ui.components.CyberButton
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Feature page model — ImageVector instead of emoji, mirrors iOS FeaturePage
// ─────────────────────────────────────────────────────────────────────────────

private data class FeaturePage(
    val icon:     ImageVector,
    val eyebrow:  String,
    val title:    String,
    val subtitle: String,
    val accent:   Color,
)

private val featurePages = listOf(
    FeaturePage(
        icon     = Icons.Default.FlashOn,
        eyebrow  = "WELCOME TO",
        title    = "THE\nCHANGELOG",
        subtitle = "The fastest way to stay on top of tech news. Built for people who are busy but still want to know what's happening.",
        accent   = AppColors.neon,
    ),
    FeaturePage(
        icon     = Icons.Default.Timer,
        eyebrow  = "YOUR TIME MATTERS",
        title    = "NEWS IN\nUNDER 30 SEC",
        subtitle = "Every story is distilled to its core. No fluff, no filler — just what you actually need to know, as fast as you can read it.",
        accent   = AppColors.neon,
    ),
    FeaturePage(
        icon     = Icons.Default.Block,
        eyebrow  = "ZERO FRICTION",
        title    = "NO ACCOUNT\nNEEDED",
        subtitle = "Open the app and go. No sign-up, no email, no password to forget. We don't know who you are and we don't need to.",
        accent   = AppColors.magenta,
    ),
    FeaturePage(
        icon     = Icons.Default.Shield,
        eyebrow  = "YOUR PRIVACY",
        title    = "NO TRACKING\nEVER",
        subtitle = "No ads profiling. No data sold. No subscription. We use a random anonymous device ID — nothing tied to you personally.",
        accent   = AppColors.magenta,
    ),
    FeaturePage(
        icon     = Icons.Default.Tune,
        eyebrow  = "MADE FOR YOU",
        title    = "PICK YOUR\nCHANNELS",
        subtitle = "Choose the topics you care about. We'll surface the most relevant stories and filter out everything else.",
        accent   = AppColors.neon,
    ),
)

// ─────────────────────────────────────────────────────────────────────────────
// OnboardingScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val totalPages    = featurePages.size + 1
    val pagerState    = rememberPagerState { totalPages }
    val selectedSlugs by CategoryStore.selectedSlugs.collectAsState()
    val currentPage   = pagerState.currentPage
    val scope         = rememberCoroutineScope()

    val accent = if (currentPage < featurePages.size) featurePages[currentPage].accent else AppColors.neon

    fun goTo(page: Int) = scope.launch { pagerState.animateScrollToPage(page) }

    CyberBackground(modifier = Modifier.fillMaxSize()) {

        // ── Cyber grid — matches iOS CyberGridView().opacity(0.035)
        CyberGridOverlay(modifier = Modifier.fillMaxSize())

        // ── Dual atmospheric glow as background — pointer events pass through
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = 800f,
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(AppColors.magenta.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(Float.MAX_VALUE, Float.MAX_VALUE),
                        radius = 600f,
                    )
                )
        )

        // ── Main content — rendered on top of glows so skip button is always tappable
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Skip button — statusBarsPadding pushes below Android status bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (currentPage < featurePages.size - 1) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, AppColors.divider, RoundedCornerShape(AppRadius.pill))
                            .clip(RoundedCornerShape(AppRadius.pill))
                            .clickable { goTo(featurePages.size) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text          = "SKIP",
                            style         = AppTypography.caption,
                            color         = AppColors.textMuted,
                            letterSpacing = AppTypography.trackingWide,
                        )
                    }
                } else {
                    Spacer(Modifier.height(30.dp))
                }
            }

            // ── Pager
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                if (page < featurePages.size) {
                    FeaturePageView(page = featurePages[page], isFirst = page == 0)
                } else {
                    ChannelPickerPage(
                        selectedSlugs = selectedSlugs,
                        onToggle      = { CategoryStore.toggle(it) },
                        onSelectAll   = { CategoryStore.selectAll() },
                    )
                }
            }

            // ── Bottom controls
            Column(
                modifier            = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Page dots — matches iOS Capsule with neon glow on active
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    repeat(totalPages) { i ->
                        val isActive = i == currentPage
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(if (isActive) 20.dp else 5.dp)
                                .background(
                                    if (isActive) AppColors.neon else AppColors.divider,
                                    RoundedCornerShape(AppRadius.pill)
                                )
                        )
                    }
                }

                Spacer(Modifier.height(AppSpacing.lg))

                // CTA — mirrors iOS label logic exactly
                val isChannelPage = currentPage == featurePages.size
                val isLastFeature = currentPage == featurePages.size - 1
                val channelsEmpty = selectedSlugs.isEmpty()

                CyberButton(
                    label   = when {
                        isChannelPage && channelsEmpty -> "SELECT A CHANNEL"
                        isChannelPage                  -> "GO LIVE ⚡"
                        isLastFeature                  -> "PICK YOUR CHANNELS →"
                        else                           -> "NEXT →"
                    },
                    enabled = !(isChannelPage && channelsEmpty),
                    onClick = {
                        when {
                            isChannelPage -> onComplete()
                            else          -> goTo(currentPage + 1)
                        }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FeaturePageView — mirrors iOS FeaturePageView layout exactly
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FeaturePageView(page: FeaturePage, isFirst: Boolean = false) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(page) { delay(50); appeared = true }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isSmall  = maxHeight < 640.dp
        val titleSp  = if (isSmall) 30.sp else 38.sp
        val iconSize = if (isSmall) 80.dp  else 100.dp
        val orbSize  = if (isSmall) 56.dp  else 72.dp
        val iconDp   = if (isSmall) 22.dp  else 28.dp

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.lg),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // ── Icon — Delta logo page 0, icon orb all others
            AnimatedVisibility(
                visible = appeared,
                enter   = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 },
            ) {
                Box(modifier = Modifier.padding(bottom = AppSpacing.xl)) {
                    if (isFirst) {
                        DeltaLogo()
                    } else {
                        // Matches iOS: outer Circle stroke + inner Circle fill + Icon
                        Box(
                            modifier         = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .border(1.dp, page.accent.copy(alpha = 0.15f), CircleShape)
                            )
                            Box(
                                modifier         = Modifier
                                    .size(72.dp)
                                    .background(page.accent.copy(alpha = 0.1f), CircleShape)
                                    .border(1.dp, page.accent.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector        = page.icon,
                                    contentDescription = null,
                                    tint               = page.accent,
                                    modifier           = Modifier.size(28.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ── Eyebrow — matches iOS .caption + trackingXWide + accent.opacity(0.7)
            AnimatedVisibility(appeared, enter = fadeIn(tween(400, 100))) {
                Text(
                    text          = page.eyebrow,
                    style         = AppTypography.caption,
                    color         = page.accent.copy(alpha = 0.7f),
                    letterSpacing = AppTypography.trackingXWide,
                )
            }

            Spacer(Modifier.height(AppSpacing.xs))

            // ── Title — matches iOS .system(size: 38, weight: .black, design: .monospaced)
            AnimatedVisibility(appeared, enter = fadeIn(tween(450, 180))) {
                Text(
                    text          = page.title,
                    fontFamily    = FontFamily.Monospace,
                    fontWeight    = FontWeight.Black,
                    fontSize      = titleSp,
                    letterSpacing = 0.1.sp,
                    lineHeight    = (titleSp.value * 1.15f).sp,
                    color         = AppColors.textPrimary,
                )
            }

            Spacer(Modifier.height(AppSpacing.lg))

            // ── Divider line — matches iOS HStack { Circle + Rectangle }
            AnimatedVisibility(appeared, enter = fadeIn(tween(400, 250))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(page.accent, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(page.accent.copy(alpha = 0.2f))
                    )
                }
            }

            Spacer(Modifier.height(AppSpacing.lg))

            // ── Subtitle — matches iOS .body + textSecondary + lineSpacing(5)
            AnimatedVisibility(appeared, enter = fadeIn(tween(450, 300))) {
                Text(
                    text       = page.subtitle,
                    style      = AppTypography.body,
                    color      = AppColors.textSecondary,
                    lineHeight = 24.sp,
                )
            }

            Spacer(Modifier.height(if (isSmall) AppSpacing.lg else AppSpacing.xxl))
        }
    } // end BoxWithConstraints
}

// ─────────────────────────────────────────────────────────────────────────────
// DeltaLogo — mirrors iOS OnboardingDeltaLogo exactly
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeltaLogo() {
    val transition = rememberInfiniteTransition(label = "delta")

    // 360° rotation over 2s, linear — matches iOS .linear(duration: 2).repeatForever
    val rotation by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label         = "rotation",
    )

    // Pulse 0.6 → 1.0 over 1.2s — matches iOS .easeInOut(duration: 1.2).repeatForever
    val pulse by transition.animateFloat(
        initialValue  = 0.6f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label         = "pulse",
    )

    Box(
        modifier         = Modifier.size(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Spinning arc — neon → magenta → transparent, 270° sweep
        Canvas(
            modifier = Modifier
                .size(100.dp)
                .rotate(rotation)
        ) {
            drawArc(
                brush      = Brush.sweepGradient(
                    listOf(AppColors.neon, AppColors.magenta, Color.Transparent)
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter  = false,
                style      = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Delta triangle + pulsing center dot
        Canvas(modifier = Modifier.size(56.dp)) {
            val w = size.width
            val h = size.height

            // Triangle path — matches iOS DeltaShape
            val path = Path().apply {
                moveTo(w / 2f, 0f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            drawPath(
                path  = path,
                brush = Brush.linearGradient(
                    colors = listOf(AppColors.neon, AppColors.magenta),
                    start  = Offset(0f, 0f),
                    end    = Offset(w, h),
                ),
                style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
            )

            // Pulsing dot — matches iOS offset(y: 56 * 0.72 / 2 - 2)
            drawCircle(
                brush  = Brush.radialGradient(listOf(AppColors.neon, AppColors.magenta)),
                radius = 3.dp.toPx(),
                center = Offset(w / 2f, h * 0.72f),
                alpha  = pulse,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ChannelPickerPage — mirrors iOS ChannelPickerPage
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChannelPickerPage(
    selectedSlugs: Set<String>,
    onToggle:      (String) -> Unit,
    onSelectAll:   () -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    // Use BoxWithConstraints for responsive card height, but NO verticalScroll wrapper —
    // LazyVerticalGrid owns all scrolling; headers go in as span-2 items
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cardHeight = if (maxHeight < 640.dp) 70.dp else 80.dp
        val n = selectedSlugs.size

        LazyVerticalGrid(
            columns               = GridCells.Fixed(2),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier
                .fillMaxSize()
                .padding(horizontal = AppSpacing.lg),
            contentPadding        = PaddingValues(top = AppSpacing.md, bottom = AppSpacing.xxl),
        ) {

            // ── Header — full width span
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(bottom = AppSpacing.sm)) {
                    Text(
                        text          = "LAST STEP",
                        style         = AppTypography.caption,
                        color         = AppColors.neon.copy(alpha = 0.7f),
                        letterSpacing = AppTypography.trackingXWide,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text          = "YOUR CHANNELS",
                        fontFamily    = FontFamily.Monospace,
                        fontWeight    = FontWeight.Black,
                        fontSize      = 38.sp,
                        letterSpacing = 0.1.sp,
                        lineHeight    = 44.sp,
                        color         = AppColors.textPrimary,
                    )
                }
            }

            // ── Badge + select all row — full width span
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CyberBadge(
                        text  = if (n == 0) "NONE SELECTED" else "$n SELECTED",
                        color = if (n == 0) AppColors.textMuted else AppColors.neon,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text          = if (n == CategoryStore.allCategories.size) "CLEAR ALL" else "SELECT ALL",
                        style         = AppTypography.caption,
                        color         = AppColors.textSecondary,
                        letterSpacing = AppTypography.trackingWide,
                        modifier      = Modifier
                            .border(1.dp, AppColors.divider, RoundedCornerShape(AppRadius.pill))
                            .clip(RoundedCornerShape(AppRadius.pill))
                            .clickable { onSelectAll() }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            // ── Category cards — 2 per row
            items(CategoryStore.allCategories) { cat ->
                CategoryCard(
                    category   = cat,
                    selected   = selectedSlugs.contains(cat.slug),
                    onClick    = { onToggle(cat.slug) },
                    cardHeight = cardHeight,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryCard — matches iOS CategoryCard with icon circle + checkmark
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryCard(
    category:   ArticleCategory,
    selected:   Boolean,
    onClick:    () -> Unit,
    cardHeight: androidx.compose.ui.unit.Dp = 80.dp,
) {
    val accent = AppColors.categoryAccent(category.slug)
    val bg     = if (selected) accent.copy(alpha = 0.15f) else AppColors.surface
    val border = if (selected) accent.copy(alpha = 0.5f)  else AppColors.divider

    // Outer Box: background + border + clip + click only — NO padding here
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(RoundedCornerShape(AppRadius.md))
            .background(bg, RoundedCornerShape(AppRadius.md))
            .border(1.dp, border, RoundedCornerShape(AppRadius.md))
            .clickable(onClick = onClick),
    ) {
        // Inner Row: padding lives here so it never gets clipped
        Row(
            modifier          = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon circle
            Box(
                modifier         = Modifier
                    .size(28.dp)
                    .background(accent.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = categoryIcon(category.slug),
                    contentDescription = null,
                    tint               = accent,
                    modifier           = Modifier.size(14.dp),
                )
            }

            Spacer(Modifier.width(8.dp))

            // Category name — always fully visible
            Text(
                text     = category.name,
                style    = AppTypography.mono12,
                color    = if (selected) accent else AppColors.textSecondary,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            // Checkmark
            if (selected) {
                Icon(
                    imageVector        = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint               = accent,
                    modifier           = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CyberGridOverlay — matches iOS CyberGridView at 0.035 opacity
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CyberGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.alpha(0.035f)) {
        val step = 40.dp.toPx()
        val neon = androidx.compose.ui.graphics.Color(0xFFBD93F9)
        var x = 0f
        while (x <= size.width) {
            drawLine(neon, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 0.5f)
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(neon, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 0.5f)
            y += step
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun categoryIcon(slug: String?): ImageVector = when (slug) {
    "technology"  -> Icons.Default.Computer
    "ai"          -> Icons.Default.Psychology
    "security"    -> Icons.Default.Security
    "science"     -> Icons.Default.Science
    "business"    -> Icons.Default.BusinessCenter
    "crypto"      -> Icons.Default.CurrencyBitcoin
    "gaming"      -> Icons.Default.SportsEsports
    "space"       -> Icons.Default.RocketLaunch
    "health"      -> Icons.Default.Favorite
    "open-source" -> Icons.Default.Code
    else          -> Icons.Default.Article
}