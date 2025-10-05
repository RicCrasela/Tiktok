package com.bytedance.tiktok.bean

data class ChatMessage(
    val user: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)