package com.sharvari.changelog.ui.screen.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharvari.changelog.ui.components.CyberBackground
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppTypography

@Composable
fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash")
    val rotation by transition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label         = "rotation",
    )
    val pulse by transition.animateFloat(
        initialValue  = 0.6f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label         = "pulse",
    )

    CyberBackground {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Delta (Δ) logo — matching iOS icon design
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Spinning arc around logo
                    Canvas(
                        modifier = Modifier
                            .size(100.dp)
                            .rotate(rotation)
                    ) {
                        drawArc(
                            brush       = Brush.sweepGradient(listOf(AppColors.neon, AppColors.magenta, Color.Transparent)),
                            startAngle  = 0f,
                            sweepAngle  = 270f,
                            useCenter   = false,
                            style       = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }

                    // Delta triangle
                    Canvas(modifier = Modifier.size(56.dp)) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(w / 2f, 0f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(
                            path  = path,
                            brush = Brush.linearGradient(
                                listOf(AppColors.neon, AppColors.magenta),
                                start = Offset(0f, 0f),
                                end   = Offset(w, h),
                            ),
                            style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
                        )
                        // Center accent dot
                        drawCircle(
                            brush  = Brush.radialGradient(listOf(AppColors.neon, AppColors.magenta)),
                            radius = 3.dp.toPx(),
                            center = Offset(w / 2f, h * 0.72f),
                            alpha  = pulse,
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text          = "THE CHANGELOG",
                    style         = AppTypography.label,
                    color         = AppColors.neon,
                    letterSpacing = AppTypography.trackingXWide,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text  = "loading...",
                    style = AppTypography.mono10,
                    color = AppColors.textMuted,
                )
            }
        }
    }
}
