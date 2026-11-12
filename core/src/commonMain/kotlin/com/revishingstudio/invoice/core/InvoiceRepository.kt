package com.revishingstudio.invoice.core

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.db.SqlDriver
import com.revishingstudio.invoice.db.InvoiceDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Clock
import com.revishingstudio.invoice.db.Bank_account as BankAccountRow
import com.revishingstudio.invoice.db.Business_profile as ProfileRow
import com.revishingstudio.invoice.db.Client as ClientRow

/**
 * The single place the app reads and writes invoice data.
 *
 * Each platform supplies its own SQLite [SqlDriver]: the Android driver on
 * phones, the JDBC driver in tests and (later) on macOS.
 */
class InvoiceRepository(
    driver: SqlDriver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.System,
) {
    private val db = InvoiceDatabase(driver)
    private val profiles = db.profileQueries
    private val clients = db.clientQueries
    private val invoices = db.invoiceQueries

    // ---- Business profile ------------------------------------------------

    fun profile(): BusinessProfile? = profiles.getProfile().executeAsOneOrNull()?.toModel()

    fun observeProfile(): Flow<BusinessProfile?> =
        profiles.getProfile().asFlow().mapToOneOrNull(dispatcher).map { it?.toModel() }

    fun saveProfile(profile: BusinessProfile) {
        val n = profile.numbering
        profiles.upsertProfile(
            owner_name = profile.ownerName,
            studio_name = profile.studioName,
            address = profile.address,
            pan = profile.pan,
            email = profile.email,
            phone = profile.phone,
            logo_path = profile.logoPath,
            show_gst_note = profile.showGstNote.toLong(),
            show_due_date = profile.showDueDate.toLong(),
            default_due_days = profile.defaultDueDays.toLong(),
            number_pattern = n.pattern,
            number_prefix = n.prefix,
            number_reset = n.reset.name,
            number_start = n.startAt.toLong(),
        )
    }

    // ---- Bank accounts ---------------------------------------------------

    fun bankAccounts(): List<BankAccount> = profiles.allBankAccounts().executeAsList().map { it.toModel() }

    fun observeBankAccounts(): Flow<List<BankAccount>> =
        profiles.allBankAccounts().asFlow().mapToList(dispatcher).map { rows -> rows.map { it.toModel() } }

    /** Adds or updates an account and returns its id. The first account becomes the default. */
    fun saveBankAccount(account: BankAccount): Long = profiles.transactionWithResult {
        val makeDefault = account.isDefault || profiles.allBankAccounts().executeAsList().none { it.id != account.id }
        if (makeDefault) profiles.clearDefaultBankAccount()
        if (account.id == 0L) {
            profiles.insertBankAccount(
                account.label, account.bankName, account.accountName, account.accountNumber,
                account.ifsc, account.swift, makeDefault.toLong(),
            )
            profiles.lastInsertRowId().executeAsOne()
        } else {
            profiles.updateBankAccount(
                account.label, account.bankName, account.accountName, account.accountNumber,
                account.ifsc, account.swift, makeDefault.toLong(), account.id,
            )
            account.id
        }
    }

    fun deleteBankAccount(id: Long) = profiles.deleteBankAccount(id)

    // ---- Clients ---------------------------------------------------------

    fun clients(): List<Client> = clients.activeClients().executeAsList().map { it.toModel() }

    fun observeClients(): Flow<List<Client>> =
        clients.activeClients().asFlow().mapToList(dispatcher).map { rows -> rows.map { it.toModel() } }

    fun client(id: Long): Client? = clients.clientById(id).executeAsOneOrNull()?.toModel()

    /** Adds or updates a client and returns its id. */
    fun saveClient(client: Client): Long = clients.transactionWithResult {
        if (client.id == 0L) {
            clients.insertClient(
                client.name, client.legalName, client.address, client.phone,
                client.email, client.gstin.trim(), TaxIds.encode(client.taxFields), client.defaultCurrency.code,
            )
            profiles.lastInsertRowId().executeAsOne()
        } else {
            clients.updateClient(
                client.name, client.legalName, client.address, client.phone,
                client.email, client.gstin.trim(), TaxIds.encode(client.taxFields), client.defaultCurrency.code, client.id,
            )
            client.id
        }
    }

    /** Clients with invoices are archived rather than deleted, so old invoices stay intact. */
    fun archiveClient(id: Long, archived: Boolean = true) = clients.setClientArchived(archived.toLong(), id)

    // ---- Invoices --------------------------------------------------------

    /** The number the next invoice dated [issueDate] will get. */
    fun previewNextNumber(issueDate: LocalDate): String {
        val scheme = numberingScheme()
        return scheme.format(issueDate, nextSequence(scheme, issueDate))
    }

    /** Creates an invoice as a draft, assigns it the next number, and returns its id. */
    fun createInvoice(draft: InvoiceDraft): Long = invoices.transactionWithResult {
        validate(draft)
        val scheme = numberingScheme()
        val periodKey = scheme.periodKey(draft.issueDate)
        var sequence = nextSequence(scheme, draft.issueDate)
        var number = scheme.format(draft.issueDate, sequence)
        // Guard against clashes, e.g. after changing the pattern mid-year.
        while (invoices.numberExists(number).executeAsOne() > 0) {
            sequence++
            number = scheme.format(draft.issueDate, sequence)
        }
        val bankId = draft.bankAccountId ?: bankAccounts().firstOrNull { it.isDefault }?.id
        invoices.insertInvoice(
            number = number,
            period_key = periodKey,
            sequence = sequence.toLong(),
            client_id = draft.clientId,
            bank_account_id = bankId,
            currency = draft.currency.code,
            issue_date = draft.issueDate.toString(),
            due_date = draft.dueDate.toString(),
            state = InvoiceState.DRAFT.name,
            notes = draft.notes,
            created_at = clock.now().toEpochMilliseconds(),
        )
        val id = profiles.lastInsertRowId().executeAsOne()
        insertItems(id, draft.items)
        id
    }

    /** Replaces an invoice's details and line items. Its number does not change. */
    fun updateInvoice(id: Long, draft: InvoiceDraft) = invoices.transaction {
        validate(draft)
        requireNotNull(invoices.invoiceById(id).executeAsOneOrNull()) { "Invoice $id not found" }
        invoices.updateInvoice(
            client_id = draft.clientId,
            bank_account_id = draft.bankAccountId,
            currency = draft.currency.code,
            issue_date = draft.issueDate.toString(),
            due_date = draft.dueDate.toString(),
            notes = draft.notes,
            id = id,
        )
        invoices.deleteLineItemsForInvoice(id)
        insertItems(id, draft.items)
    }

    fun markSent(id: Long) = invoices.setInvoiceState(InvoiceState.SENT.name, id)

    fun markDraft(id: Long) = invoices.setInvoiceState(InvoiceState.DRAFT.name, id)

    /** Cancels an invoice but keeps its number so it is never reused. */
    fun void(id: Long) = invoices.setInvoiceState(InvoiceState.VOID.name, id)

    /** Only drafts can be deleted; sent invoices should be voided instead. */
    fun deleteDraft(id: Long) = invoices.transaction {
        val row = requireNotNull(invoices.invoiceById(id).executeAsOneOrNull()) { "Invoice $id not found" }
        require(row.state == InvoiceState.DRAFT.name) { "Only draft invoices can be deleted. Void it instead." }
        // Deleted explicitly so this works even where SQLite foreign keys are switched off.
        invoices.deletePaymentsForInvoice(id)
        invoices.deleteLineItemsForInvoice(id)
        invoices.deleteInvoice(id)
    }

    fun invoice(id: Long): Invoice? {
        val row = invoices.invoiceById(id).executeAsOneOrNull() ?: return null
        val client = requireNotNull(client(row.client_id)) { "Client ${row.client_id} missing" }
        val bank = row.bank_account_id?.let { profiles.bankAccountById(it).executeAsOneOrNull()?.toModel() }
        val items = invoices.lineItemsForInvoice(id).executeAsList().map {
            LineItem(it.description, it.rate_minor, it.quantity_milli, it.amount_minor)
        }
        val payments = invoices.paymentsForInvoice(id).executeAsList().map {
            Payment(
                id = it.id,
                receivedOn = LocalDate.parse(it.received_on),
                amountMinor = it.amount_minor,
                method = PaymentMethod.valueOf(it.method),
                inrReceivedMinor = it.inr_received_minor,
                note = it.note,
            )
        }
        return Invoice(
            id = row.id,
            number = row.number,
            client = client,
            bankAccount = bank,
            currency = Currency.fromCode(row.currency),
            issueDate = LocalDate.parse(row.issue_date),
            dueDate = LocalDate.parse(row.due_date),
            state = InvoiceState.valueOf(row.state),
            items = items,
            payments = payments,
            notes = row.notes,
        )
    }

    fun invoiceSummaries(): List<InvoiceSummary> =
        invoices.invoiceSummaries(::summary).executeAsList()

    fun observeInvoiceSummaries(): Flow<List<InvoiceSummary>> =
        invoices.invoiceSummaries(::summary).asFlow().mapToList(dispatcher)

    // ---- Payments --------------------------------------------------------

    /** Records money received against an invoice. Part payments simply add up. */
    fun recordPayment(invoiceId: Long, payment: Payment): Long = invoices.transactionWithResult {
        require(payment.amountMinor > 0) { "Payment amount must be more than zero" }
        val row = requireNotNull(invoices.invoiceById(invoiceId).executeAsOneOrNull()) { "Invoice $invoiceId not found" }
        require(row.state != InvoiceState.VOID.name) { "Cannot record a payment on a void invoice" }
        invoices.insertPayment(
            invoice_id = invoiceId,
            received_on = payment.receivedOn.toString(),
            amount_minor = payment.amountMinor,
            method = payment.method.name,
            inr_received_minor = payment.inrReceivedMinor,
            note = payment.note,
        )
        // Receiving money means the invoice has clearly been sent.
        if (row.state == InvoiceState.DRAFT.name) invoices.setInvoiceState(InvoiceState.SENT.name, invoiceId)
        profiles.lastInsertRowId().executeAsOne()
    }

    fun deletePayment(paymentId: Long) = invoices.deletePayment(paymentId)

    /** Total still owed per currency, across sent invoices that are not void. */
    fun outstandingByCurrency(): Map<Currency, Long> =
        invoiceSummaries()
            .filter { it.state == InvoiceState.SENT }
            .groupBy { it.currency }
            .mapValues { (_, list) -> list.sumOf { it.outstandingMinor } }
            .filterValues { it > 0 }

    // ---- Helpers ---------------------------------------------------------

    private fun numberingScheme(): NumberingScheme = profile()?.numbering ?: NumberingScheme()

    private fun nextSequence(scheme: NumberingScheme, date: LocalDate): Int {
        val highest = invoices.maxSequenceInPeriod(scheme.periodKey(date)).executeAsOne().max_seq
        return scheme.nextSequence(highest?.toInt())
    }

    private fun validate(draft: InvoiceDraft) {
        require(draft.items.isNotEmpty()) { "An invoice needs at least one line item" }
        require(draft.items.all { it.description.isNotBlank() }) { "Every line item needs a description" }
        require(draft.dueDate >= draft.issueDate) { "Due date cannot be before the invoice date" }
        requireNotNull(clients.clientById(draft.clientId).executeAsOneOrNull()) { "Client ${draft.clientId} not found" }
    }

    private fun insertItems(invoiceId: Long, items: List<LineItem>) {
        items.forEachIndexed { index, item ->
            invoices.insertLineItem(
                invoice_id = invoiceId,
                position = index.toLong(),
                description = item.description.trim(),
                rate_minor = item.rateMinor,
                quantity_milli = item.quantityMilli,
                amount_minor = item.amountMinor,
            )
        }
    }

    private fun summary(
        id: Long,
        number: String,
        currency: String,
        issueDate: String,
        dueDate: String,
        state: String,
        clientName: String,
        totalMinor: Long,
        paidMinor: Long,
    ) = InvoiceSummary(
        id = id,
        number = number,
        clientName = clientName,
        currency = Currency.fromCode(currency),
        issueDate = LocalDate.parse(issueDate),
        dueDate = LocalDate.parse(dueDate),
        state = InvoiceState.valueOf(state),
        totalMinor = totalMinor,
        paidMinor = paidMinor,
    )

    companion object {
        /** Creates the tables on a brand-new database. Platform drivers usually do this for you. */
        fun createSchema(driver: SqlDriver) {
            InvoiceDatabase.Schema.create(driver)
        }
    }
}

private fun Boolean.toLong(): Long = if (this) 1L else 0L

private fun ProfileRow.toModel() = BusinessProfile(
    ownerName = owner_name,
    studioName = studio_name,
    address = address,
    pan = pan,
    email = email,
    phone = phone,
    logoPath = logo_path,
    showGstNote = show_gst_note != 0L,
    showDueDate = show_due_date != 0L,
    defaultDueDays = default_due_days.toInt(),
    numbering = NumberingScheme(
        pattern = number_pattern,
        prefix = number_prefix,
        reset = NumberReset.valueOf(number_reset),
        startAt = number_start.toInt(),
    ),
)

private fun BankAccountRow.toModel() = BankAccount(
    id = id,
    label = label,
    bankName = bank_name,
    accountName = account_name,
    accountNumber = account_number,
    ifsc = ifsc,
    swift = swift,
    isDefault = is_default != 0L,
)

private fun ClientRow.toModel(): Client {
    // Clients saved before version 3 only have free-text tax_ids; read them into the new
    // fields. Saving the client writes the new columns and clears tax_ids.
    val legacy = if (tax_ids.isNotBlank()) TaxIds.parseLegacy(tax_ids) else null
    return Client(
        id = id,
        name = name,
        legalName = legal_name,
        address = address,
        phone = phone,
        email = email,
        gstin = gstin.ifBlank { legacy?.gstin.orEmpty() },
        taxFields = TaxIds.decode(tax_fields) + legacy?.fields.orEmpty(),
        defaultCurrency = Currency.fromCode(default_currency),
        archived = archived != 0L,
    )
}
