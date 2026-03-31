package com.sharvari.changelog.ui.screen.article

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sharvari.changelog.model.article.Article
import com.sharvari.changelog.service.analytics.AnalyticsManager
import com.sharvari.changelog.store.bookmark.BookmarkStore
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

// In-memory vote cache — survives tab switches, cleared on process death
private val voteCache = mutableMapOf<String, String>() // articleId -> "up" | "down"

@Composable
fun ArticleCardView(
    article:           Article,
    isTop:             Boolean,
    onSwipeLeft:       () -> Unit,
    onSwipeRight:      () -> Unit,
    onReadFullArticle: () -> Unit = {},
    onVote:            (direction: String, isActive: Boolean) -> Unit = { _, _ -> },
    modifier:          Modifier = Modifier,
) {
    val offsetX = remember { Animatable(0f) }
    val scope   = rememberCoroutineScope()
    val context = LocalContext.current

    val bookmarks    by BookmarkStore.bookmarks.collectAsStateWithLifecycle()
    val isBookmarked  = bookmarks.any { it.id == article.id }
    // Use cached vote if available, fall back to API value
    var localVote by remember(article.id) {
        mutableStateOf(voteCache[article.id] ?: article.yourVote)
    }
    var showDoubleTapIcon by remember { mutableStateOf(false) }

    val rawOffset    = offsetX.value
    val dragProgress = (abs(rawOffset) / SWIPE_THRESHOLD).coerceIn(0f, 1f)
    val rotation     = (rawOffset / 20f).coerceIn(-8f, 8f)

    val tintColor = when {
        rawOffset < -20 -> AppColors.magenta.copy(alpha = 0.15f * dragProgress)
        rawOffset >  20 -> AppColors.neon.copy(alpha = 0.12f * dragProgress)
        else            -> Color.Transparent
    }

    fun shareUrl() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, article.originalUrl)
        }
        context.startActivity(Intent.createChooser(intent, article.title))
    }

    fun flyOff(left: Boolean, action: () -> Unit) {
        scope.launch {
            offsetX.animateTo(if (left) -FLY_DISTANCE else FLY_DISTANCE, tween(280))
            action()
        }
    }

    fun handleDragEnd() {
        when {
            offsetX.value < -SWIPE_THRESHOLD -> flyOff(true)  { onSwipeLeft() }
            offsetX.value >  SWIPE_THRESHOLD -> flyOff(false) { onSwipeRight() }
            else -> scope.launch { offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
        }
    }

    // Horizontal swipe on content area — intercepts when clearly horizontal
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dx = available.x; val dy = available.y
                return if (isTop && abs(dx) > abs(dy) * 1.5f) {
                    scope.launch { offsetX.snapTo(offsetX.value + dx) }
                    Offset(dx, 0f)
                } else Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (isTop && abs(available.x) > abs(available.y) * 1.5f) { handleDragEnd(); return available }
                return Velocity.Zero
            }
        }
    }

    // Horizontal drag on image area
    val dragModifier = if (isTop) Modifier.draggable(
        orientation = Orientation.Horizontal,
        state = rememberDraggableState { delta -> scope.launch { offsetX.snapTo(offsetX.value + delta) } },
        onDragStopped = { handleDragEnd() },
    ) else Modifier

    Box(
        modifier = modifier
            .offset { IntOffset(rawOffset.roundToInt(), 0) }
            .rotate(rotation)
            .then(dragModifier)
            .shadow(if (isTop) 24.dp else 8.dp, RoundedCornerShape(AppRadius.xl))
            .clip(RoundedCornerShape(AppRadius.xl))
            .border(1.dp, if (isTop) AppColors.neon.copy(alpha = 0.25f) else AppColors.divider, RoundedCornerShape(AppRadius.xl))
            .background(AppColors.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── IMAGE AREA (38%) ────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.38f)) {
                ArticleImage(
                    article = article,
                    modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                AnalyticsManager.trackClick("double_tap_upvote", "ArticleCard")
                                localVote = "up"
                                voteCache[article.id] = "up"
                                onVote("up", true)
                                showDoubleTapIcon = true
                                scope.launch {
                                    kotlinx.coroutines.delay(800)
                                    showDoubleTapIcon = false
                                }
                            }
                        )
                    },
                )
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, AppColors.surface))))

                // Top right: Read count
                Row(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .padding(top = AppSpacing.sm, end = AppSpacing.sm)
                        .background(AppColors.surface.copy(alpha = 0.6f), RoundedCornerShape(AppRadius.pill))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.RemoveRedEye, null, tint = AppColors.textSecondary, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(4.dp))
                    val count = article.readCount
                    Text(
                        if (count >= 1000) String.format("%.1fK", count / 1000.0) else "$count",
                        style = AppTypography.mono9, color = AppColors.textSecondary,
                    )
                }

                // Bottom bar on image: Category (left) + Vote buttons (right)
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    article.category?.let { cat -> CategoryPill(cat.name, cat.slug) }
                    Spacer(Modifier.weight(1f))

                    // Upvote — 44dp round
                    val isUpvoted = localVote == "up"
                    Box(
                        modifier = Modifier.size(44.dp)
                            .background(if (isUpvoted) AppColors.green.copy(alpha = 0.2f) else AppColors.surface.copy(alpha = 0.7f), CircleShape)
                            .border(1.5.dp, if (isUpvoted) AppColors.green.copy(alpha = 0.6f) else AppColors.divider.copy(alpha = 0.4f), CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                AnalyticsManager.trackClick("upvote", "ArticleCard")
                                val wasActive = isUpvoted
                                val newVote = if (wasActive) null else "up"
                                localVote = newVote
                                if (newVote != null) voteCache[article.id] = newVote else voteCache.remove(article.id)
                                onVote("up", !wasActive)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.ThumbUp, "Upvote", tint = if (isUpvoted) AppColors.green else AppColors.textSecondary, modifier = Modifier.size(20.dp))
                    }

                    Spacer(Modifier.width(8.dp))

                    // Downvote — 44dp round
                    val isDownvoted = localVote == "down"
                    Box(
                        modifier = Modifier.size(44.dp)
                            .background(if (isDownvoted) AppColors.orange.copy(alpha = 0.2f) else AppColors.surface.copy(alpha = 0.7f), CircleShape)
                            .border(1.5.dp, if (isDownvoted) AppColors.orange.copy(alpha = 0.6f) else AppColors.divider.copy(alpha = 0.4f), CircleShape)
                            .clip(CircleShape)
                            .clickable {
                                AnalyticsManager.trackClick("downvote", "ArticleCard")
                                val wasActive = isDownvoted
                                val newVote = if (wasActive) null else "down"
                                localVote = newVote
                                if (newVote != null) voteCache[article.id] = newVote else voteCache.remove(article.id)
                                onVote("down", !wasActive)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.ThumbDown, "Downvote", tint = if (isDownvoted) AppColors.orange else AppColors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                // Double-tap upvote animation
                if (showDoubleTapIcon) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = "Upvoted",
                        tint = AppColors.green,
                        modifier = Modifier.align(Alignment.Center).size(64.dp),
                    )
                }
            }

            // ── CONTENT AREA (scrollable) ───────────────────────────────
            Column(modifier = Modifier.fillMaxWidth().weight(1f).background(AppColors.surface).nestedScroll(nestedScrollConnection)) {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg)) {

                    Row(modifier = Modifier.fillMaxWidth()) {
                        article.sourceName?.let { Text(it.uppercase(), style = AppTypography.mono11, color = AppColors.textMuted, letterSpacing = 0.10.sp) }
                        Spacer(Modifier.weight(1f))
                        article.publishedAt?.timeAgo()?.let { Text(it, style = AppTypography.mono11, color = AppColors.neon.copy(alpha = 0.8f)) }
                    }

                    Spacer(Modifier.height(AppSpacing.sm))
                    Text(article.title, style = AppTypography.headline, color = AppColors.textPrimary, lineHeight = 26.sp)
                    Spacer(Modifier.height(AppSpacing.sm))
                    CyberDivider()
                    Spacer(Modifier.height(AppSpacing.sm))
                    Text(article.summary, style = AppTypography.body, color = AppColors.textSecondary, lineHeight = 24.sp)
                    Spacer(Modifier.height(AppSpacing.lg))

                    // ── BOTTOM BAR: Bookmark + Share | Read Article ────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Bookmark
                        Box(
                            modifier = Modifier.size(36.dp)
                                .background(if (isBookmarked) AppColors.neon.copy(alpha = 0.15f) else AppColors.surfaceHigh, CircleShape)
                                .border(1.dp, if (isBookmarked) AppColors.neon.copy(alpha = 0.4f) else AppColors.divider, CircleShape)
                                .clip(CircleShape)
                                .clickable {
                                    AnalyticsManager.trackClick("bookmark", "ArticleCard")
                                    BookmarkStore.toggle(context, article)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                "Bookmark",
                                tint = if (isBookmarked) AppColors.neon else AppColors.textSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // Share
                        Box(
                            modifier = Modifier.size(36.dp)
                                .background(AppColors.surfaceHigh, CircleShape)
                                .border(1.dp, AppColors.divider, CircleShape)
                                .clip(CircleShape)
                                .clickable {
                                    AnalyticsManager.trackClick("share", "ArticleCard")
                                    shareUrl()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Share, "Share", tint = AppColors.textSecondary, modifier = Modifier.size(16.dp))
                        }

                        Spacer(Modifier.weight(1f))

                        // Read Article
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(AppColors.neon.copy(alpha = 0.1f), RoundedCornerShape(AppRadius.pill))
                                .border(1.dp, AppColors.neon.copy(alpha = 0.4f), RoundedCornerShape(AppRadius.pill))
                                .clip(RoundedCornerShape(AppRadius.pill))
                                .clickable {
                                    AnalyticsManager.trackClick("read_article", "ArticleCard")
                                    onReadFullArticle()
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text("READ ARTICLE", style = AppTypography.mono11, color = AppColors.neon, letterSpacing = 0.10.sp)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowOutward, null, tint = AppColors.neon, modifier = Modifier.size(12.dp))
                        }
                    }

                    Spacer(Modifier.height(AppSpacing.sm))
                }
            }
        }

        // Tint overlay
        Box(modifier = Modifier.fillMaxSize().background(tintColor))

        // Swipe badges — horizontal only
        if (isTop && rawOffset < -20) SwipeBadge("NEXT", Icons.Default.Close, AppColors.magenta, dragProgress,
            Modifier.align(Alignment.TopEnd).padding(top = AppSpacing.lg, end = AppSpacing.lg).rotate(-12f))
        if (isTop && rawOffset > 20)  SwipeBadge("READ", Icons.Default.ArrowOutward, AppColors.neon, dragProgress,
            Modifier.align(Alignment.TopStart).padding(top = AppSpacing.lg, start = AppSpacing.lg).rotate(12f))
    }
}

// ── Supporting composables ───────────────────────────────────────────────────

@Composable
private fun ArticleImage(article: Article, modifier: Modifier = Modifier) {
    val accent = AppColors.categoryAccent(article.category?.slug)
    Box(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accent.copy(alpha = 0.25f), AppColors.background, accent.copy(alpha = 0.08f)))), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 40.dp.toPx()
                val gridColor = accent.copy(alpha = 0.08f)
                val strokeW = 0.5.dp.toPx()
                var x = 0f
                while (x <= size.width) { drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = strokeW); x += step }
                var y = 0f
                while (y <= size.height) { drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = strokeW); y += step }
            }
            Box(modifier = Modifier.size(160.dp).align(Alignment.TopEnd).offset(x = 40.dp, y = (-20).dp).background(Brush.radialGradient(listOf(accent.copy(alpha = 0.3f), Color.Transparent)), CircleShape))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(72.dp).background(accent.copy(alpha = 0.12f), CircleShape).border(1.dp, accent.copy(alpha = 0.25f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(categoryIcon(article.category?.slug), null, tint = accent.copy(alpha = 0.9f), modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(8.dp))
                article.sourceName?.let { Text(it.uppercase(), style = AppTypography.mono9, color = accent.copy(alpha = 0.5f), letterSpacing = 0.15.sp) }
            }
        }
        if (!article.imageUrl.isNullOrBlank()) {
            val context = LocalContext.current
            val req = remember(article.imageUrl) { coil3.request.ImageRequest.Builder(context).data(article.imageUrl).build() }
            coil3.compose.AsyncImage(model = req, contentDescription = article.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun CategoryPill(name: String, slug: String, modifier: Modifier = Modifier) {
    val color = AppColors.categoryAccent(slug)
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(AppRadius.pill))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(AppRadius.pill))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(categoryIcon(slug), null, tint = color, modifier = Modifier.size(10.dp))
        Spacer(Modifier.width(4.dp))
        Text(name.uppercase(), style = AppTypography.mono9, color = color, letterSpacing = 0.10.sp)
    }
}

@Composable
private fun SwipeBadge(text: String, icon: ImageVector, color: Color, alpha: Float, modifier: Modifier = Modifier) {
    Row(modifier = modifier.background(color.copy(alpha = 0.15f * alpha), RoundedCornerShape(8.dp)).border(2.dp, color.copy(alpha = alpha), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color.copy(alpha = alpha), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = AppTypography.mono12, color = color.copy(alpha = alpha), fontWeight = FontWeight.Black, letterSpacing = 0.12.sp)
    }
}

private fun categoryIcon(slug: String?): ImageVector = com.sharvari.changelog.utils.categoryIcon(slug)

fun String.timeAgo(): String {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdf.parse(this) ?: return ""
        val s = ((System.currentTimeMillis() - date.time) / 1000).toInt()
        when { s < 3600 -> "${maxOf(s / 60, 1)}M AGO"; s < 86400 -> "${s / 3600}H AGO"; else -> "${s / 86400}D AGO" }
    } catch (_: Exception) { "" }
}
