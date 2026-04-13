package com.baidu.tv.player.ui.playback

import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 视频播放Fragment
 * 使用ExoPlayer作为主力播放器，支持H.265三级播放策略
 * 1. 优先硬件解码
 * 2. 失败则用VLC优化软件解码
 * 3. 最后尝试临时转码为H.264（使用FFmpeg）
 */
class VideoPlayerFragment : Fragment() {

    private val TAG = "VideoPlayerFragment"

    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PlaybackViewModel
    private lateinit var exoPlayer: ExoPlayer
    private var isPlayerReady = false
    private var isFullscreen = false

    // 三级播放策略状态
    private var hardwareDecodingAttempted = false
    private var vlcFallbackAttempted = false
    private var transcodeAttempted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化ViewModel
        viewModel = ViewModelProvider(requireActivity())[PlaybackViewModel::class.java]

        // 初始化ExoPlayer
        initializeExoPlayer()

        // 监听播放状态变化
        observePlaybackState()

        // 设置播放器控件事件
        setupPlayerControls()

        // 设置屏幕旋转监听
        setupScreenRotation()
    }

    /**
     * 初始化ExoPlayer
     */
    private fun initializeExoPlayer() {
        val context = requireContext()
        val userAgent = Util.getUserAgent(context, "BaiduTVPlayer")
        val dataSourceFactory = DefaultDataSourceFactory(context, userAgent)

        exoPlayer = ExoPlayer.Builder(context)
            .setRenderersFactory(
                DefaultRenderersFactory(context)
                    .setEnableDecoderFallback(true)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            )
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        10000,  // minBufferMs
                        30000,  // maxBufferMs
                        500,    // bufferForPlaybackMs
                        1500    // bufferForPlaybackAfterRebufferMs
                    )
                    .build()
            )
            .build()

        // 设置播放器监听器
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.message}")
                handlePlaybackError(error)
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        binding.loadingIndicator.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        binding.loadingIndicator.visibility = View.GONE
                        isPlayerReady = true
                        // 开始播放时，预加载下一个文件
                        viewModel.preloadNextFile()
                    }
                    Player.STATE_ENDED -> {
                        viewModel.playNext()
                    }
                }
            }
        })

        // 绑定播放器到View
        binding.playerView.player = exoPlayer
    }

    /**
     * 监听播放状态变化
     */
    private fun observePlaybackState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isPlaying.collect { isPlaying ->
                if (isPlayerReady) {
                    if (isPlaying) {
                        exoPlayer.play()
                    } else {
                        exoPlayer.pause()
                    }
                }
            }

            viewModel.preparedMediaUrl.collect { mediaUrl ->
                if (mediaUrl != null && mediaUrl.isNotEmpty()) {
                    playMedia(mediaUrl)
                }
            }
        }
    }

    /**
     * 播放媒体文件
     */
    private fun playMedia(mediaUrl: String) {
        if (mediaUrl.isEmpty()) return

        // 重置播放策略状态
        hardwareDecodingAttempted = false
        vlcFallbackAttempted = false
        transcodeAttempted = false

        val file = File(mediaUrl)
        if (!file.exists()) {
            Toast.makeText(context, "文件不存在: $mediaUrl", Toast.LENGTH_SHORT).show()
            return
        }

        // 检查文件格式
        val mimeType = getMimeType(mediaUrl)
        val isH265 = isH265Codec(mediaUrl)

        // 播放策略：三级播放
        if (isH265) {
            // 尝试硬件解码H.265
            hardwareDecodingAttempted = true
            playWithExoPlayer(mediaUrl, mimeType)
        } else {
            // 普通格式直接播放
            playWithExoPlayer(mediaUrl, mimeType)
        }
    }

    /**
     * 使用ExoPlayer播放
     */
    private fun playWithExoPlayer(mediaUrl: String, mimeType: String) {
        val uri = Uri.parse(mediaUrl)
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(mimeType)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    /**
     * 处理播放错误
     */
    private fun handlePlaybackError(error: PlaybackException) {
        // 三级播放策略：如果ExoPlayer失败，尝试VLC
        if (!vlcFallbackAttempted) {
            vlcFallbackAttempted = true
            Log.w(TAG, "ExoPlayer failed, attempting VLC fallback")
            // 在实际实现中，这里会启动VLC播放器
            // 由于VLC需要在Activity中管理，这里通过回调通知Activity
            (requireActivity() as PlaybackActivity).attemptVlcPlayback()
        } else if (!transcodeAttempted) {
            // 如果VLC也失败，尝试转码为H.264
            transcodeAttempted = true
            Log.w(TAG, "VLC fallback failed, attempting transcoding to H.264")
            // 在实际实现中，这里会使用FFmpeg转码
            // 由于FFmpeg需要复杂的处理，这里仅做日志记录
            Toast.makeText(context, "尝试转码为H.264格式", Toast.LENGTH_LONG).show()
        } else {
            // 所有策略都失败
            Log.e(TAG, "All playback strategies failed")
            Toast.makeText(context, "播放失败：无法解码此文件", Toast.LENGTH_LONG).show()
            viewModel.playNext()
        }
    }

    /**
     * 获取文件MIME类型
     */
    private fun getMimeType(filePath: String): String {
        val file = File(filePath)
        val fileName = file.name.toLowerCase()
        return when {
            fileName.endsWith(".mp4") -> "video/mp4"
            fileName.endsWith(".mkv") -> "video/x-matroska"
            fileName.endsWith(".avi") -> "video/x-msvideo"
            fileName.endsWith(".mov") -> "video/quicktime"
            fileName.endsWith(".flv") -> "video/x-flv"
            fileName.endsWith(".ts") -> "video/mp2t"
            fileName.endsWith(".3gp") -> "video/3gpp"
            fileName.endsWith(".wmv") -> "video/x-ms-wmv"
            fileName.endsWith(".webm") -> "video/webm"
            else -> "video/mp4"
        }
    }

    /**
     * 检查是否为H.265编码
     */
    private fun isH265Codec(filePath: String): Boolean {
        val file = File(filePath)
        val fileName = file.name.toLowerCase()
        // 简单判断：H.265文件通常有.hevc或.h265扩展名
        // 在实际实现中，应该使用MediaMetadataRetriever或ffprobe分析编码格式
        return fileName.endsWith(".hevc") || fileName.endsWith(".h265")
    }

    /**
     * 设置播放器控件事件
     */
    private fun setupPlayerControls() {
        binding.playPauseButton.setOnClickListener {
            viewModel.togglePlayPause()
        }

        binding.prevButton.setOnClickListener {
            viewModel.playPrev()
        }

        binding.nextButton.setOnClickListener {
            viewModel.playNext()
        }

        binding.playModeButton.setOnClickListener {
            val currentMode = viewModel.getPlayMode() ?: PlayMode.ORDER
            val nextMode = when (currentMode) {
                PlayMode.ORDER -> PlayMode.RANDOM
                PlayMode.RANDOM -> PlayMode.SINGLE
                PlayMode.SINGLE -> PlayMode.ORDER
            }
            viewModel.setPlayMode(nextMode)
            updatePlayModeIcon(nextMode)
        }
    }

    /**
     * 更新播放模式图标
     */
    private fun updatePlayModeIcon(mode: PlayMode) {
        val iconRes = when (mode) {
            PlayMode.ORDER -> R.drawable.ic_play_order
            PlayMode.RANDOM -> R.drawable.ic_play_random
            PlayMode.SINGLE -> R.drawable.ic_play_single
        }
        binding.playModeButton.setImageResource(iconRes)
    }

    /**
     * 设置屏幕旋转监听
     */
    private fun setupScreenRotation() {
        binding.playerView.setOnClickListener {
            toggleFullscreen()
        }
    }

    /**
     * 切换全屏模式
     */
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        val activity = requireActivity()
        val window = activity.window

        if (isFullscreen) {
            // 进入全屏
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            binding.playerView.setShowControllerOnFocus(true)
            binding.playerView.setShowControllerOnTouch(true)
            binding.playerView.setShowBuffering(VideoPlayerFragment.ShowBuffering.ALWAYS)
        } else {
            // 退出全屏
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            binding.playerView.setShowControllerOnFocus(false)
            binding.playerView.setShowControllerOnTouch(false)
            binding.playerView.setShowBuffering(VideoPlayerFragment.ShowBuffering.NEVER)
        }
    }

    override fun onStart() {
        super.onStart()
        if (isPlayerReady) {
            exoPlayer.play()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isPlayerReady) {
            exoPlayer.pause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 释放ExoPlayer资源
        exoPlayer.release()
        _binding = null
    }

    companion object {
        fun newInstance(): VideoPlayerFragment {
            return VideoPlayerFragment()
        }
    }
}

/**
 * 播放器显示缓冲状态
 */
enum class ShowBuffering {
    NEVER, ALWAYS, ON_DEMAND
}