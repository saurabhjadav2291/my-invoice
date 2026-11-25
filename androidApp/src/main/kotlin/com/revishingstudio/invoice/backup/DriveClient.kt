package com.revishingstudio.invoice.backup

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

/** A Drive error with a sentence for the person. */
class DriveException(message: String, val code: Int) : IOException(message)

/** The backup file in the Drive app folder. */
data class DriveFile(
    val id: String,
    val modifiedAt: Long,
    /** When the data in it was taken, as saved by the app (falls back to the upload time). */
    val takenAt: Long,
    val invoiceCount: Int?,
    val clientCount: Int?,
)

/**
 * Calls the Google Drive REST API directly, limited to the hidden app folder
 * ("appDataFolder"). Plain HTTPS, so no Google API client library is bundled.
 */
class DriveClient(private val token: String) {

    /** The newest backup, or null if this Google account has none yet. */
    fun latestBackup(): DriveFile? = listBackups().firstOrNull()

    /** Uploads a new backup, then removes the older ones, so a failed upload never loses the last good copy. */
    fun upload(bytes: ByteArray, takenAt: Long, invoiceCount: Int, clientCount: Int): DriveFile {
        val metadata = JSONObject()
            .put("name", FILE_NAME)
            .put("parents", listOf("appDataFolder").let { org.json.JSONArray(it) })
            .put(
                "appProperties",
                JSONObject()
                    .put("takenAt", takenAt.toString())
                    .put("invoices", invoiceCount.toString())
                    .put("clients", clientCount.toString()),
            )
        val boundary = "my-invoice-${System.nanoTime()}"
        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
            write(metadata.toString().toByteArray())
            write("\r\n--$boundary\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray())
            write(bytes)
            write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()
        val created = JSONObject(
            request(
                "POST",
                "$UPLOAD/files?uploadType=multipart&fields=${enc(FIELDS)}",
                body,
                "multipart/related; boundary=$boundary",
            ).toString(Charsets.UTF_8),
        ).toBackup()
        listBackups().filter { it.id != created.id }.forEach { old -> runCatching { delete(old.id) } }
        return created
    }

    fun download(id: String): ByteArray = request("GET", "$API/files/$id?alt=media")

    /** The Google account's email, if Drive shares it. */
    fun accountEmail(): String? = runCatching {
        JSONObject(request("GET", "$API/about?fields=user(emailAddress)").toString(Charsets.UTF_8))
            .optJSONObject("user")?.optString("emailAddress")?.ifBlank { null }
    }.getOrNull()

    private fun listBackups(): List<DriveFile> {
        val q = enc("name = '$FILE_NAME' and trashed = false")
        val url = "$API/files?spaces=appDataFolder&q=$q&orderBy=${enc("modifiedTime desc")}&pageSize=20&fields=${enc("files($FIELDS)")}"
        val files = JSONObject(request("GET", url).toString(Charsets.UTF_8)).optJSONArray("files") ?: return emptyList()
        return (0 until files.length()).map { files.getJSONObject(it).toBackup() }
    }

    private fun delete(id: String) {
        request("DELETE", "$API/files/$id")
    }

    private fun JSONObject.toBackup(): DriveFile {
        val modified = optString("modifiedTime").let { runCatching { Instant.parse(it).toEpochMilli() }.getOrDefault(0L) }
        val props = optJSONObject("appProperties")
        return DriveFile(
            id = getString("id"),
            modifiedAt = modified,
            takenAt = props?.optString("takenAt")?.toLongOrNull() ?: modified,
            invoiceCount = props?.optString("invoices")?.toIntOrNull(),
            clientCount = props?.optString("clients")?.toIntOrNull(),
        )
    }

    private fun request(method: String, url: String, body: ByteArray? = null, contentType: String? = null): ByteArray {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", contentType)
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val detail = runCatching { conn.errorStream?.readBytes()?.toString(Charsets.UTF_8) }.getOrNull().orEmpty()
                throw DriveException(messageFor(code, detail), code)
            }
            return conn.inputStream.use { it.readBytes() }
        } finally {
            conn.disconnect()
        }
    }

    private fun messageFor(code: Int, detail: String): String = when {
        code == 401 -> "Google Drive access has expired. Turn backup off and on again."
        code == 403 && ("accessNotConfigured" in detail || "SERVICE_DISABLED" in detail) ->
            "Google Drive isn't switched on for this app yet (Drive API)"
        code == 403 && "storageQuotaExceeded" in detail -> "Your Google Drive is full"
        code == 403 -> "Google Drive refused the backup (403)"
        code == 404 -> "The backup is no longer in Google Drive"
        code >= 500 -> "Google Drive is having trouble. The app will try again later."
        else -> "Google Drive error $code"
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    companion object {
        const val FILE_NAME = "my-invoice-backup.mib"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
        private const val FIELDS = "id,modifiedTime,appProperties"

        /** Withdraws the app's Drive access for this token's account. */
        fun revoke(token: String) {
            val conn = URL("https://oauth2.googleapis.com/revoke").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.outputStream.use { it.write("token=${URLEncoder.encode(token, "UTF-8")}".toByteArray()) }
                conn.responseCode
            } finally {
                conn.disconnect()
            }
        }
    }
}
