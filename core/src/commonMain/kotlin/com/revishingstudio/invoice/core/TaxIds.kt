package com.revishingstudio.invoice.core

/**
 * Stores a client's custom tax fields in one text column and reads the free-text
 * "Tax IDs" that clients had before the GSTIN field and custom fields existed.
 */
object TaxIds {

    /** One "label<TAB>value" pair per line. Blank pairs are dropped. */
    fun encode(fields: List<TaxField>): String =
        fields
            .map { TaxField(clean(it.label), clean(it.value)) }
            .filter { it.label.isNotEmpty() || it.value.isNotEmpty() }
            .joinToString("\n") { "${it.label}\t${it.value}" }

    fun decode(text: String): List<TaxField> =
        text.lineSequence()
            .filter { it.isNotBlank() }
            .map { line ->
                val tab = line.indexOf('\t')
                if (tab < 0) TaxField("", line.trim()) else TaxField(line.substring(0, tab).trim(), line.substring(tab + 1).trim())
            }
            .toList()

    /** A GSTIN and the other tax fields read from old free text. */
    data class Split(val gstin: String, val fields: List<TaxField>)

    /**
     * Reads old free text such as "ABN: 27685519573  ACN: 685519573", "GSTIN: 27AAPFU0939F1ZV"
     * or a bare GSTIN. A GSTIN goes to its own field; any other "Label: value" becomes a custom
     * field; text without a label is kept as a field called "Tax ID" so nothing is lost.
     */
    fun parseLegacy(text: String): Split {
        val source = text.trim()
        if (source.isEmpty()) return Split("", emptyList())

        var gstin = ""
        val fields = mutableListOf<TaxField>()
        fun add(label: String, rawValue: String) {
            val value = rawValue.trim().trim(',', ';', '|', '/').trim().replace(Regex("""\s+"""), " ")
            if (value.isEmpty()) return
            val isGstLabel = label.uppercase().replace(".", "").replace(" ", "") in GST_LABELS
            if (gstin.isEmpty() && (isGstLabel || (label.isEmpty() && GSTIN.matches(value.uppercase())))) {
                gstin = value
            } else {
                fields += TaxField(label.ifEmpty { "Tax ID" }, value)
            }
        }

        val labels = LABEL.findAll(source).toList()
        if (labels.isEmpty()) {
            // Bare values: a GSTIN on its own, or anything else kept as one field.
            val tokens = source.split(Regex("""[\s,;]+""")).filter { it.isNotEmpty() }
            val gst = tokens.firstOrNull { GSTIN.matches(it.uppercase()) }
            if (gst != null && tokens.size == 1) add("", gst) else add("", source)
            return Split(gstin, fields)
        }
        add("", source.substring(0, labels.first().range.first))
        labels.forEachIndexed { i, match ->
            val end = labels.getOrNull(i + 1)?.range?.first ?: source.length
            add(match.groupValues[1].trim(), source.substring(match.range.last + 1, end))
        }
        return Split(gstin, fields)
    }

    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim()

    /** "ABN:", "VAT No:", "Tax ID:" - letters (and dots) before a colon. */
    private val LABEL = Regex("""([A-Za-z][A-Za-z.]*(?:[ ][A-Za-z.]+)*)\s*:""")

    /** 15-character Indian GST number, e.g. 27AAPFU0939F1ZV. */
    private val GSTIN = Regex("""[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][0-9A-Z]Z[0-9A-Z]""")

    private val GST_LABELS = setOf("GSTIN", "GST", "GSTNO", "GSTNUMBER", "GSTINNO")
}
