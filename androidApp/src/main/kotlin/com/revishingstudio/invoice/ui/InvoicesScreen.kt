@file:OptIn(ExperimentalMaterial3Api::class)

package com.revishingstudio.invoice.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revishingstudio.invoice.core.Currency
import com.revishingstudio.invoice.core.InvoiceDates
import com.revishingstudio.invoice.core.InvoiceSummary
import com.revishingstudio.invoice.core.MoneyFormat
import com.revishingstudio.invoice.core.PaymentStatus

/** The studio logo from Settings, decoded once per path. */
@Composable
fun rememberLogo(path: String?, version: Int = 0): ImageBitmap? = remember(path, version) {
    path?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() }
}

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
    val profile by actions.repository.observeProfile().collectAsStateWithLifecycle(null)
    var filter by remember { mutableStateOf(InvoiceFilter.ALL) }
    val today = today()
    val logo = rememberLogo(profile?.logoPath)
    val list = invoices

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomBarPadding.current + 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            val name = profile?.ownerName?.substringBefore(' ').orEmpty()
            SkyHeader(
                title = greeting() + if (name.isNotBlank()) ", $name" else "",
                subtitle = profile?.studioName?.takeIf { it.isNotBlank() } ?: "Your invoices",
                avatar = { Avatar(logo, profile?.studioName ?: profile?.ownerName ?: "") },
            )
        }
        if (list == null) return@LazyColumn
        if (list.isEmpty()) {
            item(key = "empty") {
                // No background block behind empty states (design.md section 7.1).
                Column(Modifier.padding(horizontal = ScreenMargin, vertical = 16.dp)) {
                    IllustratedEmptyState(
                        "No invoices yet",
                        if (profile == null) {
                            "Start in Settings: add your details and bank account. Then add a client and create your first invoice."
                        } else {
                            "Tap the blue + button to create your first one."
                        },
                        AppIcons.Document,
                    )
                }
            }
            return@LazyColumn
        }
        item(key = "outstanding") { OutstandingCard(list, today, Modifier.padding(horizontal = ScreenMargin)) }
        item(key = "filters") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 8.dp, start = ScreenMargin, end = ScreenMargin),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InvoiceFilter.entries.forEach { f ->
                    FilterPill(f.label, selected = filter == f, onClick = { filter = f })
                }
            }
        }
        val shown = list.filter { filter.matches(it.status(today)) }
        item(key = "title") {
            SectionHeader(
                if (filter == InvoiceFilter.ALL) "All invoices" else filter.label,
                Modifier.padding(horizontal = ScreenMargin),
                trailing = if (shown.size == 1) "1 invoice" else "${shown.size} invoices",
            )
        }
        if (shown.isEmpty()) {
            item(key = "none") { EmptyState("Nothing here", "No invoices match this filter.") }
        }
        items(shown, key = { it.id }) { invoice ->
            InvoiceRow(invoice, today, Modifier.padding(horizontal = ScreenMargin)) { nav.go(Screen.InvoiceDetail(invoice.id)) }
        }
    }
}

private fun greeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

/** Money still owed, per currency, across sent invoices (design.md 3.2 two-weight headline). */
@Composable
private fun OutstandingCard(list: List<InvoiceSummary>, today: kotlinx.datetime.LocalDate, modifier: Modifier = Modifier) {
    val c = Ui.colors
    val unpaid = list.filter {
        val s = it.status(today)
        s == PaymentStatus.SENT || s == PaymentStatus.PARTLY_PAID || s == PaymentStatus.OVERDUE
    }
    val owed = unpaid.groupBy { it.currency }.mapValues { (_, l) -> l.sumOf { it.outstandingMinor } }.filterValues { it > 0 }
    val overdue = unpaid.count { it.status(today) == PaymentStatus.OVERDUE }
    HeroCard(modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                if (owed.isEmpty()) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = c.ink, fontWeight = FontWeight.SemiBold)) { append("All paid") }
                            withStyle(SpanStyle(color = c.greyText, fontWeight = FontWeight.Normal)) { append("\nnothing to collect") }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                    )
                } else {
                    Currency.entries.filter { it in owed }.forEach { cur ->
                        Text(
                            MoneyFormat.format(owed.getValue(cur), cur),
                            style = MaterialTheme.typography.displaySmall.tabular(),
                            color = c.ink,
                        )
                    }
                    Text("still to collect", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal), color = c.greyText)
                }
            }
            RupeeCoin()
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val waiting = unpaid.size
            OutlineChip(if (waiting == 1) "1 unpaid invoice" else "$waiting unpaid invoices")
            if (overdue > 0) StatusPill(if (overdue == 1) "1 overdue" else "$overdue overdue", statusColors(PaymentStatus.OVERDUE))
        }
    }
}

@Composable
private fun StatusPill(text: String, colors: StatusColors) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(colors.fill)
            .padding(start = 10.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(colors.content))
        Spacer(Modifier.width(6.dp))
        Text(text, color = colors.content, style = MaterialTheme.typography.labelMedium)
    }
}

/** A small glossy blue coin stamped with the rupee sign, the home screen's mascot. */
@Composable
private fun RupeeCoin() {
    val c = Ui.colors
    Box(
        Modifier
            .size(64.dp)
            .shadow(12.dp, CircleShape, ambientColor = c.primary.copy(alpha = 0.35f), spotColor = c.primary.copy(alpha = 0.45f))
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF9CC3F8), c.primary, Color(0xFF2559B8)), center = androidx.compose.ui.geometry.Offset(40f, 40f)))
            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("₹", color = Color.White, style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
private fun InvoiceRow(invoice: InvoiceSummary, today: kotlinx.datetime.LocalDate, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Ui.colors
    AppCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlineChip(invoice.number)
            Spacer(Modifier.weight(1f))
            RoundIconButton(AppIcons.ArrowUpRight, "Open invoice", onClick, style = RoundStyle.Dark, size = 40.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text(invoice.clientName, style = MaterialTheme.typography.titleMedium, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "Issued ${InvoiceDates.format(invoice.issueDate)}",
            style = MaterialTheme.typography.bodySmall,
            color = c.greyText,
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(invoice.status(today))
            Spacer(Modifier.weight(1f))
            Text(
                MoneyFormat.format(invoice.totalMinor, invoice.currency),
                style = MaterialTheme.typography.titleMedium.tabular(),
                color = c.ink,
            )
        }
    }
}
