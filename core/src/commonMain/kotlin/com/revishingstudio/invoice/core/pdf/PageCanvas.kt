package com.revishingstudio.invoice.core.pdf

/** The font weights bundled with the app (Inter). */
enum class FontWeight(val fileName: String) {
    REGULAR("Inter-Regular.ttf"),
    MEDIUM("Inter-Medium.ttf"),
    SEMIBOLD("Inter-SemiBold.ttf"),
    BOLD("Inter-Bold.ttf"),
}

/**
 * Font files live in the core module's resources under /fonts. Italic exists
 * only in the regular weight, which is all the invoice uses.
 */
object InvoiceFonts {
    const val RESOURCE_DIR = "/fonts/"
    const val ITALIC_FILE = "Inter-Italic.ttf"

    fun fileFor(weight: FontWeight, italic: Boolean): String =
        if (italic) ITALIC_FILE else weight.fileName

    val allFiles: List<String> = FontWeight.entries.map { it.fileName } + ITALIC_FILE
}

/** Colours as 0xRRGGBB. */
object InvoiceColors {
    const val BLACK = 0x000000
    const val TEXT = 0x000000
    const val MUTED = 0x7F7F7F
    const val ACCENT = 0xFF5400
    const val BORDER = 0xCCCCCC
    const val BORDER_LIGHT = 0xE6E6E6
}

data class TextStyle(
    val size: Float,
    val weight: FontWeight = FontWeight.REGULAR,
    val italic: Boolean = false,
    val color: Int = InvoiceColors.TEXT,
    /** Extra space between letters, as a fraction of the font size (0.1 = 10%). */
    val tracking: Float = 0f,
)

/**
 * A drawing surface for one A4 page, measured in points (1/72 inch) with the
 * origin at the top-left. Android and desktop each provide one.
 */
interface PageCanvas {
    fun textWidth(text: String, style: TextStyle): Float

    /** Draws text with its baseline at [baseline]. */
    fun text(text: String, x: Float, baseline: Float, style: TextStyle)

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, color: Int)

    fun strokeRect(x: Float, y: Float, width: Float, height: Float, strokeWidth: Float, color: Int)

    /**
     * Draws the image at [path] scaled to fit inside the box, keeping its shape,
     * aligned to the box's top-left. Does nothing if the image cannot be read.
     */
    fun image(path: String, x: Float, y: Float, maxWidth: Float, maxHeight: Float)
}

/** A PDF being written. Each platform supplies one. */
interface PdfDocumentWriter {
    fun newPage(): PageCanvas
}

object A4 {
    const val WIDTH = 595f
    const val HEIGHT = 842f
}
