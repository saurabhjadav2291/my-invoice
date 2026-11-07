@file:OptIn(ExperimentalMaterial3Api::class)

package com.revishingstudio.invoice.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revishingstudio.invoice.core.Currency
import com.revishingstudio.invoice.core.InvoiceDates
import com.revishingstudio.invoice.core.InvoiceSummary
import com.revishingstudio.invoice.core.MoneyFormat
import com.revishingstudio.invoice.core.PaymentStatus

private enum class InvoiceFilter(val label: String, val matches: (PaymentStatus) -> Boolean) {
    ALL("All", { true }),
    UNPAID("Unpaid", { it == PaymentStatus.SENT || it == PaymentStatus.PARTLY_PAID || it == PaymentStatus.OVERDUE }),
    OVERDUE("Overdue", { it == PaymentStatus.OVERDUE }),
    PAID("Paid", { it == PaymentStatus.PAID }),
    DRAFT("Drafts", { it == PaymentStatus.DRAFT }),
}

@Composable
fun InvoicesScreen(nav: Navigator, modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    val invoices by actions.repository.observeInvoiceSummaries().collectAsStateWithLifecycle(null)
    val clients by actions.repository.observeClients().collectAsStateWithLifecycle(null)
    val profile by actions.repository.observeProfile().collectAsStateWithLifecycle(null)
    var filter by remember { mutableStateOf(InvoiceFilter.ALL) }
    val today = today()

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { LargeTitleBar("Invoices") },
        floatingActionButton = {
            GlossyFab(
                contentDescription = "New invoice",
                onClick = {
                    when {
                        profile == null -> {
                            actions.message("First add your details in Settings")
                            nav.tab(Screen.Settings)
                        }
                        clients.isNullOrEmpty() -> {
                            actions.message("First add a client")
                            nav.go(Screen.ClientEditor(null))
                        }
                        else -> nav.go(Screen.InvoiceEditor(null))
                    }
                },
            )
        },
    ) { padding ->
        val list = invoices ?: return@Scaffold
        if (list.isEmpty()) {
            IllustratedEmptyState(
                "No invoices yet",
                if (profile == null) {
                    "Start in Settings: add your details and bank account. Then add a client and create your first invoice."
                } else {
                    "Tap the + button to create your first one."
                },
                AppIcons.Document,
                Modifier.padding(padding),
            )
            return@Scaffold
        }
        val shown = list.filter { filter.matches(it.status(today)) }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { OutstandingCard(list, today) }
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InvoiceFilter.entries.forEach { f ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = { Text(f.label) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White.copy(alpha = 0.85f),
                                selectedContainerColor = Palette.Peach,
                                selectedLabelColor = Palette.OrangeDeep,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = filter == f,
                                borderColor = Palette.FieldBorder,
                                selectedBorderColor = Color.White,
                            ),
                        )
                    }
                }
            }
            if (shown.isEmpty()) {
                item { EmptyState("Nothing here", "No invoices match this filter.") }
            }
            items(shown, key = { it.id }) { invoice ->
                InvoiceRow(invoice, today) { nav.go(Screen.InvoiceDetail(invoice.id)) }
            }
        }
    }
}

/** Money still owed, per currency, across sent invoices. */
@Composable
private fun OutstandingCard(list: List<InvoiceSummary>, today: kotlinx.datetime.LocalDate) {
    val unpaid = list.filter {
        val s = it.status(today)
        s == PaymentStatus.SENT || s == PaymentStatus.PARTLY_PAID || s == PaymentStatus.OVERDUE
    }
    val owed = unpaid.groupBy { it.currency }.mapValues { (_, l) -> l.sumOf { it.outstandingMinor } }.filterValues { it > 0 }
    val overdue = unpaid.count { it.status(today) == PaymentStatus.OVERDUE }
    HeroCard {
        Text("Still to be paid", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
        Spacer(Modifier.height(4.dp))
        if (owed.isEmpty()) {
            Text("Nothing outstanding", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        } else {
            Currency.entries.filter { it in owed }.forEach { c ->
                Text(MoneyFormat.format(owed.getValue(c), c), style = MaterialTheme.typography.headlineMedium, color = Color.White)
            }
        }
        if (overdue > 0) {
            Spacer(Modifier.height(10.dp))
            Surface(color = Color.White.copy(alpha = 0.22f), shape = CircleShape) {
                Text(
                    if (overdue == 1) "1 invoice overdue" else "$overdue invoices overdue",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun InvoiceRow(invoice: InvoiceSummary, today: kotlinx.datetime.LocalDate, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(invoice.clientName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${invoice.number} · ${InvoiceDates.format(invoice.issueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(MoneyFormat.format(invoice.totalMinor, invoice.currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusChip(invoice.status(today))
            }
        }
    }
}
