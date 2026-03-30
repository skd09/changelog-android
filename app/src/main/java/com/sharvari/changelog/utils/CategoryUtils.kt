package com.sharvari.changelog.utils

import androidx.compose.material.icons.Icons
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
    else          -> Icons.Default.Article
}

fun categoryAccentColor(slug: String?): Color = when (slug) {
    "ai", "artificial-intelligence" -> Color(0xFF00FF9F)
    "crypto", "blockchain"          -> Color(0xFFFFCC00)
    "cybersecurity", "security"     -> Color(0xFFFF3348)
    "gaming"                        -> Color(0xFF9933FF)
    "science"                       -> Color(0xFF00CCFF)
    "startups", "business"          -> Color(0xFFFF8000)
    "health", "biotech"             -> Color(0xFF33E566)
    "space"                         -> Color(0xFF6666FF)
    "social-media"                  -> Color(0xFFFF4DCC)
    "hardware", "gadgets"           -> Color(0xFFCCCCCC)
    "technology"                    -> Color(0xFF00FF9F)
    "open-source"                   -> Color(0xFF00CCFF)
    else                            -> Color(0xFF00FF9F)
}

fun timeAgo(isoDate: String): String {
    return try {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val date = fmt.parse(isoDate) ?: return ""
        val diff = (System.currentTimeMillis() - date.time) / 1000
        when {
            diff < 60     -> "${diff}s ago"
            diff < 3600   -> "${diff / 60}m ago"
            diff < 86400  -> "${diff / 3600}h ago"
            else          -> "${diff / 86400}d ago"
        }
    } catch (_: Exception) { "" }
}
