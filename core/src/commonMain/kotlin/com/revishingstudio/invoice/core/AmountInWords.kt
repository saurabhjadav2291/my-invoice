package com.revishingstudio.invoice.core

/**
 * Writes an amount in words for the "Total Invoice Value (in words)" line.
 *
 * INR uses the Indian system (lakh, crore):
 *   1250000 rupees -> "Rupees Twelve Lakh Fifty Thousand Only"
 * Other currencies use the international system (thousand, million):
 *   1250.50 USD -> "US Dollars One Thousand Two Hundred Fifty and Fifty Cents Only"
 */
object AmountInWords {

    private val ones = listOf(
        "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen",
    )
    private val tens = listOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety",
    )

    fun format(amountMinor: Long, currency: Currency): String {
        require(amountMinor >= 0) { "Amount in words needs a non-negative amount" }
        val major = amountMinor / 100
        val minor = (amountMinor % 100).toInt()
        val majorText = numberToWords(major, currency.indianNumbering)
        return buildString {
            append(currency.majorWords).append(' ').append(majorText)
            if (minor > 0) {
                append(" and ").append(belowHundred(minor)).append(' ').append(currency.minorWords)
            }
            append(" Only")
        }
    }

    fun numberToWords(value: Long, indian: Boolean): String {
        require(value >= 0)
        if (value == 0L) return ones[0]
        return if (indian) indianWords(value) else internationalWords(value)
    }

    private fun indianWords(value: Long): String {
        val parts = mutableListOf<String>()
        var rest = value
        val crore = rest / 10_000_000
        rest %= 10_000_000
        if (crore > 0) {
            // Amounts of 100 crore and above read as "One Hundred Crore" etc.
            parts += indianWords(crore) + " Crore"
        }
        val lakh = rest / 100_000
        rest %= 100_000
        if (lakh > 0) parts += belowHundred(lakh.toInt()) + " Lakh"
        val thousand = rest / 1000
        rest %= 1000
        if (thousand > 0) parts += belowHundred(thousand.toInt()) + " Thousand"
        if (rest > 0) parts += belowThousand(rest.toInt())
        return parts.joinToString(" ")
    }

    private fun internationalWords(value: Long): String {
        val scales = listOf(
            1_000_000_000_000L to "Trillion",
            1_000_000_000L to "Billion",
            1_000_000L to "Million",
            1_000L to "Thousand",
        )
        val parts = mutableListOf<String>()
        var rest = value
        for ((size, name) in scales) {
            val count = rest / size
            if (count > 0) {
                parts += internationalWords(count) + " " + name
                rest %= size
            }
        }
        if (rest > 0) parts += belowThousand(rest.toInt())
        return parts.joinToString(" ")
    }

    private fun belowThousand(n: Int): String {
        require(n in 0..999)
        val hundreds = n / 100
        val rest = n % 100
        return when {
            hundreds == 0 -> belowHundred(rest)
            rest == 0 -> "${ones[hundreds]} Hundred"
            else -> "${ones[hundreds]} Hundred ${belowHundred(rest)}"
        }
    }

    private fun belowHundred(n: Int): String {
        require(n in 0..99)
        return when {
            n < 20 -> ones[n]
            n % 10 == 0 -> tens[n / 10]
            else -> "${tens[n / 10]} ${ones[n % 10]}"
        }
    }
}
