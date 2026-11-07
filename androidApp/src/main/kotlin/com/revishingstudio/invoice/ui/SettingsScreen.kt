@file:OptIn(ExperimentalMaterial3Api::class)

package com.revishingstudio.invoice.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revishingstudio.invoice.core.BankAccount
import com.revishingstudio.invoice.core.BusinessProfile
import com.revishingstudio.invoice.core.NumberReset
import com.revishingstudio.invoice.core.NumberingScheme
import com.revishingstudio.invoice.pdf.InvoicePdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private enum class NumberStyle(val label: String, val pattern: String?) {
    FINANCIAL_YEAR("Financial year, e.g. RS/26-27/001", NumberingScheme.DEFAULT_PATTERN),
    YEAR_NUMBER("Year + number, e.g. 26031", "{YY}{SEQ:3}"),
    CUSTOM("Custom pattern", null),
}

private fun resetLabel(reset: NumberReset) = when (reset) {
    NumberReset.FINANCIAL_YEAR -> "Every 1 April"
    NumberReset.CALENDAR_YEAR -> "Every 1 January"
    NumberReset.NEVER -> "Never"
}

@Composable
fun SettingsScreen(nav: Navigator, modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    val context = LocalContext.current
    val banks by actions.repository.observeBankAccounts().collectAsStateWithLifecycle(emptyList())

    var loaded by remember { mutableStateOf(false) }
    var ownerName by remember { mutableStateOf("") }
    var studioName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var logoPath by remember { mutableStateOf<String?>(null) }
    var logoVersion by remember { mutableStateOf(0) }
    var showGstNote by remember { mutableStateOf(true) }
    var showDueDate by remember { mutableStateOf(true) }
    var dueDays by remember { mutableStateOf("15") }
    var pattern by remember { mutableStateOf(NumberingScheme.DEFAULT_PATTERN) }
    var prefix by remember { mutableStateOf("RS") }
    var reset by remember { mutableStateOf(NumberReset.FINANCIAL_YEAR) }
    var startAt by remember { mutableStateOf("1") }

    LaunchedEffect(Unit) {
        val profile = withContext(Dispatchers.IO) { actions.repository.profile() }
        profile?.let { p ->
            ownerName = p.ownerName
            studioName = p.studioName
            address = p.address
            pan = p.pan
            email = p.email
            phone = p.phone
            logoPath = p.logoPath
            showGstNote = p.showGstNote
            showDueDate = p.showDueDate
            dueDays = p.defaultDueDays.toString()
            pattern = p.numbering.pattern
            prefix = p.numbering.prefix
            reset = p.numbering.reset
            startAt = p.numbering.startAt.toString()
        }
        loaded = true
    }

    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val target = File(context.filesDir, "logo")
            runCatching {
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    target.outputStream().use { input.copyTo(it) }
                }
            }.onSuccess {
                logoPath = target.absolutePath
                logoVersion++
            }.onFailure { actions.message("Could not read that image") }
        }
    }

    val numberingResult = runCatching {
        NumberingScheme(pattern.trim(), prefix.trim(), reset, startAt.toIntOrNull() ?: 0)
    }
    val numberPreview = numberingResult.map { it.format(today(), it.startAt) }

    fun buildProfile(): BusinessProfile? {
        if (ownerName.isBlank()) {
            actions.message("Please enter your name")
            return null
        }
        val numbering = numberingResult.getOrElse {
            actions.message(it.message ?: "Check the numbering settings")
            return null
        }
        return BusinessProfile(
            ownerName = ownerName.trim(),
            studioName = studioName.trim(),
            address = address.trim(),
            pan = pan.trim().uppercase(),
            email = email.trim(),
            phone = phone.trim(),
            logoPath = logoPath,
            showGstNote = showGstNote,
            showDueDate = showDueDate,
            defaultDueDays = dueDays.toIntOrNull()?.coerceIn(0, 365) ?: 15,
            numbering = numbering,
        )
    }

    val c = Ui.colors
    val logo = rememberLogo(logoPath, logoVersion)
    fun saveAll() {
        buildProfile()?.let { profile -> actions.run("Saved") { saveProfile(profile) } }
    }

    if (!loaded) return
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SkyHeader(
            title = "Settings",
            subtitle = studioName.ifBlank { "Your details, numbering and bank" },
            avatar = { Avatar(logo, studioName.ifBlank { ownerName }) },
        ) {
            HeaderSavePill(::saveAll)
        }
        Column(
            Modifier.padding(horizontal = ScreenMargin),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Your details", Modifier.padding(top = 0.dp))
            AppCard(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Field("Your name (as on invoice)", ownerName, { ownerName = it })
                Field("Studio name (for the sign-off)", studioName, { studioName = it })
                Field("Address", address, { address = it }, singleLine = false, minLines = 3)
                Field("PAN", pan, { pan = it })
                Field("Email", email, { email = it }, keyboardType = KeyboardType.Email)
                Field("Phone", phone, { phone = it }, keyboardType = KeyboardType.Phone)
            }

            SectionTitle("Logo")
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (logo != null) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, c.hairline, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(logo, contentDescription = "Logo", modifier = Modifier.size(40.dp))
                        }
                    }
                    SecondaryButton(if (logoPath == null) "Choose logo" else "Change logo", { pickLogo.launch("image/*") }, height = 46.dp)
                    if (logoPath != null) TextPill("Remove", { logoPath = null }, color = c.error)
                }
            }

            SectionTitle("Invoice numbers")
            AppCard(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                DropdownField(
                    label = "Style",
                    selected = NumberStyle.entries.firstOrNull { it.pattern == pattern } ?: NumberStyle.CUSTOM,
                    options = NumberStyle.entries,
                    optionLabel = { it.label },
                    onSelect = { style ->
                        style.pattern?.let { pattern = it }
                        reset = when (style) {
                            NumberStyle.FINANCIAL_YEAR -> NumberReset.FINANCIAL_YEAR
                            NumberStyle.YEAR_NUMBER -> NumberReset.NEVER
                            NumberStyle.CUSTOM -> reset
                        }
                    },
                )
                Field(
                    "Pattern",
                    pattern,
                    { pattern = it },
                    supportingText = "{PREFIX} {FY} {FYS} {YYYY} {YY} {SEQ} or {SEQ:3}",
                )
                Field("Prefix", prefix, { prefix = it })
                DropdownField(
                    label = "Restart numbering",
                    selected = reset,
                    options = NumberReset.entries,
                    optionLabel = ::resetLabel,
                    onSelect = { reset = it },
                )
                Field(
                    "Start from number",
                    startAt,
                    { startAt = it.filter(Char::isDigit) },
                    keyboardType = KeyboardType.Number,
                    supportingText = "To continue your old series, enter the next number (e.g. 32)",
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Next invoice", style = MaterialTheme.typography.bodyLarge, color = c.greyText, modifier = Modifier.weight(1f).padding(start = 4.dp))
                    if (numberPreview.isSuccess) {
                        OutlineChip(numberPreview.getOrThrow())
                    } else {
                        Text(numberPreview.exceptionOrNull()?.message ?: "Invalid pattern", color = c.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            SectionTitle("On the invoice")
            AppCard(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SwitchRow("Show due date", null, showDueDate) { showDueDate = it }
                SwitchRow("Show \"not registered under GST\"", "Recommended while you are not GST registered", showGstNote) {
                    showGstNote = it
                }
                Field(
                    "Days until due",
                    dueDays,
                    { dueDays = it.filter(Char::isDigit) },
                    keyboardType = KeyboardType.Number,
                )
            }

            SectionTitle("Bank accounts")
            AppCard(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                banks.forEach { bank -> BankRow(bank) { nav.go(Screen.BankEditor(bank.id)) } }
                SecondaryButton("Add bank account", { nav.go(Screen.BankEditor(null)) }, Modifier.fillMaxWidth(), icon = AppIcons.Plus, height = 48.dp)
            }

            SectionTitle("Preview")
            SecondaryButton(
                "Preview a sample invoice",
                {
                    val profile = buildProfile()
                    if (profile != null) {
                        actions.run {
                            val (sample, _) = InvoicePdfExporter.sampleInvoice()
                            val withBank = sample.copy(bankAccount = banks.firstOrNull { it.isDefault } ?: sample.bankAccount)
                            val file = InvoicePdfExporter.export(context, withBank, profile)
                            withContext(Dispatchers.Main) { InvoicePdfExporter.open(context, file) }
                        }
                    }
                },
                Modifier.fillMaxWidth(),
                icon = AppIcons.Document,
            )

            PrimaryButton("Save settings", ::saveAll, Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Frosted "Save" pill on the sky header. */
@Composable
private fun HeaderSavePill(onClick: () -> Unit) {
    val c = Ui.colors
    Box(
        Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (c.isDark) 0.08f else 0.22f))
            .border(1.dp, Color.White.copy(alpha = if (c.isDark) 0.12f else 0.45f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Save", style = MaterialTheme.typography.labelLarge, color = c.onSky)
    }
}

@Composable
private fun BankRow(bank: BankAccount, onClick: () -> Unit) {
    val c = Ui.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(bank.label.ifBlank { bank.bankName }, style = MaterialTheme.typography.titleSmall, color = c.ink)
            Text("${bank.bankName} · ${bank.accountNumber}", style = MaterialTheme.typography.bodySmall, color = c.greyText)
        }
        if (bank.isDefault) {
            OutlineChip("Default")
            Spacer(Modifier.width(8.dp))
        }
        RoundIconButton(AppIcons.ArrowUpRight, "Edit bank account", onClick, style = RoundStyle.Dark, size = 36.dp)
    }
}

@Composable
fun BankEditorScreen(id: Long?, nav: Navigator, modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    var loaded by remember { mutableStateOf(id == null) }
    var label by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var ifsc by remember { mutableStateOf("") }
    var swift by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != null) {
            withContext(Dispatchers.IO) { actions.repository.bankAccounts().firstOrNull { it.id == id } }?.let { b ->
                label = b.label
                bankName = b.bankName
                accountName = b.accountName
                accountNumber = b.accountNumber
                ifsc = b.ifsc
                swift = b.swift
                isDefault = b.isDefault
            }
            loaded = true
        }
    }

    fun save() {
        if (bankName.isBlank() || accountNumber.isBlank()) {
            actions.message("Bank name and account number are needed")
            return
        }
        val account = BankAccount(
            id = id ?: 0,
            label = label.trim().ifBlank { bankName.trim() },
            bankName = bankName.trim().uppercase(),
            accountName = accountName.trim(),
            accountNumber = accountNumber.trim(),
            ifsc = ifsc.trim().uppercase(),
            swift = swift.trim().uppercase(),
            isDefault = isDefault,
        )
        actions.run("Bank account saved", onDone = { nav.back() }) { saveBankAccount(account) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            BackTopBar(if (id == null) "Add bank account" else "Bank account", nav::back) {
                TextPill("Save", ::save)
            }
        },
    ) { padding ->
        if (!loaded) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenMargin, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Field("Nickname (only in the app)", label, { label = it })
                Field("Bank name", bankName, { bankName = it })
                Field("Account name", accountName, { accountName = it })
                Field("Account number", accountNumber, { accountNumber = it }, keyboardType = KeyboardType.Number)
                Field("IFSC code", ifsc, { ifsc = it })
                Field("SWIFT code (for overseas payments)", swift, { swift = it })
            }
            AppCard {
                SwitchRow("Use by default on new invoices", null, isDefault) { isDefault = it }
            }
            PrimaryButton("Save", ::save, Modifier.padding(top = 8.dp))
            if (id != null) {
                TextPill("Delete this account", { confirmDelete = true }, color = Ui.colors.error, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
    if (confirmDelete && id != null) {
        ConfirmDialog(
            title = "Delete bank account?",
            message = "Invoices that used it will no longer show bank details.",
            confirmLabel = "Delete",
            onConfirm = { actions.run("Deleted", onDone = { nav.back() }) { deleteBankAccount(id) } },
            onDismiss = { confirmDelete = false },
        )
    }
}
