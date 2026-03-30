package com.sharvari.changelog.ui.screen.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sharvari.changelog.model.article.ALL_CATEGORIES
import com.sharvari.changelog.service.analytics.AnalyticsManager
import com.sharvari.changelog.store.category.CategoryStore
import com.sharvari.changelog.ui.components.CyberBackground
import com.sharvari.changelog.ui.components.CyberBadge
import com.sharvari.changelog.ui.components.CyberButton
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography
import com.sharvari.changelog.utils.categoryIcon

@Composable
fun ChannelPickerDialog(onDismiss: () -> Unit) {
    val selectedSlugs by CategoryStore.selectedSlugs.collectAsStateWithLifecycle()
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
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.neon)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("MANAGE CHANNELS", style = AppTypography.label, color = AppColors.neon, letterSpacing = AppTypography.trackingWide)
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

                LazyVerticalGrid(
                    columns               = GridCells.Fixed(2),
                    modifier              = Modifier.fillMaxSize().padding(horizontal = AppSpacing.md),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding        = PaddingValues(bottom = AppSpacing.xxl),
                ) {
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

                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(bottom = AppSpacing.sm),
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

                    items(ALL_CATEGORIES) { cat ->
                        val isSelected  = cat.slug in selectedSlugs
                        val accent      = AppColors.categoryAccent(cat.slug)
                        val bgColor     by animateColorAsState(
                            if (isSelected) accent.copy(alpha = 0.12f) else AppColors.surfaceHigh, label = "bg${cat.slug}")
                        val borderColor by animateColorAsState(
                            if (isSelected) accent.copy(alpha = 0.5f) else AppColors.divider, label = "border${cat.slug}")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(AppRadius.md))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(AppRadius.md))
                                .clickable {
                                    CategoryStore.toggle(cat.slug)
                                    AnalyticsManager.categoryChanged(cat.slug)
                                }
                                .padding(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .align(Alignment.TopStart)
                                    .background(accent.copy(alpha = 0.15f), CircleShape)
                                    .border(1.dp, accent.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(categoryIcon(cat.slug), null, tint = accent, modifier = Modifier.size(20.dp))
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint     = accent,
                                    modifier = Modifier.size(22.dp).align(Alignment.TopEnd),
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

                    item(span = { GridItemSpan(2) }) {
                        CyberButton(
                            label    = "DONE",
                            onClick  = onDismiss,
                            modifier = Modifier.fillMaxWidth().padding(top = AppSpacing.sm),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CyberSettingsGrid(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.alpha(0.03f)) {
        val step = 40.dp.toPx()
        val neon = Color(0xFFBD93F9)
        var x = 0f
        while (x <= size.width) { drawLine(neon, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.5f); x += step }
        var y = 0f
        while (y <= size.height) { drawLine(neon, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5f); y += step }
    }
}
