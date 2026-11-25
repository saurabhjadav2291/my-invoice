package com.revishingstudio.invoice.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google authorization for the Drive app folder: a hidden folder in the person's own
 * Drive that only this app can see. No other Drive files are ever visible to the app.
 */
object DriveAuth {
    const val DRIVE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"

    private fun request(): AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_APPDATA), Scope("email")))
        .build()

    /**
     * Asks Google for access. The result either carries a token, or a consent screen to
     * show first ([AuthorizationResult.hasResolution]); the first time, that screen also
     * lets the person pick their Google account.
     */
    suspend fun authorize(context: Context): AuthorizationResult =
        Identity.getAuthorizationClient(context).authorize(request()).await()

    /** The result after the consent screen closes. */
    fun resultFromIntent(context: Context, data: Intent?): AuthorizationResult =
        Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)

    /** A token without showing anything, or null if the person has to approve access again. */
    suspend fun silentToken(context: Context): String? {
        val result = authorize(context)
        return if (result.hasResolution()) null else result.accessToken
    }

    /** The account's email, from the sign-in Google already did, if it shared one. */
    @Suppress("DEPRECATION")
    fun email(result: AuthorizationResult): String? = runCatching { result.toGoogleSignInAccount()?.email }.getOrNull()
}

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

/** A sentence for the person about a failed Google or Drive step. */
fun Throwable.backupMessage(): String = when {
    this is ApiException && statusCode == CommonStatusCodes.DEVELOPER_ERROR ->
        "Google sign-in isn't set up for this version of the app yet"
    this is ApiException && statusCode == CommonStatusCodes.NETWORK_ERROR -> "No internet connection. Try again when you're online."
    this is ApiException && statusCode == CommonStatusCodes.CANCELED -> "Google sign-in was cancelled"
    this is ApiException -> "Google sign-in didn't work (code $statusCode)"
    this is DriveException -> message ?: "Google Drive didn't respond. Try again later."
    this is java.io.IOException -> "Couldn't reach Google Drive. Check your connection and try again."
    (this is IllegalArgumentException || this is IllegalStateException) && !message.isNullOrBlank() -> message!!
    else -> "Something went wrong with the backup. Please try again."
}
