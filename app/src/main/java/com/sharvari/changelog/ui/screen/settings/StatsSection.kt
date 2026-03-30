package com.sharvari.changelog.ui.screen.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharvari.changelog.store.stats.StatsStore
import com.sharvari.changelog.ui.components.CyberBadge
import com.sharvari.changelog.ui.components.NeonBar
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography

// ─────────────────────────────────────────────────────────────────────────────
// Reader Rank
// ─────────────────────────────────────────────────────────────────────────────

data class ReaderRank(
    val title:         String,
    val emoji:         String,
    val description:   String,
    val color:         Color,
    val milestone:     Int,
    val nextMilestone: Int,
)

fun readerRank(reads: Int): ReaderRank = when {
    reads < 100   -> ReaderRank("Rookie",       "🌱", "Just getting started. Keep swiping!",               AppColors.textSecondary, 0,     100)
    reads < 500   -> ReaderRank("Curious",      "👀", "You're picking up momentum.",                       Color(0xFF66CCFF),       100,   500)
    reads < 2000  -> ReaderRank("Informed",     "📰", "A regular on the feed. Respect.",                   AppColors.neon,          500,   2000)
    reads < 7500  -> ReaderRank("Tech Insider", "⚡", "You know things before others do.",                 Color(0xFF9966FF),       2000,  7500)
    reads < 12000 -> ReaderRank("Signal Pro",   "🔥", "Cutting through noise like a pro.",                 Color(0xFFFF8019),       7500,  12000)
    reads < 20000 -> ReaderRank("Cyber Oracle", "🔮", "You see the future in the feed.",                   AppColors.magenta,       12000, 20000)
    else          -> ReaderRank("Legend",       "👑", "Nothing escapes your attention. You are the news.", Color(0xFFFFCC33),       20000, 99999)
}

fun formatTime(s: Int): String = when {
    s == 0   -> "—"
    s < 60   -> "${s}s"
    s < 3600 -> "${s / 60}m"
    else     -> "${s / 3600}h${(s % 3600) / 60}m"
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatsSection() {
    val totalReads     by StatsStore.totalReads.collectAsStateWithLifecycle()
    val totalSkips     by StatsStore.totalSkips.collectAsStateWithLifecycle()
    val totalSessions  by StatsStore.totalSessions.collectAsStateWithLifecycle()
    val avgReadTime    by StatsStore.avgReadTime.collectAsStateWithLifecycle()
    val avgSessionTime by StatsStore.avgSessionTime.collectAsStateWithLifecycle()

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
            Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(AppColors.divider, RoundedCornerShape(AppRadius.pill))) {
                Box(
                    modifier = Modifier.fillMaxWidth(animatedFraction).height(5.dp)
                        .background(Brush.horizontalGradient(listOf(rank.color.copy(alpha = 0.7f), rank.color)), RoundedCornerShape(AppRadius.pill))
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
            StatEntry("$totalReads",             "ARTICLES READ", AppColors.neon,          Icons.Default.MenuBook),
            StatEntry("$totalSkips",             "SKIPPED",       AppColors.textSecondary, Icons.Default.FastForward),
            StatEntry(formatTime(avgReadTime),   "AVG READ TIME", Color(0xFF66CCFF),       Icons.Default.Timer),
            StatEntry(formatTime(avgSessionTime),"AVG SESSION",   Color(0xFF9966FF),       Icons.Default.AccessTime),
            StatEntry("$totalSessions",          "SESSIONS",      Color(0xFFFFB86C),       Icons.Default.LayersClear),
            StatEntry(readRatio,                 "READ RATIO",    AppColors.magenta,       Icons.Default.BarChart),
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
        Row(modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.size(5.dp).background(color.copy(alpha = 0.6f), CircleShape))
        }
        Spacer(Modifier.height(8.dp))
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 24.sp, color = AppColors.textPrimary)
        Spacer(Modifier.height(2.dp))
        Text(label, style = AppTypography.mono9, color = AppColors.textMuted, letterSpacing = AppTypography.trackingWide)
    }
}
