package com.revishingstudio.invoice.core.pdf

import com.revishingstudio.invoice.core.AmountInWords
import com.revishingstudio.invoice.core.BusinessProfile
import com.revishingstudio.invoice.core.Invoice
import com.revishingstudio.invoice.core.InvoiceDates
import com.revishingstudio.invoice.core.LineItem
import com.revishingstudio.invoice.core.MoneyFormat

/**
 * Lays out an invoice in the same design as the original hand-made invoices:
 * logo and big INVOICE title, your details in a box, Bill To, the items table,
 * the total in orange, the amount in words, and the bank details box at the
 * bottom of the page.
 *
 * All positions are in points on an A4 page (595 x 842), measured from the original.
 * Long addresses, descriptions and many line items wrap and flow onto extra pages.
 */
class InvoicePdfLayout(
    private val invoice: Invoice,
    private val profile: BusinessProfile,
) {
    private val showDueDate = profile.showDueDate
    private val currency = invoice.currency

    fun render(writer: PdfDocumentWriter) {
        var page = writer.newPage()
        drawHeader(page)
        var y = drawIssuerBox(page, top = 114f)
        y = drawBillTo(page, y + 19.5f)
        y = drawTableHeader(page, y + 40.5f)

        y += 23f
        invoice.items.forEachIndexed { index, item ->
            val rowHeight = rowHeight(page, item)
            if (y + rowHeight > ITEMS_BOTTOM) {
                page = writer.newPage()
                y = drawTableHeader(page, CONTINUED_TOP) + 23f
            }
            drawItemRow(page, item, y)
            y += rowHeight
            if (index == invoice.items.lastIndex) y -= ROW_GAP
        }

        // Totals and amount in words, then the bank box pinned to the page bottom.
        val totalsHeight = totalsBlockHeight(page)
        var totalsTop = maxOf(y + 18f, MIN_TOTALS_TOP)
        if (totalsTop + totalsHeight > BANK_BOX_TOP - 16f) {
            page = writer.newPage()
            totalsTop = CONTINUED_TOP
        }
        drawTotals(page, totalsTop)
        drawBankBox(page)
    }

    // ---- Header ------------------------------------------------------------

    private fun drawHeader(page: PageCanvas) {
        profile.logoPath?.let { page.image(it, LEFT - 5f, 35f, maxWidth = 180f, maxHeight = 64f) }
        page.textRight("INVOICE", RIGHT, 72f, TITLE)
        page.textRight("#${invoice.number}", RIGHT, 96f, NUMBER)
    }

    /** Your name, address, PAN and invoice details. Returns the box's bottom edge. */
    private fun drawIssuerBox(page: PageCanvas, top: Float): Float {
        val innerLeft = LEFT + 12f
        val rightColumn = 309f
        val leftWidth = rightColumn - innerLeft - 12f

        var baseline = top + 24f
        drawLabelled(page, "Name: ", profile.ownerName, innerLeft, baseline, BODY_SEMIBOLD, BODY_SEMIBOLD)

        baseline += 23f
        page.text("Address:", innerLeft, baseline, BODY)
        val addressLines = wrapParagraphs(page, profile.address, BODY, leftWidth)
        addressLines.forEach { line ->
            baseline += LINE
            page.text(line, innerLeft, baseline, BODY)
        }
        val lastLeftBaseline = baseline

        // Right column is bottom-aligned with the address, as in the original.
        val details = buildList {
            add(Triple("PAN: ", profile.pan, BODY_BOLD))
            add(Triple("Invoice Number: ", invoice.number, BODY))
            add(Triple("Invoice Date: ", InvoiceDates.format(invoice.issueDate), BODY))
            if (showDueDate) add(Triple("Due Date: ", InvoiceDates.format(invoice.dueDate), BODY))
        }.filter { it.second.isNotBlank() }
        var rightBaseline = maxOf(lastLeftBaseline - LINE * (details.size - 1), top + 24f)
        details.forEach { (label, value, valueStyle) ->
            drawLabelled(page, label, value, rightColumn, rightBaseline, BODY, valueStyle)
            rightBaseline += LINE
        }

        val bottom = maxOf(lastLeftBaseline, rightBaseline - LINE) + 14f
        page.strokeRect(LEFT, top, RIGHT - LEFT, bottom - top, 1f, InvoiceColors.BORDER)
        return bottom
    }

    /** The client block. [top] is the top of the "BILL TO" label. Returns the y below it. */
    private fun drawBillTo(page: PageCanvas, top: Float): Float {
        val client = invoice.client
        var baseline = top + 6f
        page.text("BILL TO", LEFT, baseline, LABEL)

        baseline += 23f
        val nameWidth = page.textWidth(client.name, CLIENT_NAME)
        page.text(client.name, LEFT, baseline, CLIENT_NAME)
        if (client.legalName.isNotBlank()) {
            page.text(" (${client.legalName})", LEFT + nameWidth, baseline, CLIENT_LEGAL)
        }

        baseline += 7f
        val contentWidth = RIGHT - LEFT
        val lines = buildList {
            if (client.address.isNotBlank()) {
                addAll(wrapParagraphs(page, "Address: " + client.address.replace("\n", ", "), BODY, contentWidth))
            }
            if (client.phone.isNotBlank()) add("Phone: ${client.phone}")
            if (client.email.isNotBlank()) add("Email: ${client.email}")
        }
        lines.forEach { line ->
            baseline += LINE
            page.text(line, LEFT, baseline, BODY)
        }

        if (client.taxIds.isNotBlank()) {
            baseline += 20.5f
            drawTaxIds(page, client.taxIds, baseline)
        }
        return baseline
    }

    /**
     * Tax IDs such as "ABN: 27685519573  ACN: 685519573" are drawn in italics with
     * grey labels and dark numbers, like the original.
     */
    private fun drawTaxIds(page: PageCanvas, text: String, baseline: Float) {
        var x = LEFT
        TAX_ID_TOKEN.findAll(text.replace("\n", "  ")).forEach { match ->
            val token = match.value
            val style = if (token.endsWith(":")) TAX_LABEL else TAX_VALUE
            page.text(token, x, baseline, style)
            x += page.textWidth(token, style)
        }
    }

    // ---- Items table -------------------------------------------------------

    /** Draws the column headings and the rule under them. Returns the rule's y. */
    private fun drawTableHeader(page: PageCanvas, baseline: Float): Float {
        page.text("DESCRIPTION OF ITEM/SERVICES", LEFT, baseline, LABEL)
        page.textCentered("RATE", RATE_CENTER, baseline, LABEL)
        page.textCentered("QTY", QTY_CENTER, baseline, LABEL)
        page.textRight("AMOUNT", RIGHT, baseline, LABEL)
        val ruleY = baseline + 10f
        page.line(LEFT, ruleY, RIGHT, ruleY, 1f, InvoiceColors.BORDER)
        return ruleY
    }

    private fun descriptionLines(page: PageCanvas, item: LineItem): List<String> =
        wrapParagraphs(page, item.description, ITEM, DESCRIPTION_WIDTH)

    private fun rowHeight(page: PageCanvas, item: LineItem): Float =
        (descriptionLines(page, item).size - 1) * ITEM_LINE + ROW_GAP

    /** [baseline] is the first line's baseline. */
    private fun drawItemRow(page: PageCanvas, item: LineItem, baseline: Float) {
        descriptionLines(page, item).forEachIndexed { i, line ->
            page.text(line, LEFT, baseline + i * ITEM_LINE, ITEM)
        }
        val rate = item.rateMinor?.let { MoneyFormat.format(it, currency) } ?: "-"
        val qty = item.quantityMilli?.let(::formatQuantity) ?: "-"
        page.textCentered(rate, RATE_CENTER, baseline, ITEM)
        page.textCentered(qty, QTY_CENTER, baseline, ITEM)
        page.textRight(MoneyFormat.format(item.amountMinor, currency), RIGHT, baseline, ITEM_AMOUNT)
    }

    // ---- Totals --------------------------------------------------------------

    private fun wordsLines(page: PageCanvas): List<String> {
        val words = AmountInWords.format(invoice.totalMinor, currency)
        return wrapText(page, "Total Invoice Value (in words): $words", BODY, RIGHT - LEFT)
    }

    private fun noteLines(page: PageCanvas): List<String> = buildList {
        if (invoice.notes.isNotBlank()) addAll(wrapParagraphs(page, invoice.notes, SMALL, RIGHT - LEFT))
        if (profile.showGstNote) add("Supplier not registered under GST.")
    }

    private fun totalsBlockHeight(page: PageCanvas): Float {
        val notes = noteLines(page)
        return 80f + wordsLines(page).size * LINE + if (notes.isEmpty()) 0f else 10f + notes.size * SMALL_LINE
    }

    /** [top] is where the heavy black rule goes. */
    private fun drawTotals(page: PageCanvas, top: Float) {
        page.line(LEFT, top, RIGHT, top, 1f, InvoiceColors.BLACK)

        val totalBaseline = top + 26f
        val total = MoneyFormat.format(invoice.totalMinor, currency)
        val totalWidth = page.textWidth(total, TOTAL_VALUE)
        // Drawn left to right so copied text reads naturally.
        page.textRight("Total Amount:", RIGHT - maxOf(totalWidth, 81f) - 22f, totalBaseline, TOTAL_LABEL)
        page.textRight(total, RIGHT, totalBaseline, TOTAL_VALUE)

        var baseline = totalBaseline + 40f
        page.text("Total Invoice Value (in figures) : $total", LEFT, baseline, BODY)
        wordsLines(page).forEach { line ->
            baseline += LINE
            page.text(line, LEFT, baseline, BODY)
        }

        val notes = noteLines(page)
        if (notes.isNotEmpty()) baseline += 10f
        notes.forEach { line ->
            baseline += SMALL_LINE
            page.text(line, LEFT, baseline, SMALL)
        }
    }

    // ---- Bank box ------------------------------------------------------------

    private fun drawBankBox(page: PageCanvas) {
        val bank = invoice.bankAccount
        val top = BANK_BOX_TOP
        page.strokeRect(LEFT, top, RIGHT - LEFT, BANK_BOX_HEIGHT, 1f, InvoiceColors.BORDER_LIGHT)

        val innerLeft = LEFT + 12f
        val valueX = LEFT + 102f
        if (bank != null) {
            page.text("BANK ACCOUNT INFO.", innerLeft, top + 21f, BANK_HEADING)
            val rows = listOf(
                "Bank name:" to bank.bankName,
                "AC. Name:" to bank.accountName,
                "Account No:" to bank.accountNumber,
                "IFSC Code:" to bank.ifsc,
                "SWIFT Code:" to bank.swift,
            ).filter { it.second.isNotBlank() }
            var baseline = top + 45.5f
            rows.forEach { (label, value) ->
                page.text(label, innerLeft, baseline, BANK_LABEL)
                page.text(value, valueX, baseline, BANK_VALUE)
                baseline += 15.9f
            }
        }

        val thanksRight = RIGHT - 12f
        page.textRight("Thank You!", thanksRight, top + 91.5f, THANKS)
        val studio = profile.studioName.ifBlank { profile.ownerName }
        val studioWidth = page.textWidth(studio, SIGN_OFF_BOLD)
        page.textRight("from ", thanksRight - studioWidth, top + 104.5f, SIGN_OFF)
        page.textRight(studio, thanksRight, top + 104.5f, SIGN_OFF_BOLD)
    }

    // ---- Helpers -------------------------------------------------------------

    private fun drawLabelled(
        page: PageCanvas,
        label: String,
        value: String,
        x: Float,
        baseline: Float,
        labelStyle: TextStyle,
        valueStyle: TextStyle,
    ) {
        page.text(label, x, baseline, labelStyle)
        page.text(value, x + page.textWidth(label, labelStyle), baseline, valueStyle)
    }

    private fun formatQuantity(milli: Long): String {
        val whole = milli / 1000
        val fraction = (milli % 1000).toString().padStart(3, '0').trimEnd('0')
        return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
    }

    companion object {
        const val LEFT = 40f
        const val RIGHT = 555f
        private const val LINE = 15f
        private const val ITEM_LINE = 14f
        private const val ROW_GAP = 26f
        private const val SMALL_LINE = 13f
        private const val RATE_CENTER = 379.5f
        private const val QTY_CENTER = 441f
        private const val DESCRIPTION_WIDTH = 290f
        private const val CONTINUED_TOP = 60f
        private const val MIN_TOTALS_TOP = 448f
        private const val BANK_BOX_TOP = 679f
        private const val BANK_BOX_HEIGHT = 122f
        private const val ITEMS_BOTTOM = BANK_BOX_TOP - 30f

        private val TAX_ID_TOKEN = Regex("""\S+|\s+""")

        private val TITLE = TextStyle(30f, FontWeight.BOLD)
        private val NUMBER = TextStyle(18.5f, FontWeight.REGULAR)
        private val BODY = TextStyle(12f, tracking = -0.012f)
        private val BODY_SEMIBOLD = TextStyle(12f, FontWeight.SEMIBOLD)
        private val BODY_BOLD = TextStyle(12f, FontWeight.BOLD)
        private val SMALL = TextStyle(9.5f, color = InvoiceColors.MUTED)
        private val LABEL = TextStyle(9f, FontWeight.MEDIUM, color = InvoiceColors.MUTED, tracking = 0.1f)
        private val CLIENT_NAME = TextStyle(15f, FontWeight.BOLD)
        private val CLIENT_LEGAL = TextStyle(15f)
        private val TAX_LABEL = TextStyle(12f, italic = true, color = InvoiceColors.MUTED)
        private val TAX_VALUE = TextStyle(12f, italic = true)
        private val ITEM = TextStyle(10.5f, FontWeight.MEDIUM)
        private val ITEM_AMOUNT = TextStyle(10.5f, FontWeight.BOLD)
        private val TOTAL_LABEL = TextStyle(13.8f, FontWeight.SEMIBOLD)
        private val TOTAL_VALUE = TextStyle(14.5f, FontWeight.BOLD, color = InvoiceColors.ACCENT)
        private val BANK_HEADING = TextStyle(9f, color = InvoiceColors.BLACK, tracking = 0.09f)
        private val BANK_LABEL = TextStyle(10f)
        private val BANK_VALUE = TextStyle(9.8f, FontWeight.SEMIBOLD)
        private val THANKS = TextStyle(11.7f)
        private val SIGN_OFF = TextStyle(10f)
        private val SIGN_OFF_BOLD = TextStyle(10f, FontWeight.SEMIBOLD)
    }
}

internal fun PageCanvas.textRight(text: String, right: Float, baseline: Float, style: TextStyle) =
    text(text, right - textWidth(text, style), baseline, style)

internal fun PageCanvas.textCentered(text: String, center: Float, baseline: Float, style: TextStyle) =
    text(text, center - textWidth(text, style) / 2f, baseline, style)

/** Wraps each paragraph (separated by newlines) to [maxWidth]. Blank paragraphs are dropped. */
internal fun wrapParagraphs(page: PageCanvas, text: String, style: TextStyle, maxWidth: Float): List<String> =
    text.lines().map { it.trim() }.filter { it.isNotEmpty() }.flatMap { wrapText(page, it, style, maxWidth) }

/** Greedy word wrap. A single word wider than the line is broken by characters. */
internal fun wrapText(page: PageCanvas, text: String, style: TextStyle, maxWidth: Float): List<String> {
    val words = text.split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return listOf("")
    val lines = mutableListOf<String>()
    var current = ""
    for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (page.textWidth(candidate, style) <= maxWidth) {
            current = candidate
            continue
        }
        if (current.isNotEmpty()) lines += current
        current = word
        while (page.textWidth(current, style) > maxWidth && current.length > 1) {
            var cut = current.length - 1
            while (cut > 1 && page.textWidth(current.take(cut), style) > maxWidth) cut--
            lines += current.take(cut)
            current = current.drop(cut)
        }
    }
    if (current.isNotEmpty()) lines += current
    return lines
}
