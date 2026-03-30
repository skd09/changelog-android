package com.sharvari.changelog.ui.screen.article

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.sharvari.changelog.model.article.Article
import com.sharvari.changelog.service.analytics.AnalyticsManager
import com.sharvari.changelog.store.bookmark.BookmarkStore
import com.sharvari.changelog.ui.theme.AppColors
import com.sharvari.changelog.ui.theme.AppTypography
import java.net.URI

// ─────────────────────────────────────────────────────────────────────────────
// ArticleReaderScreen — matches iOS ArticleReaderView
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ArticleReaderScreen(
    article:   Article,
    onDismiss: () -> Unit,
) {
    var isLoading    by remember { mutableStateOf(true) }
    var progress     by remember { mutableFloatStateOf(0f) }
    var isReaderMode by remember { mutableStateOf(false) }
    var webView      by remember { mutableStateOf<WebView?>(null) }

    val bookmarks    by BookmarkStore.bookmarks.collectAsStateWithLifecycle()
    val isBookmarked  = bookmarks.any { it.id == article.id }
    val context       = LocalContext.current

    val animatedProgress by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(200),
        label         = "progress",
    )

    DisposableEffect(article.id) {
        AnalyticsManager.articleOpened(article)
        onDispose { AnalyticsManager.articleClosed() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.background)
            .statusBarsPadding()
    ) {
        // ── Toolbar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surface)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarButton(Icons.Default.Close) { onDismiss() }

            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                article.sourceName?.let { name ->
                    Text(
                        text          = name.uppercase(),
                        fontFamily    = FontFamily.Monospace,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 10.sp,
                        letterSpacing = 1.5.sp,
                        color         = AppColors.neon,
                        maxLines      = 1,
                        overflow      = TextOverflow.Ellipsis,
                    )
                }
                val host = try {
                    URI(article.originalUrl).host?.removePrefix("www.") ?: ""
                } catch (_: Exception) { "" }
                if (host.isNotEmpty()) {
                    Text(
                        text     = host,
                        fontSize = 11.sp,
                        color    = AppColors.textMuted,
                        maxLines = 1,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                // Reader mode toggle
                ToolbarButton(
                    icon  = Icons.AutoMirrored.Filled.Article,
                    tint  = if (isReaderMode) AppColors.neon else AppColors.textSecondary,
                ) {
                    isReaderMode = !isReaderMode
                    webView?.evaluateJavascript(
                        if (isReaderMode) READER_MODE_ON_JS else READER_MODE_OFF_JS,
                        null,
                    )
                }

                // Bookmark
                ToolbarButton(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    tint = if (isBookmarked) AppColors.neon else AppColors.textSecondary,
                ) {
                    BookmarkStore.toggle(context, article)
                }

                // Share
                ToolbarButton(Icons.Default.Share) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, article.originalUrl)
                    }
                    context.startActivity(Intent.createChooser(intent, article.title))
                }

                // Reload / Stop
                ToolbarButton(
                    icon = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                ) {
                    if (isLoading) webView?.stopLoading() else webView?.reload()
                }
            }
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.divider)
        )

        // ── Progress bar ────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().height(2.dp)) {
            if (isLoading) {
                LinearProgressIndicator(
                    progress  = { animatedProgress },
                    modifier  = Modifier.fillMaxSize(),
                    color     = AppColors.neon,
                    trackColor = AppColors.divider,
                )
            }
        }

        // ── WebView ─────────────────────────────────────────────────────────
        ArticleWebView(
            url             = article.originalUrl,
            isReaderMode    = isReaderMode,
            onProgressChange = { progress = it },
            onLoadingChange  = { isLoading = it },
            onWebViewReady   = { webView = it },
            modifier         = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    tint: Color = AppColors.textSecondary,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = tint,
            modifier           = Modifier.size(18.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WebView composable
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArticleWebView(
    url: String,
    isReaderMode: Boolean,
    onProgressChange: (Float) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onWebViewReady: (WebView) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.setSupportZoom(true)

                setBackgroundColor(android.graphics.Color.parseColor("#0a0a0f"))

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onLoadingChange(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingChange(false)
                        onProgressChange(1f)
                        // Inject base dark CSS
                        view?.evaluateJavascript(BASE_DARK_JS, null)
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgressChange(newProgress / 100f)
                    }
                }

                onWebViewReady(this)
                loadUrl(url)
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Reader mode JavaScript — matches iOS exactly
// ─────────────────────────────────────────────────────────────────────────────

private const val BASE_DARK_JS = """
javascript:(function() {
    var s = document.createElement('style');
    s.id = '__cl_base_style';
    s.innerText = ':root { color-scheme: dark; } body { background-color: #0a0a0f !important; }';
    document.head.appendChild(s);
})();
"""

private const val READER_MODE_ON_JS = """
javascript:(function() {
    var selectors = [
        'header','footer','nav','aside',
        '[class*="sidebar"]','[class*="banner"]',
        '[class*="advertisement"]','[class*="ad-"]',
        '[class*="newsletter"]','[class*="subscribe"]',
        '[class*="related"]','[class*="recommended"]',
        '[class*="comment"]','[class*="social"]',
        '[id*="sidebar"]','[id*="header"]','[id*="footer"]',
        '[id*="nav"]','[id*="ad"]','[id*="cookie"]','[id*="popup"]'
    ];
    selectors.forEach(function(sel) {
        document.querySelectorAll(sel).forEach(function(el) {
            el.style.display = 'none';
        });
    });

    var style = document.createElement('style');
    style.id = '__cl_reader_style';
    style.innerText = 'body { background-color: #0a0a0f !important; color: #e2e8f0 !important; font-family: Georgia, serif !important; font-size: 18px !important; line-height: 1.85 !important; max-width: 680px !important; margin: 0 auto !important; padding: 24px 20px 80px !important; } * { background-color: transparent !important; color: #e2e8f0 !important; border-color: #1e2030 !important; box-shadow: none !important; } h1, h2, h3, h4 { color: #f8fafc !important; font-weight: 700 !important; line-height: 1.3 !important; margin-top: 1.5em !important; } h1 { font-size: 1.6em !important; } h2 { font-size: 1.3em !important; } a { color: #00ff9d !important; text-decoration: none !important; } img { max-width: 100% !important; height: auto !important; border-radius: 8px !important; margin: 16px 0 !important; } p { margin-bottom: 1.2em !important; } blockquote { border-left: 3px solid #00ff9d !important; padding-left: 16px !important; margin-left: 0 !important; color: #94a3b8 !important; font-style: italic !important; } pre, code { background-color: #12131f !important; color: #00ff9d !important; border-radius: 6px !important; padding: 2px 6px !important; font-size: 0.9em !important; }';
    document.head.appendChild(style);
    window.__clReaderActive = true;
})();
"""

private const val READER_MODE_OFF_JS = """
javascript:(function() {
    var style = document.getElementById('__cl_reader_style');
    if (style) style.remove();
    document.querySelectorAll('[style*="display: none"]').forEach(function(el) {
        el.style.display = '';
    });
    window.__clReaderActive = false;
})();
"""
