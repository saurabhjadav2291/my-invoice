package com.revishingstudio.invoice.pdf

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.revishingstudio.invoice.core.BankAccount
import com.revishingstudio.invoice.core.BusinessProfile
import com.revishingstudio.invoice.core.Client
import com.revishingstudio.invoice.core.Currency
import com.revishingstudio.invoice.core.Invoice
import com.revishingstudio.invoice.core.InvoiceState
import com.revishingstudio.invoice.core.LineItem
import com.revishingstudio.invoice.core.pdf.InvoicePdfLayout
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import java.io.File
import kotlin.time.Clock

object InvoicePdfExporter {

    /** Writes the invoice to the app's cache and returns the file. */
    fun export(context: Context, invoice: Invoice, profile: BusinessProfile): File {
        val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
        val safeName = invoice.number.replace(Regex("[^A-Za-z0-9-]"), "-")
        val file = File(dir, "Invoice-$safeName.pdf")
        AndroidPdfWriter(context).use { writer ->
            InvoicePdfLayout(invoice, profile).render(writer)
            writer.writeTo(file)
        }
        return file
    }

    /** Opens the PDF in a viewer app, with the option to share it from there. */
    fun open(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(view, file.name).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(share))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /** Made-up data so the design can be checked before real invoices exist. */
    fun sampleInvoice(): Pair<Invoice, BusinessProfile> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val profile = BusinessProfile(
            ownerName = "Your Name",
            studioName = "Your Studio",
            address = "Flat 101, Sample Residency,\nNear Example Circle,\nAhmedabad - 380001",
            pan = "ABCDE1234F",
        )
        val invoice = Invoice(
            id = 0,
            number = "INV/26-27/001",
            client = Client(
                name = "Sample Client",
                legalName = "Sample Client Pty Ltd",
                address = "12 Harbour St, Sydney NSW 2000, Australia.",
                phone = "+61 400 000 000",
                email = "accounts@client.example",
                taxIds = "ABN: 12345678901  ACN: 123456789",
            ),
            bankAccount = BankAccount(
                label = "Main",
                bankName = "EXAMPLE BANK",
                accountName = "Your Name",
                accountNumber = "0000000000",
                ifsc = "EXMP0000001",
                swift = "EXMPINBB",
            ),
            currency = Currency.INR,
            issueDate = today,
            dueDate = today.plus(DatePeriod(days = 15)),
            state = InvoiceState.SENT,
            items = listOf(
                LineItem.fixed("Advance payment for brand identity", 12_000_00),
                LineItem.priced("Social media post designs", 1_500_00, 6_000),
            ),
            payments = emptyList(),
            notes = "",
        )
        return invoice to profile
    }
}
