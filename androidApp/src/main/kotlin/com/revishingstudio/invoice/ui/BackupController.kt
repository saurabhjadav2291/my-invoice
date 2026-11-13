package com.revishingstudio.invoice.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.revishingstudio.invoice.backup.BackupEngine
import com.revishingstudio.invoice.backup.BackupPrefs
import com.revishingstudio.invoice.backup.BackupScheduler
import com.revishingstudio.invoice.backup.DriveAuth
import com.revishingstudio.invoice.backup.DriveBackup
import com.revishingstudio.invoice.backup.DriveClient
import com.revishingstudio.invoice.backup.DriveFile
import com.revishingstudio.invoice.backup.FolderBackup
import com.revishingstudio.invoice.backup.OpenedBackup
import com.revishingstudio.invoice.backup.backupMessage
import com.revishingstudio.invoice.core.backup.BackupFormat
import com.revishingstudio.invoice.core.backup.WrongPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** A question the backup flow is waiting on. */
sealed interface BackupDialog {
    /** Choose a backup password (first time, or [changing] it). */
    class SetPassword(val changing: Boolean, val then: (CharArray) -> Unit) : BackupDialog

    /** Type the password a backup was made with. */
    class EnterPassword(val wrongTry: Boolean, val then: (CharArray) -> Unit) : BackupDialog

    class ConfirmRestore(val summary: String, val then: () -> Unit) : BackupDialog

    /** Turning on Drive backup found a backup already there. */
    class ExistingBackup(val summary: String, val restore: () -> Unit, val replace: () -> Unit) : BackupDialog

    class ConfirmTurnOff(val then: () -> Unit) : BackupDialog
}

/**
 * Everything the backup buttons do, shared by the Backup & restore screen and the
 * restore offer on a fresh install. Long steps show [busy]; questions show [dialog].
 */
class BackupController(context: Context, private val actions: Actions) {
    private val context = context.applicationContext
    private val prefs = BackupPrefs.get(context)
    private val engine = BackupEngine(context)
    private val scope = actions.scope

    /** What is running, e.g. "Backing up…", or null. */
    var busy by mutableStateOf<String?>(null)
        private set
    var dialog by mutableStateOf<BackupDialog?>(null)

    internal lateinit var launchConsent: (IntentSenderRequest) -> Unit
    internal lateinit var launchExport: (String) -> Unit
    internal lateinit var launchImport: () -> Unit
    internal lateinit var launchFolder: () -> Unit

    private var afterConsent: (suspend (String, AuthorizationResult) -> Unit)? = null
    private var exportPassword: CharArray? = null

    // ---- Google Drive ------------------------------------------------------

    /** Turns on Drive backup: password, Google approval, then the first backup. */
    fun turnOnDrive() = withPassword { _ ->
        withDrive { token, result ->
            val (email, existing) = working("Connecting to Google Drive…") {
                val client = DriveClient(token)
                (DriveAuth.email(result) ?: client.accountEmail()) to client.latestBackup()
            }
            if (existing == null) {
                enableAndBackUp(email, token)
            } else {
                dialog = BackupDialog.ExistingBackup(
                    summary = describe(existing),
                    restore = { restoreFromDrive(token, existing, email) },
                    replace = { scope.launch { enableAndBackUp(email, token) } },
                )
            }
        }
    }

    private suspend fun enableAndBackUp(email: String?, token: String) {
        prefs.setDrive(true, email)
        if (prefs.status.value.changedAt == 0L) prefs.changed()
        BackupScheduler.ensureScheduled(context)
        backUp(token)
    }

    fun turnOffDrive() {
        dialog = BackupDialog.ConfirmTurnOff {
            scope.launch {
                working("Turning off…") {
                    // Withdraw the app's Drive access too, so turning it on again can pick another account.
                    runCatching { DriveAuth.silentToken(context)?.let { DriveClient.revoke(it) } }
                }
                prefs.setDrive(false, null)
                BackupScheduler.cancelAll(context)
                actions.message("Drive backup turned off", BannerKind.Success)
            }
        }
    }

    fun backUpNow() = withPassword { _ -> withDrive { token, _ -> backUp(token) } }

    private suspend fun backUp(token: String) {
        val outcome = working("Backing up…") { DriveBackup.run(context, interactive = true, token = token) }
        when (outcome) {
            is DriveBackup.Outcome.Done -> actions.message("Backed up to Google Drive", BannerKind.Success)
            is DriveBackup.Outcome.Failed -> actions.message(outcome.message, BannerKind.Error)
        }
    }

    /** Finds the backup in the person's Drive and restores it. */
    fun restoreFromDrive() = withDrive { token, result ->
        val (email, file) = working("Looking for your backup…") {
            val client = DriveClient(token)
            (DriveAuth.email(result) ?: client.accountEmail()) to client.latestBackup()
        }
        if (file == null) {
            actions.message("No backup found in this Google account", BannerKind.Warning)
        } else {
            restoreFromDrive(token, file, email)
        }
    }

    private fun restoreFromDrive(token: String, file: DriveFile, email: String?) {
        dialog = BackupDialog.ConfirmRestore(describe(file)) { scope.launch { downloadAndRestore(token, file, email) } }
    }

    private suspend fun downloadAndRestore(token: String, file: DriveFile, email: String?) {
        val bytes = runStep("Downloading your backup…") { DriveClient(token).download(file.id) } ?: return
        unlockAndRestore(bytes) { password ->
            // This phone carries on backing up to the same Drive, with the same password.
            prefs.setPassword(password)
            prefs.setDrive(true, email ?: prefs.status.value.account)
        }
    }

    // ---- Welcome (fresh install) ---------------------------------------------

    /** The Google account someone signed in with on the welcome screen, and its backup if any. */
    class SignedIn(val token: String, val email: String?, val backup: DriveFile?) {
        val summary: String? get() = backup?.let(::describeBackup)
    }

    /** Signs in with Google and looks for a backup in that account's Drive. */
    fun signIn(then: (SignedIn) -> Unit) = withDrive { token, result ->
        val (email, file) = working("Looking for your backup…") {
            val client = DriveClient(token)
            (DriveAuth.email(result) ?: client.accountEmail()) to client.latestBackup()
        }
        then(SignedIn(token, email, file))
    }

    /** Restores the backup found at sign-in. Nothing is on the phone yet, so there is no extra confirm. */
    fun restoreSignedIn(account: SignedIn) {
        val file = account.backup ?: return
        scope.launch { downloadAndRestore(account.token, file, account.email) }
    }

    /** Turns on Drive backup for the account signed in on the welcome screen. */
    fun turnOnSignedIn(account: SignedIn, onDone: () -> Unit) = withPassword { _ ->
        scope.launch {
            enableAndBackUp(account.email, account.token)
            onDone()
        }
    }

    // ---- Backup file ---------------------------------------------------------

    fun exportFile() = withPassword { password ->
        exportPassword = password
        launchExport("My Invoice backup ${LocalDate.now()}.mib")
    }

    internal fun onExportTarget(uri: Uri?) {
        val password = exportPassword ?: return
        exportPassword = null
        if (uri == null) return
        scope.launch {
            runStep("Saving backup file…") {
                val made = engine.create(password)
                context.contentResolver.openOutputStream(uri, "w")!!.use { it.write(made.bytes) }
            } ?: return@launch
            actions.message("Backup file saved", BannerKind.Success)
        }
    }

    fun importFile() = launchImport()

    // ---- Weekly backup file in a folder -----------------------------------------

    /** Picks a folder for the weekly backup file, after making sure a password is set. */
    fun turnOnFolderBackup() = withPassword { _ -> launchFolder() }

    internal fun onFolderPicked(tree: Uri?) {
        if (tree == null) return
        scope.launch {
            runStep(null) { FolderBackup.choose(context, tree) } ?: return@launch
            saveToFolderNow()
        }
    }

    fun saveToFolderNow() {
        scope.launch {
            val error = working("Saving backup file…") { FolderBackup.run(context) }
            if (error == null) {
                actions.message("Backup file saved", BannerKind.Success)
            } else {
                actions.message(error, BannerKind.Error)
            }
        }
    }

    fun turnOffFolderBackup() {
        FolderBackup.turnOff(context)
        actions.message("Weekly backup file turned off. Files already saved stay in the folder.", BannerKind.Success)
    }

    internal fun onImportPicked(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            val bytes = runStep("Reading backup file…") {
                context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
            } ?: return@launch
            if (!BackupFormat.isBackup(bytes)) {
                actions.message("This file isn't a My Invoice backup", BannerKind.Error)
                return@launch
            }
            dialog = BackupDialog.ConfirmRestore("Backup file") {
                scope.launch {
                    unlockAndRestore(bytes) { password ->
                        if (!prefs.status.value.hasPassword) prefs.setPassword(password)
                    }
                }
            }
        }
    }

    // ---- Password ------------------------------------------------------------

    fun changePassword() {
        dialog = BackupDialog.SetPassword(changing = true) { password ->
            prefs.setPassword(password)
            actions.message("Backup password changed", BannerKind.Success)
            // The copy in Drive moves to the new password straight away.
            if (prefs.status.value.driveEnabled) backUpNow()
        }
    }

    private fun withPassword(then: (CharArray) -> Unit) {
        val saved = prefs.password()
        if (saved != null) {
            then(saved)
        } else {
            dialog = BackupDialog.SetPassword(changing = false) { password ->
                prefs.setPassword(password)
                then(password)
            }
        }
    }

    // ---- Restore -------------------------------------------------------------

    /** Opens the backup with the saved password, or asks for it, then restores and restarts. */
    private suspend fun unlockAndRestore(bytes: ByteArray, beforeRestore: (CharArray) -> Unit) {
        suspend fun attempt(password: CharArray?, wrongTry: Boolean) {
            if (password == null) {
                dialog = BackupDialog.EnterPassword(wrongTry) { typed -> scope.launch { attempt(typed, wrongTry = true) } }
                return
            }
            val opened: OpenedBackup = try {
                working("Unlocking backup…") { engine.open(bytes, password) }
            } catch (e: WrongPasswordException) {
                dialog = BackupDialog.EnterPassword(wrongTry) { typed -> scope.launch { attempt(typed, wrongTry = true) } }
                return
            } catch (e: Exception) {
                Log.w("MyInvoice", "Backup can't be opened", e)
                actions.message(e.backupMessage(), BannerKind.Error)
                return
            }
            beforeRestore(password)
            // Restarts the app when it is done.
            runStep("Restoring…") { engine.restore(opened) }
        }
        attempt(prefs.password(), wrongTry = false)
    }

    // ---- Google approval -------------------------------------------------------

    /** Gets a Drive token, showing Google's consent screen first if needed. */
    private fun withDrive(then: suspend (String, AuthorizationResult) -> Unit) {
        scope.launch {
            val result = runStep("Connecting to Google…") { DriveAuth.authorize(context) } ?: return@launch
            val pending = result.pendingIntent
            if (result.hasResolution() && pending != null) {
                afterConsent = then
                launchConsent(IntentSenderRequest.Builder(pending.intentSender).build())
            } else {
                proceed(result, then)
            }
        }
    }

    internal fun onConsent(result: ActivityResult) {
        val then = afterConsent ?: return
        afterConsent = null
        if (result.resultCode != Activity.RESULT_OK) {
            actions.message("Google Drive wasn't connected", BannerKind.Warning)
            return
        }
        scope.launch {
            val auth = runStep(null) { DriveAuth.resultFromIntent(context, result.data) } ?: return@launch
            proceed(auth, then)
        }
    }

    private suspend fun proceed(result: AuthorizationResult, then: suspend (String, AuthorizationResult) -> Unit) {
        val token = result.accessToken
        if (token == null) {
            actions.message("Google didn't allow access to Drive", BannerKind.Error)
            return
        }
        try {
            then(token, result)
        } catch (e: Exception) {
            Log.w("MyInvoice", "Backup step failed", e)
            actions.message(e.backupMessage(), BannerKind.Error)
        }
    }

    // ---- Helpers ---------------------------------------------------------------

    /** Runs [block] off the main thread with [label] showing; errors go up to the caller. */
    private suspend fun <T> working(label: String?, block: suspend () -> T): T {
        busy = label
        try {
            return withContext(Dispatchers.IO) { block() }
        } finally {
            busy = null
        }
    }

    /** Like [working], but shows any error as a message and returns null. */
    private suspend fun <T : Any> runStep(label: String?, block: suspend () -> T): T? = try {
        working(label, block)
    } catch (e: Exception) {
        Log.w("MyInvoice", "Backup step failed", e)
        actions.message(e.backupMessage(), BannerKind.Error)
        null
    }

    private fun describe(file: DriveFile): String = describeBackup(file)
}

/** "Backup from 12 Nov 2026, 14:05 · 14 invoices, 3 clients". */
fun describeBackup(file: DriveFile): String {
    val counts = listOfNotNull(
        file.invoiceCount?.let { if (it == 1) "1 invoice" else "$it invoices" },
        file.clientCount?.let { if (it == 1) "1 client" else "$it clients" },
    ).joinToString(", ")
    return "Backup from ${formatBackupTime(file.takenAt)}" + if (counts.isNotEmpty()) " · $counts" else ""
}

/** "Today, 14:05", "Yesterday, 09:12" or "12 Nov 2026, 14:05". */
fun formatBackupTime(millis: Long): String {
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(millis).atZone(zone)
    val today = LocalDate.now(zone)
    val clock = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    return when (time.toLocalDate()) {
        today -> "Today, $clock"
        today.minusDays(1) -> "Yesterday, $clock"
        else -> time.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"))
    }
}

/** A [BackupController] wired to this screen's file pickers and Google's consent screen. */
@Composable
fun rememberBackupController(): BackupController {
    val context = LocalContext.current
    val actions = LocalActions.current
    val controller = remember { BackupController(context, actions) }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { controller.onConsent(it) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { controller.onExportTarget(it) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { controller.onImportPicked(it) }
    controller.launchConsent = { consent.launch(it) }
    controller.launchExport = { export.launch(it) }
    controller.launchImport = { import.launch(arrayOf("*/*")) }
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { controller.onFolderPicked(it) }
    controller.launchFolder = { folder.launch(null) }
    return controller
}
