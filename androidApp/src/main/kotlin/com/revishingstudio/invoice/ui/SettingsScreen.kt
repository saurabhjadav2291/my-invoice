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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.HorizontalDivider
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

    // Plain nullable values, not kotlin.Result: Result is an inline class, and mapping it in a
    // composable showed the scheme object itself ("NumberingScheme(...)") instead of the number.
    var numberingError: String? = null
    val numbering: NumberingScheme? = try {
        NumberingScheme(pattern.trim(), prefix.trim(), reset, startAt.toIntOrNull() ?: 0)
    } catch (e: IllegalArgumentException) {
        numberingError = e.message ?: "Check the numbering settings"
        null
    }
    val numberPreview: String? = numbering?.let { scheme -> scheme.format(today(), scheme.startAt) }

    fun buildProfile(): BusinessProfile? {
        if (ownerName.isBlank()) {
            actions.message("Please enter your name")
            return null
        }
        if (numbering == null) {
            actions.message(numberingError ?: "Check the numbering settings")
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
        ScreenHeader(
            title = "Settings",
            subtitle = studioName.ifBlank { "Your details, numbering and bank" },
        ) {
            AppButton("Save", ::saveAll, variant = ButtonVariant.Link)
        }
        Column(Modifier.padding(horizontal = ScreenMargin)) {
            SettingsSection("Your details", first = true) {
                Field("Your name (as on invoice)", ownerName, { ownerName = it }, placeholder = "Your full name")
                Field("Studio name (for the sign-off)", studioName, { studioName = it }, placeholder = "e.g. Revishing Studio")
                Field("Address", address, { address = it }, singleLine = false, minLines = 3, placeholder = "Street, city, PIN code")
                Field("PAN", pan, { pan = it }, placeholder = "e.g. ABCDE1234F")
                Field("Email", email, { email = it }, keyboardType = KeyboardType.Email, placeholder = "e.g. hello@yourstudio.com")
                Field("Phone", phone, { phone = it }, keyboardType = KeyboardType.Phone, placeholder = "e.g. +91 98765 43210")
            }

            SettingsSection("Logo") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (logo != null) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, c.hairline, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(logo, contentDescription = "Logo", modifier = Modifier.size(40.dp))
                        }
                    }
                    AppButton(if (logoPath == null) "Choose logo" else "Change logo", { pickLogo.launch("image/*") }, variant = ButtonVariant.Secondary)
                    if (logoPath != null) AppButton("Remove", { logoPath = null }, variant = ButtonVariant.Tertiary, tint = c.error)
                }
            }

            SettingsSection("Invoice numbers") {
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
                    placeholder = "{PREFIX}/{FY}/{SEQ:3}",
                    supportingText = "{PREFIX} {FY} {FYS} {YYYY} {YY} {SEQ} or {SEQ:3}",
                )
                Field("Prefix", prefix, { prefix = it }, placeholder = "e.g. RS")
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
                    placeholder = "1",
                    keyboardType = KeyboardType.Number,
                    supportingText = "To continue your old series, enter the next number (e.g. 32)",
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Next invoice", style = MaterialTheme.typography.bodyLarge, color = c.greyText, modifier = Modifier.weight(1f).padding(start = 4.dp))
                    if (numberPreview != null) {
                        OutlineChip(numberPreview, mono = true)
                    } else {
                        Text(numberingError ?: "Invalid pattern", color = c.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            SettingsSection("On the invoice") {
                SwitchRow("Show due date", null, showDueDate) { showDueDate = it }
                SwitchRow("Show \"not registered under GST\"", "Recommended while you are not GST registered", showGstNote) {
                    showGstNote = it
                }
                Field(
                    "Days until due",
                    dueDays,
                    { dueDays = it.filter(Char::isDigit) },
                    placeholder = "15",
                    keyboardType = KeyboardType.Number,
                )
            }

            SettingsSection("Bank accounts") {
                banks.forEach { bank -> BankRow(bank) { nav.go(Screen.BankEditor(bank.id)) } }
                AppButton("Add bank account", { nav.go(Screen.BankEditor(null)) }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = AppIcons.Plus)
            }

            SettingsSection("Preview") {
            AppButton(
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
                variant = ButtonVariant.Secondary,
                icon = AppIcons.Document,
            )
            }

            HorizontalDivider(color = c.hairline, modifier = Modifier.padding(top = 20.dp))
            AppButton("Save settings", ::saveAll, Modifier.fillMaxWidth().padding(top = 20.dp))
            Spacer(Modifier.height(LocalBottomBarPadding.current + 16.dp))
        }
    }
}

/**
 * One group of settings: a hairline above it (except the first), the heading, then the
 * fields with 12 dp between them. No card behind it (design.md section 8, Settings).
 */
@Composable
private fun SettingsSection(title: String, first: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (!first) HorizontalDivider(color = Ui.colors.hairline, modifier = Modifier.padding(top = 20.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = Ui.colors.ink,
            modifier = Modifier.padding(top = if (first) 0.dp else 20.dp, bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun BankRow(bank: BankAccount, onClick: () -> Unit) {
    val c = Ui.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FormShape)
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
        Icon(AppIcons.ArrowUpRight, contentDescription = "Edit bank account", tint = c.grey, modifier = Modifier.size(18.dp))
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

    InnerScaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(if (id == null) "Add bank account" else "Bank account", nav::back) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Field("Nickname (only in the app)", label, { label = it }, placeholder = "e.g. Kotak current")
                Field("Bank name", bankName, { bankName = it }, placeholder = "e.g. Kotak Mahindra Bank")
                Field("Account name", accountName, { accountName = it }, placeholder = "Name on the account")
                Field("Account number", accountNumber, { accountNumber = it }, keyboardType = KeyboardType.Number, placeholder = "e.g. 1234567890")
                Field("IFSC code", ifsc, { ifsc = it }, placeholder = "e.g. KKBK0000123")
                Field("SWIFT code (for overseas payments)", swift, { swift = it }, placeholder = "e.g. KKBKINBB")
            }
            AppCard {
                SwitchRow("Use by default on new invoices", null, isDefault) { isDefault = it }
            }
            AppButton("Save", ::save, Modifier.fillMaxWidth().padding(top = 8.dp))
            if (id != null) {
                AppButton("Delete this account", { confirmDelete = true }, Modifier.align(Alignment.CenterHorizontally), variant = ButtonVariant.Tertiary, tint = Ui.colors.error)
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
