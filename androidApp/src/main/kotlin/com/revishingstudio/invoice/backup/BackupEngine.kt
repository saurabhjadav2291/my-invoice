package com.revishingstudio.invoice.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import app.cash.sqldelight.db.QueryResult
import com.revishingstudio.invoice.InvoiceApp
import com.revishingstudio.invoice.core.backup.BackupContents
import com.revishingstudio.invoice.core.backup.BackupFormat
import com.revishingstudio.invoice.core.backup.BackupManifest
import com.revishingstudio.invoice.db.InvoiceDatabase
import java.io.File

/** A backup ready to restore: what it holds, and the decrypted contents. */
class OpenedBackup(val contents: BackupContents)

/**
 * Makes backups of this phone's data and puts a backup back.
 *
 * A backup is a consistent copy of the SQLite database plus the logo files, packed
 * and encrypted by [BackupFormat]. A restore checks the backup, swaps the database
 * file and logos, then restarts the app so everything reads the restored data.
 */
class BackupEngine(context: Context) {
    private val app = context.applicationContext as InvoiceApp

    /** Encrypted backup bytes, plus the counts shown next to it in Drive. */
    class Made(val bytes: ByteArray, val takenAt: Long, val invoiceCount: Int, val clientCount: Int)

    fun create(password: CharArray): Made {
        val takenAt = System.currentTimeMillis()
        val dir = File(app.cacheDir, "backup").apply { mkdirs() }
        val snapshot = File(dir, "snapshot.db")
        try {
            copyDatabase(snapshot)
            val invoices = count("invoice")
            val clients = count("client")
            val manifest = BackupManifest(
                schemaVersion = InvoiceDatabase.Schema.version,
                createdAtMillis = takenAt,
                filesDir = app.filesDir.absolutePath,
                appVersion = appVersion(),
                invoiceCount = invoices,
                clientCount = clients,
            )
            val contents = BackupContents(manifest, snapshot.readBytes(), appFiles())
            return Made(BackupFormat.write(contents, password), takenAt, invoices, clients)
        } finally {
            snapshot.delete()
            File(snapshot.path + "-journal").delete()
        }
    }

    /** Decrypts and checks a backup. Throws WrongPasswordException or a message for the person. */
    fun open(bytes: ByteArray, password: CharArray): OpenedBackup {
        val contents = BackupFormat.read(bytes, password)
        check(contents.manifest.schemaVersion <= InvoiceDatabase.Schema.version) {
            "This backup was made by a newer version of My Invoice. Update the app, then restore."
        }
        return OpenedBackup(contents)
    }

    /**
     * Replaces everything on this phone with the backup and restarts the app.
     * The backup is checked in a staging copy first, so a bad backup never touches the live data.
     */
    fun restore(backup: OpenedBackup) {
        val contents = backup.contents
        val staging = File(app.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        val staged = File(staging, InvoiceApp.DATABASE_NAME)
        staged.writeBytes(contents.database)
        prepareStaged(staged, contents.manifest)

        // Copied next to the live file first, so the swap itself is a rename.
        val live = app.getDatabasePath(InvoiceApp.DATABASE_NAME)
        live.parentFile?.mkdirs()
        val incoming = File(live.path + ".restore")
        staged.copyTo(incoming, overwrite = true)
        staging.deleteRecursively()

        BackupScheduler.cancelAll(app)
        app.driver.close()
        try {
            app.deleteDatabase(InvoiceApp.DATABASE_NAME)
            check(incoming.renameTo(live)) { "The backup couldn't be put in place. Please try again." }
            APP_FILES.forEach { File(app.filesDir, it).deleteRecursively() }
            contents.files.forEach { (path, data) ->
                File(app.filesDir, path).apply { parentFile?.mkdirs() }.writeBytes(data)
            }
            BackupPrefs.get(app).restored(contents.manifest.createdAtMillis)
        } finally {
            // The database is closed now, so the app starts again either way.
            restartApp()
        }
    }

    /** Checks the staged database opens, and moves logo paths to this phone's files folder. */
    private fun prepareStaged(file: File, manifest: BackupManifest) {
        val db = try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
        } catch (e: Exception) {
            throw IllegalStateException("This backup's data can't be opened")
        }
        try {
            check(db.version.toLong() == manifest.schemaVersion && tableExists(db, "invoice")) {
                "This backup's data can't be opened"
            }
            val from = manifest.filesDir
            val to = app.filesDir.absolutePath
            if (from.isNotBlank() && from != to) {
                db.execSQL("UPDATE business_profile SET logo_path = replace(logo_path, ?, ?) WHERE logo_path IS NOT NULL", arrayOf(from, to))
                if (columnExists(db, "invoice", "snapshot")) {
                    db.execSQL("UPDATE invoice SET snapshot = replace(snapshot, ?, ?) WHERE snapshot IS NOT NULL", arrayOf(from, to))
                }
            }
        } finally {
            db.close()
        }
        // Only the database file itself is copied in.
        listOf("-wal", "-shm", "-journal").forEach { File(file.path + it).delete() }
    }

    private fun tableExists(db: SQLiteDatabase, table: String): Boolean =
        db.rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table)).use { it.moveToFirst() }

    private fun columnExists(db: SQLiteDatabase, table: String, column: String): Boolean =
        db.rawQuery("PRAGMA table_info($table)", null).use { c ->
            val name = c.getColumnIndex("name")
            generateSequence { if (c.moveToNext()) c.getString(name) else null }.any { it == column }
        }

    /**
     * A consistent copy of the live database. VACUUM INTO (Android 11 and later) writes a
     * clean copy in one step; older phones fold the write-ahead log in and copy the file.
     */
    private fun copyDatabase(target: File) {
        target.delete()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val path = target.absolutePath.replace("'", "''")
            runCatching { app.driver.execute(null, "VACUUM INTO '$path'", 0) }
            if (target.length() > 0) return
            target.delete()
        }
        runCatching {
            app.driver.executeQuery(null, "PRAGMA wal_checkpoint(TRUNCATE)", { QueryResult.Value(Unit) }, 0)
        }
        app.getDatabasePath(InvoiceApp.DATABASE_NAME).copyTo(target, overwrite = true)
        val wal = File(app.getDatabasePath(InvoiceApp.DATABASE_NAME).path + "-wal")
        check(!wal.exists() || wal.length() == 0L) { "The app was busy saving. Try the backup again in a moment." }
    }

    private fun count(table: String): Int = runCatching {
        app.driver.executeQuery(null, "SELECT count(*) FROM $table", { cursor ->
            QueryResult.Value(if (cursor.next().value) cursor.getLong(0)?.toInt() ?: 0 else 0)
        }, 0).value
    }.getOrDefault(0)

    /** Logos: the current one and the ones kept for sent invoices. */
    private fun appFiles(): Map<String, ByteArray> {
        val root = app.filesDir
        return APP_FILES.map { File(root, it) }
            .flatMap { f -> if (f.isDirectory) f.walkTopDown().filter { it.isFile }.toList() else listOf(f) }
            .filter { it.isFile }
            .associate { it.relativeTo(root).invariantSeparatorsPath to it.readBytes() }
    }

    private fun appVersion(): String = runCatching {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName
    }.getOrNull() ?: ""

    private fun restartApp() {
        val launch = app.packageManager.getLaunchIntentForPackage(app.packageName)
        val restart = Intent.makeRestartActivityTask(launch?.component)
        app.startActivity(restart)
        Runtime.getRuntime().exit(0)
    }

    private companion object {
        /** What the app keeps in its files folder besides the database. */
        val APP_FILES = listOf("logo", "logos")
    }
}
