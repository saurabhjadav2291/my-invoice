@file:OptIn(ExperimentalMaterial3Api::class)

package com.revishingstudio.invoice.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revishingstudio.invoice.core.Currency
import com.revishingstudio.invoice.core.InvoiceDates
import com.revishingstudio.invoice.core.InvoiceSummary
import com.revishingstudio.invoice.core.MoneyFormat
import com.revishingstudio.invoice.core.PaymentStatus
import com.revishingstudio.invoice.backup.BackupPrefs

/** The studio logo from Settings, decoded once per path. */
@Composable
fun rememberLogo(path: String?, version: Int = 0): ImageBitmap? = remember(path, version) {
    path?.let { runCatching { com.revishingstudio.invoice.pdf.LogoImages.decode(it, maxSide = 512)?.asImageBitmap() }.getOrNull() }
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val backupPrefs = remember { BackupPrefs.get(context) }
    val backup by backupPrefs.status.collectAsStateWithLifecycle()
    val backupController = rememberBackupController()
    BackupDialogs(backupController)
    // Once the person has set up their details, ask once about Drive backup, as other apps do.
    if (profile != null && !backup.driveEnabled && !backup.drivePromptAnswered) {
        DriveBackupPrompt(
            onTurnOn = {
                backupPrefs.answerDrivePrompt()
                backupController.turnOnDrive()
            },
            onNotNow = backupPrefs::answerDrivePrompt,
        )
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomBarPadding.current + 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            ScreenHeader(
                title = "Invoices",
                subtitle = profile?.studioName?.takeIf { it.isNotBlank() } ?: profile?.ownerName,
                avatar = logo?.let { image -> @Composable { Avatar(image, profile?.studioName ?: "", size = null) } },
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
                            "Tap + to create your first one"
                        },
                        AppIcons.Document,
                    )
                }
            }
            return@LazyColumn
        }
        backupNudge(backup, list.size)?.let { (title, detail) ->
            item(key = "backup") {
                BackupNudge(
                    title,
                    detail,
                    onOpen = { nav.go(Screen.Backup) },
                    onDismiss = backupPrefs::dismissBanner,
                    modifier = Modifier.padding(horizontal = ScreenMargin),
                )
            }
        }
        item(key = "outstanding") { OutstandingCard(list, today, Modifier.padding(horizontal = ScreenMargin)) }
        item(key = "filters") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 4.dp, start = ScreenMargin, end = ScreenMargin),
            ) {
                PillChoice(InvoiceFilter.entries, filter, { it.label }, { filter = it }, fill = false)
            }
        }
        val shown = list.filter { filter.matches(it.status(today)) }
        item(key = "title") {
            SectionHeader(
                if (filter == InvoiceFilter.ALL) "All invoices" else filter.label,
                Modifier.padding(start = ScreenMargin, end = ScreenMargin, bottom = 4.dp),
                trailing = if (shown.size == 1) "1 invoice" else "${shown.size} invoices",
            )
        }
        if (shown.isEmpty()) {
            item(key = "none") { EmptyState("Nothing here", "No invoices match this filter") }
        }
        items(shown, key = { it.id }) { invoice ->
            InvoiceRow(invoice, today, Modifier.padding(horizontal = ScreenMargin)) { nav.go(Screen.InvoiceDetail(invoice.id)) }
        }
    }
}

/** Money still owed, per currency, across sent invoices. The home screen's key figure. */
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
                Text(if (owed.isEmpty()) "Nothing to collect" else "Still to collect", style = MaterialTheme.typography.bodySmall, color = c.greyText)
                Spacer(Modifier.height(4.dp))
                if (owed.isEmpty()) {
                    Text("All paid", style = MaterialTheme.typography.displaySmall, color = c.ink)
                } else {
                    Currency.entries.filter { it in owed }.forEach { cur ->
                        Text(
                            MoneyFormat.format(owed.getValue(cur), cur),
                            style = MaterialTheme.typography.displaySmall.tabular(),
                            color = c.ink,
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            PaperInvoice(unpaid.firstOrNull()?.number ?: list.firstOrNull()?.number)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val waiting = unpaid.size
            Text(
                if (waiting == 1) "1 unpaid invoice" else "$waiting unpaid invoices",
                style = MaterialTheme.typography.bodySmall,
                color = c.greyText,
            )
            if (overdue > 0) StatusPill(if (overdue == 1) "1 overdue" else "$overdue overdue", statusColors(PaymentStatus.OVERDUE))
        }
    }
}

@Composable
private fun StatusPill(text: String, colors: StatusColors) {
    Row(
        Modifier
            .clip(TagShape)
            .background(colors.fill)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(colors.content))
        Spacer(Modifier.width(4.dp))
        Text(text, color = colors.content, style = MaterialTheme.typography.labelMedium)
    }
}

/** One invoice: number and status on top, client and date, then the amount on the right. */
@Composable
private fun InvoiceRow(invoice: InvoiceSummary, today: kotlinx.datetime.LocalDate, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Ui.colors
    AppCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InvoiceNumber(invoice.number, Modifier.weight(1f))
            StatusChip(invoice.status(today))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(invoice.clientName, style = MaterialTheme.typography.titleMedium, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Issued ${InvoiceDates.format(invoice.issueDate)}", style = MaterialTheme.typography.bodySmall, color = c.grey)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                MoneyFormat.format(invoice.totalMinor, invoice.currency),
                style = MaterialTheme.typography.titleMedium.tabular(),
                color = c.ink,
            )
        }
    }
}
