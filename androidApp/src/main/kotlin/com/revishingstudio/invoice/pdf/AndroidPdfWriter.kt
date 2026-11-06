package com.revishingstudio.invoice.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.revishingstudio.invoice.core.pdf.A4
import com.revishingstudio.invoice.core.pdf.InvoiceFonts
import com.revishingstudio.invoice.core.pdf.PageCanvas
import com.revishingstudio.invoice.core.pdf.PdfDocumentWriter
import com.revishingstudio.invoice.core.pdf.TextStyle
import java.io.File
import java.io.OutputStream

/**
 * Writes invoice PDFs on Android with the platform's PdfDocument. Page units are
 * points (1/72 inch), matching the shared layout.
 */
class AndroidPdfWriter(context: Context) : PdfDocumentWriter, AutoCloseable {
    private val document = PdfDocument()
    private val typefaces = TypefaceCache(context)
    private var current: PdfDocument.Page? = null
    private var pageCount = 0

    override fun newPage(): PageCanvas {
        finishCurrent()
        pageCount++
        val info = PdfDocument.PageInfo.Builder(A4.WIDTH.toInt(), A4.HEIGHT.toInt(), pageCount).create()
        val page = document.startPage(info)
        current = page
        return AndroidPage(page.canvas, typefaces)
    }

    fun writeTo(out: OutputStream) {
        finishCurrent()
        document.writeTo(out)
    }

    fun writeTo(file: File) = file.outputStream().use { writeTo(it) }

    override fun close() {
        document.close()
    }

    private fun finishCurrent() {
        current?.let { document.finishPage(it) }
        current = null
    }
}

/**
 * The fonts ship as resources inside the shared core module. Android can only
 * load a Typeface from a file or asset, so each one is copied to the cache once.
 */
private class TypefaceCache(private val context: Context) {
    private val loaded = mutableMapOf<String, Typeface>()

    fun get(style: TextStyle): Typeface {
        val file = InvoiceFonts.fileFor(style.weight, style.italic)
        return loaded.getOrPut(file) {
            val target = File(context.cacheDir, "fonts/$file")
            if (!target.exists()) {
                target.parentFile?.mkdirs()
                val stream = requireNotNull(InvoiceFonts::class.java.getResourceAsStream(InvoiceFonts.RESOURCE_DIR + file)) {
                    "Font $file missing from resources"
                }
                stream.use { input -> target.outputStream().use { input.copyTo(it) } }
            }
            Typeface.createFromFile(target)
        }
    }
}

private class AndroidPage(
    private val canvas: Canvas,
    private val typefaces: TypefaceCache,
) : PageCanvas {
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    private fun paintFor(style: TextStyle): Paint = textPaint.apply {
        typeface = typefaces.get(style)
        textSize = style.size
        color = 0xFF000000.toInt() or style.color
        letterSpacing = style.tracking
    }

    override fun textWidth(text: String, style: TextStyle): Float = paintFor(style).measureText(text)

    override fun text(text: String, x: Float, baseline: Float, style: TextStyle) {
        if (text.isEmpty()) return
        canvas.drawText(text, x, baseline, paintFor(style))
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int) {
        strokePaint.strokeWidth = width
        strokePaint.color = 0xFF000000.toInt() or color
        canvas.drawLine(x1, y1, x2, y2, strokePaint)
    }

    override fun strokeRect(x: Float, y: Float, width: Float, height: Float, strokeWidth: Float, color: Int) {
        strokePaint.strokeWidth = strokeWidth
        strokePaint.color = 0xFF000000.toInt() or color
        canvas.drawRect(x, y, x + width, y + height, strokePaint)
    }

    override fun image(path: String, x: Float, y: Float, maxWidth: Float, maxHeight: Float) {
        val bitmap = BitmapFactory.decodeFile(path) ?: return
        val scale = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height)
        val dest = RectF(x, y, x + bitmap.width * scale, y + bitmap.height * scale)
        canvas.drawBitmap(bitmap, null, dest, Paint(Paint.FILTER_BITMAP_FLAG))
        bitmap.recycle()
    }
}
