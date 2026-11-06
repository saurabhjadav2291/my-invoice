@file:OptIn(ExperimentalMaterial3Api::class)

package com.revishingstudio.invoice.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revishingstudio.invoice.core.Client
import com.revishingstudio.invoice.core.Currency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ClientsScreen(nav: Navigator, modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    val clients by actions.repository.observeClients().collectAsStateWithLifecycle(emptyList())

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Clients") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.go(Screen.ClientEditor(null)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New client") },
            )
        },
    ) { padding ->
        if (clients.isEmpty()) {
            EmptyState(
                "No clients yet",
                "Add the people and companies you invoice. Their details appear under \"Bill to\".",
                Modifier.padding(padding),
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(clients, key = { it.id }) { client ->
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable { nav.go(Screen.ClientEditor(client.id)) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(client.name, style = MaterialTheme.typography.titleMedium)
                        val sub = listOf(client.legalName, client.email, client.defaultCurrency.code)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun ClientEditorScreen(id: Long?, nav: Navigator, modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    var loaded by remember { mutableStateOf(id == null) }
    var name by remember { mutableStateOf("") }
    var legalName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var taxIds by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(Currency.INR) }
    var confirmArchive by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != null) {
            withContext(Dispatchers.IO) { actions.repository.client(id) }?.let { c ->
                name = c.name
                legalName = c.legalName
                address = c.address
                phone = c.phone
                email = c.email
                taxIds = c.taxIds
                currency = c.defaultCurrency
            }
            loaded = true
        }
    }

    fun save() {
        if (name.isBlank()) {
            actions.message("Please enter the client's name")
            return
        }
        val client = Client(
            id = id ?: 0,
            name = name.trim(),
            legalName = legalName.trim(),
            address = address.trim(),
            phone = phone.trim(),
            email = email.trim(),
            taxIds = taxIds.trim(),
            defaultCurrency = currency,
        )
        actions.run("Client saved", onDone = { nav.back() }) { saveClient(client) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(if (id == null) "New client" else "Client", nav::back) {
                TextButton(onClick = ::save) { Text("Save") }
            }
        },
    ) { padding ->
        if (!loaded) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Field("Name (shown in bold)", name, { name = it })
            Field("Registered name (optional)", legalName, { legalName = it }, supportingText = "e.g. YLS Marketing Consultant Pty Ltd")
            Field("Address", address, { address = it }, singleLine = false, minLines = 2)
            Field("Phone", phone, { phone = it }, keyboardType = KeyboardType.Phone)
            Field("Email", email, { email = it }, keyboardType = KeyboardType.Email)
            Field("Tax IDs (optional)", taxIds, { taxIds = it }, supportingText = "e.g. ABN: 12345678901  ACN: 123456789, or a GSTIN")
            DropdownField(
                label = "Usual currency",
                selected = currency,
                options = Currency.entries,
                optionLabel = { "${it.code} (${it.symbol})" },
                onSelect = { currency = it },
            )
            Button(onClick = ::save, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Save") }
            if (id != null) {
                TextButton(onClick = { confirmArchive = true }) {
                    Text("Archive client", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmArchive && id != null) {
        ConfirmDialog(
            title = "Archive client?",
            message = "They disappear from your client list. Their past invoices stay as they are.",
            confirmLabel = "Archive",
            onConfirm = { actions.run("Client archived", onDone = { nav.back() }) { archiveClient(id) } },
            onDismiss = { confirmArchive = false },
        )
    }
}
