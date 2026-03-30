package com.sharvari.changelog.ui.screen.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sharvari.changelog.data.other.APIService
import com.sharvari.changelog.service.analytics.AnalyticsManager
import com.sharvari.changelog.ui.components.CyberBackground
import com.sharvari.changelog.ui.components.CyberButton
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography
import kotlinx.coroutines.launch

enum class FeedbackType(val label: String, val icon: ImageVector) {
    SUGGESTION("Suggestion", Icons.Default.Lightbulb),
    BUG("Bug Report",        Icons.Default.BugReport),
    OTHER("Other",           Icons.Default.ChatBubble),
}

@Composable
fun FeedbackSheet(onDismiss: () -> Unit) {
    var text      by remember { mutableStateOf("") }
    var type      by remember { mutableStateOf(FeedbackType.SUGGESTION) }
    var isSending by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    val scope     = rememberCoroutineScope()
    val isValid   = text.trim().length >= 10

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside   = false,
            decorFitsSystemWindows  = false,
        ),
    ) {
        CyberBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", style = AppTypography.caption, color = AppColors.textSecondary, letterSpacing = AppTypography.trackingWide)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("FEEDBACK", style = AppTypography.label, color = AppColors.neon, letterSpacing = AppTypography.trackingXWide)
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(72.dp))
                }

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                ) {
                    if (submitted) {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(AppSpacing.xxl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                        ) {
                            Spacer(Modifier.height(AppSpacing.xxl))
                            Box(
                                modifier         = Modifier.size(88.dp)
                                    .background(AppColors.neon.copy(alpha = 0.1f), CircleShape)
                                    .border(1.dp, AppColors.neon.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Check, null, tint = AppColors.neon, modifier = Modifier.size(36.dp))
                            }
                            Text("THANKS!", style = AppTypography.label, color = AppColors.neon, letterSpacing = AppTypography.trackingXWide)
                            Text("Your feedback helps us improve.", style = AppTypography.body, color = AppColors.textSecondary)
                            Spacer(Modifier.height(AppSpacing.lg))
                            CyberButton(label = "CLOSE", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.lg)) {
                            Spacer(Modifier.height(AppSpacing.md))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FeedbackType.entries.forEach { t ->
                                    val isActive = t == type
                                    val bg by animateColorAsState(if (isActive) AppColors.neon else AppColors.surfaceHigh, label = "bg")
                                    val tc by animateColorAsState(if (isActive) AppColors.background else AppColors.textSecondary, label = "tc")
                                    Row(
                                        modifier = Modifier
                                            .background(bg, RoundedCornerShape(AppRadius.pill))
                                            .border(1.dp, if (isActive) AppColors.neon else AppColors.divider, RoundedCornerShape(AppRadius.pill))
                                            .clip(RoundedCornerShape(AppRadius.pill))
                                            .clickable { type = t }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(t.icon, null, tint = tc, modifier = Modifier.size(11.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(t.label, style = AppTypography.mono9, color = tc, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(AppSpacing.lg))
                            OutlinedTextField(
                                value         = text,
                                onValueChange = { text = it },
                                placeholder   = {
                                    Text("Tell us what you think, report a bug, or suggest a feature...", color = AppColors.textMuted, style = AppTypography.body)
                                },
                                minLines = 6,
                                modifier = Modifier.fillMaxWidth(),
                                colors   = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor   = AppColors.neon,
                                    unfocusedBorderColor = AppColors.divider,
                                    focusedTextColor     = AppColors.textPrimary,
                                    unfocusedTextColor   = AppColors.textPrimary,
                                    cursorColor          = AppColors.neon,
                                ),
                            )
                            if (text.isNotEmpty() && !isValid) {
                                Spacer(Modifier.height(4.dp))
                                Text("${10 - text.trim().length} more characters needed", style = AppTypography.mono9, color = AppColors.magenta.copy(alpha = 0.8f))
                            }
                            Spacer(Modifier.height(AppSpacing.lg))
                            if (isSending) {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(28.dp), color = AppColors.neon, strokeWidth = 2.dp)
                                }
                            } else {
                                CyberButton(
                                    label   = "SEND FEEDBACK",
                                    enabled = isValid,
                                    onClick = {
                                        isSending = true
                                        scope.launch {
                                            APIService.shared.submitFeedback(type.name.lowercase(), text)
                                            AnalyticsManager.feedbackSent()
                                            submitted = true
                                            isSending = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Spacer(Modifier.height(AppSpacing.md))
                            Spacer(Modifier.navigationBarsPadding())
                        }
                    }
                }
            }
        }
    }
}
