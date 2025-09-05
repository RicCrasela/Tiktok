package com.bytedance.tiktok.fragment

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout.LayoutParams
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.Glide
import com.bytedance.tiktok.R
import com.bytedance.tiktok.activity.PlayListActivity
import com.bytedance.tiktok.adapter.VideoAdapter
import com.bytedance.tiktok.base.BaseBindingFragment
import com.bytedance.tiktok.bean.CurUserBean
import com.bytedance.tiktok.bean.DataCreate
import com.bytedance.tiktok.bean.MainPageChangeEvent
import com.bytedance.tiktok.bean.PauseVideoEvent
import com.bytedance.tiktok.bean.VideoBean
import com.bytedance.tiktok.databinding.FragmentRecommendBinding
import com.bytedance.tiktok.player.VideoPlayer
import com.bytedance.tiktok.utils.OnVideoControllerListener
import com.bytedance.tiktok.utils.RxBus
import com.bytedance.tiktok.view.CommentDialog
import com.bytedance.tiktok.view.ControllerView
import com.bytedance.tiktok.view.LikeView
import com.bytedance.tiktok.view.ShareDialog
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import rx.Subscription
import rx.functions.Action1
import java.io.File
import java.io.FileOutputStream


/**
 * create by libo
 * create on 2020-05-19
 * description 推荐播放页
 */
class RecommendFragment : BaseBindingFragment<FragmentRecommendBinding>({FragmentRecommendBinding.inflate(it)}) {
    private var adapter: VideoAdapter?= null

    /** 当前播放视频位置  */
    private var curPlayPos = -1
    private lateinit var videoView: VideoPlayer

    private var ivCurCover: ImageView? = null
    private var subscribe: Subscription?= null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val dataUri: Uri? = result.data?.data
            dataUri?.let { uri ->
                val type = requireContext().contentResolver.getType(uri) ?: ""
                val isPhoto = type.startsWith("image/")
                val cachedFile = copyToCache(uri)
                if (cachedFile != null) {
                    setUploading(true)
                    // Upload to Firebase and then refresh feed
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val post = com.bytedance.tiktok.utils.FirebaseUploadRepository.uploadPost(Uri.fromFile(cachedFile), isPhoto, if (isPhoto) "Photo post" else "Video post")
                            // Map to local bean and insert at top
                            val bean = VideoBean().apply {
                                videoId = post.id.hashCode()
                                videoRes = post.mediaUrl
                                this.isPhoto = post.isPhoto
                                userBean = DataCreate.userList.firstOrNull() ?: VideoBean.UserBean().apply {
                                    uid = 999
                                    nickName = "User"
                                    head = com.bytedance.tiktok.R.mipmap.head1
                                }
                                content = post.content
                                likeCount = 0
                                commentCount = 0
                                shareCount = 0
                            }
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                DataCreate.datas.add(0, bean)
                                adapter?.submitList(mutableListOf<VideoBean>().apply { addAll(DataCreate.datas) })
                                binding.recyclerView.setCurrentItem(0, false)
                                showToast("Upload selesai")
                                setUploading(false)
                            }
                        } catch (_: Exception) {
                            // On failure fallback to local add
                            val bean = VideoBean().apply {
                                videoId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
                                videoRes = cachedFile.toURI().toString()
                                this.isPhoto = isPhoto
                                userBean = DataCreate.userList.firstOrNull() ?: VideoBean.UserBean().apply {
                                    uid = 999
                                    nickName = "User"
                                    head = com.bytedance.tiktok.R.mipmap.head1
                                }
                                content = if (isPhoto) "Photo post" else "Video post"
                            }
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                DataCreate.datas.add(0, bean)
                                adapter?.submitList(mutableListOf<VideoBean>().apply { addAll(DataCreate.datas) })
                                binding.recyclerView.setCurrentItem(0, false)
                                showToast("Upload gagal, ditambahkan lokal")
                                setUploading(false)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
        initVideoPlayer()
        setViewPagerLayoutManager()
        setRefreshEvent()
        observeEvent()

        binding.btnPost.setOnClickListener {
            openMediaPicker()
        }

        // Optional: on first load, try to fetch latest posts from Firestore and prepend
        // We will not block UI if Firebase not configured.
        tryFetchRemote()
    }

    private fun openMediaPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*","video/*"))
        }
        pickMedia.launch(intent)
    }

    private fun copyToCache(uri: Uri): File? {
        return try {
            val cr: ContentResolver = requireContext().contentResolver
            val name = queryDisplayName(uri) ?: "post_${System.currentTimeMillis()}"
            val ext = when {
                name.contains('.', ignoreCase = true) -> name.substring(name.lastIndexOf('.'))
                (cr.getType(uri) ?: "").startsWith("image/") -> ".jpg"
                (cr.getType(uri) ?: "").startsWith("video/") -> ".mp4"
                else -> ""
            }
            val outFile = File(requireContext().cacheDir, "post_${System.currentTimeMillis()}$ext")
            cr.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cr = requireContext().contentResolver
        val cursor = cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return it.getString(idx)
            }
        }
        return null
    }

    private fun initRecyclerView() {
        adapter  = VideoAdapter(requireContext(), binding.recyclerView.getChildAt(0) as RecyclerView)
        binding.recyclerView.adapter = adapter
        adapter?.appendList(DataCreate.datas)
    }

    private fun initVideoPlayer() {
        var params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        videoView = VideoPlayer(requireActivity())
        videoView.layoutParams = params
        lifecycle.addObserver(videoView)
    }

    private fun observeEvent() {
        //监听播放或暂停事件
        subscribe = RxBus.getDefault().toObservable(PauseVideoEvent::class.java)
            .subscribe(Action1 { event: PauseVideoEvent ->
                if (event.isPlayOrPause) {
                    videoView!!.play()
                } else {
                    videoView!!.pause()
                }
            } as Action1<PauseVideoEvent>)
    }

    override fun onDestroy() {
        super.onDestroy()
        subscribe?.unsubscribe()
    }

    private fun setViewPagerLayoutManager() {
        with(binding.recyclerView) {
            orientation = ViewPager2.ORIENTATION_VERTICAL
            offscreenPageLimit = 1
            registerOnPageChangeCallback(pageChangeCallback)
            (binding.recyclerView.getChildAt(0) as RecyclerView).scrollToPosition(PlayListActivity.initPos)
        }
    }

    private val pageChangeCallback = object: OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            playCurVideo(position)
        }
    }

    private fun setRefreshEvent() {
        binding.refreshLayout.setColorSchemeResources(R.color.color_link)
        binding.refreshLayout.setOnRefreshListener {
            tryFetchRemote(true)
        }
    }

    private fun setUploading(uploading: Boolean) {
        try {
            binding.progressUpload.visibility = if (uploading) View.VISIBLE else View.GONE
            binding.btnPost.isEnabled = !uploading
            binding.btnPost.alpha = if (uploading) 0.5f else 1.0f
        } catch (_: Exception) { }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private fun playCurVideo(position: Int) {
        if (position == curPlayPos) {
            return
        }
        val itemView = adapter!!.getRootViewAt(position)
        val rootView = itemView!!.findViewById<ViewGroup>(R.id.rl_container)
        val likeView: LikeView = rootView.findViewById(R.id.likeview)
        val controllerView: ControllerView = rootView.findViewById(R.id.controller)
        val ivPlay = rootView.findViewById<ImageView>(R.id.iv_play)
        val ivCover = rootView.findViewById<ImageView>(R.id.iv_cover)

        // 如果是照片内容，不进行视频播放，直接显示封面
        val bean = adapter!!.getDatas()[position]
        if (bean.isPhoto) {
            // 确保封面显示
            ivCover.visibility = View.VISIBLE
            // 将照片加载为封面
            try {
                Glide.with(this).load(Uri.parse(bean.videoRes)).into(ivCover)
            } catch (_: Exception) {}
            // 隐藏/移除 videoView 以避免叠加
            videoView?.parent?.let {
                (it as ViewGroup).removeView(videoView)
            }
            curPlayPos = position
            likeShareEvent(controllerView)
            RxBus.getDefault().post(CurUserBean(bean.userBean!!))
            return
        }

        //播放暂停事件
        likeView.setOnPlayPauseListener(object: LikeView.OnPlayPauseListener {
            override fun onPlayOrPause() {
                if (videoView!!.isPlaying()) {
                    videoView?.pause()
                    ivPlay.visibility = View.VISIBLE
                } else {
                    videoView?.play()
                    ivPlay.visibility = View.GONE
                }
            }

        })

        //评论点赞事件
        likeShareEvent(controllerView)

        //切换播放视频的作者主页数据
        RxBus.getDefault().post(CurUserBean(DataCreate.datas[position]?.userBean!!))
        curPlayPos = position

        //切换播放器位置
        dettachParentView(rootView)
        autoPlayVideo(curPlayPos, ivCover)
    }

    /**
     * 移除videoview父view
     */
    private fun dettachParentView(rootView: ViewGroup) {
        //1.添加videoView到当前需要播放的item中,添加进item之前，保证videoView没有父view
        videoView?.parent?.let {
            (it as ViewGroup).removeView(videoView)
        }

        rootView.addView(videoView, 0)
    }

    /**
     * 自动播放视频
     */
    private fun autoPlayVideo(position: Int, ivCover: ImageView) {
        videoView.playVideo(adapter!!.getDatas()[position].mediaSource!!)

        videoView.getplayer()?.addListener(object: Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                // 播放状态发生变化时的回调
                // 播放状态包括：Player.STATE_IDLE、Player.STATE_BUFFERING、Player.STATE_READY、Player.STATE_ENDED
                if (state == Player.STATE_READY) {

                }
            }

            fun onPlayerError(error: ExoPlaybackException?) {
                // 播放发生错误时的回调
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // 播放状态变为播放或暂停时的回调
            }

            override fun onRenderedFirstFrame() {
                //第一帧已渲染，隐藏封面
                ivCover.visibility = View.GONE
                ivCurCover = ivCover
            }
        })
    }

    /**
     * 用户操作事件
     */
    private fun likeShareEvent(controllerView: ControllerView) {
        controllerView.setListener(object : OnVideoControllerListener {
            override fun onHeadClick() {
                RxBus.getDefault().post(MainPageChangeEvent(1))
            }

            override fun onLikeClick() {}
            override fun onCommentClick() {
                val commentDialog = CommentDialog()
                commentDialog.show(childFragmentManager, "")
            }

            override fun onShareClick() {
                ShareDialog().show(childFragmentManager, "")
            }
        })
    }

    private fun tryFetchRemote(fromRefresh: Boolean = false) {
        // fetch latest posts from Firestore and prepend to local list
        try {
            // Use reflection-free direct call to our Firebase repo in a coroutine
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val posts = com.bytedance.tiktok.utils.FirebaseFeedRepository.fetchLatest()
                    if (posts.isNotEmpty()) {
                        val mapped = posts.map { post ->
                            VideoBean().apply {
                                videoId = post.id.hashCode()
                                videoRes = post.mediaUrl
                                isPhoto = post.isPhoto
                                userBean = DataCreate.userList.firstOrNull() ?: VideoBean.UserBean().apply {
                                    uid = 999
                                    nickName = "User"
                                    head = com.bytedance.tiktok.R.mipmap.head1
                                }
                                content = post.content
                            }
                        }
                        // Prepend remote posts
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            val merged = ArrayList<VideoBean>()
                            merged.addAll(mapped)
                            merged.addAll(DataCreate.datas)
                            DataCreate.datas = merged
                            adapter?.submitList(ArrayList<VideoBean>().apply { addAll(DataCreate.datas) })
                            if (fromRefresh) binding.refreshLayout.isRefreshing = false
                        }
                    } else if (fromRefresh) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            binding.refreshLayout.isRefreshing = false
                        }
                    }
                } catch (_: Exception) {
                    if (fromRefresh) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            binding.refreshLayout.isRefreshing = false
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            if (fromRefresh) binding.refreshLayout.isRefreshing = false
        }
    }
}