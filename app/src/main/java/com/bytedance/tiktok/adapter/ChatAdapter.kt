package com.bytedance.tiktok.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bytedance.tiktok.bean.ChatMessage
import com.bytedance.tiktok.databinding.ItemChatMessageBinding

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.VH>() {

    private val items = mutableListOf<ChatMessage>()

    inner class VH(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvUser.text = item.user
        holder.binding.tvText.text = item.text
    }

    fun submit(message: ChatMessage) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }
}