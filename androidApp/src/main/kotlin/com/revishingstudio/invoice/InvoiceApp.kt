package com.revishingstudio.invoice

import android.app.Application
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.revishingstudio.invoice.core.InvoiceRepository
import com.revishingstudio.invoice.db.InvoiceDatabase

class InvoiceApp : Application() {

    /** One repository for the whole app, backed by an on-device SQLite file. */
    val repository: InvoiceRepository by lazy {
        val driver = AndroidSqliteDriver(InvoiceDatabase.Schema, this, "invoices.db")
        InvoiceRepository(driver)
    }
}
