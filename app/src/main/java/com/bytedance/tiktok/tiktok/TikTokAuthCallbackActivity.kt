package com.bytedance.tiktok.tiktok

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * Handles TikTok OAuth redirect with custom scheme.
 * Redirects result back to the Activity that initiated auth via a broadcast or a pending listener.
 */
class TikTokAuthCallbackActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data: Uri? = intent?.data
        // Forward to manager and finish
        TikTokAuthManager.onAuthRedirect(data)
        finish()
    }
}