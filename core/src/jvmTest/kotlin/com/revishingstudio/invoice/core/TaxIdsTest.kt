package com.revishingstudio.invoice.core

import kotlin.test.Test
import kotlin.test.assertEquals

class TaxIdsTest {

    @Test
    fun encodeDecodeRoundTrip() {
        val fields = listOf(TaxField("ABN", "12 345 678 901"), TaxField("VAT\tNo", "GB 123\n456"), TaxField("", ""))
        val decoded = TaxIds.decode(TaxIds.encode(fields))
        assertEquals(listOf(TaxField("ABN", "12 345 678 901"), TaxField("VAT No", "GB 123 456")), decoded)
    }

    @Test
    fun legacyLabelledIds() {
        val split = TaxIds.parseLegacy("ABN: 12 345 678 901  ACN: 123456789\nGSTIN: 27AAPFU0939F1ZV")
        assertEquals("27AAPFU0939F1ZV", split.gstin)
        assertEquals(listOf(TaxField("ABN", "12 345 678 901"), TaxField("ACN", "123456789")), split.fields)
    }

    @Test
    fun legacyBareGstin() {
        assertEquals(TaxIds.Split("27AAPFU0939F1ZV", emptyList()), TaxIds.parseLegacy(" 27AAPFU0939F1ZV "))
    }

    @Test
    fun legacyUnlabelledTextIsKept() {
        assertEquals(TaxIds.Split("", listOf(TaxField("Tax ID", "12345678901"))), TaxIds.parseLegacy("12345678901"))
    }

    @Test
    fun allTaxIdsPutsGstinFirstAndSkipsBlanks() {
        val client = Client(name = "X", gstin = "27AAPFU0939F1ZV", taxFields = listOf(TaxField("ABN", "1"), TaxField("VAT", " ")))
        assertEquals(listOf(TaxField("GSTIN", "27AAPFU0939F1ZV"), TaxField("ABN", "1")), client.allTaxIds)
    }
}
