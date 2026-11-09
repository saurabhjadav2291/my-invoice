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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
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

/**
 * Everything the business profile holds, as editable text. Account details and Business info
 * each edit their own part of one of these and save the whole profile, so the other part
 * keeps its saved values.
 */
private class ProfileForm {
    var loaded by mutableStateOf(false)
    var ownerName by mutableStateOf("")
    var studioName by mutableStateOf("")
    var address by mutableStateOf("")
    var pan by mutableStateOf("")
    var email by mutableStateOf("")
    var phone by mutableStateOf("")
    var logoPath by mutableStateOf<String?>(null)
    var logoVersion by mutableStateOf(0)
    var showGstNote by mutableStateOf(true)
    var showDueDate by mutableStateOf(true)
    var dueDays by mutableStateOf("15")
    var pattern by mutableStateOf(NumberingScheme.DEFAULT_PATTERN)
    var prefix by mutableStateOf("RS")
    var reset by mutableStateOf(NumberReset.FINANCIAL_YEAR)
    var startAt by mutableStateOf("1")

    fun load(p: BusinessProfile?) {
        if (p != null) {
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

    /**
     * The numbering scheme, or why it is invalid. Plain nullable values, not kotlin.Result:
     * Result is an inline class, and mapping it in a composable showed the scheme object
     * itself ("NumberingScheme(...)") instead of the number.
     */
    fun numbering(): Pair<NumberingScheme?, String?> = try {
        NumberingScheme(pattern.trim(), prefix.trim(), reset, startAt.toIntOrNull() ?: 0) to null
    } catch (e: IllegalArgumentException) {
        null to (e.message ?: "Check the numbering settings")
    }

    /** The profile to save, or null after telling the person what is missing. */
    fun build(actions: Actions, missingName: String): BusinessProfile? {
        if (ownerName.isBlank()) {
            actions.message(missingName)
            return null
        }
        val (scheme, error) = numbering()
        if (scheme == null) {
            actions.message(error ?: "Check the numbering settings")
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
            numbering = scheme,
        )
    }
}

@Composable
private fun rememberProfileForm(): ProfileForm {
    val actions = LocalActions.current
    val form = remember { ProfileForm() }
    LaunchedEffect(Unit) {
        form.load(withContext(Dispatchers.IO) { actions.repository.profile() })
    }
    return form
}

/**
 * The Settings tab: a short list of places to go, not the fields themselves
 * (design.md section 6.5, Settings).
 */
@Composable
fun SettingsScreen(nav: Navigator, modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    val context = LocalContext.current
    val profile by actions.repository.observeProfile().collectAsStateWithLifecycle(null)
    val banks by actions.repository.observeBankAccounts().collectAsStateWithLifecycle(emptyList())

    fun previewInvoice() {
        val saved = profile
        if (saved == null) {
            actions.message("First add your name in Account details")
            return
        }
        actions.run {
            val (sample, _) = InvoicePdfExporter.sampleInvoice()
            val withBank = sample.copy(bankAccount = banks.firstOrNull { it.isDefault } ?: sample.bankAccount)
            val file = InvoicePdfExporter.export(context, withBank, saved)
            withContext(Dispatchers.Main) { InvoicePdfExporter.open(context, file) }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "Settings",
            subtitle = profile?.studioName?.ifBlank { null } ?: profile?.ownerName ?: "Set up your details to start invoicing",
        )
        Column(Modifier.padding(horizontal = ScreenMargin)) {
            SettingsOption(
                icon = AppIcons.Person,
                title = "Account details",
                subtitle = profile?.let { p -> listOf(p.ownerName, p.email).filter { it.isNotBlank() }.joinToString(" · ") }
                    ?.ifBlank { null } ?: "Your name, email and phone",
                first = true,
            ) { nav.go(Screen.AccountDetails) }
            SettingsOption(
                icon = AppIcons.Briefcase,
                title = "Business info",
                subtitle = "Studio, address, PAN, logo, invoice numbers and bank",
            ) { nav.go(Screen.BusinessInfo) }
            SettingsOption(
                icon = AppIcons.Document,
                title = "Preview invoice",
                subtitle = "See a sample invoice with your details",
            ) { previewInvoice() }
            HorizontalDivider(color = Ui.colors.hairline)
            Spacer(Modifier.height(LocalBottomBarPadding.current + 16.dp))
        }
    }
}

/** One row of the Settings list: icon tile, title, a quiet line of what is inside, chevron. */
@Composable
private fun SettingsOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    first: Boolean = false,
    onClick: () -> Unit,
) {
    val c = Ui.colors
    Column(Modifier.fillMaxWidth()) {
        if (!first) HorizontalDivider(color = c.hairline)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(FormShape)
                .clickable(onClick = onClick, role = Role.Button)
                .padding(vertical = 14.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.primaryTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.greyText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(AppIcons.ChevronRight, contentDescription = null, tint = c.grey, modifier = Modifier.size(20.dp))
        }
    }
}

/** The person behind the invoices: name, email and phone. Sign-in and backup will live here too. */
@Composable
fun AccountDetailsScreen(nav: Navigator, modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    val form = rememberProfileForm()

    fun save() {
        form.build(actions, "Please enter your name")?.let { profile ->
            actions.run("Saved", onDone = { nav.back() }) { saveProfile(profile) }
        }
    }

    InnerScaffold(
        modifier = modifier,
        topBar = {
            BackTopBar("Account details", nav::back) {
                AppButton("Save", ::save, variant = ButtonVariant.Link)
            }
        },
    ) { padding ->
        if (!form.loaded) return@InnerScaffold
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = ScreenMargin, vertical = 8.dp),
        ) {
            SettingsSection("You", first = true) {
                Field("Your name (as on invoice)", form.ownerName, { form.ownerName = it }, placeholder = "Your full name")
                Field("Email", form.email, { form.email = it }, keyboardType = KeyboardType.Email, placeholder = "e.g. hello@yourstudio.com")
                Field("Phone", form.phone, { form.phone = it }, keyboardType = KeyboardType.Phone, placeholder = "e.g. +91 98765 43210")
            }
            AppButton("Save", ::save, Modifier.fillMaxWidth().padding(top = 24.dp))
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** What the invoice says about the business: studio, address, PAN, logo, numbering and bank. */
@Composable
fun BusinessInfoScreen(nav: Navigator, modifier: Modifier = Modifier) {
    val actions = LocalActions.current
    val context = LocalContext.current
    val banks by actions.repository.observeBankAccounts().collectAsStateWithLifecycle(emptyList())
    val form = rememberProfileForm()
    val c = Ui.colors

    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val target = File(context.filesDir, "logo")
            runCatching {
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    target.outputStream().use { input.copyTo(it) }
                }
            }.onSuccess {
                form.logoPath = target.absolutePath
                form.logoVersion++
            }.onFailure { actions.message("Could not read that image") }
        }
    }

    fun save() {
        form.build(actions, "First add your name in Account details")?.let { profile ->
            actions.run("Saved", onDone = { nav.back() }) { saveProfile(profile) }
        }
    }

    InnerScaffold(
        modifier = modifier,
        topBar = {
            BackTopBar("Business info", nav::back) {
                AppButton("Save", ::save, variant = ButtonVariant.Link)
            }
        },
    ) { padding ->
        if (!form.loaded) return@InnerScaffold
        val logo = rememberLogo(form.logoPath, form.logoVersion)
        val (scheme, numberingError) = form.numbering()
        val numberPreview: String? = scheme?.let { s -> s.format(today(), s.startAt) }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = ScreenMargin, vertical = 8.dp),
        ) {
            SettingsSection("Business", first = true) {
                Field("Studio name (for the sign-off)", form.studioName, { form.studioName = it }, placeholder = "e.g. Revishing Studio")
                Field("Address", form.address, { form.address = it }, singleLine = false, minLines = 3, placeholder = "Street, city, PIN code")
                Field("PAN", form.pan, { form.pan = it }, placeholder = "e.g. ABCDE1234F")
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
                    AppButton(if (form.logoPath == null) "Choose logo" else "Change logo", { pickLogo.launch("image/*") }, variant = ButtonVariant.Secondary)
                    if (form.logoPath != null) AppButton("Remove", { form.logoPath = null }, variant = ButtonVariant.Tertiary, tint = c.error)
                }
            }

            SettingsSection("Invoice numbers") {
                DropdownField(
                    label = "Style",
                    selected = NumberStyle.entries.firstOrNull { it.pattern == form.pattern } ?: NumberStyle.CUSTOM,
                    options = NumberStyle.entries,
                    optionLabel = { it.label },
                    onSelect = { style ->
                        style.pattern?.let { form.pattern = it }
                        form.reset = when (style) {
                            NumberStyle.FINANCIAL_YEAR -> NumberReset.FINANCIAL_YEAR
                            NumberStyle.YEAR_NUMBER -> NumberReset.NEVER
                            NumberStyle.CUSTOM -> form.reset
                        }
                    },
                )
                Field(
                    "Pattern",
                    form.pattern,
                    { form.pattern = it },
                    placeholder = "{PREFIX}/{FY}/{SEQ:3}",
                    supportingText = "{PREFIX} {FY} {FYS} {YYYY} {YY} {SEQ} or {SEQ:3}",
                )
                Field("Prefix", form.prefix, { form.prefix = it }, placeholder = "e.g. RS")
                DropdownField(
                    label = "Restart numbering",
                    selected = form.reset,
                    options = NumberReset.entries,
                    optionLabel = ::resetLabel,
                    onSelect = { form.reset = it },
                )
                Field(
                    "Start from number",
                    form.startAt,
                    { form.startAt = it.filter(Char::isDigit) },
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
                SwitchRow("Show due date", null, form.showDueDate) { form.showDueDate = it }
                SwitchRow("Show \"not registered under GST\"", "Recommended while you are not GST registered", form.showGstNote) {
                    form.showGstNote = it
                }
                Field(
                    "Days until due",
                    form.dueDays,
                    { form.dueDays = it.filter(Char::isDigit) },
                    placeholder = "15",
                    keyboardType = KeyboardType.Number,
                )
            }

            SettingsSection("Bank accounts") {
                banks.forEach { bank -> BankRow(bank) { nav.go(Screen.BankEditor(bank.id)) } }
                AppButton("Add bank account", { nav.go(Screen.BankEditor(null)) }, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = AppIcons.Plus)
            }

            HorizontalDivider(color = c.hairline, modifier = Modifier.padding(top = 20.dp))
            AppButton("Save", ::save, Modifier.fillMaxWidth().padding(top = 20.dp))
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * One group of settings: a hairline above it (except the first), the heading, then the
 * fields with 12 dp between them. No card behind it (design.md section 6.5, Settings).
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
