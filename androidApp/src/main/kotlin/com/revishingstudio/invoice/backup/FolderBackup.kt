package com.revishingstudio.invoice.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * A weekly backup file in a folder the person picked (for example Documents or a USB
 * drive). Files there stay when the app is uninstalled, so a reinstall can restore from it
 * without Google. The file is the same encrypted backup as Drive's, locked with the backup
 * password; nothing readable is ever written. The app can only reach the one folder that
 * was picked, through Android's folder picker.
 */
object FolderBackup {
    const val FILE_NAME = "My Invoice auto-backup.mib"
    private const val TEMP_NAME = "My Invoice auto-backup (saving).mib"
    private const val MIME = "application/octet-stream"
    private const val WEEKLY = "folder-backup-weekly"

    private val lock = Mutex()

    /** Keeps access to the picked folder across restarts, and remembers it. */
    fun choose(context: Context, tree: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(tree, flags)
        BackupPrefs.get(context).setFolder(tree.toString(), folderName(context, tree))
        schedule(context)
    }

    fun turnOff(context: Context) {
        val prefs = BackupPrefs.get(context)
        prefs.status.value.folderUri?.let { uri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(uri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        prefs.setFolder(null, null)
        runCatching { WorkManager.getInstance(context).cancelUniqueWork(WEEKLY) }
    }

    fun schedule(context: Context) {
        if (!BackupPrefs.get(context).status.value.folderBackupOn) return
        val request = PeriodicWorkRequestBuilder<FolderBackupWorker>(7, TimeUnit.DAYS).build()
        runCatching { WorkManager.getInstance(context).enqueueUniquePeriodicWork(WEEKLY, ExistingPeriodicWorkPolicy.KEEP, request) }
    }

    /** Writes the backup file now. Returns null when done, or a message for the person. */
    suspend fun run(context: Context): String? = lock.withLock {
        val prefs = BackupPrefs.get(context)
        withContext(Dispatchers.IO) {
            try {
                val tree = prefs.status.value.folderUri?.let(Uri::parse) ?: return@withContext null
                val password = prefs.password() ?: return@withContext fail(prefs, "Set a backup password first")
                val writable = context.contentResolver.persistedUriPermissions.any { it.uri == tree && it.isWritePermission }
                if (!writable) return@withContext fail(prefs, "The app can't reach the backup folder any more. Pick it again.")
                val made = BackupEngine(context).create(password)
                write(context, tree, made.bytes)
                prefs.exported(made.takenAt)
                null
            } catch (e: Exception) {
                Log.w("MyInvoice", "Folder backup failed", e)
                fail(prefs, if (e is SecurityException) "The app can't reach the backup folder any more. Pick it again." else e.backupMessage())
            }
        }
    }

    private fun fail(prefs: BackupPrefs, message: String): String {
        prefs.exportFailed(message)
        return message
    }

    /**
     * Writes a new file first, then removes the old one and gives the new one its name,
     * so a failed write never leaves the folder without a good backup.
     */
    private fun write(context: Context, tree: Uri, bytes: ByteArray) {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        child(context, tree, TEMP_NAME)?.let { DocumentsContract.deleteDocument(resolver, it) }
        val fresh = DocumentsContract.createDocument(resolver, parent, MIME, TEMP_NAME)
            ?: throw IllegalStateException("Couldn't create a file in the backup folder")
        resolver.openOutputStream(fresh, "w")!!.use { it.write(bytes) }
        child(context, tree, FILE_NAME)?.let { DocumentsContract.deleteDocument(resolver, it) }
        // Some folders can't rename; the backup is then kept under the temporary name.
        runCatching { DocumentsContract.renameDocument(resolver, fresh, FILE_NAME) }
    }

    private fun child(context: Context, tree: Uri, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        context.contentResolver.query(children, columns, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == name) return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
            }
        }
        return null
    }

    private fun folderName(context: Context, tree: Uri): String? = runCatching {
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        context.contentResolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull()
}

/** The weekly backup file, written only if something changed since the last one. */
class FolderBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val status = BackupPrefs.get(applicationContext).status.value
        if (!status.folderBackupOn || status.folderUpToDate) return Result.success()
        FolderBackup.run(applicationContext)
        return Result.success()
    }
}
