package com.sharvari.changelog.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.sharvari.changelog.ui.theme.AppColors

// ── CyberBackground — dark bg + atmospheric glow + grid ───────────────────────
// Mirrors iOS CyberBackgroundModifier + CyberGridView
@Composable
fun CyberBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background)
    ) {
        // Atmospheric gradient from top (purple glow)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AppColors.neon.copy(alpha = 0.06f),
                            AppColors.background.copy(alpha = 0f),
                        ),
                        endY = 900f,
                    )
                )
        )

        // Grid — matches iOS CyberGridView (40pt spacing, 0.5pt stroke)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 40.dp.toPx()  // density-independent, matches iOS 40pt
            val gridColor = AppColors.neon.copy(alpha = 0.05f)
            val strokeW = 0.5.dp.toPx()
            var x = 0f
            while (x <= size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = strokeW)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = strokeW)
                y += step
            }
        }

        content()
    }
}