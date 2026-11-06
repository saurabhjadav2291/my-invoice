package com.revishingstudio.invoice.core

import kotlinx.datetime.LocalDate

/** When the running number goes back to the starting value. */
enum class NumberReset {
    /** Restart every 1 April (the usual practice in India). */
    FINANCIAL_YEAR,
    /** Restart every 1 January. */
    CALENDAR_YEAR,
    /** Never restart. */
    NEVER,
}

/**
 * How invoice numbers are built.
 *
 * The pattern can use these placeholders:
 *  - `{PREFIX}`  your prefix, e.g. RS
 *  - `{FY}`      financial year, e.g. 26-27
 *  - `{FYS}`     financial year without the dash, e.g. 2627
 *  - `{YYYY}`    calendar year, e.g. 2026
 *  - `{YY}`      two-digit calendar year, e.g. 26
 *  - `{SEQ}`     running number; `{SEQ:3}` pads it to three digits (001)
 *
 * Examples:
 *  - `{PREFIX}/{FY}/{SEQ:3}`  ->  RS/26-27/001   (default)
 *  - `{YY}{SEQ:3}`            ->  26031          (your current style)
 */
data class NumberingScheme(
    val pattern: String = DEFAULT_PATTERN,
    val prefix: String = "RS",
    val reset: NumberReset = NumberReset.FINANCIAL_YEAR,
    /** First running number to use in a period with no invoices yet. */
    val startAt: Int = 1,
) {
    init {
        require(SEQ_REGEX.containsMatchIn(pattern)) { "The pattern must contain {SEQ} so numbers stay unique" }
        require(startAt >= 1) { "Numbering must start at 1 or higher" }
    }

    /** Groups invoices that share one running-number series. */
    fun periodKey(date: LocalDate): String = when (reset) {
        NumberReset.FINANCIAL_YEAR -> "FY${InvoiceDates.financialYearStart(date)}"
        NumberReset.CALENDAR_YEAR -> "CY${date.year}"
        NumberReset.NEVER -> "ALL"
    }

    fun format(date: LocalDate, sequence: Int): String {
        val fy = InvoiceDates.financialYearLabel(date)
        var out = pattern
            .replace("{PREFIX}", prefix)
            .replace("{FY}", fy)
            .replace("{FYS}", fy.replace("-", ""))
            .replace("{YYYY}", date.year.toString())
            .replace("{YY}", (date.year % 100).toString().padStart(2, '0'))
        out = SEQ_REGEX.replace(out) { match ->
            val width = match.groupValues[1].toIntOrNull() ?: 0
            sequence.toString().padStart(width, '0')
        }
        return out
    }

    /** The next running number after [highestInPeriod] (null when the period is empty). */
    fun nextSequence(highestInPeriod: Int?): Int =
        if (highestInPeriod == null) startAt else maxOf(highestInPeriod + 1, startAt)

    companion object {
        const val DEFAULT_PATTERN = "{PREFIX}/{FY}/{SEQ:3}"
        private val SEQ_REGEX = Regex("""\{SEQ(?::(\d+))?}""")
    }
}
