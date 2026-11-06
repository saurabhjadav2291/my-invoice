package com.revishingstudio.invoice.core

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentStatusTest {
    private val due = LocalDate(2026, 11, 20)
    private val beforeDue = LocalDate(2026, 11, 10)
    private val afterDue = LocalDate(2026, 11, 21)

    private fun status(state: InvoiceState, paid: Long, today: LocalDate) =
        PaymentStatusRules.statusOf(state, totalMinor = 1000, paidMinor = paid, dueDate = due, today = today)

    @Test
    fun statuses() {
        assertEquals(PaymentStatus.DRAFT, status(InvoiceState.DRAFT, 0, afterDue))
        assertEquals(PaymentStatus.SENT, status(InvoiceState.SENT, 0, beforeDue))
        assertEquals(PaymentStatus.SENT, status(InvoiceState.SENT, 0, due))
        assertEquals(PaymentStatus.PARTLY_PAID, status(InvoiceState.SENT, 400, beforeDue))
        assertEquals(PaymentStatus.OVERDUE, status(InvoiceState.SENT, 0, afterDue))
        assertEquals(PaymentStatus.OVERDUE, status(InvoiceState.SENT, 400, afterDue))
        assertEquals(PaymentStatus.PAID, status(InvoiceState.SENT, 1000, afterDue))
        assertEquals(PaymentStatus.PAID, status(InvoiceState.SENT, 1200, beforeDue))
        assertEquals(PaymentStatus.VOID, status(InvoiceState.VOID, 1000, beforeDue))
    }
}
