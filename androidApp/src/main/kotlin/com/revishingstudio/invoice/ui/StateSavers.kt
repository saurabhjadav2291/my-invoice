package com.revishingstudio.invoice.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.revishingstudio.invoice.core.TaxField
import kotlinx.datetime.LocalDate

/*
 * Savers let screens keep what you typed when Android closes the app in the
 * background (e.g. while you copy details from WhatsApp) and brings it back later.
 */

val LocalDateSaver: Saver<LocalDate, String> = Saver(save = { it.toString() }, restore = { LocalDate.parse(it) })

/** The back stack, one string per screen. */
val ScreenStackSaver: Saver<SnapshotStateList<Screen>, Any> = listSaver(
    save = { stack -> stack.map { it.encode() } },
    restore = { saved -> saved.mapNotNull { decodeScreen(it) }.ifEmpty { listOf(Screen.Invoices) }.toMutableStateList() },
)

val TaxFieldsSaver: Saver<SnapshotStateList<TaxField>, Any> = listSaver(
    save = { list -> list.flatMap { listOf(it.label, it.value) } },
    restore = { flat -> flat.chunked(2).map { TaxField(it[0], it.getOrElse(1) { "" }) }.toMutableStateList() },
)

private fun Screen.encode(): String = when (this) {
    Screen.Invoices -> "invoices"
    Screen.Clients -> "clients"
    Screen.Settings -> "settings"
    Screen.AccountDetails -> "account"
    Screen.BusinessInfo -> "business"
    is Screen.InvoiceDetail -> "invoice\t$id"
    is Screen.InvoiceEditor -> "invoiceEditor\t${id ?: ""}"
    is Screen.ClientEditor -> "clientEditor\t${id ?: ""}"
    is Screen.BankEditor -> "bankEditor\t${id ?: ""}"
    is Screen.PdfPreview -> "pdf\t$path\t$title"
    Screen.Backup -> "backup"
}

private fun decodeScreen(text: String): Screen? {
    val parts = text.split('\t')
    val id = parts.getOrNull(1)?.toLongOrNull()
    return when (parts[0]) {
        "invoices" -> Screen.Invoices
        "clients" -> Screen.Clients
        "settings" -> Screen.Settings
        "account" -> Screen.AccountDetails
        "business" -> Screen.BusinessInfo
        "invoice" -> id?.let { Screen.InvoiceDetail(it) }
        "invoiceEditor" -> Screen.InvoiceEditor(id)
        "clientEditor" -> Screen.ClientEditor(id)
        "bankEditor" -> Screen.BankEditor(id)
        // The PDF lives in the cache, which may be cleared; the preview shows a message if so.
        "backup" -> Screen.Backup
        "pdf" -> Screen.PdfPreview(parts.getOrElse(1) { "" }, parts.getOrElse(2) { "" })
        else -> null
    }
}
