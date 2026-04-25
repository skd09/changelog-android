package com.sharvari.changelog.ui.screen.discover

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sharvari.changelog.model.article.Article
import com.sharvari.changelog.service.analytics.AnalyticsManager
import com.sharvari.changelog.store.bookmark.BookmarkStore
import com.sharvari.changelog.store.discover.SearchUiState
import com.sharvari.changelog.store.discover.SearchViewModel
import com.sharvari.changelog.store.discover.TrendingUiState
import com.sharvari.changelog.store.discover.TrendingViewModel
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography
import com.sharvari.changelog.utils.categoryIcon
import com.sharvari.changelog.utils.timeAgo

// ─────────────────────────────────────────────────────────────────────────────
// Segments
// ─────────────────────────────────────────────────────────────────────────────

enum class DiscoverSegment(val label: String, val icon: ImageVector, val filledIcon: ImageVector) {
    TRENDING("TRENDING", Icons.Default.LocalFireDepartment, Icons.Default.LocalFireDepartment),
    SEARCH("SEARCH",     Icons.Default.Search,              Icons.Default.Search),
    SAVED("SAVED",       Icons.Default.BookmarkBorder,      Icons.Default.Bookmark),
}

// ─────────────────────────────────────────────────────────────────────────────
// DiscoverScreen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen() {
    var selectedSegment by remember { mutableStateOf(DiscoverSegment.TRENDING) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { AnalyticsManager.trackScreen("Discover") }

    Scaffold(
        containerColor = AppColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "DISCOVER",
                            style         = AppTypography.label,
                            color         = AppColors.neon,
                            letterSpacing = AppTypography.trackingXWide,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.background,
                    scrolledContainerColor = AppColors.background,
                ),
            )
        }
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .navigationBarsPadding()
    ) {
        // Segment control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DiscoverSegment.entries.forEach { segment ->
                val isActive = selectedSegment == segment
                val bgColor by animateColorAsState(
                    if (isActive) AppColors.neon else AppColors.surfaceHigh,
                    animationSpec = tween(180), label = "seg_bg_${segment.name}",
                )
                val textColor by animateColorAsState(
                    if (isActive) AppColors.background else AppColors.textSecondary,
                    animationSpec = tween(180), label = "seg_txt_${segment.name}",
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(bgColor, RoundedCornerShape(10.dp))
                        .border(1.dp, if (isActive) AppColors.neon else AppColors.divider, RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            AnalyticsManager.trackClick("segment_${segment.label.lowercase()}", "Discover")
                            selectedSegment = segment
                            if (segment != DiscoverSegment.SEARCH) focusManager.clearFocus()
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (isActive) segment.filledIcon else segment.icon,
                        null, tint = textColor, modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(segment.label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.8.sp, color = textColor)
                }
            }
        }
        HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp)

        when (selectedSegment) {
            DiscoverSegment.TRENDING -> TrendingContent()
            DiscoverSegment.SEARCH   -> SearchContent()
            DiscoverSegment.SAVED    -> SavedContent()
        }
    }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Trending
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrendingContent(viewModel: TrendingViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        if (state is TrendingUiState.Success) {
            val successState = state as TrendingUiState.Success
            Row(
                modifier = Modifier.fillMaxWidth().background(AppColors.surface).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryChip("ALL", selectedCategory == null, AppColors.neon, icon = Icons.Default.GridView) { selectedCategory = null; viewModel.filter(null) }
                successState.categories.forEach { cat ->
                    val color = AppColors.categoryAccent(cat.slug)
                    CategoryChip(cat.name.uppercase(), selectedCategory == cat.slug, color, icon = categoryIcon(cat.slug)) { selectedCategory = cat.slug; viewModel.filter(cat.slug) }
                }
            }
            HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp)
        }

        when (state) {
            is TrendingUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AppColors.neon, modifier = Modifier.size(36.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("LOADING TRENDING", style = AppTypography.mono10, color = AppColors.textMuted, letterSpacing = AppTypography.trackingXWide)
                    }
                }
            }
            is TrendingUiState.Empty -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = AppColors.textMuted, modifier = Modifier.size(44.dp))
                        Text("No trending stories right now", style = AppTypography.body, color = AppColors.textMuted)
                    }
                }
            }
            is TrendingUiState.Success -> {
                val successState = state as TrendingUiState.Success
                if (successState.isFiltering) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppColors.neon, modifier = Modifier.size(36.dp), strokeWidth = 2.dp)
                    }
                } else if (successState.filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Default.LocalFireDepartment, null, tint = AppColors.textMuted, modifier = Modifier.size(44.dp))
                            Text("No trending stories right now", style = AppTypography.body, color = AppColors.textMuted)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                        items(successState.filtered, key = { it.id }) { article ->
                            val rank = successState.filtered.indexOf(article) + 1
                            TrendingRow(article, rank, viewModel.rankChange(article.id)) {
                                AnalyticsManager.trackClick("trending_article", "Discover")
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.originalUrl)))
                            }
                            HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, isActive: Boolean, color: Color, icon: ImageVector? = null, onClick: () -> Unit) {
    val bg by animateColorAsState(if (isActive) color else color.copy(alpha = 0.08f), label = "chip_bg")
    val textColor by animateColorAsState(if (isActive) AppColors.background else AppColors.textPrimary, label = "chip_txt")
    val borderColor by animateColorAsState(if (isActive) color else AppColors.divider, label = "chip_border")
    Row(
        modifier = Modifier.background(bg, RoundedCornerShape(100.dp)).border(1.dp, borderColor, RoundedCornerShape(100.dp)).clip(RoundedCornerShape(100.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(it, null, tint = textColor, modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp, color = textColor)
    }
}

@Composable
private fun TrendingRow(article: Article, rank: Int, rankChange: Int, onTap: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onTap).padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(36.dp)
                    .background(if (rank <= 3) AppColors.neon.copy(alpha = 0.12f) else AppColors.surfaceHigh, RoundedCornerShape(8.dp))
                    .border(1.dp, if (rank <= 3) AppColors.neon else AppColors.divider, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$rank", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = if (rank <= 3) 15.sp else 13.sp, color = if (rank <= 3) AppColors.neon else AppColors.textSecondary)
            }
            Spacer(Modifier.height(4.dp))
            when {
                rankChange > 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, null, tint = AppColors.green, modifier = Modifier.size(8.dp))
                    Text("$rankChange", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = AppColors.green, fontWeight = FontWeight.Bold)
                }
                rankChange < 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDownward, null, tint = AppColors.red, modifier = Modifier.size(8.dp))
                    Text("${-rankChange}", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = AppColors.red, fontWeight = FontWeight.Bold)
                }
                else -> Text("–", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = AppColors.textSecondary, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                article.sourceName?.let { Text(it.uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp, color = AppColors.neon) }
                Spacer(Modifier.weight(1f))
                val readCount = article.readCount
                val formatted = if (readCount >= 1000) String.format("%.1fK", readCount / 1000.0) else "$readCount"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RemoveRedEye, null, tint = AppColors.textSecondary, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(formatted, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AppColors.textSecondary)
                }
                article.publishedAt?.let { Text(" · ${timeAgo(it)}", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AppColors.textSecondary) }
            }
            Text(article.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.textPrimary, lineHeight = 20.sp, maxLines = 2)
            article.category?.let { cat ->
                val color = AppColors.categoryAccent(cat.slug)
                Row(
                    modifier = Modifier.background(color.copy(alpha = 0.08f), RoundedCornerShape(100.dp)).border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(100.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(categoryIcon(cat.slug), null, tint = AppColors.textPrimary, modifier = Modifier.size(9.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(cat.name.uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp, color = AppColors.textPrimary)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchContent(viewModel: SearchViewModel = viewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val focusManager   = LocalFocusManager.current
    val context        = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(AppColors.surface).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, null, tint = if (query.isEmpty()) AppColors.textMuted else AppColors.neon, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query, onValueChange = viewModel::onQueryChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                textStyle = TextStyle(color = AppColors.textPrimary, fontSize = 15.sp),
                cursorBrush = SolidColor(AppColors.neon), singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                decorationBox = { inner -> if (query.isEmpty()) Text("Search articles, sources, topics...", color = AppColors.textMuted, fontSize = 15.sp); inner() },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearQuery(); focusManager.clearFocus() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Cancel, null, tint = AppColors.textMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
        HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp)

        when (state) {
            is SearchUiState.Idle -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.Search, null, tint = AppColors.textMuted, modifier = Modifier.size(44.dp))
                        Text("Search across all articles,\nsources and topics", style = AppTypography.body, color = AppColors.textMuted, textAlign = TextAlign.Center)
                    }
                }
            }
            is SearchUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.neon, modifier = Modifier.size(36.dp), strokeWidth = 2.dp) }
            is SearchUiState.NoResults -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.SearchOff, null, tint = AppColors.textMuted, modifier = Modifier.size(44.dp))
                        Text("No results for", style = AppTypography.body, color = AppColors.textMuted)
                        Text("\"$query\"", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = AppColors.textSecondary)
                    }
                }
            }
            is SearchUiState.Success -> {
                val results = (state as SearchUiState.Success).results
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
                    item { Text("${results.size} RESULTS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 2.sp, color = AppColors.textSecondary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
                    items(results, key = { it.id }) { article ->
                        SearchResultRow(article, query) { AnalyticsManager.trackClick("search_result", "Discover"); focusManager.clearFocus(); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.originalUrl))) }
                        HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(article: Article, query: String, onTap: () -> Unit) {
    val color = AppColors.categoryAccent(article.category?.slug)
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onTap).padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(44.dp).background(color.copy(alpha = 0.10f), CircleShape).border(1.dp, color, CircleShape), contentAlignment = Alignment.Center) {
            Icon(categoryIcon(article.category?.slug), null, tint = color, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                article.sourceName?.let { Text(it.uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp, color = AppColors.neon) }
                Spacer(Modifier.weight(1f))
                article.publishedAt?.let { Text(timeAgo(it), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AppColors.textSecondary) }
            }
            val annotated = buildAnnotatedString {
                val lower = article.title.lowercase(); val qLower = query.lowercase(); var i = 0
                while (i < article.title.length) {
                    val idx = lower.indexOf(qLower, i)
                    if (idx == -1) { withStyle(SpanStyle(color = AppColors.textPrimary)) { append(article.title.substring(i)) }; break }
                    withStyle(SpanStyle(color = AppColors.textPrimary)) { append(article.title.substring(i, idx)) }
                    withStyle(SpanStyle(color = AppColors.neon, fontWeight = FontWeight.Bold)) { append(article.title.substring(idx, idx + query.length)) }
                    i = idx + query.length
                }
            }
            Text(annotated, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, maxLines = 2)
            Text(article.summary, style = AppTypography.footnote, color = AppColors.textSecondary, maxLines = 2, lineHeight = 18.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Saved (Bookmarks)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SavedContent() {
    val bookmarks by BookmarkStore.bookmarks.collectAsStateWithLifecycle()
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filtered = remember(bookmarks, searchText) {
        if (searchText.isEmpty()) bookmarks
        else {
            val q = searchText.lowercase()
            bookmarks.filter { it.title.lowercase().contains(q) || it.summary.lowercase().contains(q) || it.sourceName?.lowercase()?.contains(q) == true }
        }
    }

    if (bookmarks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(AppSpacing.xl)) {
                Box(modifier = Modifier.size(100.dp).background(AppColors.neon.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.BookmarkBorder, null, tint = AppColors.neon, modifier = Modifier.size(44.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NO SAVED ARTICLES", style = AppTypography.label, color = AppColors.neon, letterSpacing = AppTypography.trackingXWide)
                    Text("Bookmark articles from the feed\nto read them later.", style = AppTypography.body, color = AppColors.textSecondary, textAlign = TextAlign.Center)
                }
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).background(AppColors.surface, RoundedCornerShape(12.dp)).border(1.dp, AppColors.divider, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, null, tint = AppColors.textMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = searchText, onValueChange = { searchText = it }, modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = AppColors.textPrimary, fontSize = 14.sp), cursorBrush = SolidColor(AppColors.neon), singleLine = true,
                    decorationBox = { inner -> if (searchText.isEmpty()) Text("Search saved...", color = AppColors.textMuted, fontSize = 14.sp); inner() },
                )
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }, modifier = Modifier.size(20.dp)) { Icon(Icons.Default.Cancel, null, tint = AppColors.textMuted, modifier = Modifier.size(14.dp)) }
                }
            }
        }
        if (filtered.isEmpty() && searchText.isNotEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Search, null, tint = AppColors.textMuted, modifier = Modifier.size(32.dp))
                        Text("No results for \"$searchText\"", style = AppTypography.body, color = AppColors.textMuted)
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { article ->
                BookmarkRow(article, onTap = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.originalUrl))) }, onRemove = { BookmarkStore.remove(context, article.id) })
                HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun BookmarkRow(article: Article, onTap: () -> Unit, onRemove: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onTap).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row {
                article.sourceName?.let { Text(it.uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp, color = AppColors.neon) }
                Spacer(Modifier.weight(1f))
                article.publishedAt?.let { Text(timeAgo(it), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AppColors.textSecondary) }
            }
            Text(article.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.textPrimary, maxLines = 2, lineHeight = 20.sp)
            article.category?.let { cat ->
                Text(cat.name.uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp, color = AppColors.neon)
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Bookmark, null, tint = AppColors.neon, modifier = Modifier.size(18.dp)) }
    }
}
