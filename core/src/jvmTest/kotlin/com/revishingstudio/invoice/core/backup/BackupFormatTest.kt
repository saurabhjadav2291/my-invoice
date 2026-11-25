package com.revishingstudio.invoice.core.backup

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupFormatTest {
    // Few iterations keep the tests fast; the app uses DEFAULT_ITERATIONS.
    private val fast = 10_000
    private val password = "correct horse".toCharArray()

    private fun sample() = BackupContents(
        manifest = BackupManifest(
            schemaVersion = 4,
            createdAtMillis = 1_700_000_000_000,
            filesDir = "/data/user/0/com.myinvoice.app/files",
            appVersion = "0.1.0",
            invoiceCount = 12,
            clientCount = 3,
        ),
        database = ByteArray(5000) { (it % 251).toByte() },
        files = mapOf("logo" to byteArrayOf(1, 2, 3), "logos/logo-1.png" to byteArrayOf(4, 5)),
    )

    @Test
    fun roundTripKeepsEverything() {
        val bytes = BackupFormat.write(sample(), password, fast)
        assertTrue(BackupFormat.isBackup(bytes))
        val back = BackupFormat.read(bytes, password)
        assertEquals(sample().manifest, back.manifest)
        assertContentEquals(sample().database, back.database)
        assertEquals(setOf("logo", "logos/logo-1.png"), back.files.keys)
        assertContentEquals(byteArrayOf(4, 5), back.files["logos/logo-1.png"])
    }

    @Test
    fun theDatabaseIsNotReadableInTheFile() {
        val contents = sample()
        val marker = "Acme Client Pvt Ltd".toByteArray()
        val withName = BackupContents(contents.manifest, marker + contents.database, contents.files)
        val bytes = BackupFormat.write(withName, password, fast)
        val text = String(bytes, Charsets.ISO_8859_1)
        assertFalse("Acme Client" in text)
    }

    @Test
    fun wrongPasswordIsReported() {
        val bytes = BackupFormat.write(sample(), password, fast)
        assertFailsWith<WrongPasswordException> { BackupFormat.read(bytes, "wrong password".toCharArray()) }
    }

    @Test
    fun anyChangedByteIsCaught() {
        val bytes = BackupFormat.write(sample(), password, fast)
        val tampered = bytes.copyOf().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() }
        assertFailsWith<WrongPasswordException> { BackupFormat.read(tampered, password) }
        // The header (here the salt) is checked too.
        val header = bytes.copyOf().also { it[20] = (it[20] + 1).toByte() }
        assertFailsWith<WrongPasswordException> { BackupFormat.read(header, password) }
    }

    @Test
    fun otherFilesAreRejected() {
        assertFalse(BackupFormat.isBackup("%PDF-1.7 hello world, not a backup at all....".toByteArray()))
        assertFailsWith<NotABackupException> { BackupFormat.read(ByteArray(100), password) }
    }

    @Test
    fun shortPasswordsAreRefused() {
        assertFailsWith<IllegalArgumentException> { BackupFormat.write(sample(), "short".toCharArray(), fast) }
    }

    @Test
    fun filePathsMustStayInsideTheFilesFolder() {
        val c = sample()
        val bad = BackupContents(c.manifest, c.database, mapOf("../databases/x" to byteArrayOf(1)))
        assertFailsWith<NotABackupException> { BackupFormat.write(bad, password, fast) }
    }
}
