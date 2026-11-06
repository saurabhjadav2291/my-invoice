package com.revishingstudio.invoice.core

import kotlin.math.abs

/** Formats amounts the way they appear on the invoice, e.g. ₹12,000.00 or $1,250.50. */
object MoneyFormat {

    fun format(amountMinor: Long, currency: Currency): String {
        val sign = if (amountMinor < 0) "-" else ""
        return sign + currency.symbol + plain(abs(amountMinor), currency)
    }

    /** The number without a currency symbol: "1,23,456.78" for INR, "123,456.78" otherwise. */
    fun plain(amountMinor: Long, currency: Currency): String {
        val major = amountMinor / 100
        val minor = (amountMinor % 100).toString().padStart(2, '0')
        val grouped = if (currency.indianNumbering) groupIndian(major) else groupInternational(major)
        return "$grouped.$minor"
    }

    /**
     * Parses user input such as "12000", "12,000.5" or "₹ 12,000.50" into minor units.
     * Returns null if the text is not a valid amount.
     */
    fun parseToMinor(text: String): Long? {
        val cleaned = text.filter { it.isDigit() || it == '.' || it == '-' }
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == ".") return null
        val negative = cleaned.startsWith("-")
        val unsigned = cleaned.removePrefix("-")
        if (unsigned.count { it == '.' } > 1 || unsigned.contains('-')) return null
        val parts = unsigned.split('.')
        val majorPart = parts[0].ifEmpty { "0" }
        val minorPart = parts.getOrNull(1) ?: ""
        if (minorPart.length > 2) return null
        val major = majorPart.toLongOrNull() ?: return null
        val minor = minorPart.padEnd(2, '0').toLong()
        val value = major * 100 + minor
        return if (negative) -value else value
    }

    private fun groupInternational(value: Long): String =
        value.toString().reversed().chunked(3).joinToString(",").reversed()

    private fun groupIndian(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val restGrouped = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$restGrouped,$lastThree"
    }
}
