package com.sharvari.changelog.ui.screen.onboarding

import android.app.TimePickerDialog
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharvari.changelog.data.other.APIService
import com.sharvari.changelog.model.article.ArticleCategory
import com.sharvari.changelog.store.category.CategoryStore
import com.sharvari.changelog.service.analytics.AnalyticsManager
import com.sharvari.changelog.ui.components.CyberBackground
import com.sharvari.changelog.ui.components.CyberBadge
import com.sharvari.changelog.ui.components.CyberButton
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

// ─────────────────────────────────────────────────────────────────────────────
// OnboardingScreen — 3 pages matching iOS exactly:
//   Page 0: Welcome (logo + 4 feature bullets + GET STARTED)
//   Page 1: Your Channels (category picker)
//   Page 2: Stay in the Loop (notification prefs)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val totalPages    = 3
    val pagerState    = rememberPagerState { totalPages }
    val selectedSlugs by CategoryStore.selectedSlugs.collectAsStateWithLifecycle()
    val currentPage   = pagerState.currentPage
    val scope         = rememberCoroutineScope()
    val context       = LocalContext.current

    // Notification prefs state — page 2
    val prefs = remember { context.getSharedPreferences("changelog", android.content.Context.MODE_PRIVATE) }
    var notifFrequency by remember { mutableStateOf("daily") }
    var localHour      by remember { mutableIntStateOf(8) }  // default 8:00 AM
    var localMinute    by remember { mutableIntStateOf(0) }

    fun goTo(page: Int) = scope.launch { pagerState.animateScrollToPage(page) }

    LaunchedEffect(Unit) { AnalyticsManager.trackScreen("Onboarding") }

    CyberBackground(modifier = Modifier.fillMaxSize()) {
        CyberGridOverlay(modifier = Modifier.fillMaxSize())

        // Atmospheric glow
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(AppColors.neon.copy(alpha = 0.1f), Color.Transparent),
                center = Offset(0f, 0f), radius = 800f,
            )
        ))
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(AppColors.magenta.copy(alpha = 0.05f), Color.Transparent),
                center = Offset(Float.MAX_VALUE, Float.MAX_VALUE), radius = 600f,
            )
        ))

        Column(modifier = Modifier.fillMaxSize()) {

            // Skip button (only on page 0)
            Box(
                modifier         = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (currentPage == 0) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, AppColors.divider, RoundedCornerShape(AppRadius.pill))
                            .clip(RoundedCornerShape(AppRadius.pill))
                            .clickable { goTo(1) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text("SKIP", style = AppTypography.caption, color = AppColors.textMuted, letterSpacing = AppTypography.trackingWide)
                    }
                } else {
                    Spacer(Modifier.height(30.dp))
                }
            }

            // Pager
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false, // we control navigation with buttons
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> ChannelPickerPage(selectedSlugs = selectedSlugs, onToggle = { CategoryStore.toggle(it) })
                    2 -> NotificationPage(
                        frequency  = notifFrequency,
                        localHour  = localHour,
                        localMinute = localMinute,
                        onFrequencyChange = { notifFrequency = it },
                        onTimeTap = {
                            TimePickerDialog(context, { _, h, m -> localHour = h; localMinute = m }, localHour, localMinute, false).show()
                        },
                    )
                }
            }

            // Bottom controls
            Column(
                modifier            = Modifier.padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Page dots
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    repeat(totalPages) { i ->
                        val isActive = i == currentPage
                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(if (isActive) 20.dp else 5.dp)
                                .background(
                                    if (isActive) AppColors.neon else AppColors.divider,
                                    RoundedCornerShape(AppRadius.pill),
                                )
                        )
                    }
                }

                Spacer(Modifier.height(AppSpacing.lg))

                val channelsEmpty = selectedSlugs.isEmpty()

                CyberButton(
                    label = when (currentPage) {
                        0 -> "GET STARTED →"
                        1 -> if (channelsEmpty) "SELECT A CHANNEL" else "NEXT →"
                        2 -> "SAVE & GET STARTED ⚡"
                        else -> "NEXT →"
                    },
                    enabled = !(currentPage == 1 && channelsEmpty),
                    onClick = {
                        when (currentPage) {
                            0 -> {
                                AnalyticsManager.trackClick("get_started", "Onboarding")
                                goTo(1)
                            }
                            1 -> {
                                AnalyticsManager.trackClick("next_channels", "Onboarding")
                                goTo(2)
                            }
                            2 -> {
                                AnalyticsManager.trackClick("save_and_get_started", "Onboarding")
                                // Save notification prefs then complete
                                val utcTime = localHourMinuteToUtcString(localHour, localMinute)
                                val categories = selectedSlugs.toList()
                                prefs.edit()
                                    .putString("notif_frequency", notifFrequency)
                                    .putString("notif_time", utcTime)
                                    .putString("notif_categories", categories.joinToString(","))
                                    .apply()
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val response = APIService.shared.fetchCategories()
                                        val slugToId = response.data.associate { it.slug to it.id }
                                        val categoryIds = selectedSlugs.mapNotNull { slugToId[it] }.ifEmpty { null }
                                        APIService.shared.updateNotificationPreferences(
                                            frequency   = notifFrequency,
                                            time        = utcTime,
                                            categoryIds = categoryIds,
                                        )
                                    } catch (_: Exception) { /* non-fatal */ }
                                }
                                onComplete()
                            }
                        }
                    },
                )

                // Skip for now on notification page
                if (currentPage == 2) {
                    Spacer(Modifier.height(AppSpacing.md))
                    Text(
                        text     = "Skip for now",
                        style    = AppTypography.body,
                        color    = AppColors.textMuted,
                        modifier = Modifier.clickable { onComplete() },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 0 — Welcome (matches iOS page 1)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomePage() {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); appeared = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppSpacing.lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Delta logo
        AnimatedVisibility(appeared, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 }) {
            Box(modifier = Modifier.padding(bottom = AppSpacing.xl)) {
                DeltaLogo()
            }
        }

        AnimatedVisibility(appeared, enter = fadeIn(tween(400, 100))) {
            Text("WELCOME TO", style = AppTypography.caption, color = AppColors.neon.copy(alpha = 0.7f), letterSpacing = AppTypography.trackingXWide)
        }
        Spacer(Modifier.height(AppSpacing.xs))
        AnimatedVisibility(appeared, enter = fadeIn(tween(450, 180))) {
            Text(
                text       = "THE\nCHANGELOG",
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black,
                fontSize   = 38.sp, lineHeight = 44.sp, color = AppColors.textPrimary,
            )
        }

        Spacer(Modifier.height(AppSpacing.lg))

        // Divider
        AnimatedVisibility(appeared, enter = fadeIn(tween(400, 250))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(AppColors.neon, CircleShape))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.neon.copy(alpha = 0.2f)))
            }
        }

        Spacer(Modifier.height(AppSpacing.lg))

        // Feature bullets — matches iOS 4 bullets
        val features = listOf(
            Pair(Icons.Default.FlashOn,       "Stories distilled to 10 seconds"),
            Pair(Icons.Default.Block,          "No account, no sign-up, ever"),
            Pair(Icons.Default.Shield,         "No personal data collected"),
            Pair(Icons.Default.BookmarkBorder, "Save, share, read at your pace"),
        )

        AnimatedVisibility(appeared, enter = fadeIn(tween(450, 300))) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                features.forEach { (icon, text) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier         = Modifier.size(36.dp)
                                .background(AppColors.neon.copy(alpha = 0.08f), CircleShape)
                                .border(1.dp, AppColors.neon.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(icon, null, tint = AppColors.neon, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(AppSpacing.md))
                        Text(text, style = AppTypography.body, color = AppColors.textSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(AppSpacing.xxl))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 1 — Channel Picker (matches iOS page 2)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChannelPickerPage(
    selectedSlugs: Set<String>,
    onToggle:      (String) -> Unit,
) {
    val n = selectedSlugs.size

    LazyVerticalGrid(
        columns               = GridCells.Fixed(2),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier              = Modifier.fillMaxSize().padding(horizontal = AppSpacing.lg),
        contentPadding        = PaddingValues(top = AppSpacing.md, bottom = AppSpacing.xxl),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(modifier = Modifier.padding(bottom = AppSpacing.sm)) {
                Text("STEP 2 OF 3", style = AppTypography.caption, color = AppColors.neon.copy(alpha = 0.7f), letterSpacing = AppTypography.trackingXWide)
                Spacer(Modifier.height(6.dp))
                Text("YOUR\nCHANNELS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 38.sp, lineHeight = 44.sp, color = AppColors.textPrimary)
                Spacer(Modifier.height(6.dp))
                Text("Choose what appears in your feed", style = AppTypography.body, color = AppColors.textSecondary)
            }
        }

        item(span = { GridItemSpan(2) }) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = AppSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
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
                        .clickable { if (n == CategoryStore.allCategories.size) CategoryStore.clearAll() else CategoryStore.selectAll() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        items(CategoryStore.allCategories) { cat ->
            CategoryCard(
                category = cat,
                selected = selectedSlugs.contains(cat.slug),
                onClick  = { onToggle(cat.slug) },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 2 — Notification Prefs (matches iOS page 3)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationPage(
    frequency:         String,
    localHour:         Int,
    localMinute:       Int,
    onFrequencyChange: (String) -> Unit,
    onTimeTap:         () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.lg),
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(Modifier.height(AppSpacing.md))

        Text("STEP 3 OF 3", style = AppTypography.caption, color = AppColors.neon.copy(alpha = 0.7f), letterSpacing = AppTypography.trackingXWide)
        Spacer(Modifier.height(6.dp))
        Text("STAY IN\nTHE LOOP", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 38.sp, lineHeight = 44.sp, color = AppColors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Get notified about the stories that matter to you.", style = AppTypography.body, color = AppColors.textSecondary)

        Spacer(Modifier.height(AppSpacing.xl))

        // Frequency options
        Text("FREQUENCY", style = AppTypography.caption, color = AppColors.textSecondary, letterSpacing = AppTypography.trackingWide)
        Spacer(Modifier.height(AppSpacing.sm))

        data class FreqOption(val value: String, val label: String, val desc: String, val icon: ImageVector)
        val options = listOf(
            FreqOption("daily",   "Daily Digest", "One summary at your preferred time",  Icons.Default.CalendarMonth),
            FreqOption("instant", "Instant",      "Get notified when top stories break", Icons.Default.Bolt),
            FreqOption("off",     "Off",          "No push notifications",               Icons.Default.NotificationsOff),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surfaceHigh, RoundedCornerShape(AppRadius.md))
                .border(1.dp, AppColors.divider, RoundedCornerShape(AppRadius.md))
                .clip(RoundedCornerShape(AppRadius.md)),
        ) {
            options.forEachIndexed { i, opt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFrequencyChange(opt.value) }
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    Icon(opt.icon, null, tint = if (frequency == opt.value) AppColors.neon else AppColors.textSecondary, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(opt.label, style = AppTypography.body, color = AppColors.textPrimary, fontWeight = FontWeight.Medium)
                        Text(opt.desc, style = AppTypography.caption, color = AppColors.textSecondary)
                    }
                    if (frequency == opt.value) {
                        Icon(Icons.Default.CheckCircle, null, tint = AppColors.neon, modifier = Modifier.size(20.dp))
                    }
                }
                if (i < options.size - 1) {
                    androidx.compose.material3.HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp)
                }
            }
        }

        // Digest time — only shown when daily
        if (frequency == "daily") {
            Spacer(Modifier.height(AppSpacing.lg))
            Text("DIGEST TIME", style = AppTypography.caption, color = AppColors.textSecondary, letterSpacing = AppTypography.trackingWide)
            Spacer(Modifier.height(AppSpacing.sm))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.surfaceHigh, RoundedCornerShape(AppRadius.md))
                    .border(1.dp, AppColors.divider, RoundedCornerShape(AppRadius.md))
                    .clip(RoundedCornerShape(AppRadius.md))
                    .clickable(onClick = onTimeTap)
                    .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, null, tint = AppColors.neon.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(AppSpacing.sm))
                    Text("Send at", style = AppTypography.body, color = AppColors.textPrimary)
                }
                Text(
                    text       = formatLocalTime(localHour, localMinute),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = AppColors.neon,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text("Times are in your local timezone.", style = AppTypography.caption, color = AppColors.textSecondary)
        }

        Spacer(Modifier.height(AppSpacing.xxl))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DeltaLogo (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeltaLogo() {
    val transition = rememberInfiniteTransition(label = "delta")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "rotation")
    val pulse    by transition.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse")

    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(100.dp).rotate(rotation)) {
            drawArc(
                brush = Brush.sweepGradient(listOf(AppColors.neon, AppColors.magenta, Color.Transparent)),
                startAngle = 0f, sweepAngle = 270f, useCenter = false,
                style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Canvas(modifier = Modifier.size(56.dp)) {
            val w = size.width; val h = size.height
            val path = Path().apply { moveTo(w / 2f, 0f); lineTo(w, h); lineTo(0f, h); close() }
            drawPath(path, Brush.linearGradient(listOf(AppColors.neon, AppColors.magenta), Offset(0f, 0f), Offset(w, h)), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(Brush.radialGradient(listOf(AppColors.neon, AppColors.magenta)), 3.dp.toPx(), Offset(w / 2f, h * 0.72f), alpha = pulse)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryCard(category: ArticleCategory, selected: Boolean, onClick: () -> Unit) {
    val accent = AppColors.categoryAccent(category.slug)
    Box(
        modifier = Modifier
            .fillMaxWidth().height(80.dp)
            .clip(RoundedCornerShape(AppRadius.md))
            .background(if (selected) accent.copy(alpha = 0.15f) else AppColors.surface, RoundedCornerShape(AppRadius.md))
            .border(1.dp, if (selected) accent.copy(alpha = 0.5f) else AppColors.divider, RoundedCornerShape(AppRadius.md))
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier.size(28.dp).background(accent.copy(alpha = 0.15f), CircleShape).border(1.dp, accent.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(categoryIcon(category.slug), null, tint = accent, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(category.name, style = AppTypography.mono12, color = if (selected) accent else AppColors.textSecondary, maxLines = 1, modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Default.CheckCircle, null, tint = accent, modifier = Modifier.size(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CyberGridOverlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CyberGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.alpha(0.035f)) {
        val step = 40.dp.toPx()
        val neon = Color(0xFFBD93F9)
        var x = 0f
        while (x <= size.width) { drawLine(neon, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f); x += step }
        var y = 0f
        while (y <= size.height) { drawLine(neon, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f); y += step }
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
    else          -> Icons.AutoMirrored.Filled.Article
}

fun localHourMinuteToUtcString(hour: Int, minute: Int): String {
    val local = Calendar.getInstance()
    local.set(Calendar.HOUR_OF_DAY, hour)
    local.set(Calendar.MINUTE, minute)
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.timeInMillis = local.timeInMillis
    return String.format("%02d:%02d", utc.get(Calendar.HOUR_OF_DAY), utc.get(Calendar.MINUTE))
}

fun formatLocalTime(hour: Int, minute: Int): String = String.format(
    "%d:%02d %s",
    if (hour % 12 == 0) 12 else hour % 12,
    minute,
    if (hour < 12) "AM" else "PM",
)