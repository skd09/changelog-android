package com.sharvari.changelog.ui.activity.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Tab(
    val label:      String,
    val icon:       ImageVector,
    val activeIcon: ImageVector,
) {
    FEED("FEED",         Icons.Outlined.Newspaper,    Icons.Filled.Newspaper),
    DISCOVER("DISCOVER", Icons.Outlined.AutoAwesome,  Icons.Default.AutoAwesome),
    SETTINGS("SETTINGS", Icons.Outlined.Settings,     Icons.Filled.Settings),
}
