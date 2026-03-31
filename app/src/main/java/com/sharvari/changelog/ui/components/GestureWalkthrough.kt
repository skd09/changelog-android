package com.sharvari.changelog.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography

/**
 * Full-screen overlay that teaches the 4 swipe gestures on the card stack.
 * Shows animated directional hints with pulsing arrows.
 * Displayed only once on first launch after onboarding.
 */
@Composable
fun GestureWalkthrough(
    visible: Boolean = true,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "gestureHints")

        // Pulsing offset for arrows (0 to 8dp and back)
        val pulseOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseOffset",
        )

        // Alpha pulse for the center icon
        val centerAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "centerAlpha",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                Spacer(Modifier.weight(1f))

                // ── Gesture hint area ────────────────────────────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(280.dp, 180.dp)
                        .border(
                            width = 1.dp,
                            color = AppColors.neon.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(AppRadius.lg),
                        ),
                ) {
                    // LEFT — SKIP (magenta)
                    GestureHintHorizontal(
                        icon = Icons.Default.ArrowBack,
                        label = "NEXT",
                        color = AppColors.magenta,
                        offsetX = -pulseOffset,
                        modifier = Modifier.align(Alignment.CenterStart)
                            .padding(start = 12.dp),
                    )

                    // RIGHT — READ (neon/purple)
                    GestureHintHorizontal(
                        icon = Icons.Default.ArrowForward,
                        label = "READ",
                        color = AppColors.neon,
                        offsetX = pulseOffset,
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .padding(end = 12.dp),
                        labelFirst = true,
                    )

                    // CENTER — Swipe icon
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "Swipe gesture",
                        tint = AppColors.textSecondary.copy(alpha = centerAlpha),
                        modifier = Modifier.size(48.dp),
                    )
                }

                Spacer(Modifier.height(AppSpacing.xl))

                // ── Subtitle ─────────────────────────────────────────────────
                Text(
                    text = "Swipe left to skip, right to read full article",
                    style = AppTypography.mono12,
                    color = AppColors.textSecondary,
                    letterSpacing = AppTypography.trackingWide,
                )

                Spacer(Modifier.height(AppSpacing.lg))

                // ── GOT IT button ────────────────────────────────────────────
                CyberButton(
                    label = "GOT IT",
                    style = CyberButtonStyle.Primary,
                    fullWidth = true,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(0.5f),
                )

                Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ── Vertical gesture hint (UP / DOWN) ────────────────────────────────────────
@Composable
private fun GestureHintVertical(
    icon: ImageVector,
    secondaryIcon: ImageVector,
    label: String,
    color: Color,
    offsetY: Float,
    modifier: Modifier = Modifier,
    arrowOnTop: Boolean = true,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.offset { IntOffset(0, (offsetY * density).toInt()) },
    ) {
        if (arrowOnTop) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = secondaryIcon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = AppTypography.mono10,
                color = color,
                fontWeight = FontWeight.Bold,
                letterSpacing = AppTypography.trackingWide,
            )
        } else {
            Text(
                text = label,
                style = AppTypography.mono10,
                color = color,
                fontWeight = FontWeight.Bold,
                letterSpacing = AppTypography.trackingWide,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = secondaryIcon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ── Horizontal gesture hint (LEFT / RIGHT) ───────────────────────────────────
@Composable
private fun GestureHintHorizontal(
    icon: ImageVector,
    label: String,
    color: Color,
    offsetX: Float,
    modifier: Modifier = Modifier,
    labelFirst: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.offset { IntOffset((offsetX * density).toInt(), 0) },
    ) {
        if (labelFirst) {
            Text(
                text = label,
                style = AppTypography.mono10,
                color = color,
                fontWeight = FontWeight.Bold,
                letterSpacing = AppTypography.trackingWide,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = AppTypography.mono10,
                color = color,
                fontWeight = FontWeight.Bold,
                letterSpacing = AppTypography.trackingWide,
            )
        }
    }
}
