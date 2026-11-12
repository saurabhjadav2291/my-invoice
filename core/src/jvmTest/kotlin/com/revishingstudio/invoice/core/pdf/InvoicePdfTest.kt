package com.revishingstudio.invoice.core.pdf

import com.revishingstudio.invoice.core.BankAccount
import com.revishingstudio.invoice.core.BusinessProfile
import com.revishingstudio.invoice.core.Client
import com.revishingstudio.invoice.core.Currency
import com.revishingstudio.invoice.core.Invoice
import com.revishingstudio.invoice.core.InvoiceState
import com.revishingstudio.invoice.core.LineItem
import com.revishingstudio.invoice.core.TaxField
import kotlinx.datetime.LocalDate
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvoicePdfTest {

    private val profile = BusinessProfile(
        ownerName = "Asha Example",
        studioName = "Example Studio",
        address = "A-101, Sample Residency,\nNear Test Circle,\nAhmedabad - 380001",
        pan = "ABCDE1234F",
    )

    private val client = Client(
        id = 1,
        name = "Northwind",
        legalName = "Northwind Traders Pty Ltd",
        address = "12 Harbour St, Sydney NSW 2000, Australia.",
        phone = "+61 400 000 000",
        email = "accounts@northwind.example",
        taxFields = listOf(TaxField("ABN", "12345678901"), TaxField("ACN", "123456789")),
    )

    private val bank = BankAccount(
        id = 1,
        label = "Main",
        bankName = "EXAMPLE BANK",
        accountName = "Asha Example",
        accountNumber = "0000000000",
        ifsc = "EXMP0000001",
        swift = "EXMPINBB",
        isDefault = true,
    )

    private fun invoice(
        items: List<LineItem> = listOf(LineItem.fixed("Advance Payment for Website Design", 12_000_00)),
        currency: Currency = Currency.INR,
        notes: String = "",
    ) = Invoice(
        id = 1,
        number = "RS/26-27/001",
        client = client,
        bankAccount = bank,
        currency = currency,
        issueDate = LocalDate(2026, 11, 6),
        dueDate = LocalDate(2026, 11, 21),
        state = InvoiceState.SENT,
        items = items,
        payments = emptyList(),
        notes = notes,
    )

    private fun render(invoice: Invoice, profile: BusinessProfile = this.profile): ByteArray {
        val out = ByteArrayOutputStream()
        PdfBoxWriter().use { writer ->
            InvoicePdfLayout(invoice, profile).render(writer)
            writer.save(out)
        }
        // Set -Dinvoice.pdf.out=<dir> to keep the generated PDFs for a visual check.
        System.getProperty("invoice.pdf.out")?.let { dir ->
            File(dir).mkdirs()
            File(dir, "invoice-${invoice.items.size}-${invoice.currency}.pdf").writeBytes(out.toByteArray())
        }
        return out.toByteArray()
    }

    private fun textOf(pdf: ByteArray): Pair<Int, String> =
        Loader.loadPDF(pdf).use { doc -> doc.numberOfPages to PDFTextStripper().getText(doc) }

    @Test
    fun singlePageInvoiceHasEverySection() {
        val (pages, text) = textOf(render(invoice()))
        assertEquals(1, pages)
        listOf(
            "INVOICE",
            "#RS/26-27/001",
            "Name: Asha Example",
            "PAN: ABCDE1234F",
            "Invoice Date: 6-NOV-2026",
            "Due Date: 21-NOV-2026",
            "BILL TO",
            "Northwind (Northwind Traders Pty Ltd)",
            "Advance Payment for Website Design",
            "Total Amount:",
            "₹12,000.00",
            "Rupees Twelve Thousand Only",
            "Supplier not registered under GST.",
            "BANK ACCOUNT INFO.",
            "EXMP0000001",
            "Thank You!",
            "from Example Studio",
        ).forEach { assertTrue(it in text, "Missing \"$it\" in:\n$text") }
    }

    @Test
    fun gstinAndCustomTaxFieldsShowUnderBillTo() {
        val taxed = client.copy(
            gstin = "27AAPFU0939F1ZV",
            taxFields = listOf(
                TaxField("ABN", "12 345 678 901"),
                TaxField("ACN", "123 456 789"),
                TaxField("VAT No", "GB 123 4567 89"),
                TaxField("Company Reg", "09876543"),
                TaxField("EORI", "GB123456789000"),
            ),
        )
        val pdf = render(invoice().copy(client = taxed))
        val (pages, text) = textOf(pdf)
        assertEquals(1, pages)
        listOf("GSTIN: 27AAPFU0939F1ZV", "ABN: 12 345 678 901", "VAT No: GB 123 4567 89", "EORI: GB123456789000")
            .forEach { assertTrue(it in text, "Missing \"$it\" in:\n$text") }
    }

    @Test
    fun optionalPartsCanBeHidden() {
        val (_, text) = textOf(render(invoice(), profile.copy(showGstNote = false, showDueDate = false)))
        assertFalse("Due Date" in text)
        assertFalse("GST" in text)
    }

    @Test
    fun foreignCurrencyAndPricedLines() {
        val items = listOf(
            LineItem.priced("UI design", 40_00, 12_500),
            LineItem.fixed("Hosting setup", 150_00),
        )
        val (_, text) = textOf(render(invoice(items, Currency.USD)))
        assertTrue("$40.00" in text)
        assertTrue("12.5" in text)
        assertTrue("$650.00" in text)
        assertTrue("US Dollars Six Hundred Fifty Only" in text)
    }

    @Test
    fun manyItemsFlowOntoMorePages() {
        val items = (1..40).map { LineItem.fixed("Design revision round $it for the brand refresh project", 1_000_00) }
        val (pages, text) = textOf(render(invoice(items, notes = "Thank you for your business.")))
        assertTrue(pages >= 2, "Expected more than one page, got $pages")
        assertTrue("round 40" in text)
        assertTrue("Rupees Forty Thousand Only" in text)
        // The table headings repeat on each page with items.
        assertTrue(text.split("DESCRIPTION OF ITEM/SERVICES").size - 1 >= 2)
    }

    @Test
    fun longTextWraps() {
        val long = "Complete brand identity including logo, colour palette, typography, " +
            "business cards, letterheads and social media templates"
        val (_, text) = textOf(render(invoice(listOf(LineItem.fixed(long, 85_000_00)))))
        assertTrue("Complete brand identity" in text)
        assertTrue("templates" in text)
        assertTrue("Rupees Eighty Five Thousand Only" in text)
    }

    @Test
    fun longClientNameWrapsInsteadOfRunningOffThePage() {
        val long = client.copy(
            name = "International Brand Consultancy Partners",
            legalName = "International Brand Consultancy Partners Private Limited",
            email = "accounts.payable.department.northwind.traders@northwindtradersinternational.example.com",
        )
        val (_, text) = textOf(render(invoice().copy(client = long, number = "REVISHINGSTUDIO/2026-2027/000123")))
        assertTrue("Private Limited" in text)
        assertTrue("REVISHINGSTUDIO/2026-2027/000123" in text)
    }

    @Test
    fun sentInvoicePrintsItsFrozenBusinessDetails() {
        val frozen = profile.copy(address = "Old studio address")
        val (_, text) = textOf(render(invoice().copy(issuer = frozen)))
        assertTrue("Old studio address" in text)
    }
}
