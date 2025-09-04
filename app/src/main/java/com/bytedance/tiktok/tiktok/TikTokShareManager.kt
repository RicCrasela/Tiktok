package com.bytedance.tiktok.tiktok

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Minimal helper to share a video Uri to TikTok if app is installed.
 * Uses standard ACTION_SEND intent targeting TikTok package names.
 */
object TikTokShareManager {

    private val tiktokPackages = listOf(
        "com.zhiliaoapp.musically", // global
        "com.ss.android.ugc.trill"  // China/alt
    )

    fun shareVideo(context: Context, videoUri: Uri, mimeType: String = "video/*") {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Try targeting TikTok package specifically
        for (pkg in tiktokPackages) {
            val targeted = Intent(sendIntent).setPackage(pkg)
            if (targeted.resolveActivity(context.packageManager) != null) {
                try {
                    context.startActivity(targeted)
                    return
                } catch (_: ActivityNotFoundException) {
                }
            }
        }

        // Fallback to system chooser
        try {
            context.startActivity(Intent.createChooser(sendIntent, "Share to TikTok"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share video", Toast.LENGTH_SHORT).show()
        }
    }
}