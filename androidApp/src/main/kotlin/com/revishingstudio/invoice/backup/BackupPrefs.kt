package com.revishingstudio.invoice.backup

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Where backup stands, for the Settings screen and the Invoices banner. */
data class BackupStatus(
    /** Automatic Google Drive backup is on. */
    val driveEnabled: Boolean = false,
    /** The Google account the backup goes to, when known. */
    val account: String? = null,
    /** When the last successful Drive backup was taken (epoch millis), or 0. */
    val lastBackupAt: Long = 0,
    /** When the invoices, clients or settings last changed (epoch millis), or 0. */
    val changedAt: Long = 0,
    /** Why the last automatic backup failed, until one succeeds. */
    val lastError: String? = null,
    val hasPassword: Boolean = false,
    /** When the "not backed up" banner on Invoices was last closed. */
    val bannerDismissedAt: Long = 0,
    /** The "restore from another phone" offer on a fresh install was closed. */
    val restoreOfferDismissed: Boolean = false,
) {
    /** Nothing has changed since the last Drive backup. */
    val upToDate: Boolean get() = driveEnabled && lastBackupAt > 0 && lastBackupAt >= changedAt
}

/**
 * Backup settings, kept on this phone only (the app opts out of Android's own backup).
 * The backup password is stored encrypted with a key that never leaves the phone's
 * keystore, so automatic backups can run without asking for it.
 */
class BackupPrefs private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("backup", Context.MODE_PRIVATE)
    private val _status = MutableStateFlow(read())
    val status: StateFlow<BackupStatus> = _status.asStateFlow()

    private fun read() = BackupStatus(
        driveEnabled = prefs.getBoolean(DRIVE_ENABLED, false),
        account = prefs.getString(ACCOUNT, null),
        lastBackupAt = prefs.getLong(LAST_BACKUP, 0),
        changedAt = prefs.getLong(CHANGED, 0),
        lastError = prefs.getString(LAST_ERROR, null),
        hasPassword = prefs.contains(PASSWORD),
        bannerDismissedAt = prefs.getLong(BANNER_DISMISSED, 0),
        restoreOfferDismissed = prefs.getBoolean(RESTORE_OFFER_DISMISSED, false),
    )

    /** commit = true writes before returning; used right before the app restarts. */
    private fun edit(commit: Boolean = false, block: SharedPreferences.Editor.() -> Unit) {
        synchronized(this) {
            val editor = prefs.edit()
            editor.block()
            if (commit) editor.commit() else editor.apply()
            _status.value = read()
        }
    }

    fun setDrive(enabled: Boolean, account: String?, commit: Boolean = false) = edit(commit) {
        putBoolean(DRIVE_ENABLED, enabled)
        if (account != null) putString(ACCOUNT, account) else remove(ACCOUNT)
        if (!enabled) remove(LAST_ERROR)
    }

    fun setAccount(account: String) = edit { putString(ACCOUNT, account) }

    fun backedUp(at: Long) = edit {
        putLong(LAST_BACKUP, at)
        remove(LAST_ERROR)
    }

    fun failed(message: String) = edit { putString(LAST_ERROR, message) }

    fun changed(at: Long = System.currentTimeMillis()) = edit { putLong(CHANGED, at) }

    fun dismissBanner() = edit { putLong(BANNER_DISMISSED, System.currentTimeMillis()) }

    fun dismissRestoreOffer() = edit { putBoolean(RESTORE_OFFER_DISMISSED, true) }

    /** After a restore, the restored data is what the last backup held. */
    fun restored(backupTakenAt: Long) = edit(commit = true) {
        putLong(LAST_BACKUP, backupTakenAt)
        putLong(CHANGED, backupTakenAt)
        putBoolean(RESTORE_OFFER_DISMISSED, true)
        remove(LAST_ERROR)
    }

    fun password(): CharArray? {
        val stored = prefs.getString(PASSWORD, null) ?: return null
        return runCatching { PasswordVault.open(stored) }.getOrNull()
    }

    fun setPassword(password: CharArray, commit: Boolean = false) = edit(commit) { putString(PASSWORD, PasswordVault.seal(password)) }

    companion object {
        private const val DRIVE_ENABLED = "drive_enabled"
        private const val ACCOUNT = "account"
        private const val LAST_BACKUP = "last_backup"
        private const val CHANGED = "changed"
        private const val LAST_ERROR = "last_error"
        private const val PASSWORD = "password"
        private const val BANNER_DISMISSED = "banner_dismissed"
        private const val RESTORE_OFFER_DISMISSED = "restore_offer_dismissed"

        @Volatile
        private var instance: BackupPrefs? = null

        fun get(context: Context): BackupPrefs =
            instance ?: synchronized(this) { instance ?: BackupPrefs(context.applicationContext).also { instance = it } }
    }
}

/** Seals the backup password with an AES key kept in the Android keystore. */
private object PasswordVault {
    private const val ALIAS = "my_invoice_backup_password"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    fun seal(password: CharArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val plain = String(password).toByteArray(Charsets.UTF_8)
        return Base64.encodeToString(cipher.iv + cipher.doFinal(plain), Base64.NO_WRAP)
    }

    fun open(sealed: String): CharArray {
        val bytes = Base64.decode(sealed, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, 12))
        return String(cipher.doFinal(bytes, 12, bytes.size - 12), Charsets.UTF_8).toCharArray()
    }
}
