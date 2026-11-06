package com.revishingstudio.invoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revishingstudio.invoice.core.InvoiceDates
import com.revishingstudio.invoice.core.InvoiceSummary
import com.revishingstudio.invoice.core.MoneyFormat
import com.revishingstudio.invoice.pdf.InvoicePdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** The orange accent from your invoice design. */
private val Accent = Color(0xFFF26B1D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as InvoiceApp).repository
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Accent)) {
                val invoices by repository.observeInvoiceSummaries()
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                InvoiceListScreen(invoices)
            }
        }
    }
}

// Placeholder list. The full screens (clients, invoice editor, payments) come in step 3.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceListScreen(invoices: List<InvoiceSummary>) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val previewSample: () -> Unit = {
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                val (invoice, profile) = InvoicePdfExporter.sampleInvoice()
                InvoicePdfExporter.export(context, invoice, profile)
            }
            InvoicePdfExporter.open(context, file)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Invoice") },
                actions = { TextButton(onClick = previewSample) { Text("Sample PDF") } },
            )
        },
    ) { padding ->
        if (invoices.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("No invoices yet", style = MaterialTheme.typography.titleMedium)
                Text("Creating invoices arrives in a coming update.")
                Text("Tap \"Sample PDF\" to see how your invoices will look.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(invoices, key = { it.id }) { invoice ->
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(invoice.clientName, style = MaterialTheme.typography.titleMedium)
                                Text("${invoice.number} · ${InvoiceDates.format(invoice.issueDate)}")
                            }
                            Column {
                                Text(MoneyFormat.format(invoice.totalMinor, invoice.currency))
                                Text(invoice.status(today).label, color = Accent)
                            }
                        }
                    }
                }
            }
        }
    }
}
