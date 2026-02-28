package com.sharvari.changelog.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography

// ─────────────────────────────────────────────────────────────────────────────
// CyberButton — matches iOS CyberButton with all 4 styles + press animation
// ─────────────────────────────────────────────────────────────────────────────

enum class CyberButtonStyle {
    Primary,     // Solid neon fill — main CTA  (matches iOS .primary)
    Secondary,   // Dark surface + neon border   (matches iOS .secondary)
    Ghost,       // Transparent + dim border     (matches iOS .ghost)
    Destructive, // Magenta fill                 (matches iOS .destructive)
}

@Composable
fun CyberButton(
    label:     String,
    modifier:  Modifier = Modifier,
    icon:      ImageVector? = null,
    style:     CyberButtonStyle = CyberButtonStyle.Primary,
    fullWidth: Boolean = true,
    enabled:   Boolean = true,
    onClick:   () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Scale 0.97 on press — matches iOS scaleEffect(isPressed ? 0.97 : 1.0)
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1.0f,
        animationSpec = tween(if (isPressed) 100 else 150),
        label         = "scale",
    )

    // Glow radius shrinks on press — matches iOS shadow(radius: isPressed ? 4 : 10)
    val glowRadius by animateFloatAsState(
        targetValue   = if (isPressed) 4f else 10f,
        animationSpec = tween(100),
        label         = "glow",
    )

    val containerColor = when (style) {
        CyberButtonStyle.Primary     -> Color.Transparent   // gradient handled in Box
        CyberButtonStyle.Secondary   -> AppColors.surface
        CyberButtonStyle.Ghost       -> Color.Transparent
        CyberButtonStyle.Destructive -> AppColors.magenta
    }

    val labelColor = when (style) {
        CyberButtonStyle.Primary     -> AppColors.background
        CyberButtonStyle.Secondary   -> AppColors.neon
        CyberButtonStyle.Ghost       -> AppColors.textSecondary
        CyberButtonStyle.Destructive -> Color.White
    }

    val borderColor = when (style) {
        CyberButtonStyle.Primary     -> AppColors.neon.copy(alpha = 0.5f)
        CyberButtonStyle.Secondary   -> AppColors.neon.copy(alpha = 0.35f)
        CyberButtonStyle.Ghost       -> AppColors.divider
        CyberButtonStyle.Destructive -> AppColors.magenta.copy(alpha = 0.5f)
    }

    val glowColor = when (style) {
        CyberButtonStyle.Primary     -> AppColors.glowNeon
        CyberButtonStyle.Secondary   -> AppColors.neon.copy(alpha = 0.1f)
        CyberButtonStyle.Ghost       -> Color.Transparent
        CyberButtonStyle.Destructive -> AppColors.glowMagenta
    }

    Button(
        onClick           = onClick,
        enabled           = enabled,
        interactionSource = interactionSource,
        modifier          = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .height(52.dp)
            .scale(scale)
            .shadow(
                elevation       = glowRadius.dp,
                shape           = RoundedCornerShape(AppRadius.sm),
                ambientColor    = glowColor,
                spotColor       = glowColor,
            ),
        shape          = RoundedCornerShape(AppRadius.sm),
        colors         = ButtonDefaults.buttonColors(
            containerColor         = containerColor,
            disabledContainerColor = AppColors.divider,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        // Primary uses solid neon fill (not gradient) — matches iOS .fill(Theme.Colors.neon)
        Box(
            modifier = Modifier
                .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
                .height(52.dp)
                .then(
                    if (style == CyberButtonStyle.Primary && enabled)
                        Modifier.background(AppColors.neon, RoundedCornerShape(AppRadius.sm))
                    else
                        Modifier
                )
                .border(1.dp, borderColor, RoundedCornerShape(AppRadius.sm)),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text          = label,
                    style         = AppTypography.label,
                    letterSpacing = AppTypography.trackingWide,
                    color         = if (enabled) labelColor else AppColors.textMuted,
                    fontWeight    = FontWeight.Black,
                )
                if (icon != null) {
                    Spacer(Modifier.width(AppSpacing.sm))
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = if (enabled) labelColor else AppColors.textMuted,
                        modifier           = Modifier.size(11.dp),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CyberIconButton — matches iOS CyberIconButton (circular icon button)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CyberIconButton(
    icon:     ImageVector,
    isActive: Boolean = false,
    tint:     Color   = AppColors.neon,
    onClick:  () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.92f else 1f,
        animationSpec = tween(100),
        label         = "iconScale",
    )

    Box(
        modifier         = Modifier
            .size(38.dp)
            .scale(scale)
            .background(
                color = if (isActive) tint else tint.copy(alpha = 0.1f),
                shape = CircleShape,
            )
            .border(1.dp, tint.copy(alpha = 0.3f), CircleShape)
            .shadow(
                elevation    = if (isActive) 8.dp else 0.dp,
                shape        = CircleShape,
                ambientColor = tint.copy(alpha = 0.4f),
                spotColor    = tint.copy(alpha = 0.4f),
            )
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = if (isActive) AppColors.background else tint,
            modifier           = Modifier.size(15.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CyberBadge — unchanged, already matches iOS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CyberBadge(text: String, color: Color = AppColors.neon) {
    Text(
        text          = text,
        style         = AppTypography.mono9,
        color         = color,
        letterSpacing = AppTypography.trackingWide,
        modifier      = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CyberChip — category filter pill
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CyberChip(
    text:     String,
    selected: Boolean,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg     = if (selected) AppColors.neon.copy(alpha = 0.15f) else Color.Transparent
    val border = if (selected) AppColors.neon.copy(alpha = 0.5f)  else AppColors.divider
    val color  = if (selected) AppColors.neon                     else AppColors.textSecondary

    Text(
        text          = text.uppercase(),
        style         = AppTypography.mono9,
        color         = color,
        letterSpacing = AppTypography.trackingWide,
        modifier      = modifier
            .background(bg, RoundedCornerShape(AppRadius.pill))
            .border(1.dp, border, RoundedCornerShape(AppRadius.pill))
            .clip(RoundedCornerShape(AppRadius.pill))
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CyberDivider
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CyberDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppColors.neon.copy(alpha = 0.2f))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CyberLoader — spinning arc, matches iOS style
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CyberLoader(size: Dp = 48.dp, strokeWidth: Dp = 2.5.dp) {
    val transition = rememberInfiniteTransition(label = "loader")
    val angle by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label         = "rotation",
    )
    val neon = AppColors.neon
    Box(
        modifier = Modifier
            .size(size)
            .rotate(angle)
            .drawBehind {
                drawArc(
                    color      = neon,
                    startAngle = 0f,
                    sweepAngle = 234f,
                    useCenter  = false,
                    style      = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round),
                )
            }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// NeonBar — vertical accent bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NeonBar(height: Dp = 10.dp) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(height)
            .background(AppColors.neon)
    )
}