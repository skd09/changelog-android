package com.sharvari.changelog.ui.discover

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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sharvari.changelog.data.api.APIService
import com.sharvari.changelog.data.model.Article
import com.sharvari.changelog.data.model.ArticleCategory
import com.sharvari.changelog.data.store.BookmarkStore
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppRadius
import com.sharvari.changelog.ui.theme.AppSpacing
import com.sharvari.changelog.ui.theme.AppTypography
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// Category colour palette — mirrors iOS categoryColor(for:)
// ─────────────────────────────────────────────────────────────────────────────

fun categoryAccentColor(slug: String?): Color = when (slug) {
    "ai", "artificial-intelligence" -> Color(0xFF00FF9F) // neon green
    "crypto", "blockchain"          -> Color(0xFFFFCC00) // gold
    "cybersecurity", "security"     -> Color(0xFFFF3348) // red
    "gaming"                        -> Color(0xFF9933FF) // purple
    "science"                       -> Color(0xFF00CCFF) // cyan
    "startups", "business"          -> Color(0xFFFF8000) // orange
    "health", "biotech"             -> Color(0xFF33E566) // mint
    "space"                         -> Color(0xFF6666FF) // indigo
    "social-media"                  -> Color(0xFFFF4DCC) // pink
    "hardware", "gadgets"           -> Color(0xFFCCCCCC) // silver
    "technology"                    -> Color(0xFF00FF9F) // neon fallback
    "open-source"                   -> Color(0xFF00CCFF)
    else                            -> Color(0xFF00FF9F)
}

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

@Composable
fun DiscoverScreen() {
    var selectedSegment by remember { mutableStateOf(DiscoverSegment.TRENDING) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .navigationBarsPadding()
    ) {
        // Nav header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(AppColors.background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text          = "DISCOVER",
                style         = AppTypography.label,
                color         = AppColors.neon,
                letterSpacing = AppTypography.trackingXWide,
            )
        }
        HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp)

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
                    animationSpec = tween(180), label = "seg_bg_${segment.name}"
                )
                val textColor by animateColorAsState(
                    if (isActive) AppColors.background else AppColors.textSecondary,
                    animationSpec = tween(180), label = "seg_txt_${segment.name}"
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(bgColor, RoundedCornerShape(10.dp))
                        .border(
                            1.dp,
                            if (isActive) AppColors.neon else AppColors.divider,
                            RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            selectedSegment = segment
                            if (segment != DiscoverSegment.SEARCH) focusManager.clearFocus()
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector        = if (isActive) segment.filledIcon else segment.icon,
                        contentDescription = null,
                        tint               = textColor,
                        modifier           = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text          = segment.label,
                        fontFamily    = FontFamily.Monospace,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 11.sp,
                        letterSpacing = 0.8.sp,
                        color         = textColor,
                    )
                }
            }
        }
        HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp)

        // Content
        when (selectedSegment) {
            DiscoverSegment.TRENDING -> TrendingContent()
            DiscoverSegment.SEARCH   -> SearchContent()
            DiscoverSegment.SAVED    -> SavedContent()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Trending
// ─────────────────────────────────────────────────────────────────────────────

class TrendingViewModel : ViewModel() {
    private val _articles        = MutableStateFlow<List<Article>>(emptyList())
    private val _filtered        = MutableStateFlow<List<Article>>(emptyList())
    private val _categories      = MutableStateFlow<List<ArticleCategory>>(emptyList())
    private val _isLoading       = MutableStateFlow(false)

    val articles:   StateFlow<List<Article>>         = _articles.asStateFlow()
    val filtered:   StateFlow<List<Article>>         = _filtered.asStateFlow()
    val categories: StateFlow<List<ArticleCategory>> = _categories.asStateFlow()
    val isLoading:  StateFlow<Boolean>               = _isLoading.asStateFlow()

    // Previous rank snapshot for rank-change arrows — mirrors iOS TrendingViewModel
    private var previousRanks: Map<String, Int> = emptyMap()

    init { load() }

    fun load(refresh: Boolean = false) {
        viewModelScope.launch {
            if (!refresh) _isLoading.value = true
            // Snapshot current ranks before reload (for rank change arrows)
            val snapshot = _articles.value.mapIndexed { i, a -> a.id to (i + 1) }.toMap()
            try {
                val response = APIService.shared.fetchTrending()
                if (refresh) previousRanks = snapshot
                _articles.value   = response.data
                _filtered.value   = response.data
                _categories.value = response.data
                    .mapNotNull { it.category }
                    .distinctBy { it.slug }
                    .sortedBy { it.name }
            } catch (e: Exception) {
                println("TrendingViewModel: ${e.message}")
            }
            _isLoading.value = false
        }
    }

    fun filter(slug: String?) {
        _filtered.value = if (slug == null) _articles.value
        else _articles.value.filter { it.category?.slug == slug }
    }

    // Positive = moved up, negative = moved down, 0 = new/unchanged
    fun rankChange(articleId: String): Int {
        val prev    = previousRanks[articleId] ?: return 0
        val current = (_filtered.value.indexOfFirst { it.id == articleId } + 1)
        return prev - current
    }
}

@Composable
fun TrendingContent(viewModel: TrendingViewModel = viewModel()) {
    val articles   by viewModel.articles.collectAsState()
    val filtered   by viewModel.filtered.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        // Category filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ALL chip
            CategoryChip(
                label    = "ALL",
                isActive = selectedCategory == null,
                color    = AppColors.neon,
                onClick  = { selectedCategory = null; viewModel.filter(null) },
            )
            categories.forEach { cat ->
                val color = categoryAccentColor(cat.slug)
                CategoryChip(
                    label    = cat.name.uppercase(),
                    isActive = selectedCategory == cat.slug,
                    color    = color,
                    onClick  = { selectedCategory = cat.slug; viewModel.filter(cat.slug) },
                )
            }
        }
        HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp)

        when {
            isLoading && articles.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AppColors.neon, modifier = Modifier.size(36.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.height(16.dp))
                        Text("LOADING TRENDING", style = AppTypography.mono10, color = AppColors.textMuted, letterSpacing = AppTypography.trackingXWide)
                    }
                }
            }
            filtered.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = AppColors.textMuted, modifier = Modifier.size(44.dp))
                        Text("No trending stories right now", style = AppTypography.body, color = AppColors.textMuted)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    items(filtered, key = { it.id }) { article ->
                        val rank = filtered.indexOf(article) + 1
                        TrendingRow(
                            article    = article,
                            rank       = rank,
                            rankChange = viewModel.rankChange(article.id),
                            onTap      = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.originalUrl)))
                            },
                        )
                        HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, isActive: Boolean, color: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (isActive) color else color.copy(alpha = 0.12f), label = "chip_bg"
    )
    val textColor by animateColorAsState(
        if (isActive) AppColors.background else color, label = "chip_txt"
    )
    val borderColor by animateColorAsState(
        if (isActive) color else color.copy(alpha = 0.4f), label = "chip_border"
    )
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(100.dp))
            .border(1.dp, borderColor, RoundedCornerShape(100.dp))
            .clip(RoundedCornerShape(100.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text          = label,
            fontFamily    = FontFamily.Monospace,
            fontWeight    = FontWeight.Bold,
            fontSize      = 10.sp,
            letterSpacing = 1.sp,
            color         = textColor,
        )
    }
}

@Composable
private fun TrendingRow(article: Article, rank: Int, rankChange: Int, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.Top,
    ) {
        // Rank badge + change arrow
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (rank <= 3) AppColors.neon.copy(alpha = 0.15f) else AppColors.surface,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (rank <= 3) AppColors.neon.copy(alpha = 0.4f) else AppColors.divider,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "$rank",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize   = if (rank <= 3) 15.sp else 13.sp,
                    color      = if (rank <= 3) AppColors.neon else AppColors.textMuted,
                )
            }
            Spacer(Modifier.height(4.dp))
            // Rank change indicator
            when {
                rankChange > 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(8.dp))
                    Text("$rankChange", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
                rankChange < 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowDownward, null, tint = Color(0xFFF44336), modifier = Modifier.size(8.dp))
                    Text("${-rankChange}", fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                }
                else -> Text("–", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = AppColors.textMuted.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
            }
        }

        // Content
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                article.sourceName?.let {
                    Text(
                        text          = it.uppercase(),
                        fontFamily    = FontFamily.Monospace,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 10.sp,
                        letterSpacing = 1.2.sp,
                        color         = AppColors.neon.copy(alpha = 0.7f),
                    )
                }
                Spacer(Modifier.weight(1f))
                // Read count
                val readCount = article.readCount
                val formatted = if (readCount >= 1000) String.format("%.1fK", readCount / 1000.0) else "$readCount"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RemoveRedEye, null, tint = AppColors.textMuted, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(formatted, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AppColors.textMuted)
                }
                article.publishedAt?.let { pubAt ->
                    Text(
                        text          = " · ${timeAgo(pubAt)}",
                        fontFamily    = FontFamily.Monospace,
                        fontSize      = 10.sp,
                        color         = AppColors.textMuted,
                    )
                }
            }

            Text(
                text       = article.title,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = AppColors.textPrimary,
                lineHeight = 20.sp,
                maxLines   = 2,
            )

            article.category?.let { cat ->
                val color = categoryAccentColor(cat.slug)
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                        .border(0.7.dp, color.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text          = cat.name.uppercase(),
                        fontFamily    = FontFamily.Monospace,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 10.sp,
                        letterSpacing = 1.sp,
                        color         = color,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Search
// ─────────────────────────────────────────────────────────────────────────────

class SearchViewModel : ViewModel() {
    private val _query     = MutableStateFlow("")
    private val _results   = MutableStateFlow<List<Article>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    val query:     StateFlow<String>       = _query.asStateFlow()
    val results:   StateFlow<List<Article>> = _results.asStateFlow()
    val isLoading: StateFlow<Boolean>      = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.isBlank()) { _results.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(350) // debounce 350ms — matches iOS
            _isLoading.value = true
            try {
                val response = APIService.shared.searchArticles(q.trim())
                _results.value = response.data
            } catch (e: Exception) {
                _results.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun clearQuery() { _query.value = ""; _results.value = emptyList() }
}

@Composable
fun SearchContent(viewModel: SearchViewModel = viewModel()) {
    val query     by viewModel.query.collectAsState()
    val results   by viewModel.results.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager   = LocalFocusManager.current
    val context        = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                null,
                tint     = if (query.isEmpty()) AppColors.textMuted else AppColors.neon,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value         = query,
                onValueChange = viewModel::onQueryChange,
                modifier      = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle     = TextStyle(color = AppColors.textPrimary, fontSize = 15.sp),
                cursorBrush   = SolidColor(AppColors.neon),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Search articles, sources, topics...", color = AppColors.textMuted, fontSize = 15.sp)
                    }
                    inner()
                },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearQuery(); focusManager.clearFocus() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Cancel, null, tint = AppColors.textMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
        HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp)

        when {
            query.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.Search, null, tint = AppColors.textMuted, modifier = Modifier.size(44.dp))
                        Text(
                            "Search across all articles,\nsources and topics",
                            style = AppTypography.body, color = AppColors.textMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.neon, modifier = Modifier.size(36.dp), strokeWidth = 2.dp)
                }
            }
            results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.SearchOff, null, tint = AppColors.textMuted, modifier = Modifier.size(44.dp))
                        Text("No results for", style = AppTypography.body, color = AppColors.textMuted)
                        Text("\"$query\"", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = AppColors.textSecondary)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    item {
                        Text(
                            text          = "${results.size} RESULTS",
                            fontFamily    = FontFamily.Monospace,
                            fontWeight    = FontWeight.Bold,
                            fontSize      = 10.sp,
                            letterSpacing = 2.sp,
                            color         = AppColors.textMuted,
                            modifier      = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                    items(results, key = { it.id }) { article ->
                        SearchResultRow(
                            article = article,
                            query   = query,
                            onTap   = {
                                focusManager.clearFocus()
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.originalUrl)))
                            },
                        )
                        HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp, modifier = Modifier.padding(start = 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(article: Article, query: String, onTap: () -> Unit) {
    val color = categoryAccentColor(article.category?.slug)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment     = Alignment.Top,
    ) {
        // Category icon circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.12f), CircleShape)
                .border(1.dp, color.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = categoryIcon(article.category?.slug),
                contentDescription = null,
                tint               = color,
                modifier           = Modifier.size(20.dp),
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                article.sourceName?.let {
                    Text(it.uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp, color = AppColors.neon.copy(alpha = 0.7f))
                }
                Spacer(Modifier.weight(1f))
                article.publishedAt?.let {
                    Text(timeAgo(it), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AppColors.textMuted)
                }
            }

            // Highlighted title — mirrors iOS HighlightedText
            val annotated = buildAnnotatedString {
                val lower  = article.title.lowercase()
                val qLower = query.lowercase()
                var i = 0
                while (i < article.title.length) {
                    val idx = lower.indexOf(qLower, i)
                    if (idx == -1) {
                        withStyle(SpanStyle(color = AppColors.textPrimary)) { append(article.title.substring(i)) }
                        break
                    }
                    withStyle(SpanStyle(color = AppColors.textPrimary)) { append(article.title.substring(i, idx)) }
                    withStyle(SpanStyle(color = AppColors.neon, fontWeight = FontWeight.Bold)) {
                        append(article.title.substring(idx, idx + query.length))
                    }
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
fun SavedContent() {
    val bookmarks by BookmarkStore.bookmarks.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filtered = remember(bookmarks, searchText) {
        if (searchText.isEmpty()) bookmarks
        else {
            val q = searchText.lowercase()
            bookmarks.filter {
                it.title.lowercase().contains(q) ||
                        it.summary.lowercase().contains(q) ||
                        it.sourceName?.lowercase()?.contains(q) == true
            }
        }
    }

    if (bookmarks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(AppSpacing.xl),
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(AppColors.neon.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.BookmarkBorder, null, tint = AppColors.neon, modifier = Modifier.size(44.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NO SAVED ARTICLES", style = AppTypography.label, color = AppColors.neon, letterSpacing = AppTypography.trackingXWide)
                    Text(
                        "Bookmark articles from the feed\nto read them later.",
                        style     = AppTypography.body,
                        color     = AppColors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        item {
            // Search bar for saved
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(AppColors.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, AppColors.divider, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, null, tint = AppColors.textMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value         = searchText,
                    onValueChange = { searchText = it },
                    modifier      = Modifier.weight(1f),
                    textStyle     = TextStyle(color = AppColors.textPrimary, fontSize = 14.sp),
                    cursorBrush   = SolidColor(AppColors.neon),
                    singleLine    = true,
                    decorationBox = { inner ->
                        if (searchText.isEmpty()) Text("Search saved...", color = AppColors.textMuted, fontSize = 14.sp)
                        inner()
                    },
                )
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { searchText = "" }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Cancel, null, tint = AppColors.textMuted, modifier = Modifier.size(14.dp))
                    }
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
                BookmarkRow(
                    article = article,
                    onTap   = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.originalUrl))) },
                    onRemove = { BookmarkStore.remove(context, article.id) },
                )
                HorizontalDivider(color = AppColors.divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun BookmarkRow(article: Article, onTap: () -> Unit, onRemove: () -> Unit) {
    val color = categoryAccentColor(article.category?.slug)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row {
                article.sourceName?.let {
                    Text(it.uppercase(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.2.sp, color = AppColors.neon.copy(alpha = 0.7f))
                }
                Spacer(Modifier.weight(1f))
                article.publishedAt?.let {
                    Text(timeAgo(it), fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = AppColors.textMuted)
                }
            }
            Text(article.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.textPrimary, maxLines = 2, lineHeight = 20.sp)
            article.category?.let { cat ->
                Text(
                    cat.name.uppercase(),
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    fontSize = 10.sp, letterSpacing = 1.sp, color = color,
                )
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Bookmark, null, tint = AppColors.neon, modifier = Modifier.size(18.dp))
        }
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

fun timeAgo(isoDate: String): String {
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = fmt.parse(isoDate) ?: return ""
        val diff = (System.currentTimeMillis() - date.time) / 1000
        when {
            diff < 60     -> "${diff}s ago"
            diff < 3600   -> "${diff / 60}m ago"
            diff < 86400  -> "${diff / 3600}h ago"
            else          -> "${diff / 86400}d ago"
        }
    } catch (e: Exception) { "" }
}