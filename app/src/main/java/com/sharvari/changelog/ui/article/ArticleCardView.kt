package com.sharvari.changelog.ui.article

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharvari.changelog.data.model.Article
import com.sharvari.changelog.ui.components.CyberDivider
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SWIPE_THRESHOLD = 110f
private const val FLY_DISTANCE    = 1400f

@Composable
fun ArticleCardView(
    article:      Article,
    isTop:        Boolean,
    onSwipeLeft:  () -> Unit,
    onSwipeRight: () -> Unit,
    modifier:     Modifier = Modifier,
) {
    val offsetX    = remember { Animatable(0f) }
    val scope      = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    val rawOffset    = offsetX.value
    val dragProgress = (abs(rawOffset) / SWIPE_THRESHOLD).coerceIn(0f, 1f)
    val rotation     = (rawOffset / 20f).coerceIn(-8f, 8f)

    val tintColor = when {
        rawOffset < -20 -> AppColors.magenta.copy(alpha = 0.15f * dragProgress)
        rawOffset >  20 -> AppColors.neon.copy(alpha = 0.12f * dragProgress)
        else            -> Color.Transparent
    }

    fun openUrl() {
        try { uriHandler.openUri(article.originalUrl) } catch (_: Exception) { }
    }

    fun flyOff(left: Boolean, action: () -> Unit) {
        scope.launch {
            offsetX.animateTo(
                targetValue   = if (left) -FLY_DISTANCE else FLY_DISTANCE,
                animationSpec = tween(durationMillis = 280),
            )
            action()
        }
    }

    fun handleDragEnd() {
        when {
            offsetX.value < -SWIPE_THRESHOLD -> flyOff(true)  { onSwipeLeft() }
            offsetX.value >  SWIPE_THRESHOLD -> flyOff(false) { openUrl(); onSwipeRight() }
            else -> scope.launch {
                offsetX.animateTo(
                    0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)
                )
            }
        }
    }

    // ── Dual gesture — mirrors iOS:
    // Image: full drag (no scroll conflict)
    // Content: horizontal-only simultaneous with vertical scroll
    val imageDragGesture = if (isTop) Modifier.pointerInput(article.id) {
        detectDragGestures(
            onDragEnd = { handleDragEnd() },
            onDrag    = { change, dragAmount ->
                change.consume()
                scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
            }
        )
    } else Modifier

    val contentSwipeGesture = if (isTop) Modifier.pointerInput(article.id) {
        detectHorizontalDragGestures(
            onDragEnd        = { handleDragEnd() },
            onHorizontalDrag = { change, delta ->
                change.consume()
                scope.launch { offsetX.snapTo(offsetX.value + delta) }
            }
        )
    } else Modifier

    Box(
        modifier = modifier
            .offset { IntOffset(rawOffset.roundToInt(), 0) }
            .rotate(rotation)
            .shadow(if (isTop) 24.dp else 8.dp, RoundedCornerShape(AppRadius.xl))
            .clip(RoundedCornerShape(AppRadius.xl))
            .border(
                1.dp,
                if (isTop) AppColors.neon.copy(alpha = 0.25f) else AppColors.divider,
                RoundedCornerShape(AppRadius.xl),
            )
            .background(AppColors.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Image — top 38%, drag gesture lives here ──────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.38f)
                    .then(imageDragGesture)
            ) {
                ArticleImage(article = article)

                // Gradient fade into surface
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, AppColors.surface))
                        )
                )

                article.category?.let { cat ->
                    CategoryPill(
                        name     = cat.name,
                        slug     = cat.slug,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(AppSpacing.md)
                    )
                }
            }

            // ── Content — remaining 62%, scrollable vertically ────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(AppColors.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .then(contentSwipeGesture)
                        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg)
                ) {
                    // Source + timestamp
                    Row(modifier = Modifier.fillMaxWidth()) {
                        article.sourceName?.let { src ->
                            Text(
                                text          = src.uppercase(),
                                style         = AppTypography.mono11,
                                color         = AppColors.textMuted,
                                letterSpacing = 0.10.sp,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        article.publishedAt?.timeAgo()?.let { ts ->
                            Text(
                                text  = ts,
                                style = AppTypography.mono11,
                                color = AppColors.neon.copy(alpha = 0.8f),
                            )
                        }
                    }

                    Spacer(Modifier.height(AppSpacing.sm))

                    Text(
                        text       = article.title,
                        style      = AppTypography.headline,
                        color      = AppColors.textPrimary,
                        lineHeight = 26.sp,
                    )

                    Spacer(Modifier.height(AppSpacing.sm))
                    CyberDivider()
                    Spacer(Modifier.height(AppSpacing.sm))

                    Text(
                        text       = article.summary,
                        style      = AppTypography.body,
                        color      = AppColors.textSecondary,
                        lineHeight = 24.sp,
                    )

                    Spacer(Modifier.height(AppSpacing.md))

                    // Full article link — right-aligned capsule
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier
                                .background(
                                    AppColors.neon.copy(alpha = 0.1f),
                                    RoundedCornerShape(AppRadius.pill)
                                )
                                .border(
                                    1.dp,
                                    AppColors.neon.copy(alpha = 0.4f),
                                    RoundedCornerShape(AppRadius.pill)
                                )
                                .clip(RoundedCornerShape(AppRadius.pill))
                                .clickable { openUrl() }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text          = "FULL ARTICLE",
                                style         = AppTypography.mono11,
                                color         = AppColors.neon,
                                letterSpacing = 0.10.sp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector        = Icons.Default.ArrowOutward,
                                contentDescription = null,
                                tint               = AppColors.neon,
                                modifier           = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }
        }

        // Tint overlay
        Box(modifier = Modifier.fillMaxSize().background(tintColor))

        // SKIP badge
        if (isTop && rawOffset < -20) {
            SwipeBadge(
                text     = "SKIP",
                icon     = Icons.Default.Close,
                color    = AppColors.magenta,
                alpha    = dragProgress,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = AppSpacing.lg, end = AppSpacing.lg)
                    .rotate(-12f)
            )
        }

        // READ badge
        if (isTop && rawOffset > 20) {
            SwipeBadge(
                text     = "READ",
                icon     = Icons.Default.ArrowOutward,
                color    = AppColors.neon,
                alpha    = dragProgress,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = AppSpacing.lg, start = AppSpacing.lg)
                    .rotate(12f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ArticleImage — matches iOS imageLayer + placeholderImage
// Uses Icon (vector) instead of emoji — matches rest of app
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArticleImage(article: Article) {
    val accent = AppColors.categoryAccent(article.category?.slug)

    Box(modifier = Modifier.fillMaxSize()) {

        // Placeholder — gradient + dot grid + glow orb + icon circle
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.25f),
                            AppColors.background,
                            accent.copy(alpha = 0.08f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Dot grid — matches iOS Canvas dot grid
            Canvas(modifier = Modifier.fillMaxSize()) {
                val spacing = 28f
                var x = 0f
                while (x < size.width) {
                    var y = 0f
                    while (y < size.height) {
                        drawCircle(accent.copy(alpha = 0.06f), 1f, Offset(x, y))
                        y += spacing
                    }
                    x += spacing
                }
            }

            // Glow orb — top right, matches iOS RadialGradient + blur
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-20).dp)
                    .background(
                        Brush.radialGradient(listOf(accent.copy(alpha = 0.3f), Color.Transparent)),
                        CircleShape,
                    )
            )

            // Icon circle + source name
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier         = Modifier
                        .size(72.dp)
                        .background(accent.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, accent.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = categoryIcon(article.category?.slug),
                        contentDescription = null,
                        tint               = accent.copy(alpha = 0.9f),
                        modifier           = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                article.sourceName?.let { src ->
                    Text(
                        text          = src.uppercase(),
                        style         = AppTypography.mono9,
                        color         = accent.copy(alpha = 0.5f),
                        letterSpacing = 0.15.sp,
                    )
                }
            }
        }

        // Real image on top via Coil
        if (!article.imageUrl.isNullOrBlank()) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val imageRequest = remember(article.imageUrl) {
                coil3.request.ImageRequest.Builder(context)
                    .data(article.imageUrl)
                    .build()
            }
            coil3.compose.AsyncImage(
                model              = imageRequest,
                contentDescription = article.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CategoryPill — icon + name, matches iOS categoryPill
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryPill(name: String, slug: String, modifier: Modifier = Modifier) {
    val color = AppColors.categoryAccent(slug)
    Row(
        modifier          = modifier
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(AppRadius.pill))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(AppRadius.pill))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = categoryIcon(slug),
            contentDescription = null,
            tint               = color,
            modifier           = Modifier.size(10.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text          = name.uppercase(),
            style         = AppTypography.mono9,
            color         = color,
            letterSpacing = 0.10.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SwipeBadge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SwipeBadge(
    text:     String,
    icon:     ImageVector,
    color:    Color,
    alpha:    Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier
            .background(color.copy(alpha = 0.15f * alpha), RoundedCornerShape(8.dp))
            .border(2.dp, color.copy(alpha = alpha), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = color.copy(alpha = alpha),
            modifier           = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text          = text,
            style         = AppTypography.mono12,
            color         = color.copy(alpha = alpha),
            fontWeight    = FontWeight.Black,
            letterSpacing = 0.12.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun categoryIcon(slug: String?): ImageVector = when (slug) {
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

fun String.timeAgo(): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(this) ?: return ""
        val s = ((System.currentTimeMillis() - date.time) / 1000).toInt()
        when {
            s < 3600  -> "${maxOf(s / 60, 1)}M AGO"
            s < 86400 -> "${s / 3600}H AGO"
            else      -> "${s / 86400}D AGO"
        }
    } catch (_: Exception) { "" }
}