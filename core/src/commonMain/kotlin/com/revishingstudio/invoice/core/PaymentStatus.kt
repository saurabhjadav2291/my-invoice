package com.revishingstudio.invoice.core

import kotlinx.datetime.LocalDate

/** What you have done with an invoice. Payment progress is worked out separately. */
enum class InvoiceState {
    DRAFT,
    SENT,
    /** Cancelled. Kept so the number is not reused, but excluded from totals. */
    VOID,
}

/** The status shown in the invoice list. */
enum class PaymentStatus(val label: String) {
    DRAFT("Draft"),
    SENT("Sent"),
    PARTLY_PAID("Partly paid"),
    PAID("Paid"),
    OVERDUE("Overdue"),
    VOID("Void"),
}

object PaymentStatusRules {

    fun statusOf(
        state: InvoiceState,
        totalMinor: Long,
        paidMinor: Long,
        dueDate: LocalDate,
        today: LocalDate,
    ): PaymentStatus = when {
        state == InvoiceState.VOID -> PaymentStatus.VOID
        totalMinor > 0 && paidMinor >= totalMinor -> PaymentStatus.PAID
        state == InvoiceState.DRAFT -> PaymentStatus.DRAFT
        today > dueDate -> PaymentStatus.OVERDUE
        paidMinor > 0 -> PaymentStatus.PARTLY_PAID
        else -> PaymentStatus.SENT
    }

    fun outstanding(totalMinor: Long, paidMinor: Long): Long = maxOf(totalMinor - paidMinor, 0)
}
