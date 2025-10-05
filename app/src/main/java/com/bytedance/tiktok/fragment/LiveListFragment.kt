package com.bytedance.tiktok.fragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.bytedance.tiktok.activity.LivePlayActivity
import com.bytedance.tiktok.base.BaseBindingFragment
import com.bytedance.tiktok.databinding.FragmentLiveListBinding
import com.bytedance.tiktok.bean.LiveSession
import com.bytedance.tiktok.adapter.LiveSessionAdapter
import android.content.Intent

/**
 * Simple list of ongoing live sessions. Demo uses static data.
 * Replace Repository with backend API or Realtime DB for production.
 */
class LiveListFragment : BaseBindingFragment<FragmentLiveListBinding>({ FragmentLiveListBinding.inflate(it) }) {

    private val sessions = mutableListOf<LiveSession>()
    private lateinit var adapter: LiveSessionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LiveSessionAdapter {
            val intent = Intent(requireContext(), LivePlayActivity::class.java)
            intent.putExtra("url", it.streamUrl)
            intent.putExtra("title", it.title)
            startActivity(intent)
        }
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        loadDemo()
    }

    private fun loadDemo() {
        sessions.clear()
        // Examples: replace with your RTMP/HLS endpoints
        sessions.add(LiveSession("LILY NAJWA.2", "Cari teman yang baik", 4300, "https://example.com/thumb1.jpg", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"))
        sessions.add(LiveSession("serindah_A...", "Ngobrol santai", 1280, "https://example.com/thumb2.jpg", "https://storage.googleapis.com/shaka-demo-assets/angel-one/hls/angel-one.m3u8"))
        sessions.add(LiveSession("Host Demo", "RTMP demo", 200, "https://example.com/thumb3.jpg", "rtmp://your-server/live/stream"))
        adapter.submitList(sessions.toList())
    }
}