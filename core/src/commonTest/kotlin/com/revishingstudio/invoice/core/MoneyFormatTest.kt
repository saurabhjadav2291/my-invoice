package com.revishingstudio.invoice.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyFormatTest {

    @Test
    fun formatsRupeesWithIndianGrouping() {
        assertEquals("₹12,000.00", MoneyFormat.format(12_000_00, Currency.INR))
        assertEquals("₹1,23,456.78", MoneyFormat.format(1_23_456_78, Currency.INR))
        assertEquals("₹999.05", MoneyFormat.format(999_05, Currency.INR))
        assertEquals("₹0.00", MoneyFormat.format(0, Currency.INR))
    }

    @Test
    fun formatsOtherCurrenciesWithThousands() {
        assertEquals("$123,456.78", MoneyFormat.format(123_456_78, Currency.USD))
        assertEquals("€1,000,000.00", MoneyFormat.format(1_000_000_00, Currency.EUR))
        assertEquals("A$50.00", MoneyFormat.format(50_00, Currency.AUD))
    }

    @Test
    fun parsesTypedAmounts() {
        assertEquals(12_000_00L, MoneyFormat.parseToMinor("12000"))
        assertEquals(12_000_50L, MoneyFormat.parseToMinor("₹ 12,000.5"))
        assertEquals(5L, MoneyFormat.parseToMinor("0.05"))
        assertEquals(50L, MoneyFormat.parseToMinor(".5"))
        assertNull(MoneyFormat.parseToMinor("abc"))
        assertNull(MoneyFormat.parseToMinor("1.234"))
        assertNull(MoneyFormat.parseToMinor("1.2.3"))
    }

    @Test
    fun lineItemAmountRoundsToNearestMinorUnit() {
        // 1,500.00 x 2.5 = 3,750.00
        assertEquals(3_750_00L, LineItem.priced("Design", 1_500_00, 2_500).amountMinor)
        // 33.33 x 0.333 = 11.09889 -> 11.10
        assertEquals(11_10L, LineItem.priced("x", 33_33, 333).amountMinor)
    }
}
