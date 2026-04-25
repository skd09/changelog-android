package com.sharvari.changelog.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.sharvari.changelog.model.article.Article
import java.io.File

object ShareCardGenerator {

    private const val WIDTH  = 1200
    private const val HEIGHT = 680
    private const val PAD    = 56

    // Dracula palette
    private const val BG       = 0xFF0D0E14.toInt()
    private const val NEON     = 0xFFBD93F9.toInt()
    private const val MAGENTA  = 0xFFFF79C6.toInt()
    private const val TEXT     = 0xFFF8F8F2.toInt()
    private const val TEXT_DIM = 0xFF6272A4.toInt()

    fun shareArticle(context: Context, article: Article) {
        val bitmap = render(article)
        val shareDir = File(context.cacheDir, "share_cards")
        shareDir.mkdirs()
        val file = File(shareDir, "share_card.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, article.originalUrl)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, article.title))
    }

    private fun render(article: Article): Bitmap {
        val bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Background
        canvas.drawColor(BG)

        // Subtle gradient overlay
        val gradPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), NEON and 0x14FFFFFF, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), gradPaint)

        // Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2f; color = NEON and 0x33FFFFFF.toInt()
        }
        canvas.drawRoundRect(RectF(1f, 1f, WIDTH - 1f, HEIGHT - 1f), 32f, 32f, borderPaint)

        var y = PAD.toFloat()

        // Header: "THE CHANGELOG"
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = NEON; textSize = 22f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.15f
        }
        canvas.drawText("THE CHANGELOG", PAD.toFloat(), y + 22f, headerPaint)

        // Category pill (top right)
        article.category?.let { cat ->
            val catPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = MAGENTA; textSize = 18f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                letterSpacing = 0.1f
            }
            val catText = cat.name.uppercase()
            val catWidth = catPaint.measureText(catText)
            val catX = WIDTH - PAD - catWidth - 16
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MAGENTA and 0x1FFFFFFF.toInt() }
            canvas.drawRoundRect(RectF(catX - 8, y - 2, catX + catWidth + 8, y + 26), 12f, 12f, pillPaint)
            canvas.drawText(catText, catX, y + 20f, catPaint)
        }

        y += 56f

        // Title (multi-line)
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT; textSize = 44f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleLayout = StaticLayout.Builder.obtain(article.title, 0, article.title.length, titlePaint, WIDTH - PAD * 2)
            .setMaxLines(3).setEllipsize(TextUtils.TruncateAt.END)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(PAD.toFloat(), y)
        titleLayout.draw(canvas)
        canvas.restore()
        y += titleLayout.height + 20f

        // Summary (2 lines)
        val summaryPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT_DIM; textSize = 26f; typeface = Typeface.DEFAULT
        }
        val summaryLayout = StaticLayout.Builder.obtain(article.summary, 0, article.summary.length, summaryPaint, WIDTH - PAD * 2)
            .setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        canvas.save()
        canvas.translate(PAD.toFloat(), y)
        summaryLayout.draw(canvas)
        canvas.restore()

        // Footer
        val footerY = HEIGHT - PAD.toFloat()
        article.sourceName?.let { source ->
            val srcPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = TEXT_DIM; textSize = 18f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                letterSpacing = 0.1f
            }
            canvas.drawText(source.uppercase(), PAD.toFloat(), footerY, srcPaint)
        }
        val urlPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = NEON and 0x99FFFFFF.toInt(); textSize = 18f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val urlText = "thechangelog.app"
        canvas.drawText(urlText, WIDTH - PAD - urlPaint.measureText(urlText), footerY, urlPaint)

        return bmp
    }
}
