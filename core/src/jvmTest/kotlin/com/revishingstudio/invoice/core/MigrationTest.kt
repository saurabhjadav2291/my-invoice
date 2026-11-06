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

        InvoiceDatabase.Schema.migrate(driver, 1, InvoiceDatabase.Schema.version)

        val profile = InvoiceRepository(driver, Dispatchers.Unconfined).profile()!!
        assertEquals("Owner", profile.ownerName)
        assertTrue(profile.showDueDate)
        assertEquals(2L, InvoiceDatabase.Schema.version)
    }
}
