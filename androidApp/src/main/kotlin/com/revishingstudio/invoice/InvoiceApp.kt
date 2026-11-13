package com.revishingstudio.invoice

import android.app.Application
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.revishingstudio.invoice.backup.BackupScheduler
import com.revishingstudio.invoice.core.InvoiceRepository
import com.revishingstudio.invoice.db.InvoiceDatabase

class InvoiceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Invoices sent before details were frozen get them now (a no-op once done).
        Thread { runCatching { repository.freezeMissingSnapshots() } }.start()
        // Keeps the daily Drive backup booked while backup is on.
        BackupScheduler.ensureScheduled(this)
    }

    /**
     * The open database file. Backups copy it; a restore closes it, swaps the file and
     * restarts the app. Every change is noted so the next backup can follow soon after.
     */
    val driver: AndroidSqliteDriver by lazy {
        AndroidSqliteDriver(InvoiceDatabase.Schema, this, DATABASE_NAME).also { BackupScheduler.watchChanges(this, it) }
    }

    /** One repository for the whole app, backed by an on-device SQLite file. */
    val repository: InvoiceRepository by lazy { InvoiceRepository(driver) }

    companion object {
        const val DATABASE_NAME = "invoices.db"
    }
}
