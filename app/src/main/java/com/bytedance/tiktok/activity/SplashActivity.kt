package com.bytedance.tiktok.activity

import android.content.Intent
import android.net.Uri
import android.os.CountDownTimer
import android.widget.Toast
import com.bytedance.tiktok.BuildConfig
import com.bytedance.tiktok.base.BaseBindingActivity
import com.bytedance.tiktok.bean.DataCreate
import com.bytedance.tiktok.databinding.ActivitySplashBinding
import com.bytedance.tiktok.tiktok.TikTokAuthManager

/**
 * create by libo
 * create on 2020/5/19
 * description App启动页面
 */
class SplashActivity : BaseBindingActivity<ActivitySplashBinding>({ActivitySplashBinding.inflate(it)}) {

    override fun init() {
        setFullScreen()
        DataCreate()

        // Quick check: if API key placeholder still set, continue as normal
        val hasKey = BuildConfig.TIKTOK_API_KEY.isNotEmpty() && BuildConfig.TIKTOK_API_KEY != "__REPLACE_WITH_YOUR_TIKTOK_API_KEY__"

        object : CountDownTimer(300, 300) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }.start()

        // Optional: Example of starting TikTok login flow when long-pressing the splash root (for demo)
        binding.root.setOnLongClickListener {
            if (!hasKey) {
                Toast.makeText(this, "Isi TIKTOK_API_KEY di gradle.properties terlebih dahulu.", Toast.LENGTH_SHORT).show()
                return@setOnLongClickListener true
            }
            val redirect = "com.bytedance.tiktok://tiktok-auth"
            TikTokAuthManager.startLogin(this, redirect, listener = object : TikTokAuthManager.AuthListener {
                override fun onSuccess(authorizationCode: String) {
                    Toast.makeText(this@SplashActivity, "TikTok auth code: $authorizationCode", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: String) {
                    Toast.makeText(this@SplashActivity, "TikTok auth error: $error", Toast.LENGTH_SHORT).show()
                }

                override fun onCancel() {
                    Toast.makeText(this@SplashActivity, "TikTok auth canceled", Toast.LENGTH_SHORT).show()
                }
            })
            true
        }
    }
}