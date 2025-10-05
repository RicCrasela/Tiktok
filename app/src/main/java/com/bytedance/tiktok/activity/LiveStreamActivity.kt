package com.bytedance.tiktok.activity

import android.Manifest
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bytedance.tiktok.base.BaseBindingActivity
import com.bytedance.tiktok.databinding.ActivityLiveStreamBinding
import com.pedro.rtplibrary.rtmp.RtmpCamera2
import com.pedro.rtplibrary.view.OpenGlView
import android.content.pm.PackageManager
import android.widget.ArrayAdapter

/**
 * Simple live streaming screen using RTMP.
 * Allows start/stop, switch camera, mute/unmute microphone, and set stream URL/title.
 */
class LiveStreamActivity : BaseBindingActivity<ActivityLiveStreamBinding>({ ActivityLiveStreamBinding.inflate(it) }) {

    private var rtmpCamera2: RtmpCamera2? = null
    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize RTMP camera with OpenGL view for filters
        rtmpCamera2 = RtmpCamera2(this, binding.openGlView)

        binding.btnSwitch.setOnClickListener {
            rtmpCamera2?.switchCamera()
        }

        binding.btnStart.setOnClickListener {
            startStream()
        }

        binding.btnStop.setOnClickListener {
            stopStream()
        }

        binding.btnMute.setOnClickListener {
            val isMuted = rtmpCamera2?.isAudioMuted ?: false
            rtmpCamera2?.muteAudio(!isMuted)
            binding.btnMute.text = if (!isMuted) "Unmute" else "Mute"
        }

        // some presets for quick test
        val presets = listOf(
            "rtmp://live.twitch.tv/app/<STREAM_KEY>",
            "rtmp://a.rtmp.youtube.com/live2/<STREAM_KEY>",
            "rtmp://your-server/live/stream"
        )
        binding.spinnerUrls.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, presets)
        binding.spinnerUrls.setSelection(2)

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val shouldAsk = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (shouldAsk) {
            ActivityCompat.requestPermissions(this, permissions, 1001)
        }
    }

    private fun startStream() {
        val url = binding.etUrl.text?.toString().takeIf { !it.isNullOrBlank() } ?: presetsCurrent()
        if (rtmpCamera2?.isStreaming == true) {
            Toast.makeText(this, "Already streaming", Toast.LENGTH_SHORT).show()
            return
        }
        if (rtmpCamera2?.prepareVideo() == true && rtmpCamera2?.prepareAudio() == true) {
            rtmpCamera2?.startStream(url)
            binding.status.text = "LIVE"
        } else {
            Toast.makeText(this, "Error preparing stream", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopStream() {
        if (rtmpCamera2?.isStreaming == true) {
            rtmpCamera2?.stopStream()
            binding.status.text = "OFFLINE"
        }
    }

    private fun presetsCurrent(): String {
        return binding.spinnerUrls.selectedItem?.toString() ?: "rtmp://your-server/live/stream"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (rtmpCamera2?.isStreaming == true) {
            rtmpCamera2?.stopStream()
        }
        rtmpCamera2?.stopPreview()
    }
}