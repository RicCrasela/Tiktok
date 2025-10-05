package com.bytedance.tiktok.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bytedance.tiktok.databinding.ActivityLivePlayerBinding
import com.bytedance.tiktok.player.VideoPlayer
import com.gyf.immersionbar.ImmersionBar

/**
 * Live player for RTMP streams using ExoPlayer RTMP extension
 */
class LivePlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLivePlayerBinding
    private lateinit var playerView: VideoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLivePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ImmersionBar.with(this).statusBarDarkFont(true).init()

        playerView = binding.livePlayer

        binding.btnPlay.setOnClickListener {
            val url = binding.editUrl.text?.toString() ?: ""
            if (url.isBlank()) {
                Toast.makeText(this, "Masukkan RTMP/HLS URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            playerView.playVideo(url)
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onPause() {
        super.onPause()
        playerView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        playerView.release()
    }
}