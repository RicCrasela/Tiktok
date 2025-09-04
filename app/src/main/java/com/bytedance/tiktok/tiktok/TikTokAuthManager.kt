package com.bytedance.tiktok.tiktok

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Browser
import com.bytedance.tiktok.BuildConfig
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicReference

/**
 * Minimal OAuth flow for TikTok using Authorization Code via external browser.
 * Note: Code -> Token exchange must be done on server side to protect client secret.
 */
object TikTokAuthManager {

    interface AuthListener {
        fun onSuccess(authorizationCode: String)
        fun onError(error: String)
        fun onCancel()
    }

    private val listenerRef = AtomicReference<WeakReference<AuthListener>?>(null)

    // Placeholder TikTok authorize URL; verify with TikTok OpenAPI docs
    private const val OAUTH_AUTHORIZE_URL = "https://www.tiktok.com/auth/authorize/"

    fun startLogin(context: Context, redirectUri: String, scope: String = "user.info.basic", state: String = "state", listener: AuthListener) {
        listenerRef.set(WeakReference(listener))

        val uri = Uri.parse(OAUTH_AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_key", BuildConfig.TIKTOK_API_KEY)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", scope)
            .appendQueryParameter("state", state)
            .build()

        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun onAuthRedirect(uri: Uri?) {
        val listener = listenerRef.get()?.get()
        if (uri == null) {
            listener?.onError("No redirect data")
            return
        }
        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")
        when {
            !code.isNullOrEmpty() -> listener?.onSuccess(code)
            !error.isNullOrEmpty() -> listener?.onError("$error: $errorDescription")
            else -> listener?.onCancel()
        }
        listenerRef.set(null)
    }
}