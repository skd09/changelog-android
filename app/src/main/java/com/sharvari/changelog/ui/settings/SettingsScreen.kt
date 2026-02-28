package com.sharvari.changelog.ui.settings

import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sharvari.changelog.data.api.APIService
import com.sharvari.changelog.data.model.ALL_CATEGORIES
import com.sharvari.changelog.data.store.CategoryStore
import com.sharvari.changelog.data.store.ReadArticlesStore
import com.sharvari.changelog.data.store.StatsStore
import com.sharvari.changelog.ui.components.CyberBackground
import com.sharvari.changelog.ui.components.CyberBadge
import com.sharvari.changelog.ui.components.CyberButton
import com.sharvari.changelog.ui.components.NeonBar
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onDismiss: () -> Unit) {
    var showFeedback     by remember { mutableStateOf(false) }
    var showClearHistory by remember { mutableStateOf(false) }
    var showChannels     by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val context    = LocalContext.current

    val appVersion = remember {
        try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "1.0.0"
        } catch (_: PackageManager.NameNotFoundException) { "1.0.0" }
    }

    CyberBackground {
        // Cyber grid overlay — matches iOS CyberGridView().opacity(0.03)
        CyberSettingsGrid(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "SETTINGS",
                                style         = AppTypography.label,
                                color         = AppColors.neon,
                                letterSpacing = AppTypography.trackingXWide,
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = onDismiss) {
                            Text(
                                "DONE",
                                style         = AppTypography.caption,
                                color         = AppColors.neon,
                                letterSpacing = AppTypography.trackingWide,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            }
        ) { padding ->
            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = AppSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                item { StatsSection() }
                item { Spacer(Modifier.height(AppSpacing.sm)) }

                item { SettingsSectionHeader("CHANNELS") }
                item {
                    SettingsRow(icon = Icons.Default.Settings, label = "Manage channels") {
                        showChannels = true
                    }
                }

                item { SettingsSectionHeader("SPREAD THE WORD") }
                item { SettingsRow(icon = Icons.Default.Share, label = "Share with a friend") { } }
                item {
                    SettingsRow(icon = Icons.Default.Star, label = "Rate on Play Store") {
                        uriHandler.openUri("market://details?id=com.sharvari.changelog")
                    }
                }

                item { SettingsSectionHeader("FEEDBACK") }
                item { SettingsRow(icon = Icons.Default.Mail, label = "Send feedback") { showFeedback = true } }
                item {
                    SettingsRow(icon = Icons.Default.Mail, label = "Contact us") {
                        uriHandler.openUri("mailto:sharvarid.dev@gmail.com?subject=Feedback")
                    }
                }

                item { SettingsSectionHeader("READING") }
                item {
                    SettingsRow(
                        icon        = Icons.Default.Refresh,
                        label       = "Clear read history",
                        destructive = true,
                    ) { showClearHistory = true }
                }

                item { SettingsSectionHeader("APP") }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.surfaceHigh, RoundedCornerShape(AppRadius.md))
                            .border(1.dp, AppColors.divider, RoundedCornerShape(AppRadius.md))
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Info,
                            contentDescription = null,
                            tint               = AppColors.neon.copy(alpha = 0.7f),
                            modifier           = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(AppSpacing.md))
                        Text("Version", style = AppTypography.body, color = AppColors.textPrimary)
                        Spacer(Modifier.weight(1f))
                        Text(
                            appVersion,
                            style         = AppTypography.mono11,
                            color         = AppColors.textMuted,
                            letterSpacing = 0.05.sp,
                        )
                    }
                }

                item { SettingsSectionHeader("LEGAL") }
                item {
                    SettingsRow(icon = Icons.Default.Lock, label = "Privacy Policy") {
                        uriHandler.openUri("https://thechangelog.app/#privacy")
                    }
                }
                item {
                    SettingsRow(icon = Icons.Default.Info, label = "Terms of Service") {
                        uriHandler.openUri("https://thechangelog.app/#terms")
                    }
                }

                item { Spacer(Modifier.height(AppSpacing.xxl)) }
            }
        }
    }

    if (showFeedback)     FeedbackSheet(onDismiss = { showFeedback = false })
    if (showChannels)     ChannelPickerDialog(onDismiss = { showChannels = false })

    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            title = { Text("Clear read history?", color = AppColors.textPrimary) },
            text  = { Text("You may see previously read articles again.", color = AppColors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { ReadArticlesStore.clear(); showClearHistory = false }) {
                    Text("Clear", color = AppColors.magenta)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistory = false }) {
                    Text("Cancel", color = AppColors.textSecondary)
                }
            },
            containerColor = AppColors.surfaceHigh,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ChannelPickerDialog — matches iOS CategoryPickerSheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChannelPickerDialog(onDismiss: () -> Unit) {
    val selectedSlugs by CategoryStore.selectedSlugs.collectAsState()
    val n = selectedSlugs.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows  = false,
        ),
    ) {
        CyberBackground {
            CyberSettingsGrid(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                // Top bar
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = AppColors.neon,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "MANAGE CHANNELS",
                        style         = AppTypography.label,
                        color         = AppColors.neon,
                        letterSpacing = AppTypography.trackingWide,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        if (n == ALL_CATEGORIES.size) CategoryStore.clearAll()
                        else CategoryStore.selectAll()
                    }) {
                        Text(
                            if (n == ALL_CATEGORIES.size) "CLEAR ALL" else "SELECT ALL",
                            style         = AppTypography.caption,
                            color         = AppColors.textSecondary,
                            letterSpacing = AppTypography.trackingWide,
                        )
                    }
                }

                // Grid — LazyVerticalGrid owns all scrolling
                LazyVerticalGrid(
                    columns               = GridCells.Fixed(2),
                    modifier              = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppSpacing.md),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding        = PaddingValues(bottom = AppSpacing.xxl),
                ) {
                    // Header — matches iOS "YOUR CHANNELS" + subtitle
                    item(span = { GridItemSpan(2) }) {
                        Column(modifier = Modifier.padding(vertical = AppSpacing.sm)) {
                            Text(
                                text          = "YOUR CHANNELS",
                                fontFamily    = FontFamily.Monospace,
                                fontWeight    = FontWeight.Black,
                                fontSize      = 28.sp,
                                letterSpacing = 2.sp,
                                color         = AppColors.textPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text  = "Choose what appears in your feed",
                                style = AppTypography.body,
                                color = AppColors.textSecondary,
                            )
                        }
                    }

                    // Badge + select/clear row
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
                                text          = if (n == ALL_CATEGORIES.size) "CLEAR ALL" else "SELECT ALL",
                                style         = AppTypography.caption,
                                color         = AppColors.textSecondary,
                                letterSpacing = AppTypography.trackingWide,
                                modifier      = Modifier
                                    .border(1.dp, AppColors.divider, RoundedCornerShape(AppRadius.pill))
                                    .clip(RoundedCornerShape(AppRadius.pill))
                                    .clickable {
                                        if (n == ALL_CATEGORIES.size) CategoryStore.clearAll()
                                        else CategoryStore.selectAll()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }

                    // Category cards — Icon circles, matches iOS CategoryCard
                    items(ALL_CATEGORIES) { cat ->
                        val isSelected  = cat.slug in selectedSlugs
                        val accent      = AppColors.categoryAccent(cat.slug)
                        val bgColor     by animateColorAsState(
                            if (isSelected) accent.copy(alpha = 0.12f) else AppColors.surfaceHigh,
                            label = "bg${cat.slug}",
                        )
                        val borderColor by animateColorAsState(
                            if (isSelected) accent.copy(alpha = 0.5f) else AppColors.divider,
                            label = "border${cat.slug}",
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(AppRadius.md))
                                .background(bgColor, RoundedCornerShape(AppRadius.md))
                                .border(1.dp, borderColor, RoundedCornerShape(AppRadius.md))
                                .clickable { CategoryStore.toggle(cat.slug) }
                                .padding(12.dp),
                        ) {
                            Box(
                                modifier         = Modifier
                                    .size(40.dp)
                                    .align(Alignment.TopStart)
                                    .background(accent.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, accent.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector        = categoryIcon(cat.slug),
                                    contentDescription = null,
                                    tint               = accent,
                                    modifier           = Modifier.size(20.dp),
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector        = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint               = accent,
                                    modifier           = Modifier
                                        .size(22.dp)
                                        .align(Alignment.TopEnd),
                                )
                            }
                            Text(
                                text       = cat.name,
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color      = if (isSelected) accent else AppColors.textSecondary,
                                maxLines   = 1,
                                modifier   = Modifier.align(Alignment.BottomStart),
                            )
                        }
                    }

                    // Done button
                    item(span = { GridItemSpan(2) }) {
                        CyberButton(
                            label    = "DONE",
                            onClick  = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = AppSpacing.sm),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FeedbackSheet — matches iOS FeedbackSheet
// ─────────────────────────────────────────────────────────────────────────────

enum class FeedbackType(val label: String, val icon: ImageVector) {
    SUGGESTION("Suggestion", Icons.Default.Lightbulb),
    BUG("Bug Report",        Icons.Default.BugReport),
    OTHER("Other",           Icons.Default.ChatBubble),
}

@Composable
fun FeedbackSheet(onDismiss: () -> Unit) {
    var text      by remember { mutableStateOf("") }
    var type      by remember { mutableStateOf(FeedbackType.SUGGESTION) }
    var isSending by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    val scope     = rememberCoroutineScope()
    val isValid   = text.trim().length >= 10

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside   = false,
            decorFitsSystemWindows  = false,
        ),
    ) {
        CyberBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
            ) {
                // Top bar — CANCEL left, FEEDBACK centre (matches iOS)
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "CANCEL",
                            style         = AppTypography.caption,
                            color         = AppColors.textSecondary,
                            letterSpacing = AppTypography.trackingWide,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "FEEDBACK",
                        style         = AppTypography.label,
                        color         = AppColors.neon,
                        letterSpacing = AppTypography.trackingXWide,
                    )
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(72.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    if (submitted) {
                        Column(
                            modifier            = Modifier
                                .fillMaxWidth()
                                .padding(AppSpacing.xxl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        ) {
                            Spacer(Modifier.height(AppSpacing.xxl))
                            Box(
                                modifier         = Modifier
                                    .size(88.dp)
                                    .background(AppColors.neon.copy(alpha = 0.1f), CircleShape)
                                    .border(1.dp, AppColors.neon.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint     = AppColors.neon,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                            Text("THANKS!", style = AppTypography.label, color = AppColors.neon, letterSpacing = AppTypography.trackingXWide)
                            Text("Your feedback helps us improve.", style = AppTypography.body, color = AppColors.textSecondary)
                            Spacer(Modifier.height(AppSpacing.lg))
                            CyberButton(label = "CLOSE", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppSpacing.lg),
                        ) {
                            Spacer(Modifier.height(AppSpacing.md))

                            // Type picker — Icon + label
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FeedbackType.entries.forEach { t ->
                                    val isActive = t == type
                                    val bg by animateColorAsState(
                                        if (isActive) AppColors.neon else AppColors.surfaceHigh, label = "bg")
                                    val tc by animateColorAsState(
                                        if (isActive) AppColors.background else AppColors.textSecondary, label = "tc")
                                    Row(
                                        modifier          = Modifier
                                            .background(bg, RoundedCornerShape(AppRadius.pill))
                                            .border(1.dp, if (isActive) AppColors.neon else AppColors.divider, RoundedCornerShape(AppRadius.pill))
                                            .clip(RoundedCornerShape(AppRadius.pill))
                                            .clickable { type = t }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(t.icon, contentDescription = null, tint = tc, modifier = Modifier.size(11.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(t.label, style = AppTypography.mono9, color = tc, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Spacer(Modifier.height(AppSpacing.lg))

                            OutlinedTextField(
                                value         = text,
                                onValueChange = { text = it },
                                placeholder   = {
                                    Text(
                                        "Tell us what you think, report a bug, or suggest a feature...",
                                        color = AppColors.textMuted,
                                        style = AppTypography.body,
                                    )
                                },
                                minLines = 6,
                                modifier = Modifier.fillMaxWidth(),
                                colors   = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor   = AppColors.neon,
                                    unfocusedBorderColor = AppColors.divider,
                                    focusedTextColor     = AppColors.textPrimary,
                                    unfocusedTextColor   = AppColors.textPrimary,
                                    cursorColor          = AppColors.neon,
                                ),
                            )

                            if (text.isNotEmpty() && !isValid) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${10 - text.trim().length} more characters needed",
                                    style = AppTypography.mono9,
                                    color = AppColors.magenta.copy(alpha = 0.8f),
                                )
                            }

                            Spacer(Modifier.height(AppSpacing.lg))

                            if (isSending) {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(28.dp), color = AppColors.neon, strokeWidth = 2.dp)
                                }
                            } else {
                                CyberButton(
                                    label   = "SEND FEEDBACK",
                                    enabled = isValid,
                                    onClick = {
                                        isSending = true
                                        scope.launch {
                                            APIService.submitFeedback(type.name.lowercase(), text)
                                            submitted = true
                                            isSending = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            Spacer(Modifier.height(AppSpacing.md))
                            Spacer(Modifier.navigationBarsPadding())
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CyberSettingsGrid — matches iOS CyberGridView().opacity(0.03)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CyberSettingsGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.alpha(0.03f)) {
        val step = 40.dp.toPx()
        val neon = Color(0xFFBD93F9)
        var x = 0f
        while (x <= size.width) {
            drawLine(neon, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f)
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(neon, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f)
            y += step
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StatsSection
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsSection() {
    val totalReads     by StatsStore.totalReads.collectAsState()
    val totalSkips     by StatsStore.totalSkips.collectAsState()
    val totalSessions  by StatsStore.totalSessions.collectAsState()
    val avgReadTime    by StatsStore.avgReadTime.collectAsState()
    val avgSessionTime by StatsStore.avgSessionTime.collectAsState()

    val rank = readerRank(totalReads)
    val readRatio = if (totalReads + totalSkips > 0)
        "${(totalReads.toDouble() / (totalReads + totalSkips) * 100).toInt()}%"
    else "—"

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeonBar(10.dp)
            Spacer(Modifier.width(5.dp))
            Text("YOUR STATS", style = AppTypography.caption, color = AppColors.neon, letterSpacing = AppTypography.trackingWide)
            Spacer(Modifier.weight(1f))
            CyberBadge(text = rank.title, color = rank.color)
        }

        Spacer(Modifier.height(AppSpacing.sm))

        val fraction = run {
            val range  = (rank.nextMilestone - rank.milestone).coerceAtLeast(1)
            val earned = (totalReads - rank.milestone).coerceIn(0, range)
            earned.toFloat() / range.toFloat()
        }
        val animatedFraction by animateFloatAsState(fraction, tween(1000), label = "xp")

        Column {
            Row(Modifier.fillMaxWidth()) {
                Text("$totalReads / ${rank.nextMilestone} reads to next rank", style = AppTypography.mono10, color = AppColors.textMuted)
                Spacer(Modifier.weight(1f))
                Text("${(fraction * 100).toInt()}%", style = AppTypography.mono10, color = rank.color, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(5.dp)
                    .background(AppColors.divider, RoundedCornerShape(AppRadius.pill))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(animatedFraction).height(5.dp)
                        .background(
                            Brush.horizontalGradient(listOf(rank.color.copy(alpha = 0.7f), rank.color)),
                            RoundedCornerShape(AppRadius.pill),
                        )
                )
            }
        }

        Spacer(Modifier.height(AppSpacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth()
                .background(rank.color.copy(alpha = 0.06f), RoundedCornerShape(AppRadius.md))
                .border(1.dp, rank.color.copy(alpha = 0.25f), RoundedCornerShape(AppRadius.md))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(rank.emoji, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("RANK: ${rank.title.uppercase()}", style = AppTypography.mono11, color = rank.color, fontWeight = FontWeight.Black, letterSpacing = AppTypography.trackingWide)
                Text(rank.description, style = AppTypography.body, color = AppColors.textSecondary)
            }
        }

        Spacer(Modifier.height(AppSpacing.md))

        data class StatEntry(val value: String, val label: String, val color: Color, val icon: ImageVector)
        val stats = listOf(
            StatEntry("$totalReads",             "ARTICLES READ", AppColors.neon,             Icons.Default.MenuBook),
            StatEntry("$totalSkips",             "SKIPPED",       AppColors.textSecondary,    Icons.Default.FastForward),
            StatEntry(formatTime(avgReadTime),   "AVG READ TIME", Color(0xFF66CCFF),          Icons.Default.Timer),
            StatEntry(formatTime(avgSessionTime),"AVG SESSION",   Color(0xFF9966FF),          Icons.Default.AccessTime),
            StatEntry("$totalSessions",          "SESSIONS",      Color(0xFFFFB86C),          Icons.Default.LayersClear),
            StatEntry(readRatio,                 "READ RATIO",    AppColors.magenta,          Icons.Default.BarChart),
        )

        stats.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { entry ->
                    StatCard(value = entry.value, label = entry.label, color = entry.color, icon = entry.icon, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.06f), RoundedCornerShape(AppRadius.md))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(AppRadius.md))
            .padding(14.dp),
    ) {
        // Icon top-left + glow dot top-right — matches iOS StatCard
        Row(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = color,
                modifier           = Modifier.size(12.dp),
            )
            Spacer(Modifier.weight(1f))
            // Glow dot
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(color.copy(alpha = 0.6f), CircleShape)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text       = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            fontSize   = 24.sp,
            color      = AppColors.textPrimary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text          = label,
            style         = AppTypography.mono9,
            color         = AppColors.textMuted,
            letterSpacing = AppTypography.trackingWide,
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.md, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NeonBar(7.dp)
        Spacer(Modifier.width(6.dp))
        Text(title, style = AppTypography.mono9, color = AppColors.neon, letterSpacing = AppTypography.trackingWide)
    }
}

@Composable
private fun SettingsRow(
    icon:        ImageVector,
    label:       String,
    destructive: Boolean = false,
    onClick:     () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .background(AppColors.surfaceHigh, RoundedCornerShape(AppRadius.md))
            .border(1.dp, AppColors.divider, RoundedCornerShape(AppRadius.md))
            .clip(RoundedCornerShape(AppRadius.md))
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (destructive) AppColors.magenta else AppColors.neon.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(AppSpacing.md))
        Text(label, style = AppTypography.body, color = if (destructive) AppColors.magenta else AppColors.textPrimary)
        Spacer(Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = AppColors.textMuted, modifier = Modifier.size(12.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Data + helpers
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

data class ReaderRank(
    val title:         String,
    val emoji:         String,
    val description:   String,
    val color:         Color,
    val milestone:     Int,
    val nextMilestone: Int,
)

fun readerRank(reads: Int): ReaderRank = when {
    reads < 100   -> ReaderRank("Rookie",       "🌱", "Just getting started. Keep swiping!",               AppColors.textSecondary, 0,      100)
    reads < 500   -> ReaderRank("Curious",      "👀", "You're picking up momentum.",                       Color(0xFF66CCFF),       100,    500)
    reads < 2000  -> ReaderRank("Informed",     "📰", "A regular on the feed. Respect.",                   AppColors.neon,          500,    2000)
    reads < 7500  -> ReaderRank("Tech Insider", "⚡", "You know things before others do.",                 Color(0xFF9966FF),       2000,   7500)
    reads < 12000 -> ReaderRank("Signal Pro",   "🔥", "Cutting through noise like a pro.",                 Color(0xFFFF8019),       7500,   12000)
    reads < 20000 -> ReaderRank("Cyber Oracle", "🔮", "You see the future in the feed.",                   AppColors.magenta,       12000,  20000)
    else          -> ReaderRank("Legend",       "👑", "Nothing escapes your attention. You are the news.", Color(0xFFFFCC33),       20000,  99999)
}

fun formatTime(s: Int): String = when {
    s == 0   -> "—"
    s < 60   -> "${s}s"
    s < 3600 -> "${s / 60}m"
    else     -> "${s / 3600}h${(s % 3600) / 60}m"
}