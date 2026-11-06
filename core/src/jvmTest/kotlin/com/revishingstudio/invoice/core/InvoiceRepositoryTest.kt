package com.revishingstudio.invoice.core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InvoiceRepositoryTest {
    private lateinit var repo: InvoiceRepository
    private var clientId = 0L

    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        InvoiceRepository.createSchema(driver)
        repo = InvoiceRepository(driver, Dispatchers.Unconfined)
        repo.saveProfile(
            BusinessProfile(
                ownerName = "Test Owner",
                studioName = "Test Studio",
                address = "1 Test Street",
                pan = "AAAAA0000A",
            ),
        )
        clientId = repo.saveClient(
            Client(name = "Acme", legalName = "Acme Pty Ltd", defaultCurrency = Currency.AUD),
        )
    }

    private fun draft(
        date: LocalDate = LocalDate(2026, 11, 6),
        items: List<LineItem> = listOf(LineItem.fixed("Advance payment", 12_000_00)),
        currency: Currency = Currency.INR,
    ) = InvoiceDraft(clientId, currency, date, date.plusDays(15), items)

    private fun LocalDate.plusDays(n: Int) =
        LocalDate.fromEpochDays(toEpochDays() + n)

    @Test
    fun numbersRunInSequenceAndRestartEachFinancialYear() {
        val a = repo.invoice(repo.createInvoice(draft(LocalDate(2026, 3, 5))))!!
        val b = repo.invoice(repo.createInvoice(draft(LocalDate(2026, 3, 20))))!!
        val c = repo.invoice(repo.createInvoice(draft(LocalDate(2026, 4, 2))))!!
        assertEquals("RS/25-26/001", a.number)
        assertEquals("RS/25-26/002", b.number)
        assertEquals("RS/26-27/001", c.number)
        assertEquals("RS/26-27/002", repo.previewNextNumber(LocalDate(2026, 11, 6)))
    }

    @Test
    fun continuesAnExistingSeries() {
        repo.saveProfile(
            repo.profile()!!.copy(
                numbering = NumberingScheme(pattern = "{YY}{SEQ:3}", reset = NumberReset.NEVER, startAt = 32),
            ),
        )
        val inv = repo.invoice(repo.createInvoice(draft()))!!
        assertEquals("26032", inv.number)
    }

    @Test
    fun storesAndReadsBackAFullInvoice() {
        repo.saveBankAccount(
            BankAccount(label = "Main", bankName = "Bank", accountName = "Owner", accountNumber = "123", ifsc = "IFSC0001", swift = "SWIFT01"),
        )
        val items = listOf(
            LineItem.priced("Logo design", 1_500_00, 2_000),
            LineItem.fixed("Advance received", 500_00),
        )
        val id = repo.createInvoice(draft(items = items, currency = Currency.USD))
        val inv = repo.invoice(id)!!
        assertEquals(Currency.USD, inv.currency)
        assertEquals("Acme", inv.client.name)
        assertEquals(items, inv.items)
        assertEquals(3_500_00, inv.totalMinor)
        assertEquals("Main", inv.bankAccount?.label) // default account picked automatically
        assertEquals(InvoiceState.DRAFT, inv.state)
    }

    @Test
    fun editingKeepsTheNumberAndReplacesItems() {
        val id = repo.createInvoice(draft())
        val before = repo.invoice(id)!!
        repo.updateInvoice(id, draft(items = listOf(LineItem.fixed("Final payment", 8_000_00))))
        val after = repo.invoice(id)!!
        assertEquals(before.number, after.number)
        assertEquals(listOf(LineItem.fixed("Final payment", 8_000_00)), after.items)
    }

    @Test
    fun partPaymentsAddUpToPaid() {
        val id = repo.createInvoice(draft())
        repo.markSent(id)
        val today = LocalDate(2026, 11, 10)

        repo.recordPayment(id, Payment(receivedOn = today, amountMinor = 5_000_00, method = PaymentMethod.UPI))
        var summary = repo.invoiceSummaries().single()
        assertEquals(PaymentStatus.PARTLY_PAID, summary.status(today))
        assertEquals(7_000_00, summary.outstandingMinor)
        assertEquals(mapOf(Currency.INR to 7_000_00L), repo.outstandingByCurrency())

        repo.recordPayment(id, Payment(receivedOn = today, amountMinor = 7_000_00, method = PaymentMethod.BANK_TRANSFER))
        summary = repo.invoiceSummaries().single()
        assertEquals(PaymentStatus.PAID, summary.status(today))
        assertTrue(repo.outstandingByCurrency().isEmpty())
    }

    @Test
    fun foreignPaymentRemembersRupeesReceived() {
        val id = repo.createInvoice(draft(currency = Currency.USD, items = listOf(LineItem.fixed("Website", 500_00))))
        repo.recordPayment(
            id,
            Payment(
                receivedOn = LocalDate(2026, 11, 8),
                amountMinor = 500_00,
                method = PaymentMethod.WISE,
                inrReceivedMinor = 41_250_00,
            ),
        )
        val inv = repo.invoice(id)!!
        assertEquals(41_250_00, inv.payments.single().inrReceivedMinor)
        // Recording a payment on a draft marks it as sent.
        assertEquals(InvoiceState.SENT, inv.state)
        assertEquals(PaymentStatus.PAID, inv.status(LocalDate(2026, 11, 8)))
    }

    @Test
    fun overdueOnceDueDatePasses() {
        val id = repo.createInvoice(draft(LocalDate(2026, 10, 1)))
        repo.markSent(id)
        assertEquals(PaymentStatus.OVERDUE, repo.invoice(id)!!.status(LocalDate(2026, 10, 17)))
    }

    @Test
    fun onlyDraftsCanBeDeletedAndVoidKeepsTheNumber() {
        val draftId = repo.createInvoice(draft())
        repo.deleteDraft(draftId)
        assertNull(repo.invoice(draftId))

        val sentId = repo.createInvoice(draft())
        repo.markSent(sentId)
        assertFailsWith<IllegalArgumentException> { repo.deleteDraft(sentId) }
        repo.void(sentId)
        val voided = repo.invoice(sentId)!!
        assertEquals(PaymentStatus.VOID, voided.status(LocalDate(2026, 12, 31)))
        // The next invoice does not reuse the voided number.
        val next = repo.invoice(repo.createInvoice(draft()))!!
        assertTrue(next.number != voided.number)
    }

    @Test
    fun rejectsInvalidInvoices() {
        assertFailsWith<IllegalArgumentException> { repo.createInvoice(draft(items = emptyList())) }
        assertFailsWith<IllegalArgumentException> {
            repo.createInvoice(draft().copy(dueDate = LocalDate(2026, 1, 1)))
        }
        assertFailsWith<IllegalArgumentException> { repo.createInvoice(draft().copy(clientId = 999)) }
        assertTrue(repo.invoiceSummaries().isEmpty())
    }

    @Test
    fun firstBankAccountBecomesDefault() {
        val first = repo.saveBankAccount(BankAccount(label = "A", bankName = "B", accountName = "C", accountNumber = "1", ifsc = "I", swift = "S"))
        val second = repo.saveBankAccount(BankAccount(label = "B", bankName = "B", accountName = "C", accountNumber = "2", ifsc = "I", swift = "S"))
        assertEquals(first, repo.bankAccounts().single { it.isDefault }.id)
        repo.saveBankAccount(repo.bankAccounts().single { it.id == second }.copy(isDefault = true))
        assertEquals(second, repo.bankAccounts().single { it.isDefault }.id)
    }

    @Test
    fun archivedClientsDisappearFromTheListButInvoicesKeepThem() {
        val id = repo.createInvoice(draft())
        repo.archiveClient(clientId)
        assertTrue(repo.clients().isEmpty())
        assertEquals("Acme", repo.invoice(id)!!.client.name)
    }
}
