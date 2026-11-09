@file:OptIn(ExperimentalMaterial3Api::class)

package com.revishingstudio.invoice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    val clients by actions.repository.observeClients().collectAsStateWithLifecycle(null)
    val c = Ui.colors
    val list = clients

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomBarPadding.current + 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            ScreenHeader(
                title = "Clients",
                subtitle = when (val n = list?.size ?: 0) {
                    0 -> "The people and companies you bill"
                    1 -> "1 client"
                    else -> "$n clients"
                },
            ) {
                RoundIconButton(AppIcons.Plus, "Add client", { nav.go(Screen.ClientEditor(null)) }, style = RoundStyle.Light)
            }
        }
        if (list == null) return@LazyColumn
        if (list.isEmpty()) {
            item(key = "empty") {
                // No background block behind empty states (design.md section 7.1).
                Column(Modifier.padding(horizontal = ScreenMargin, vertical = 16.dp)) {
                    IllustratedEmptyState(
                        "No clients yet",
                        "Add the people and companies you invoice. Their details appear under \"Bill to\".",
                        AppIcons.Clients,
                        actionLabel = "Add client",
                        onAction = { nav.go(Screen.ClientEditor(null)) },
                    )
                }
            }
            return@LazyColumn
        }
        items(list, key = { it.id }) { client ->
            val open = { nav.go(Screen.ClientEditor(client.id)) }
            AppCard(Modifier.padding(horizontal = ScreenMargin), onClick = open) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(null, client.name)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(client.name, style = MaterialTheme.typography.titleMedium, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val sub = listOf(client.legalName, client.email)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                        if (sub.isNotBlank()) {
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = c.greyText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    OutlineChip(client.defaultCurrency.code)
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

    InnerScaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(if (id == null) "New client" else "Client", nav::back) {
                AppButton("Save", ::save, variant = ButtonVariant.Link)
            }
        },
    ) { padding ->
        if (!loaded) return@InnerScaffold
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = ScreenMargin, vertical = 8.dp),
        ) {
            // No cards: sections split by hairlines, as in Business info (design.md section 6.5).
            SettingsSection("Client", first = true) {
                Field("Name (shown in bold)", name, { name = it }, placeholder = "e.g. Acme Studio")
                Field("Registered name (optional)", legalName, { legalName = it }, supportingText = "e.g. YLS Marketing Consultant Pty Ltd", placeholder = "Legal company name")
                Field("Address", address, { address = it }, singleLine = false, minLines = 2, placeholder = "Street, city, postcode, country")
            }
            SettingsSection("Contact and tax") {
                Field("Phone", phone, { phone = it }, keyboardType = KeyboardType.Phone, placeholder = "e.g. +61 400 123 456")
                Field("Email", email, { email = it }, keyboardType = KeyboardType.Email, placeholder = "e.g. accounts@client.com")
                Field("Tax IDs (optional)", taxIds, { taxIds = it }, supportingText = "e.g. ABN: 12345678901  ACN: 123456789, or a GSTIN", placeholder = "ABN, ACN or GSTIN")
            }
            SettingsSection("Usual currency") {
                PillChoice(Currency.entries, currency, { it.code }, { currency = it }, Modifier.fillMaxWidth())
            }
            HorizontalDivider(color = Ui.colors.hairline, modifier = Modifier.padding(top = 20.dp))
            AppButton("Save", ::save, Modifier.fillMaxWidth().padding(top = 20.dp))
            if (id != null) {
                AppButton("Archive client", { confirmArchive = true }, Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp), variant = ButtonVariant.Tertiary, tint = Ui.colors.error)
            }
            Spacer(Modifier.navigationBarsPadding().height(32.dp))
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
