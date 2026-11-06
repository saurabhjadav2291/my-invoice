@file:OptIn(ExperimentalMaterial3Api::class)

package com.revishingstudio.invoice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.revishingstudio.invoice.core.BusinessProfile
import com.revishingstudio.invoice.core.Currency
import com.revishingstudio.invoice.core.Invoice
import com.revishingstudio.invoice.core.InvoiceDates
import com.revishingstudio.invoice.core.InvoiceState
import com.revishingstudio.invoice.core.MoneyFormat
import com.revishingstudio.invoice.core.Payment
import com.revishingstudio.invoice.core.PaymentMethod
import com.revishingstudio.invoice.core.PaymentStatus
import com.revishingstudio.invoice.pdf.InvoicePdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InvoiceDetailScreen(id: Long, nav: Navigator, modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var invoice by remember { mutableStateOf<Invoice?>(null) }
    var profile by remember { mutableStateOf<BusinessProfile?>(null) }
    var showPayment by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmVoid by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<Payment?>(null) }

    LaunchedEffect(id, refresh) {
        val loadedInvoice = withContext(Dispatchers.IO) { actions.repository.invoice(id) }
        val loadedProfile = withContext(Dispatchers.IO) { actions.repository.profile() }
        invoice = loadedInvoice
        profile = loadedProfile
        if (loadedInvoice == null) nav.back()
    }
    val reload: () -> Unit = { refresh++ }

    val inv = invoice
    val today = today()
    val status = inv?.status(today)

    fun sharePdf() {
        val current = invoice ?: return
        val owner = profile ?: return actions.message("Add your details in Settings first")
        actions.run {
            val file = InvoicePdfExporter.export(context, current, owner)
            withContext(Dispatchers.Main) { InvoicePdfExporter.open(context, file) }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(inv?.number ?: "", nav::back) {
                if (inv != null) {
                    TextButton(onClick = { nav.go(Screen.InvoiceEditor(id)) }) { Text("Edit") }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (inv.state == InvoiceState.SENT) {
                                DropdownMenuItem(text = { Text("Move back to draft") }, onClick = {
                                    menuOpen = false
                                    actions.run(onDone = reload) { markDraft(id) }
                                })
                            }
                            if (inv.state != InvoiceState.VOID) {
                                DropdownMenuItem(text = { Text("Void invoice") }, onClick = {
                                    menuOpen = false
                                    confirmVoid = true
                                })
                            }
                            if (inv.state == InvoiceState.DRAFT) {
                                DropdownMenuItem(text = { Text("Delete draft") }, onClick = {
                                    menuOpen = false
                                    confirmDelete = true
                                })
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (inv == null || status == null) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(inv.client.name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Issued ${InvoiceDates.format(inv.issueDate)} · Due ${InvoiceDates.format(inv.dueDate)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(status)
            }

            Button(onClick = ::sharePdf, modifier = Modifier.fillMaxWidth()) { Text("View / share PDF") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (inv.state == InvoiceState.DRAFT) {
                    OutlinedButton(onClick = { actions.run("Marked as sent", onDone = reload) { markSent(id) } }, modifier = Modifier.weight(1f)) {
                        Text("Mark as sent")
                    }
                }
                if (inv.state != InvoiceState.VOID && status != PaymentStatus.PAID) {
                    OutlinedButton(onClick = { showPayment = true }, modifier = Modifier.weight(1f)) { Text("Record payment") }
                }
            }

            SectionTitle("Items")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    inv.items.forEach { item ->
                        Row {
                            Column(Modifier.weight(1f)) {
                                Text(item.description)
                                if (item.rateMinor != null && item.quantityMilli != null) {
                                    Text(
                                        "${MoneyFormat.format(item.rateMinor!!, inv.currency)} × ${formatQuantity(item.quantityMilli!!)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(MoneyFormat.format(item.amountMinor, inv.currency), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider()
                    AmountRow("Total", inv.totalMinor, inv.currency, bold = true)
                    if (inv.paidMinor > 0) {
                        AmountRow("Paid", inv.paidMinor, inv.currency)
                        AmountRow("Still owed", inv.outstandingMinor, inv.currency, bold = true)
                    }
                }
            }

            SectionTitle("Payments")
            if (inv.payments.isEmpty()) {
                Text("No payments recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            inv.payments.forEach { payment ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${MoneyFormat.format(payment.amountMinor, inv.currency)} · ${payment.method.label}")
                        val details = buildList {
                            add(InvoiceDates.format(payment.receivedOn))
                            payment.inrReceivedMinor?.let { add("received ${MoneyFormat.format(it, Currency.INR)}") }
                            if (payment.note.isNotBlank()) add(payment.note)
                        }
                        Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { paymentToDelete = payment }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete payment")
                    }
                }
            }

            if (inv.notes.isNotBlank()) {
                SectionTitle("Notes")
                Text(inv.notes)
            }
            inv.bankAccount?.let {
                SectionTitle("Bank account on invoice")
                Text("${it.bankName} · ${it.accountNumber}")
            }
        }
    }

    if (showPayment && inv != null) {
        RecordPaymentDialog(
            invoice = inv,
            onDismiss = { showPayment = false },
            onSave = { payment ->
                showPayment = false
                actions.run("Payment recorded", onDone = reload) { recordPayment(id, payment) }
            },
        )
    }
    if (confirmVoid) {
        ConfirmDialog(
            title = "Void this invoice?",
            message = "It stays in your list as void so the number is never reused. It no longer counts as owed.",
            confirmLabel = "Void",
            onConfirm = { actions.run("Invoice voided", onDone = reload) { void(id) } },
            onDismiss = { confirmVoid = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this draft?",
            message = "This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { actions.run("Draft deleted", onDone = { nav.back() }) { deleteDraft(id) } },
            onDismiss = { confirmDelete = false },
        )
    }
    paymentToDelete?.let { payment ->
        ConfirmDialog(
            title = "Delete this payment?",
            message = "${MoneyFormat.format(payment.amountMinor, inv?.currency ?: Currency.INR)} on ${InvoiceDates.format(payment.receivedOn)}",
            confirmLabel = "Delete",
            onConfirm = { actions.run("Payment deleted", onDone = reload) { deletePayment(payment.id) } },
            onDismiss = { paymentToDelete = null },
        )
    }
}

@Composable
private fun AmountRow(label: String, amount: Long, currency: Currency, bold: Boolean = false) {
    Row {
        Text(label, Modifier.weight(1f), fontWeight = if (bold) FontWeight.SemiBold else null)
        Text(MoneyFormat.format(amount, currency), fontWeight = if (bold) FontWeight.SemiBold else null)
    }
}

@Composable
private fun RecordPaymentDialog(invoice: Invoice, onDismiss: () -> Unit, onSave: (Payment) -> Unit) {
    var date by remember { mutableStateOf(today()) }
    var amount by remember { mutableStateOf(minorToInput(invoice.outstandingMinor)) }
    var method by remember { mutableStateOf(PaymentMethod.BANK_TRANSFER) }
    var inrReceived by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val foreign = invoice.currency != Currency.INR

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DateField("Received on", date, { date = it })
                Field("Amount (${invoice.currency.symbol})", amount, { amount = it }, keyboardType = KeyboardType.Decimal)
                DropdownField(
                    label = "Method",
                    selected = method,
                    options = PaymentMethod.entries,
                    optionLabel = { it.label },
                    onSelect = { method = it },
                )
                if (foreign) {
                    Field(
                        "Rupees received (optional)",
                        inrReceived,
                        { inrReceived = it },
                        keyboardType = KeyboardType.Decimal,
                        supportingText = "What actually reached your bank, for your records",
                    )
                }
                Field("Note (optional)", note, { note = it })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val minor = MoneyFormat.parseToMinor(amount)
                if (minor == null || minor <= 0) {
                    error = "Enter the amount received"
                    return@TextButton
                }
                val inr = if (foreign && inrReceived.isNotBlank()) MoneyFormat.parseToMinor(inrReceived) else null
                if (foreign && inrReceived.isNotBlank() && inr == null) {
                    error = "Check the rupees received"
                    return@TextButton
                }
                onSave(Payment(receivedOn = date, amountMinor = minor, method = method, inrReceivedMinor = inr, note = note.trim()))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
