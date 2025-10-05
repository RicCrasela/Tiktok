package com.bytedance.tiktok.chat

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

class ChatClient(
    private val url: String,
    private val listener: Listener
) {

    interface Listener {
        fun onOpen()
        fun onMessage(text: String)
        fun onClosed()
        fun onFailure(t: Throwable)
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private var socket: WebSocket? = null

    fun connect() {
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                listener.onOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listener.onMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                listener.onMessage(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                listener.onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                listener.onFailure(t)
            }
        })
    }

    fun send(text: String) {
        socket?.send(text)
    }

    fun close() {
        socket?.close(1000, "bye")
    }
}