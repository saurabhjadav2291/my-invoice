package com.revishingstudio.invoice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The first screen of a fresh install. Sign in with Google first; then an existing user
 * is offered their backup, and a new user is offered Drive backup. Signing in is optional:
 * "Skip for now" goes straight to the app, and a backup file can be restored without Google.
 */
@Composable
fun WelcomeScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val controller = rememberBackupController()
    var account by remember { mutableStateOf<BackupController.SignedIn?>(null) }
    val c = Ui.colors

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = ScreenMargin, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(48.dp))
        PaperInvoice(number = null)
        Spacer(Modifier.height(32.dp))

        val signedIn = account
        val title: String
        val body: String
        when {
            signedIn == null -> {
                title = "Welcome to My Invoice"
                body = "Sign in with Google to keep your invoices backed up, and to bring them back if you change phones."
            }
            signedIn.backup != null -> {
                title = "Welcome back"
                body = "We found a backup in ${signedIn.email ?: "your Google Drive"}.\n${signedIn.summary}."
            }
            else -> {
                title = "Turn on backup?"
                body = "There's no backup in ${signedIn.email ?: "this Google account"} yet. Back up your invoices to " +
                    "Google Drive, encrypted, so you can bring them back if you change phones or reinstall the app."
            }
        }
        Text(title, style = MaterialTheme.typography.headlineLarge, color = c.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = c.greyText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))

        val wide = Modifier.fillMaxWidth()
        when {
            signedIn == null -> {
                AppButton(
                    "Continue with Google",
                    { controller.signIn { account = it } },
                    wide,
                    variant = ButtonVariant.Secondary,
                    icon = AppIcons.GoogleDrive,
                    iconInColour = true,
                )
                Spacer(Modifier.height(8.dp))
                AppButton("Skip for now", onDone, wide, variant = ButtonVariant.Tertiary)
                Spacer(Modifier.height(16.dp))
                AppButton("Restore from a backup file", controller::importFile, variant = ButtonVariant.Link)
                Text(
                    "Weekly backup files are kept in the folder you chose, for example Documents, " +
                        "and are named \"My Invoice auto-backup\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.grey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            signedIn.backup != null -> {
                AppButton("Restore backup", { controller.restoreSignedIn(signedIn) }, wide, icon = AppIcons.CloudDownload)
                Spacer(Modifier.height(8.dp))
                AppButton("Start fresh instead", onDone, wide, variant = ButtonVariant.Tertiary)
            }
            else -> {
                AppButton("Turn on backup", { controller.turnOnSignedIn(signedIn, onDone) }, wide, icon = AppIcons.CloudUpload)
                Spacer(Modifier.height(8.dp))
                AppButton("Not now", onDone, wide, variant = ButtonVariant.Tertiary)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    BackupDialogs(controller)
}
