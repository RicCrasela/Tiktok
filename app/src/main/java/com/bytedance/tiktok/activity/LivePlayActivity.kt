package com.bytedance.tiktok.activity

import android.os.Bundle
import com.bytedance.tiktok.base.BaseBindingActivity
import com.bytedance.tiktok.databinding.ActivityLivePlayBinding
import com.bytedance.tiktok.player.VideoPlayer

class LivePlayActivity : BaseBindingActivity<ActivityLivePlayBinding>({ ActivityLivePlayBinding.inflate(it) }) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        binding.tvTitle.text = title
        binding.videoPlayer.playVideo(url)
    }
}