package com.revishingstudio.invoice.core

/**
 * Currencies an invoice can be raised in. Amounts are always stored in
 * minor units (paise or cents), so every currency here has 100 minor units.
 */
enum class Currency(
    val code: String,
    val symbol: String,
    /** Name used in "amount in words", e.g. "Rupees Twelve Thousand Only". */
    val majorWords: String,
    val minorWords: String,
    /** Indian grouping (1,23,456) and lakh/crore words for INR only. */
    val indianNumbering: Boolean,
) {
    INR("INR", "₹", "Rupees", "Paise", indianNumbering = true),
    USD("USD", "$", "US Dollars", "Cents", indianNumbering = false),
    AUD("AUD", "A$", "Australian Dollars", "Cents", indianNumbering = false),
    EUR("EUR", "€", "Euros", "Cents", indianNumbering = false);

    companion object {
        fun fromCode(code: String): Currency =
            entries.firstOrNull { it.code == code } ?: error("Unknown currency $code")
    }
}
