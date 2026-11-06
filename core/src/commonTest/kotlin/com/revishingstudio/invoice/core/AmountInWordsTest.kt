package com.revishingstudio.invoice.core

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountInWordsTest {

    @Test
    fun matchesYourExistingInvoice() {
        assertEquals("Rupees Twelve Thousand Only", AmountInWords.format(12_000_00, Currency.INR))
    }

    @Test
    fun indianSystemUsesLakhAndCrore() {
        assertEquals("Rupees One Lakh Only", AmountInWords.format(1_00_000_00, Currency.INR))
        assertEquals(
            "Rupees Twelve Lakh Fifty Thousand Five Hundred One Only",
            AmountInWords.format(12_50_501_00, Currency.INR),
        )
        assertEquals(
            "Rupees Two Crore Three Lakh Only",
            AmountInWords.format(2_03_00_000_00, Currency.INR),
        )
        assertEquals(
            "Rupees One Hundred Twenty Crore Only",
            AmountInWords.format(120_00_00_000_00, Currency.INR),
        )
    }

    @Test
    fun includesPaise() {
        assertEquals("Rupees Ninety Nine and Fifty Paise Only", AmountInWords.format(99_50, Currency.INR))
        assertEquals("Rupees Zero and Five Paise Only", AmountInWords.format(5, Currency.INR))
    }

    @Test
    fun internationalSystemForOtherCurrencies() {
        assertEquals("US Dollars Five Hundred Only", AmountInWords.format(500_00, Currency.USD))
        assertEquals(
            "US Dollars One Thousand Two Hundred Fifty and Twenty Cents Only",
            AmountInWords.format(1_250_20, Currency.USD),
        )
        assertEquals(
            "Euros One Million Two Hundred Thousand Only",
            AmountInWords.format(1_200_000_00, Currency.EUR),
        )
        assertEquals("Australian Dollars Fifteen Only", AmountInWords.format(15_00, Currency.AUD))
    }
}
