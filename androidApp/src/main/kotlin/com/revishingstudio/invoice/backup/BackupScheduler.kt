package com.revishingstudio.invoice.backup

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Books the automatic Drive backups: once a day, and 15 minutes after the last change
 * (so a burst of edits makes one backup). Both wait for an internet connection.
 */
object BackupScheduler {
    private const val DAILY = "drive-backup-daily"
    private const val AFTER_CHANGE = "drive-backup-after-change"
    private const val QUIET_MINUTES = 15L

    /** Tables whose changes are worth a new backup. */
    private val TABLES = arrayOf("invoice", "line_item", "payment", "client", "business_profile", "bank_account")

    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun watchChanges(context: Context, driver: SqlDriver) {
        val appContext = context.applicationContext
        driver.addListener(*TABLES, listener = Query.Listener { onDataChanged(appContext) })
    }

    private fun onDataChanged(context: Context) {
        val prefs = BackupPrefs.get(context)
        prefs.changed()
        if (!prefs.status.value.driveEnabled) return
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(online)
            .setInitialDelay(QUIET_MINUTES, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        runCatching { WorkManager.getInstance(context).enqueueUniqueWork(AFTER_CHANGE, ExistingWorkPolicy.REPLACE, request) }
    }

    fun ensureScheduled(context: Context) {
        if (!BackupPrefs.get(context).status.value.driveEnabled) return
        val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
            .setConstraints(online)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        runCatching { WorkManager.getInstance(context).enqueueUniquePeriodicWork(DAILY, ExistingPeriodicWorkPolicy.KEEP, request) }
    }

    fun cancelAll(context: Context) {
        runCatching {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(DAILY)
            wm.cancelUniqueWork(AFTER_CHANGE)
        }
    }
}

/** Runs one automatic backup if anything changed since the last one. */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val status = BackupPrefs.get(applicationContext).status.value
        if (!status.driveEnabled || status.upToDate) return Result.success()
        return when (val outcome = DriveBackup.run(applicationContext, interactive = false)) {
            is DriveBackup.Outcome.Done -> Result.success()
            is DriveBackup.Outcome.Failed -> if (outcome.retry && runAttemptCount < 5) Result.retry() else Result.success()
        }
    }
}

/** The one place a Drive backup is made, for the worker and the "Back up now" button. */
object DriveBackup {
    sealed interface Outcome {
        data class Done(val takenAt: Long) : Outcome
        data class Failed(val message: String, val retry: Boolean) : Outcome
    }

    private val lock = Mutex()

    /**
     * Backs up to Drive now. [token] comes from the UI after an approval; otherwise a
     * saved approval is used and, if Google wants it confirmed again, the backup stops
     * with a message rather than showing anything.
     */
    suspend fun run(context: Context, interactive: Boolean, token: String? = null): Outcome = lock.withLock {
        val prefs = BackupPrefs.get(context)
        withContext(Dispatchers.IO) {
            try {
                val password = prefs.password() ?: return@withContext fail(prefs, "Set a backup password first", retry = false)
                val access = token ?: DriveAuth.silentToken(context)
                    ?: return@withContext fail(prefs, "Google Drive needs your OK again. Open Backup & restore and tap Back up now.", retry = false)
                val made = BackupEngine(context).create(password)
                DriveClient(access).upload(made.bytes, made.takenAt, made.invoiceCount, made.clientCount)
                prefs.backedUp(made.takenAt)
                Outcome.Done(made.takenAt)
            } catch (e: Exception) {
                Log.w("MyInvoice", "Drive backup failed", e)
                val retry = e is java.io.IOException && (e !is DriveException || e.code >= 500 || e.code == 429)
                fail(prefs, e.backupMessage(), retry && !interactive)
            }
        }
    }

    private fun fail(prefs: BackupPrefs, message: String, retry: Boolean): Outcome.Failed {
        prefs.failed(message)
        return Outcome.Failed(message, retry)
    }
}
