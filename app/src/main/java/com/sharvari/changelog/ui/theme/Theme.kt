package com.sharvari.changelog.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sharvari.changelog.store.theme.ThemeMode
import com.sharvari.changelog.store.theme.ThemeStore

// ─────────────────────────────────────────────────────────────────────────────
// Dracula palette — mirrors iOS Theme.swift exactly
// Now dynamic: delegates to isLight flag for light/dark variants
// ─────────────────────────────────────────────────────────────────────────────

object AppColors {
    var isLight: Boolean = false

    // ── Backgrounds ─────────────────────────────────────────────────────────
    val background  get() = if (isLight) Color(0xFFFFFFFF) else Color(0xFF000000)
    val surface     get() = if (isLight) Color(0xFFF5F5F7) else Color(0xFF0D0D0D)
    val surfaceHigh get() = if (isLight) Color(0xFFEEEEF0) else Color(0xFF141414)
    val divider     get() = if (isLight) Color(0xFFD8D8DC) else Color(0xFF1C1C1C)

    // ── Accents ─────────────────────────────────────────────────────────────
    //                    Light (deeper for white bg)       Dark (original Dracula)
    val neon    get() = if (isLight) Color(0xFF8B5CF6) else Color(0xFFBD93F9)   // purple
    val magenta get() = if (isLight) Color(0xFFE44D8A) else Color(0xFFFF79C6)   // pink
    val cyan    get() = if (isLight) Color(0xFF06B6D4) else Color(0xFF8BE9FD)   // cyan
    val green   get() = if (isLight) Color(0xFF22C55E) else Color(0xFF50FA7B)   // green
    val orange  get() = if (isLight) Color(0xFFF59E0B) else Color(0xFFFFB86C)   // orange
    val red     get() = if (isLight) Color(0xFFEF4444) else Color(0xFFFF5555)   // red

    // ── Text ────────────────────────────────────────────────────────────────
    //                    Light (dark text on white)        Dark (original Dracula)
    val textPrimary   get() = if (isLight) Color(0xFF1A1A1E) else Color(0xFFF8F8F2)
    val textSecondary get() = if (isLight) Color(0xFF555568) else Color(0xFF6272A4)  // Dracula comment
    val textMuted     get() = if (isLight) Color(0xFF84849C) else Color(0xFF44475A)  // Dracula current line

    // ── Glows ───────────────────────────────────────────────────────────────
    val glowNeon    get() = neon.copy(alpha = if (isLight) 0.15f else 0.45f)
    val glowMagenta get() = magenta.copy(alpha = if (isLight) 0.12f else 0.40f)
    val glowBlue    get() = cyan.copy(alpha = if (isLight) 0.10f else 0.35f)
    val glowGreen   get() = green.copy(alpha = if (isLight) 0.12f else 0.40f)

    // ── Category accents — matches iOS categoryColor(for:) ────────────────
    //                          Light (deeper for white bg)     Dark (matches iOS vibrant)
    fun categoryAccent(slug: String?): Color = when (slug) {
        "technology"  -> if (isLight) Color(0xFF00994F) else Color(0xFF00FF9E)   // neon green
        "ai"          -> if (isLight) Color(0xFF00994F) else Color(0xFF00FF9E)   // neon green (same as tech)
        "science"     -> if (isLight) Color(0xFF0891B2) else Color(0xFF00CCFF)   // cyan
        "business"    -> if (isLight) Color(0xFFD97706) else Color(0xFFFF8000)   // orange
        "security"    -> if (isLight) Color(0xFFCC1A26) else Color(0xFFFF334D)   // red
        "crypto"      -> if (isLight) Color(0xFFCC9900) else Color(0xFFFFCC00)   // gold
        "gaming"      -> if (isLight) Color(0xFF6D28D9) else Color(0xFF9933FF)   // purple
        "health"      -> if (isLight) Color(0xFF059669) else Color(0xFF33E666)   // mint
        "space"       -> if (isLight) Color(0xFF4338CA) else Color(0xFF6666FF)   // indigo
        "open-source" -> cyan
        else          -> if (isLight) Color(0xFF00994F) else Color(0xFF00FF9E)   // neon green fallback
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

@Composable
fun ChangelogTheme(content: @Composable () -> Unit) {
    val themeMode by ThemeStore.mode.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()

    val isLight = when (themeMode) {
        ThemeMode.LIGHT  -> true
        ThemeMode.DARK   -> false
        ThemeMode.SYSTEM -> !systemDark
    }

    AppColors.isLight = isLight

    val colorScheme = if (isLight) {
        lightColorScheme(
            primary      = AppColors.neon,
            secondary    = AppColors.magenta,
            tertiary     = AppColors.cyan,
            background   = AppColors.background,
            surface      = AppColors.surface,
            onPrimary    = Color.White,
            onSecondary  = Color.White,
            onBackground = AppColors.textPrimary,
            onSurface    = AppColors.textPrimary,
            outline      = AppColors.divider,
            error        = AppColors.red,
        )
    } else {
        darkColorScheme(
            primary      = AppColors.neon,
            secondary    = AppColors.magenta,
            tertiary     = AppColors.cyan,
            background   = AppColors.background,
            surface      = AppColors.surface,
            onPrimary    = AppColors.background,
            onSecondary  = AppColors.background,
            onBackground = AppColors.textPrimary,
            onSurface    = AppColors.textPrimary,
            outline      = AppColors.divider,
            error        = AppColors.red,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content     = content,
    )
}
