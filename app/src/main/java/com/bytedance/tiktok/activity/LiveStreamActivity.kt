package com.bytedance.tiktok.activity

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bytedance.tiktok.base.BaseBindingActivity
import com.bytedance.tiktok.databinding.ActivityLiveStreamBinding
import com.pedro.rtplibrary.rtmp.RtmpCamera2
import android.content.pm.PackageManager
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.bytedance.tiktok.adapter.ChatAdapter
import com.bytedance.tiktok.bean.ChatMessage
import com.bytedance.tiktok.chat.ChatClient

/**
 * Simple live streaming screen using RTMP + demo realtime chat (WebSocket echo).
 */
class LiveStreamActivity : BaseBindingActivity<ActivityLiveStreamBinding>({ ActivityLiveStreamBinding.inflate(it) }) {

    private var rtmpCamera2: RtmpCamera2? = null
    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    private lateinit var chatAdapter: ChatAdapter
    private var chatClient: ChatClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize RTMP camera with OpenGL view for filters
        rtmpCamera2 = RtmpCamera2(this, binding.openGlView)

        binding.btnSwitch.setOnClickListener { rtmpCamera2?.switchCamera() }
        binding.btnStart.setOnClickListener { startStream() }
        binding.btnStop.setOnClickListener { stopStream() }
        binding.btnMute.setOnClickListener {
            val isMuted = rtmpCamera2?.isAudioMuted ?: false
            rtmpCamera2?.muteAudio(!isMuted)
            binding.btnMute.text = if (!isMuted) "Unmute" else "Mute"
        }

        // Chat setup
        chatAdapter = ChatAdapter()
        val lm = LinearLayoutManager(this)
        lm.stackFromEnd = true
        binding.rvChat.layoutManager = lm
        binding.rvChat.adapter = chatAdapter
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString() ?: ""
            if (text.isNotBlank()) {
                val msg = ChatMessage("Me", text)
                chatAdapter.submit(msg)
                chatClient?.send(text)
                binding.etMessage.setText("")
                binding.rvChat.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }

        // Demo echo websocket (replace with your chat server)
        chatClient = ChatClient("wss://echo.websocket.events", object : ChatClient.Listener {
            override fun onOpen() {
                runOnUiThread { Toast.makeText(this@LiveStreamActivity, "Chat connected", Toast.LENGTH_SHORT).show() }
            }
            override fun onMessage(text: String) {
                runOnUiThread {
                    chatAdapter.submit(ChatMessage("Remote", text))
                    binding.rvChat.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
            override fun onClosed() { }
            override fun onFailure(t: Throwable) {
                runOnUiThread { Toast.makeText(this@LiveStreamActivity, "Chat error: ${t.message}", Toast.LENGTH_SHORT).show() }
            }
        })
        chatClient?.connect()

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
        val shouldAsk = permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (shouldAsk) ActivityCompat.requestPermissions(this, permissions, 1001)
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
        chatClient?.close()
        if (rtmpCamera2?.isStreaming == true) {
            rtmpCamera2?.stopStream()
        }
        rtmpCamera2?.stopPreview()
    }
}