package com.sharvari.changelog.ui.screen.settings

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sharvari.changelog.data.other.APIService
import com.sharvari.changelog.model.article.ALL_CATEGORIES
import com.sharvari.changelog.ui.components.CyberBackground
import com.sharvari.changelog.ui.components.CyberButton
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Notification frequency — mirrors iOS NotificationFrequency enum
// ─────────────────────────────────────────────────────────────────────────────

enum class NotificationFrequency(
    val value:       String,
    val label:       String,
    val description: String,
    val icon:        ImageVector,
) {
    INSTANT("instant", "Instant",      "Get notified when top stories break",  Icons.Default.Bolt),
    DAILY("daily",     "Daily Digest", "One summary at your preferred time",    Icons.Default.CalendarMonth),
    OFF("off",         "Off",          "No push notifications",                 Icons.Default.NotificationsOff),
}

// ─────────────────────────────────────────────────────────────────────────────
// NotificationPreferencesScreen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs   = remember { context.getSharedPreferences("changelog", android.content.Context.MODE_PRIVATE) }
    val scope   = rememberCoroutineScope()

    // ── Load stored prefs ─────────────────────────────────────────────────────
    val storedFrequency   = prefs.getString("notif_frequency", "daily") ?: "daily"
    val storedTime        = prefs.getString("notif_time", "") ?: ""
    val storedCategories  = prefs.getString("notif_categories", "") ?: ""

    // Parse stored UTC time → local hour/minute for display
    // Default to 8:00 AM local if no stored preference yet
    val (initLocalHour, initLocalMinute) = remember {
        if (storedTime.isEmpty()) Pair(8, 0)
        else utcStringToLocalHourMinute(storedTime)
    }

    var frequency          by remember { mutableStateOf(NotificationFrequency.entries.find { it.value == storedFrequency } ?: NotificationFrequency.DAILY) }
    var localHour          by remember { mutableIntStateOf(initLocalHour) }
    var localMinute        by remember { mutableIntStateOf(initLocalMinute) }
    var selectedCategories by remember {
        mutableStateOf<Set<String>>(
            if (storedCategories.isEmpty()) emptySet()
            else storedCategories.split(",").toSet()
        )
    }

    var isSaving     by remember { mutableStateOf(false) }
    var showSuccess  by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows  = false,
        ),
    ) {
        CyberBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("NOTIFICATIONS", style = AppTypography.label, color = AppColors.neon, letterSpacing = AppTypography.trackingXWide)
                            }
                        },
                        navigationIcon = {
                            TextButton(onClick = onDismiss) {
                                Text("DONE", style = AppTypography.caption, color = AppColors.neon, letterSpacing = AppTypography.trackingWide)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AppSpacing.md)
                        .padding(bottom = AppSpacing.xxl),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {

                    // ── Frequency section ─────────────────────────────────────
                    SectionHeader("FREQUENCY")

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.surfaceHigh, RoundedCornerShape(AppRadius.md))
                            .border(1.dp, AppColors.divider, RoundedCornerShape(AppRadius.md))
                            .clip(RoundedCornerShape(AppRadius.md)),
                    ) {
                        NotificationFrequency.entries.forEachIndexed { i, option ->
                            FrequencyRow(
                                option     = option,
                                isSelected = frequency == option,
                                onClick    = { frequency = option },
                            )
                            if (i < NotificationFrequency.entries.size - 1) {
                                HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp)
                            }
                        }
                    }

                    // ── Digest time (daily only) ──────────────────────────────
                    AnimatedVisibility(
                        visible = frequency == NotificationFrequency.DAILY,
                        enter   = expandVertically(),
                        exit    = shrinkVertically(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                            SectionHeader("DIGEST TIME")

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppColors.surfaceHigh, RoundedCornerShape(AppRadius.md))
                                    .border(1.dp, AppColors.divider, RoundedCornerShape(AppRadius.md))
                                    .clip(RoundedCornerShape(AppRadius.md)),
                            ) {
                                // Time picker row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            TimePickerDialog(
                                                context,
                                                { _, h, m -> localHour = h; localMinute = m },
                                                localHour, localMinute, false
                                            ).show()
                                        }
                                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, null, tint = AppColors.neon.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(AppSpacing.sm))
                                        Text("Send at", style = AppTypography.body, color = AppColors.textPrimary)
                                    }
                                    Text(
                                        text          = formatLocalTime(localHour, localMinute),
                                        fontFamily    = FontFamily.Monospace,
                                        fontWeight    = FontWeight.Bold,
                                        fontSize      = 15.sp,
                                        color         = AppColors.neon,
                                    )
                                }

                                HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp)

                                Text(
                                    text     = "Times are in your local timezone and converted to UTC on save.",
                                    style    = AppTypography.caption,
                                    color    = AppColors.textSecondary,
                                    modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
                                )
                            }
                        }
                    }

                    // ── Categories section (not shown when off) ───────────────
                    AnimatedVisibility(visible = frequency != NotificationFrequency.OFF) {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                            SectionHeader("CATEGORIES")

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AppColors.surfaceHigh, RoundedCornerShape(AppRadius.md))
                                    .border(1.dp, AppColors.divider, RoundedCornerShape(AppRadius.md))
                                    .clip(RoundedCornerShape(AppRadius.md)),
                            ) {
                                ALL_CATEGORIES.forEachIndexed { i, cat ->
                                    val isSelected = cat.id in selectedCategories
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCategories = if (isSelected)
                                                    selectedCategories - cat.id
                                                else
                                                    selectedCategories + cat.id
                                            }
                                            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                                    ) {
                                        Icon(
                                            categoryIconFor(cat.slug),
                                            null,
                                            tint     = if (isSelected) AppColors.neon else AppColors.textSecondary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            cat.name,
                                            style      = AppTypography.body,
                                            color      = if (isSelected) AppColors.textPrimary else AppColors.textSecondary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            modifier   = Modifier.weight(1f),
                                        )
                                        Icon(
                                            if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            null,
                                            tint     = if (isSelected) AppColors.neon else AppColors.textSecondary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    if (i < ALL_CATEGORIES.size - 1) {
                                        HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }

                    // ── Save button ───────────────────────────────────────────
                    CyberButton(
                        label    = if (isSaving) "SAVING..." else "SAVE PREFERENCES",
                        enabled  = !isSaving,
                        onClick  = {
                            isSaving     = true
                            errorMessage = null
                            showSuccess  = false

                            // Convert local time → UTC HH:mm
                            val utcTime    = localHourMinuteToUtcString(localHour, localMinute)
                            val categories: List<String> = selectedCategories.toList()

                            scope.launch(Dispatchers.IO) {
                                try {
                                    // Resolve slug -> UUID mapping from API
                                    val categoryIds = try {
                                        val response = APIService.shared.fetchCategories()
                                        val slugToId = response.data.associate { it.slug to it.id }
                                        selectedCategories.mapNotNull { slugToId[it] }.ifEmpty { null }
                                    } catch (_: Exception) { null }
                                    APIService.shared.updateNotificationPreferences(
                                        frequency   = frequency.value,
                                        time        = utcTime,
                                        categoryIds = categoryIds,
                                    )
                                    // Persist locally
                                    prefs.edit()
                                        .putString("notif_frequency", frequency.value)
                                        .putString("notif_time", utcTime)
                                        .putString("notif_categories", (selectedCategories).joinToString(","))
                                        .apply()

                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        isSaving    = false
                                        showSuccess = true
                                    }
                                    delay(2000)
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        showSuccess = false
                                    }
                                } catch (e: Exception) {
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        isSaving     = false
                                        errorMessage = "Failed to save. Please try again."
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    errorMessage?.let {
                        Text(it, style = AppTypography.caption, color = AppColors.magenta)
                    }

                    if (showSuccess) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Preferences saved", style = AppTypography.caption, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Subcomponents
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text          = title,
        fontFamily    = FontFamily.Monospace,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 11.sp,
        letterSpacing = 1.sp,
        color         = AppColors.textSecondary,
        modifier      = Modifier.padding(horizontal = AppSpacing.xs, vertical = 4.dp),
    )
}

@Composable
private fun FrequencyRow(option: NotificationFrequency, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Icon(
            option.icon,
            null,
            tint     = if (isSelected) AppColors.neon else AppColors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(option.label, style = AppTypography.body, color = AppColors.textPrimary, fontWeight = FontWeight.Medium)
            Text(option.description, style = AppTypography.caption, color = AppColors.textSecondary)
        }
        if (isSelected) {
            Icon(Icons.Default.CheckCircle, null, tint = AppColors.neon, modifier = Modifier.size(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UTC ↔ Local time helpers — mirrors iOS localToUTCTime / loadState logic
// ─────────────────────────────────────────────────────────────────────────────

private fun utcStringToLocalHourMinute(utcTime: String): Pair<Int, Int> {
    return try {
        val parts = utcTime.split(":").map { it.toInt() }
        val utcHour = parts[0]; val utcMin = parts[1]
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(Calendar.HOUR_OF_DAY, utcHour)
        cal.set(Calendar.MINUTE, utcMin)
        val local = Calendar.getInstance()
        local.timeInMillis = cal.timeInMillis
        Pair(local.get(Calendar.HOUR_OF_DAY), local.get(Calendar.MINUTE))
    } catch (e: Exception) {
        Pair(8, 0) // fallback to 8:00 AM
    }
}

private fun localHourMinuteToUtcString(hour: Int, minute: Int): String {
    val local = Calendar.getInstance()
    local.set(Calendar.HOUR_OF_DAY, hour)
    local.set(Calendar.MINUTE, minute)
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.timeInMillis = local.timeInMillis
    return String.format("%02d:%02d", utc.get(Calendar.HOUR_OF_DAY), utc.get(Calendar.MINUTE))
}

private fun formatLocalTime(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, hour)
    cal.set(Calendar.MINUTE, minute)
    return String.format(
        "%d:%02d %s",
        if (hour % 12 == 0) 12 else hour % 12,
        minute,
        if (hour < 12) "AM" else "PM",
    )
}

private fun categoryIconFor(slug: String): ImageVector = when (slug) {
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