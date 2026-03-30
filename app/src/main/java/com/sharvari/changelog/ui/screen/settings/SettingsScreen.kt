package com.sharvari.changelog.ui.screen.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharvari.changelog.service.analytics.AnalyticsManager
import com.sharvari.changelog.store.article.ReadArticlesStore
import com.sharvari.changelog.utils.RatingManager
import com.sharvari.changelog.ui.components.CyberBackground
import com.sharvari.changelog.ui.components.NeonBar
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onDismiss: () -> Unit) {
    var showFeedback          by remember { mutableStateOf(false) }
    var showClearHistory      by remember { mutableStateOf(false) }
    var showChannels          by remember { mutableStateOf(false) }
    var showNotificationPrefs by remember { mutableStateOf(false) }
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
                    .navigationBarsPadding()
                    .padding(horizontal = AppSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                item { StatsSection() }
                item { Spacer(Modifier.height(AppSpacing.sm)) }

                item { SectionHeader("CHANNELS") }
                item { SettingsRow(icon = Icons.Default.Settings, label = "Manage channels") { showChannels = true } }

                item { SectionHeader("NOTIFICATIONS") }
                item { SettingsRow(icon = Icons.Default.Notifications, label = "Notification preferences") { showNotificationPrefs = true } }

                item { SectionHeader("SPREAD THE WORD") }
                item {
                    SettingsRow(icon = Icons.Default.Share, label = "Share with a friend") {
                        AnalyticsManager.appShared()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=com.sharvari.changelog")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share The Changelog"))
                    }
                }
                item {
                    SettingsRow(icon = Icons.Default.Star, label = "Rate on Play Store") {
                        AnalyticsManager.appRated()
                        RatingManager.markAsRated()
                        try {
                            uriHandler.openUri("market://details?id=com.sharvari.changelog")
                        } catch (_: ActivityNotFoundException) {
                            uriHandler.openUri("https://play.google.com/store/apps/details?id=com.sharvari.changelog")
                        }
                    }
                }

                item { SectionHeader("FEEDBACK") }
                item { SettingsRow(icon = Icons.Default.Mail, label = "Send feedback") { showFeedback = true } }
                item {
                    SettingsRow(icon = Icons.Default.Mail, label = "Contact us") {
                        uriHandler.openUri("mailto:sharvarid.dev@gmail.com?subject=Feedback")
                    }
                }

                item { SectionHeader("READING") }
                item {
                    SettingsRow(icon = Icons.Default.Refresh, label = "Clear read history", destructive = true) {
                        showClearHistory = true
                    }
                }

                item { SectionHeader("APP") }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.surfaceHigh, RoundedCornerShape(AppRadius.md))
                            .border(1.dp, AppColors.divider, RoundedCornerShape(AppRadius.md))
                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Info, null, tint = AppColors.neon.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(AppSpacing.md))
                        Text("Version", style = AppTypography.body, color = AppColors.textPrimary)
                        Spacer(Modifier.weight(1f))
                        Text(appVersion, style = AppTypography.mono11, color = AppColors.textMuted, letterSpacing = 0.05.sp)
                    }
                }

                item { SectionHeader("LEGAL") }
                item { SettingsRow(icon = Icons.Default.Lock, label = "Privacy Policy") { uriHandler.openUri("https://thechangelog.app/#privacy") } }
                item { SettingsRow(icon = Icons.Default.Info, label = "Terms of Service") { uriHandler.openUri("https://thechangelog.app/#terms") } }

                item { Spacer(Modifier.height(AppSpacing.xxl)) }
            }
        }
    }

    if (showFeedback)          FeedbackSheet(onDismiss = { showFeedback = false })
    if (showChannels)          ChannelPickerDialog(onDismiss = { showChannels = false })
    if (showNotificationPrefs) NotificationPreferencesScreen(onDismiss = { showNotificationPrefs = false })

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
// Shared settings components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
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
private fun SettingsRow(icon: ImageVector, label: String, destructive: Boolean = false, onClick: () -> Unit) {
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
