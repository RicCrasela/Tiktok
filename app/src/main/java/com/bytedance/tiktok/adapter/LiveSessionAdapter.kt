package com.bytedance.tiktok.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bytedance.tiktok.bean.LiveSession
import com.bytedance.tiktok.databinding.ItemLiveSessionBinding

class LiveSessionAdapter(private val onClick: (LiveSession) -> Unit) :
    ListAdapter<LiveSession, LiveSessionAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LiveSession>() {
            override fun areItemsTheSame(oldItem: LiveSession, newItem: LiveSession): Boolean = oldItem.title == newItem.title
            override fun areContentsTheSame(oldItem: LiveSession, newItem: LiveSession): Boolean = oldItem == newItem
        }
    }

    inner class VH(val binding: ItemLiveSessionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLiveSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvTitle.text = item.title
            tvDesc.text = item.description
            tvViewers.text = "${item.viewerCount} penonton"
            Glide.with(root).load(item.thumbnail).into(ivThumb)
            root.setOnClickListener { onClick(item) }
        }
    }
}