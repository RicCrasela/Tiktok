package com.bytedance.tiktok.tiktok

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.bytedance.tiktok.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Helper to share a video to TikTok. If given a remote URL, it downloads into cache and shares via FileProvider.
 */
object TikTokShareManager {

    private val tiktokPackages = listOf(
        "com.zhiliaoapp.musically", // global
        "com.ss.android.ugc.trill"  // China/alt
    )

    private val httpClient by lazy {
        OkHttpClient.Builder().build()
    }

    fun shareVideo(context: Context, videoUri: Uri, mimeType: String = "video/*") {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, videoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launchShare(context, sendIntent)
    }

    fun shareVideoUrl(context: Context, url: String, mimeType: String = "video/*") {
        val file = downloadToCache(context, url) ?: run {
            Toast.makeText(context, "Gagal mengunduh video untuk dibagikan", Toast.LENGTH_SHORT).show()
            return
        }
        val contentUri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        shareVideo(context, contentUri, mimeType)
    }

    private fun launchShare(context: Context, sendIntent: Intent) {
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
        try {
            context.startActivity(Intent.createChooser(sendIntent, "Share to TikTok"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadToCache(context: Context, url: String): File? {
        return try {
            val req = Request.Builder().url(url).build()
            val resp = httpClient.newCall(req).execute()
            if (!resp.isSuccessful) return null
            val ext = guessExt(url)
            val outFile = File(context.cacheDir, "share_${System.currentTimeMillis()}$ext")
            resp.body?.byteStream()?.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }

    private fun guessExt(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".mp4") -> ".mp4"
            lower.contains(".mov") -> ".mov"
            lower.contains(".webm") -> ".webm"
            else -> ".mp4"
        }
    }
}