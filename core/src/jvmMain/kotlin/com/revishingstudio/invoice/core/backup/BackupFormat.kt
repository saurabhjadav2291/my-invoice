package com.revishingstudio.invoice.core.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.io.StringWriter
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** What a backup says about itself. Shown before restoring, and used to fix file paths. */
data class BackupManifest(
    /** The database schema version (SQLite user_version) of the copy inside. */
    val schemaVersion: Long,
    val createdAtMillis: Long,
    /** The app's files folder on the phone that made it, so logo paths can be moved to this phone's. */
    val filesDir: String,
    val appVersion: String,
    val invoiceCount: Int,
    val clientCount: Int,
) {
    internal fun encode(): ByteArray {
        val p = Properties()
        p["format"] = BackupFormat.VERSION.toString()
        p["schemaVersion"] = schemaVersion.toString()
        p["createdAt"] = createdAtMillis.toString()
        p["filesDir"] = filesDir
        p["appVersion"] = appVersion
        p["invoices"] = invoiceCount.toString()
        p["clients"] = clientCount.toString()
        val out = StringWriter()
        p.store(out, "My Invoice backup")
        return out.toString().toByteArray(Charsets.UTF_8)
    }

    internal companion object {
        fun decode(bytes: ByteArray): BackupManifest {
            val p = Properties()
            p.load(StringReader(bytes.toString(Charsets.UTF_8)))
            fun long(key: String) = p.getProperty(key)?.toLongOrNull() ?: throw NotABackupException()
            return BackupManifest(
                schemaVersion = long("schemaVersion"),
                createdAtMillis = long("createdAt"),
                filesDir = p.getProperty("filesDir").orEmpty(),
                appVersion = p.getProperty("appVersion").orEmpty(),
                invoiceCount = p.getProperty("invoices")?.toIntOrNull() ?: 0,
                clientCount = p.getProperty("clients")?.toIntOrNull() ?: 0,
            )
        }
    }
}

/**
 * Everything in a backup: the SQLite database file and the app's own files (logos),
 * keyed by their path inside the app's files folder.
 */
class BackupContents(
    val manifest: BackupManifest,
    val database: ByteArray,
    val files: Map<String, ByteArray>,
)

/** The password doesn't open the backup (or the file was changed after it was made). */
class WrongPasswordException : IllegalArgumentException("That password doesn't open this backup")

class NotABackupException : IllegalArgumentException("This file isn't a My Invoice backup")

/**
 * The backup file: a zip of the database, logos and a manifest, encrypted on the phone
 * with a key made from the backup password. Google Drive only ever sees the encrypted bytes.
 *
 * Layout: "MYINVBK1" | format version (1 byte) | PBKDF2 iterations (4 bytes) | salt (16) |
 * IV (12) | AES-256-GCM ciphertext. The header is authenticated with the ciphertext, so
 * any change to the file is caught as a wrong password.
 */
object BackupFormat {
    const val VERSION: Byte = 1
    const val MIN_PASSWORD_LENGTH = 8

    /** OWASP's 2023 figure for PBKDF2-HMAC-SHA256. About a second on a mid-range phone. */
    const val DEFAULT_ITERATIONS = 600_000

    private val MAGIC = "MYINVBK1".toByteArray(Charsets.US_ASCII)
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val HEADER_BYTES = 8 + 1 + 4 + SALT_BYTES + IV_BYTES
    private const val MANIFEST = "manifest.properties"
    private const val DATABASE = "invoices.db"
    private const val FILES = "files/"

    fun isBackup(bytes: ByteArray): Boolean =
        bytes.size > HEADER_BYTES && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    fun write(
        contents: BackupContents,
        password: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
        random: SecureRandom = SecureRandom(),
    ): ByteArray {
        require(password.size >= MIN_PASSWORD_LENGTH) { "The backup password needs at least $MIN_PASSWORD_LENGTH characters" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC).put(VERSION).putInt(iterations).put(salt).put(iv)
            .array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt, iterations), GCMParameterSpec(128, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(zip(contents))
    }

    fun read(bytes: ByteArray, password: CharArray): BackupContents {
        if (!isBackup(bytes)) throw NotABackupException()
        val buffer = ByteBuffer.wrap(bytes)
        buffer.position(MAGIC.size)
        val version = buffer.get()
        require(version == VERSION) { "This backup was made by a newer version of My Invoice. Update the app, then try again." }
        val iterations = buffer.int
        if (iterations !in 10_000..10_000_000) throw NotABackupException()
        val salt = ByteArray(SALT_BYTES).also { buffer.get(it) }
        val iv = ByteArray(IV_BYTES).also { buffer.get(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt, iterations), GCMParameterSpec(128, iv))
        cipher.updateAAD(bytes, 0, HEADER_BYTES)
        val plain = try {
            cipher.doFinal(bytes, HEADER_BYTES, bytes.size - HEADER_BYTES)
        } catch (e: AEADBadTagException) {
            throw WrongPasswordException()
        }
        return unzip(plain)
    }

    private fun key(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            val raw = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(raw, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun zip(contents: BackupContents): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun put(name: String, data: ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(data)
                zip.closeEntry()
            }
            put(MANIFEST, contents.manifest.encode())
            put(DATABASE, contents.database)
            contents.files.forEach { (path, data) -> put(FILES + checkedPath(path), data) }
        }
        return out.toByteArray()
    }

    private fun unzip(bytes: ByteArray): BackupContents {
        var manifest: BackupManifest? = null
        var database: ByteArray? = null
        val files = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val data = zip.readBytes()
                when {
                    entry.isDirectory -> Unit
                    entry.name == MANIFEST -> manifest = BackupManifest.decode(data)
                    entry.name == DATABASE -> database = data
                    entry.name.startsWith(FILES) -> files[checkedPath(entry.name.removePrefix(FILES))] = data
                }
            }
        }
        return BackupContents(manifest ?: throw NotABackupException(), database ?: throw NotABackupException(), files)
    }

    /** Paths stay inside the files folder: no absolute paths and no "..". */
    private fun checkedPath(path: String): String {
        val parts = path.split('/')
        if (path.isBlank() || path.startsWith("/") || parts.any { it.isEmpty() || it == "." || it == ".." } || '\\' in path) {
            throw NotABackupException()
        }
        return path
    }
}
