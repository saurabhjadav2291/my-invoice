package com.revishingstudio.invoice.core.pdf

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.io.File
import java.io.OutputStream

/**
 * Writes invoice PDFs with Apache PDFBox and the bundled Geist fonts. Used by
 * tests to check the layout; a macOS app can move it into its own code later.
 */
class PdfBoxWriter : PdfDocumentWriter, AutoCloseable {
    private val document = PDDocument()
    private val fonts = mutableMapOf<String, PDFont>()
    private var current: PdfBoxPage? = null

    override fun newPage(): PageCanvas {
        current?.close()
        val page = PDPage(PDRectangle(A4.WIDTH, A4.HEIGHT))
        document.addPage(page)
        return PdfBoxPage(PDPageContentStream(document, page), ::font, document).also { current = it }
    }

    fun save(out: OutputStream) {
        current?.close()
        current = null
        document.save(out)
    }

    fun save(file: File) = file.outputStream().use { save(it) }

    override fun close() {
        current?.close()
        document.close()
    }

    private fun font(style: TextStyle): PDFont {
        val file = InvoiceFonts.fileFor(style.weight, style.italic)
        return fonts.getOrPut(file) {
            val stream = requireNotNull(javaClass.getResourceAsStream(InvoiceFonts.RESOURCE_DIR + file)) {
                "Font $file missing from resources"
            }
            stream.use { PDType0Font.load(document, it, true) }
        }
    }
}

private class PdfBoxPage(
    private val stream: PDPageContentStream,
    private val fontFor: (TextStyle) -> PDFont,
    private val document: PDDocument,
) : PageCanvas {
    private var closed = false

    override fun textWidth(text: String, style: TextStyle): Float {
        val glyphs = fontFor(style).getStringWidth(text) / 1000f * style.size
        return glyphs + style.tracking * style.size * text.length
    }

    override fun text(text: String, x: Float, baseline: Float, style: TextStyle) {
        if (text.isEmpty()) return
        stream.beginText()
        stream.setFont(fontFor(style), style.size)
        stream.setNonStrokingColor(rgb(style.color))
        stream.setCharacterSpacing(style.tracking * style.size)
        stream.newLineAtOffset(x, A4.HEIGHT - baseline)
        stream.showText(text)
        stream.endText()
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int) {
        stream.setStrokingColor(rgb(color))
        stream.setLineWidth(width)
        stream.moveTo(x1, A4.HEIGHT - y1)
        stream.lineTo(x2, A4.HEIGHT - y2)
        stream.stroke()
    }

    override fun strokeRect(x: Float, y: Float, width: Float, height: Float, strokeWidth: Float, color: Int) {
        stream.setStrokingColor(rgb(color))
        stream.setLineWidth(strokeWidth)
        stream.addRect(x, A4.HEIGHT - y - height, width, height)
        stream.stroke()
    }

    override fun image(path: String, x: Float, y: Float, maxWidth: Float, maxHeight: Float) {
        val image = runCatching { PDImageXObject.createFromFile(path, document) }.getOrNull() ?: return
        val scale = minOf(maxWidth / image.width, maxHeight / image.height)
        val w = image.width * scale
        val h = image.height * scale
        stream.drawImage(image, x, A4.HEIGHT - y - h, w, h)
    }

    fun close() {
        if (!closed) {
            stream.close()
            closed = true
        }
    }

    private fun rgb(color: Int) = java.awt.Color(color)
}
