package com.bytedance.tiktok.bean

data class LiveSession(
    val title: String,
    val description: String,
    val viewerCount: Int,
    val thumbnail: String,
    val streamUrl: String
)