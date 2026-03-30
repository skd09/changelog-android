package com.sharvari.changelog.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun categoryIcon(slug: String?): ImageVector = when (slug) {
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

fun categoryAccentColor(slug: String?): Color =
    com.sharvari.changelog.ui.theme.AppColors.categoryAccent(slug)

fun timeAgo(isoDate: String): String {
    return try {
        // Try with fractional seconds first, then without
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
        )
        var date: java.util.Date? = null
        for (pattern in formats) {
            try {
                val fmt = java.text.SimpleDateFormat(pattern, java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                date = fmt.parse(isoDate)
                if (date != null) break
            } catch (_: Exception) { }
        }
        if (date == null) return ""
        val diff = (System.currentTimeMillis() - date.time) / 1000
        when {
            diff < 60     -> "${diff}s ago"
            diff < 3600   -> "${diff / 60}m ago"
            diff < 86400  -> "${diff / 3600}h ago"
            else          -> "${diff / 86400}d ago"
        }
    } catch (_: Exception) { "" }
}
