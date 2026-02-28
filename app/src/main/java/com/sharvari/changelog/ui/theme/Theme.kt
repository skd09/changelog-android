package com.sharvari.changelog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Dracula palette — mirrors iOS Theme.swift exactly
// ─────────────────────────────────────────────────────────────────────────────

object AppColors {
    // Backgrounds
    val background  = Color(0xFF000000)
    val surface     = Color(0xFF0D0D0D)
    val surfaceHigh = Color(0xFF141414)
    val divider     = Color(0xFF1C1C1C)

    // Accents
    val neon         = Color(0xFFBD93F9)   // Dracula purple
    val magenta      = Color(0xFFFF79C6)   // Dracula pink
    val cyan         = Color(0xFF8BE9FD)   // Dracula cyan
    val green        = Color(0xFF50FA7B)   // Dracula green
    val orange       = Color(0xFFFFB86C)   // Dracula orange
    val red          = Color(0xFFFF5555)   // Dracula red

    // Text
    val textPrimary   = Color(0xFFF8F8F2)
    val textSecondary = Color(0xFF6272A4)  // comment color
    val textMuted     = Color(0xFF3A3F58)

    // Glows (for shadow tints)
    val glowNeon    = neon.copy(alpha = 0.45f)
    val glowMagenta = magenta.copy(alpha = 0.40f)
    val glowBlue    = cyan.copy(alpha = 0.35f)
    val glowGreen   = green.copy(alpha = 0.40f)

    // Category accent mapping — mirrors iOS categoryAccent
    fun categoryAccent(slug: String?): Color = when (slug) {
        "technology"  -> neon
        "science"     -> Color(0xFF66CCFF)
        "business"    -> Color(0xFFFFB347)
        "security"    -> magenta
        "ai"          -> Color(0xFF9966FF)
        "crypto"      -> Color(0xFFFF8019)
        "gaming"      -> Color(0xFF4DFF80)
        "health"      -> Color(0xFF33E6A0)
        "space"       -> Color(0xFF8050FF)
        "open-source" -> cyan
        else          -> neon
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Typography — monospaced for UI chrome, default for content
// ─────────────────────────────────────────────────────────────────────────────

object AppTypography {
    val display    = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black,  fontSize = 34.sp)
    val title      = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black,  fontSize = 22.sp)
    val label      = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,   fontSize = 13.sp)
    val caption    = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,   fontSize = 11.sp)
    val headline   = TextStyle(fontFamily = FontFamily.Default,   fontWeight = FontWeight.Bold,   fontSize = 19.sp)
    val body       = TextStyle(fontFamily = FontFamily.Default,   fontWeight = FontWeight.Normal, fontSize = 16.sp)
    val footnote   = TextStyle(fontFamily = FontFamily.Default,   fontWeight = FontWeight.Normal, fontSize = 13.sp)
    val mono10     = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 10.sp)
    val mono11     = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    val mono12     = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,   fontSize = 12.sp)
    val mono9      = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,   fontSize = 9.sp)

    val trackingWide  = 0.15.sp   // letter spacing in Compose is em-based
    val trackingXWide = 0.25.sp
    val trackingTight = 0.03.sp
}

// ─────────────────────────────────────────────────────────────────────────────
// Spacing & Radius
// ─────────────────────────────────────────────────────────────────────────────

object AppSpacing {
    val xs:  Dp = 4.dp
    val sm:  Dp = 8.dp
    val md:  Dp = 16.dp
    val lg:  Dp = 24.dp
    val xl:  Dp = 32.dp
    val xxl: Dp = 48.dp
}

object AppRadius {
    val sm:   Dp = 6.dp
    val md:   Dp = 12.dp
    val lg:   Dp = 18.dp
    val xl:   Dp = 24.dp
    val pill: Dp = 999.dp
}

// ─────────────────────────────────────────────────────────────────────────────
// MaterialTheme wrapper
// ─────────────────────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary          = AppColors.neon,
    secondary        = AppColors.magenta,
    tertiary         = AppColors.cyan,
    background       = AppColors.background,
    surface          = AppColors.surface,
    onPrimary        = AppColors.background,
    onSecondary      = AppColors.background,
    onBackground     = AppColors.textPrimary,
    onSurface        = AppColors.textPrimary,
    outline          = AppColors.divider,
    error            = AppColors.red,
)

@Composable
fun ChangelogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content     = content
    )
}
