package com.revishingstudio.invoice.core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.revishingstudio.invoice.db.InvoiceDatabase
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MigrationTest {

    @Test
    fun version1ProfileGainsDueDateOption() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // business_profile as it was in version 1 of the app.
        driver.execute(
            null,
            """
            CREATE TABLE business_profile (
                id INTEGER NOT NULL PRIMARY KEY CHECK (id = 1),
                owner_name TEXT NOT NULL, studio_name TEXT NOT NULL, address TEXT NOT NULL,
                pan TEXT NOT NULL, email TEXT NOT NULL, phone TEXT NOT NULL, logo_path TEXT,
                show_gst_note INTEGER NOT NULL DEFAULT 1, default_due_days INTEGER NOT NULL DEFAULT 15,
                number_pattern TEXT NOT NULL, number_prefix TEXT NOT NULL, number_reset TEXT NOT NULL,
                number_start INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent(),
            0,
        )
        driver.execute(
            null,
            """
            INSERT INTO business_profile VALUES
            (1, 'Owner', 'Studio', 'Addr', 'PAN', '', '', NULL, 1, 15, '{SEQ}', 'RS', 'NEVER', 1)
            """.trimIndent(),
            0,
        )

        InvoiceDatabase.Schema.migrate(driver, 1, 2)

        val profile = InvoiceRepository(driver, Dispatchers.Unconfined).profile()!!
        assertEquals("Owner", profile.ownerName)
        assertTrue(profile.showDueDate)
        assertEquals(3L, InvoiceDatabase.Schema.version)
    }

    @Test
    fun version2ClientTaxIdsMoveToGstinAndCustomFields() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // client as it was in version 2 of the app, with free-text tax IDs.
        driver.execute(
            null,
            """
            CREATE TABLE client (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL, legal_name TEXT NOT NULL DEFAULT '', address TEXT NOT NULL DEFAULT '',
                phone TEXT NOT NULL DEFAULT '', email TEXT NOT NULL DEFAULT '', tax_ids TEXT NOT NULL DEFAULT '',
                default_currency TEXT NOT NULL DEFAULT 'INR', archived INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
            0,
        )
        driver.execute(null, "INSERT INTO client (name, tax_ids) VALUES ('Aus', 'ABN: 27685519573  ACN: 685519573')", 0)
        driver.execute(null, "INSERT INTO client (name, tax_ids) VALUES ('Ind', '27AAPFU0939F1ZV')", 0)

        InvoiceDatabase.Schema.migrate(driver, 2, InvoiceDatabase.Schema.version)
        val repo = InvoiceRepository(driver, Dispatchers.Unconfined)

        val aus = repo.client(1)!!
        assertEquals("", aus.gstin)
        assertEquals(listOf(TaxField("ABN", "27685519573"), TaxField("ACN", "685519573")), aus.taxFields)
        val ind = repo.client(2)!!
        assertEquals("27AAPFU0939F1ZV", ind.gstin)
        assertEquals(emptyList(), ind.taxFields)

        // Saving writes the new columns and drops the old text, so nothing doubles up.
        repo.saveClient(aus.copy(taxFields = aus.taxFields + TaxField("VAT", "GB123")))
        assertEquals(
            listOf(TaxField("ABN", "27685519573"), TaxField("ACN", "685519573"), TaxField("VAT", "GB123")),
            repo.client(1)!!.taxFields,
        )
    }
}
