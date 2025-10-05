package com.bytedance.tiktok.activity

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bytedance.tiktok.databinding.ActivityLiveStreamBinding
import com.gyf.immersionbar.ImmersionBar
import com.pedro.rtplibrary.rtmp.RtmpCamera2
import com.pedro.encoder.input.video.CameraHelper

/**
 * Live streaming publisher using RootEncoder (rtmp-rtsp-stream-client-java successor).
 * Features: start/stop stream, switch camera, mute/unmute mic.
 */
class LiveStreamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLiveStreamBinding
    private lateinit var rtmpCamera2: RtmpCamera2
    private var hasAudio: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ImmersionBar.with(this).statusBarDarkFont(true).init()

        rtmpCamera2 = RtmpCamera2(binding.surfaceView, this)
        rtmpCamera2.setAuthorization(null, null)

        binding.btnStart.setOnClickListener {
            val url = binding.editRtmpUrl.text?.toString() ?: ""
            if (url.isBlank()) {
                Toast.makeText(this, "Masukkan RTMP url", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!rtmpCamera2.isStreaming) {
                if (rtmpCamera2.prepareAudio() && rtmpCamera2.prepareVideo()) {
                    rtmpCamera2.startStream(url)
                    updateUiState()
                } else {
                    Toast.makeText(this, "Tidak bisa memulai streaming", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Sudah streaming", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStop.setOnClickListener {
            if (rtmpCamera2.isStreaming) {
                rtmpCamera2.stopStream()
                updateUiState()
            }
        }

        binding.btnSwitch.setOnClickListener {
            rtmpCamera2.switchCamera()
        }

        binding.btnMic.setOnClickListener {
            hasAudio = !hasAudio
            rtmpCamera2.setAudioEnable(hasAudio)
            binding.btnMic.text = if (hasAudio) "Mute" else "Unmute"
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun updateUiState() {
        val streaming = rtmpCamera2.isStreaming
        binding.status.text = if (streaming) "Live" else "Idle"
        binding.btnStart.visibility = if (streaming) View.GONE else View.VISIBLE
        binding.btnStop.visibility = if (streaming) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 100)
        if (!rtmpCamera2.isStreaming && !rtmpCamera2.isOnPreview) {
            rtmpCamera2.startPreview(CameraHelper.Facing.FRONT)
        }
    }

    override fun onPause() {
        super.onPause()
        if (rtmpCamera2.isStreaming) {
            rtmpCamera2.stopStream()
        }
        if (rtmpCamera2.isOnPreview) {
            rtmpCamera2.stopPreview()
        }
    }
}