package com.revishingstudio.invoice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revishingstudio.invoice.backup.BackupPrefs
import com.revishingstudio.invoice.backup.BackupStatus
import com.revishingstudio.invoice.core.backup.BackupFormat

/** The quiet line under "Backup & restore" in the Settings list. */
fun backupSummary(status: BackupStatus): String = when {
    !status.driveEnabled -> "Google Drive backup is off"
    status.lastError != null && !status.upToDate -> "Needs attention: ${status.lastError}"
    status.lastBackupAt > 0 -> "Backed up ${formatBackupTime(status.lastBackupAt).replaceFirstChar { it.lowercase() }}"
    else -> "Google Drive backup is on"
}

/**
 * Backup & restore: encrypted Google Drive backup, the backup password, and a backup
 * file to keep anywhere (design.md section 6.7 form layout, no cards).
 */
@Composable
fun BackupScreen(nav: Navigator, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val status by remember { BackupPrefs.get(context).status }.collectAsStateWithLifecycle()
    val controller = rememberBackupController()
    val c = Ui.colors

    InnerScaffold(
        modifier = modifier,
        topBar = { BackTopBar("Backup & restore", nav::back) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = ScreenMargin, vertical = 8.dp),
        ) {
            SettingsSection("Google Drive", first = true) {
                Paragraph(
                    "Keeps an encrypted copy of your invoices, clients, business details and logo in a private " +
                        "folder of your Google Drive that only this app can see. It backs up once a day and soon after " +
                        "you make changes.",
                )
                SwitchRow(
                    title = "Back up to Google Drive",
                    subtitle = if (status.driveEnabled) status.account ?: "Connected" else "Off",
                    checked = status.driveEnabled,
                    onChange = { on -> if (on) controller.turnOnDrive() else controller.turnOffDrive() },
                )
                if (status.driveEnabled) {
                    InfoRow("Last backup", if (status.lastBackupAt > 0) formatBackupTime(status.lastBackupAt) else "Not yet")
                    val error = status.lastError
                    if (error != null && !status.upToDate) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = c.error, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    AppButton("Back up now", controller::backUpNow, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = AppIcons.CloudUpload)
                }
                AppButton(
                    "Restore from Google Drive",
                    controller::restoreFromDrive,
                    Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    icon = AppIcons.GoogleDrive,
                    iconInColour = true,
                )
            }
            SettingsSection("Backup password") {
                Paragraph(
                    if (status.hasPassword) {
                        "Your backups are locked with your backup password. You'll need it to restore on a new phone, " +
                            "and it can't be recovered if you forget it."
                    } else {
                        "You'll choose a password the first time you back up. It locks your backups, so keep it somewhere safe."
                    },
                )
                if (status.hasPassword) {
                    AppButton("Change password", controller::changePassword, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary)
                }
            }
            SettingsSection("Backup file") {
                Paragraph("Save an encrypted backup file to your phone or another app, and restore from it later. Works without Google.")
                AppButton("Export backup file", controller::exportFile, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = AppIcons.FileExport)
                AppButton("Restore from a file", controller::importFile, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary, icon = AppIcons.FileImport)
            }
            Spacer(Modifier.navigationBarsPadding().height(32.dp))
        }
    }
    BackupDialogs(controller)
}

@Composable
private fun Paragraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = Ui.colors.greyText, modifier = Modifier.padding(horizontal = 4.dp))
}

/** The questions and the "working" overlay of a [BackupController]. */
@Composable
fun BackupDialogs(controller: BackupController) {
    val c = Ui.colors
    when (val d = controller.dialog) {
        null -> Unit
        is BackupDialog.SetPassword -> PasswordDialog(
            title = if (d.changing) "New backup password" else "Choose a backup password",
            message = if (d.changing) {
                "New backups use this password. Backup files you exported before still open with the old one."
            } else {
                "Your backup is encrypted with this password. You'll need it to restore on a new phone. " +
                    "Keep it somewhere safe: it can't be recovered."
            },
            confirmLabel = "Save",
            confirmTwice = true,
            onDismiss = { controller.dialog = null },
            onConfirm = { pw ->
                controller.dialog = null
                d.then(pw)
            },
        )
        is BackupDialog.EnterPassword -> PasswordDialog(
            title = "Backup password",
            message = "Type the backup password this backup was made with.",
            error = if (d.wrongTry) "That password doesn't open this backup" else null,
            confirmLabel = "Unlock",
            confirmTwice = false,
            onDismiss = { controller.dialog = null },
            onConfirm = { pw ->
                controller.dialog = null
                d.then(pw)
            },
        )
        is BackupDialog.ConfirmRestore -> ConfirmDialog(
            title = "Restore this backup?",
            message = "${d.summary}.\n\nEverything on this phone is replaced by the backup: invoices, clients, " +
                "business details and bank accounts. The app restarts when it's done.",
            confirmLabel = "Restore",
            onConfirm = d.then,
            onDismiss = { controller.dialog = null },
        )
        is BackupDialog.ExistingBackup -> AlertDialog(
            onDismissRequest = { controller.dialog = null },
            title = { Text("A backup is already in this Drive", style = MaterialTheme.typography.titleLarge, color = c.ink) },
            text = {
                Text(
                    "${d.summary}.\n\nRestore it to this phone, or replace it with what's on this phone now?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.greyText,
                )
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    AppButton("Restore it", {
                        controller.dialog = null
                        d.restore()
                    }, variant = ButtonVariant.Link)
                    AppButton("Back up this phone", {
                        controller.dialog = null
                        d.replace()
                    }, variant = ButtonVariant.Link, tint = c.error)
                    AppButton("Cancel", { controller.dialog = null }, variant = ButtonVariant.Tertiary)
                }
            },
            shape = SmoothShape(20.dp),
            containerColor = c.surface,
        )
        is BackupDialog.ConfirmTurnOff -> ConfirmDialog(
            title = "Turn off Drive backup?",
            message = "New changes won't be backed up. The backup already in your Google Drive stays there, " +
                "and you can restore from it any time.",
            confirmLabel = "Turn off",
            onConfirm = d.then,
            onDismiss = { controller.dialog = null },
        )
    }
    controller.busy?.let { label ->
        Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
            Row(
                Modifier
                    .background(c.surface, SmoothShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = c.primary, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge, color = c.ink)
            }
        }
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmTwice: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
    error: String? = null,
) {
    val c = Ui.colors
    var password by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf(error) }
    fun submit() {
        problem = when {
            confirmTwice && password.length < BackupFormat.MIN_PASSWORD_LENGTH ->
                "Use at least ${BackupFormat.MIN_PASSWORD_LENGTH} characters"
            confirmTwice && password != again -> "The two passwords don't match"
            password.isEmpty() -> "Type the password"
            else -> null
        }
        if (problem == null) onConfirm(password.toCharArray())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, color = c.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message, style = MaterialTheme.typography.bodyLarge, color = c.greyText)
                Field("Password", password, { password = it }, password = true, placeholder = "At least ${BackupFormat.MIN_PASSWORD_LENGTH} characters")
                if (confirmTwice) Field("Type it again", again, { again = it }, password = true, placeholder = "Same password")
                problem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.error) }
            }
        },
        confirmButton = { AppButton(confirmLabel, ::submit, variant = ButtonVariant.Link) },
        dismissButton = { AppButton("Cancel", onDismiss, variant = ButtonVariant.Tertiary) },
        shape = SmoothShape(20.dp),
        containerColor = c.surface,
    )
}

// ---------------------------------------------------------------------------
// On the Invoices tab
// ---------------------------------------------------------------------------

/** Days a closed banner stays away. */
private const val BANNER_QUIET_DAYS = 14L
/** A backup this old with unsaved changes counts as out of date. */
private const val STALE_DAYS = 3L
private const val DAY = 86_400_000L

/** The banner's message, or null when nothing needs saying. */
fun backupNudge(status: BackupStatus, now: Long = System.currentTimeMillis()): Pair<String, String>? {
    if (now - status.bannerDismissedAt < BANNER_QUIET_DAYS * DAY) return null
    return when {
        !status.driveEnabled -> "Your invoices aren't backed up" to "Turn on Google Drive backup"
        status.upToDate -> null
        status.lastError != null && now - status.lastBackupAt > DAY -> "Backup needs attention" to status.lastError
        now - status.lastBackupAt > STALE_DAYS * DAY && now - status.changedAt > DAY ->
            "Backup is out of date" to
                if (status.lastBackupAt > 0) "Last backup ${formatBackupTime(status.lastBackupAt).replaceFirstChar { it.lowercase() }}" else "Not backed up yet"
        else -> null
    }
}

/**
 * A quiet warning card at the top of Invoices when backup is off or out of date.
 * The whole card opens Backup & restore; the cross hides it for two weeks.
 */
@Composable
fun BackupNudge(title: String, detail: String, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ui.colors
    val shape = CardShape
    Row(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.White, c.frosted)), shape)
            // The banner wash in amber, gone by the middle (design.md section 6.8).
            .background(
                Brush.horizontalGradient(
                    0f to c.warning.copy(alpha = 0.14f),
                    0.25f to c.warning.copy(alpha = 0.06f),
                    0.5f to c.warning.copy(alpha = 0f),
                ),
                shape,
            )
            .border(1.dp, c.hairline, shape)
            .clip(shape)
            .clickable(onClick = onOpen, role = Role.Button)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.CloudUpload, contentDescription = null, tint = c.warning, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = c.greyText, maxLines = 2)
        }
        androidx.compose.material3.IconButton(onClick = onDismiss) {
            Icon(AppIcons.Close, contentDescription = "Hide", tint = c.grey, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Asked once, after the person has set up their details: turn on Google Drive backup?
 * Optional. "Not now" leaves it off, and the quiet nudge on Invoices reminds them later.
 */
@Composable
fun DriveBackupPrompt(onTurnOn: () -> Unit, onNotNow: () -> Unit) {
    val c = Ui.colors
    AlertDialog(
        onDismissRequest = onNotNow,
        icon = { Icon(AppIcons.GoogleDrive, contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(40.dp)) },
        title = { Text("Back up to Google Drive?", style = MaterialTheme.typography.titleLarge, color = c.ink) },
        text = {
            Text(
                "Keep an encrypted copy of your invoices, clients and business details in your Google Drive. " +
                    "If you change phones or reinstall the app, you can bring everything back in one step.\n\n" +
                    "It's optional, and you can turn it on later in Settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.greyText,
            )
        },
        confirmButton = { AppButton("Turn on", onTurnOn, variant = ButtonVariant.Link) },
        dismissButton = { AppButton("Not now", onNotNow, variant = ButtonVariant.Tertiary) },
        shape = SmoothShape(20.dp),
        containerColor = c.surface,
    )
}
