@file:OptIn(ExperimentalMaterial3Api::class)

package com.revishingstudio.invoice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revishingstudio.invoice.core.Currency
import com.revishingstudio.invoice.core.InvoiceDraft
import com.revishingstudio.invoice.core.LineItem
import com.revishingstudio.invoice.core.MoneyFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

/** One editable line. With "rate x qty" off, only the amount is used (like an advance payment). */
private class ItemInput(
    description: String = "",
    priced: Boolean = false,
    amount: String = "",
    rate: String = "",
    qty: String = "1",
) {
    var description by mutableStateOf(description)
    var priced by mutableStateOf(priced)
    var amount by mutableStateOf(amount)
    var rate by mutableStateOf(rate)
    var qty by mutableStateOf(qty)

    /** The line item, or null with a reason when something is missing. */
    fun toLineItem(): Result<LineItem> {
        if (description.isBlank()) return Result.failure(IllegalArgumentException("Every line needs a description"))
        return if (priced) {
            val r = MoneyFormat.parseToMinor(rate) ?: return Result.failure(IllegalArgumentException("Check the rate for \"$description\""))
            val q = parseQuantityMilli(qty) ?: return Result.failure(IllegalArgumentException("Check the quantity for \"$description\""))
            Result.success(LineItem.priced(description.trim(), r, q))
        } else {
            val a = MoneyFormat.parseToMinor(amount) ?: return Result.failure(IllegalArgumentException("Check the amount for \"$description\""))
            Result.success(LineItem.fixed(description.trim(), a))
        }
    }

    companion object {
        fun from(item: LineItem): ItemInput =
            if (item.rateMinor != null && item.quantityMilli != null) {
                ItemInput(item.description, true, minorToInput(item.amountMinor), minorToInput(item.rateMinor!!), formatQuantity(item.quantityMilli!!))
            } else {
                ItemInput(item.description, false, minorToInput(item.amountMinor))
            }
    }
}

@Composable
fun InvoiceEditorScreen(id: Long?, nav: Navigator, modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    val repo = actions.repository
    val clients by repo.observeClients().collectAsStateWithLifecycle(emptyList())
    val banks by repo.observeBankAccounts().collectAsStateWithLifecycle(emptyList())

    var loaded by remember { mutableStateOf(false) }
    var number by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf<Long?>(null) }
    var currency by remember { mutableStateOf(Currency.INR) }
    var issueDate by remember { mutableStateOf(today()) }
    var dueDate by remember { mutableStateOf(today().plus(DatePeriod(days = 15))) }
    var dueDays by remember { mutableStateOf(15) }
    var bankId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<ItemInput>() }

    LaunchedEffect(id) {
        val profile = withContext(Dispatchers.IO) { repo.profile() }
        val defaultBank = withContext(Dispatchers.IO) { repo.bankAccounts().firstOrNull { it.isDefault }?.id }
        val existing = id?.let { withContext(Dispatchers.IO) { repo.invoice(it) } }
        dueDays = profile?.defaultDueDays ?: 15
        if (existing == null) {
            dueDate = issueDate.plus(DatePeriod(days = dueDays))
            bankId = defaultBank
        } else {
            number = existing.number
            clientId = existing.client.id
            currency = existing.currency
            issueDate = existing.issueDate
            dueDate = existing.dueDate
            bankId = existing.bankAccount?.id
            notes = existing.notes
            items.addAll(existing.items.map(ItemInput::from))
        }
        if (items.isEmpty()) items += ItemInput()
        loaded = true
    }

    // New invoices show the number they will get.
    LaunchedEffect(issueDate, loaded) {
        if (id == null && loaded) number = withContext(Dispatchers.IO) { repo.previewNextNumber(issueDate) }
    }

    val lineItems = items.map { it.toLineItem() }
    val total = lineItems.sumOf { it.getOrNull()?.amountMinor ?: 0L }

    fun save() {
        val client = clientId ?: return actions.message("Choose a client")
        lineItems.firstOrNull { it.isFailure }?.let { return actions.message(it.exceptionOrNull()?.message ?: "Check the items") }
        val draft = InvoiceDraft(
            clientId = client,
            currency = currency,
            issueDate = issueDate,
            dueDate = dueDate,
            items = lineItems.map { it.getOrThrow() },
            bankAccountId = bankId,
            notes = notes.trim(),
        )
        if (id == null) {
            var newId = 0L
            actions.run("Invoice created", onDone = { nav.replace(Screen.InvoiceDetail(newId)) }) {
                newId = createInvoice(draft)
            }
        } else {
            actions.run("Invoice saved", onDone = { nav.back() }) { updateInvoice(id, draft) }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            BackTopBar(if (id == null) "New invoice" else "Edit invoice", nav::back) {
                TextButton(onClick = ::save) { Text("Save") }
            }
        },
    ) { padding ->
        if (!loaded) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (id == null) "Will be numbered $number" else "Invoice $number",
                style = MaterialTheme.typography.titleMedium,
            )

            DropdownField(
                label = "Client",
                selected = clients.firstOrNull { it.id == clientId },
                options = clients,
                optionLabel = { it.name },
                onSelect = { client ->
                    clientId = client.id
                    if (id == null) currency = client.defaultCurrency
                },
                placeholder = "Choose a client",
            )

            DropdownField(
                label = "Currency",
                selected = currency,
                options = Currency.entries,
                optionLabel = { "${it.code} (${it.symbol})" },
                onSelect = { currency = it },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField("Invoice date", issueDate, { new ->
                    issueDate = new
                    dueDate = new.plus(DatePeriod(days = dueDays))
                }, Modifier.weight(1f))
                DateField("Due date", dueDate, { dueDate = maxOf(it, issueDate) }, Modifier.weight(1f))
            }

            SectionTitle("Items")
            items.forEachIndexed { index, item ->
                ItemEditor(
                    item = item,
                    currency = currency,
                    canRemove = items.size > 1,
                    onRemove = { items.removeAt(index) },
                )
            }
            OutlinedButton(onClick = { items += ItemInput() }) { Text("Add item") }

            HeroCard(Modifier.padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                    Text(MoneyFormat.format(total, currency), style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            DropdownField(
                label = "Bank account printed on the invoice",
                selected = banks.firstOrNull { it.id == bankId },
                options = banks,
                optionLabel = { "${it.label} · ${it.accountNumber}" },
                onSelect = { bankId = it.id },
                placeholder = if (banks.isEmpty()) "Add one in Settings" else "None",
            )
            Field("Notes (optional, printed on the invoice)", notes, { notes = it }, singleLine = false, minLines = 2)

            PrimaryButton(if (id == null) "Create invoice" else "Save changes", ::save, Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ItemEditor(item: ItemInput, currency: Currency, canRemove: Boolean, onRemove: () -> Unit) {
    GlassCard(contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Field("Description", item.description, { item.description = it }, Modifier.weight(1f), singleLine = false)
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove item", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !item.priced, onClick = { item.priced = false }, label = { Text("Amount") }, shape = CircleShape)
                FilterChip(selected = item.priced, onClick = { item.priced = true }, label = { Text("Rate × qty") }, shape = CircleShape)
            }
            if (item.priced) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field("Rate (${currency.symbol})", item.rate, { item.rate = it }, Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                    Field("Qty", item.qty, { item.qty = it }, Modifier.weight(0.6f), keyboardType = KeyboardType.Decimal)
                }
                val amount = item.toLineItem().getOrNull()?.amountMinor
                Text(
                    amount?.let { "= " + MoneyFormat.format(it, currency) } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Field("Amount (${currency.symbol})", item.amount, { item.amount = it }, keyboardType = KeyboardType.Decimal)
            }
        }
    }
}
