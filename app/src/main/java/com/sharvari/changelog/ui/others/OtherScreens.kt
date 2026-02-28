package com.sharvari.changelog.ui.others

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharvari.changelog.data.model.ForceUpdateConfig
import com.sharvari.changelog.data.model.MaintenanceConfig
import com.sharvari.changelog.ui.components.CyberBackground
import com.sharvari.changelog.ui.components.CyberButton
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography

// ── Maintenance Screen ────────────────────────────────────────────────────────
@Composable
fun MaintenanceScreen(config: MaintenanceConfig) {
    CyberBackground {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                modifier = Modifier.padding(AppSpacing.xl),
            ) {
                Box(
                    modifier = Modifier.size(100.dp)
                        .background(AppColors.orange.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🔧", style = androidx.compose.ui.text.TextStyle(fontSize = 44.sp))
                }

                Text(
                    "MAINTENANCE",
                    style = AppTypography.label,
                    color = AppColors.orange,
                    letterSpacing = AppTypography.trackingXWide,
                )

                Text(
                    config.message,
                    style     = AppTypography.body,
                    color     = AppColors.textSecondary,
                    textAlign = TextAlign.Center,
                )

                config.eta?.let { eta ->
                    Text(
                        "Back at $eta",
                        style = AppTypography.mono10,
                        color = AppColors.textMuted,
                    )
                }
            }
        }
    }
}

// ── Force Update Screen ───────────────────────────────────────────────────────
@Composable
fun ForceUpdateScreen(config: ForceUpdateConfig) {
    val uriHandler = LocalUriHandler.current

    CyberBackground {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
                modifier = Modifier.padding(AppSpacing.xl),
            ) {
                Box(
                    modifier = Modifier.size(100.dp)
                        .background(AppColors.neon.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⬆️", style = androidx.compose.ui.text.TextStyle(fontSize = 44.sp))
                }

                Text(
                    "UPDATE REQUIRED",
                    style = AppTypography.label,
                    color = AppColors.neon,
                    letterSpacing = AppTypography.trackingXWide,
                )

                Text(
                    config.message,
                    style     = AppTypography.body,
                    color     = AppColors.textSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(AppSpacing.sm))

                CyberButton(
                    label   = "UPDATE NOW →",
                    onClick = { uriHandler.openUri(config.appStoreUrl) },
                    modifier = Modifier.height(52.dp),
                )
            }
        }
    }
}

private val androidx.compose.ui.unit.TextUnit.sp get() = this
