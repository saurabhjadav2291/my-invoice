package com.revishingstudio.invoice.core

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InvoiceNumberingTest {

    @Test
    fun defaultUsesFinancialYear() {
        val scheme = NumberingScheme()
        assertEquals("INV/26-27/001", scheme.format(LocalDate(2026, 11, 6), 1))
        // March belongs to the previous financial year.
        assertEquals("INV/25-26/031", scheme.format(LocalDate(2026, 3, 5), 31))
    }

    @Test
    fun yourCurrentStyleStillWorks() {
        val scheme = NumberingScheme(pattern = "{YY}{SEQ:3}", reset = NumberReset.CALENDAR_YEAR)
        assertEquals("26031", scheme.format(LocalDate(2026, 3, 5), 31))
    }

    @Test
    fun otherPlaceholders() {
        val scheme = NumberingScheme(pattern = "INV-{YYYY}-{FYS}-{SEQ}", prefix = "X")
        assertEquals("INV-2026-2627-7", scheme.format(LocalDate(2026, 4, 1), 7))
    }

    @Test
    fun periodKeysFollowTheResetRule() {
        val march = LocalDate(2026, 3, 31)
        val april = LocalDate(2026, 4, 1)
        val fy = NumberingScheme(reset = NumberReset.FINANCIAL_YEAR)
        assertEquals("FY2025", fy.periodKey(march))
        assertEquals("FY2026", fy.periodKey(april))
        assertEquals("CY2026", NumberingScheme(reset = NumberReset.CALENDAR_YEAR).periodKey(march))
        assertEquals("ALL", NumberingScheme(reset = NumberReset.NEVER).periodKey(march))
    }

    @Test
    fun startAtLetsYouContinueAnExistingSeries() {
        val scheme = NumberingScheme(startAt = 32)
        assertEquals(32, scheme.nextSequence(null))
        assertEquals(32, scheme.nextSequence(5))
        assertEquals(41, scheme.nextSequence(40))
    }

    @Test
    fun patternMustContainSequence() {
        assertFailsWith<IllegalArgumentException> { NumberingScheme(pattern = "{PREFIX}/{FY}") }
    }

    @Test
    fun formatsDatesLikeYourInvoice() {
        assertEquals("5-MAR-2026", InvoiceDates.format(LocalDate(2026, 3, 5)))
        assertEquals("25-26", InvoiceDates.financialYearLabel(LocalDate(2026, 3, 5)))
    }
}
