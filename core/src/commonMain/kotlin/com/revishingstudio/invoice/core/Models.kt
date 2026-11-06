package com.revishingstudio.invoice.core

import kotlinx.datetime.LocalDate

data class BusinessProfile(
    val ownerName: String,
    val studioName: String,
    val address: String,
    val pan: String,
    val email: String = "",
    val phone: String = "",
    val logoPath: String? = null,
    /** Prints "Supplier not registered under GST" on invoices. */
    val showGstNote: Boolean = true,
    /** Prints the due date under the invoice date. */
    val showDueDate: Boolean = true,
    val defaultDueDays: Int = 15,
    val numbering: NumberingScheme = NumberingScheme(),
)

data class BankAccount(
    val id: Long = 0,
    val label: String,
    val bankName: String,
    val accountName: String,
    val accountNumber: String,
    val ifsc: String,
    val swift: String,
    val isDefault: Boolean = false,
)

data class Client(
    val id: Long = 0,
    /** Name in bold on the invoice, e.g. "Beyond Visible". */
    val name: String,
    /** Optional registered name shown next to it, e.g. "YLS Marketing Consultant Pty Ltd". */
    val legalName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    /** Free text such as "ABN: 27685519573  ACN: 685519573" or a GSTIN. */
    val taxIds: String = "",
    val defaultCurrency: Currency = Currency.INR,
    val archived: Boolean = false,
)

data class LineItem(
    val description: String,
    /** Optional. When rate and quantity are both set, amount = rate x quantity. */
    val rateMinor: Long? = null,
    /** Quantity times 1000, so 1.5 hours is 1500. */
    val quantityMilli: Long? = null,
    val amountMinor: Long,
) {
    companion object {
        /** A line with a rate and quantity; the amount is worked out (rounded to the nearest paisa/cent). */
        fun priced(description: String, rateMinor: Long, quantityMilli: Long): LineItem =
            LineItem(description, rateMinor, quantityMilli, amountFor(rateMinor, quantityMilli))

        /** A line with only an amount, like "Advance Payment for Access Embroidery". */
        fun fixed(description: String, amountMinor: Long): LineItem =
            LineItem(description, null, null, amountMinor)

        fun amountFor(rateMinor: Long, quantityMilli: Long): Long {
            val product = rateMinor * quantityMilli
            return if (product >= 0) (product + 500) / 1000 else (product - 500) / 1000
        }
    }
}

enum class PaymentMethod(val label: String) {
    BANK_TRANSFER("Bank transfer"),
    UPI("UPI"),
    PAYPAL("PayPal"),
    WISE("Wise"),
    CASH("Cash"),
    OTHER("Other"),
}

data class Payment(
    val id: Long = 0,
    val receivedOn: LocalDate,
    /** In the invoice's currency. */
    val amountMinor: Long,
    val method: PaymentMethod,
    /** For foreign-currency invoices: the rupees that actually reached your bank. */
    val inrReceivedMinor: Long? = null,
    val note: String = "",
)

/** Everything needed to create or edit an invoice. The number is assigned when it is created. */
data class InvoiceDraft(
    val clientId: Long,
    val currency: Currency,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val items: List<LineItem>,
    val bankAccountId: Long? = null,
    val notes: String = "",
)

data class Invoice(
    val id: Long,
    val number: String,
    val client: Client,
    val bankAccount: BankAccount?,
    val currency: Currency,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val state: InvoiceState,
    val items: List<LineItem>,
    val payments: List<Payment>,
    val notes: String,
) {
    val totalMinor: Long get() = items.sumOf { it.amountMinor }
    val paidMinor: Long get() = payments.sumOf { it.amountMinor }
    val outstandingMinor: Long get() = PaymentStatusRules.outstanding(totalMinor, paidMinor)

    fun status(today: LocalDate): PaymentStatus =
        PaymentStatusRules.statusOf(state, totalMinor, paidMinor, dueDate, today)
}

/** A row in the invoice list. */
data class InvoiceSummary(
    val id: Long,
    val number: String,
    val clientName: String,
    val currency: Currency,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val state: InvoiceState,
    val totalMinor: Long,
    val paidMinor: Long,
) {
    val outstandingMinor: Long get() = PaymentStatusRules.outstanding(totalMinor, paidMinor)

    fun status(today: LocalDate): PaymentStatus =
        PaymentStatusRules.statusOf(state, totalMinor, paidMinor, dueDate, today)
}
