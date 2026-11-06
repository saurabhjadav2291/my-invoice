package com.revishingstudio.invoice.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.number

object InvoiceDates {

    /** The date style used on your invoices: 5-MAR-2026. */
    fun format(date: LocalDate): String {
        val month = date.month.name.take(3)
        return "${date.day}-$month-${date.year}"
    }

    /**
     * Indian financial year (April to March) that the date falls in, as its
     * starting year. 5 March 2026 is in FY 2025-26, so this returns 2025.
     */
    fun financialYearStart(date: LocalDate): Int =
        if (date.month.number >= Month.APRIL.number) date.year else date.year - 1

    /** "25-26" for FY 2025-26. */
    fun financialYearLabel(date: LocalDate): String {
        val start = financialYearStart(date)
        val end = start + 1
        return "${(start % 100).toString().padStart(2, '0')}-${(end % 100).toString().padStart(2, '0')}"
    }
}
