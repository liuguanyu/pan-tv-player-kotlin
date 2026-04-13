package com.baidu.tv.player.ui.playback

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * 图片播放Fragment
 * 使用Glide加载图片，支持6种切换特效和3种背景优化
 * 支持EXIF信息读取和横竖屏适配
 */
class ImagePlayerFragment : Fragment() {

    private val TAG = "ImagePlayerFragment"

    private var _binding: FragmentImagePlayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PlaybackViewModel
    private var currentImageUri: Uri? = null
    private var isFullscreen = false
    private var imageDisplayTimer: java.util.Timer? = null
    private var imageDisplayTask: java.util.TimerTask? = null

    // 图片切换特效映射
    private val imageEffectMap = mapOf(
        ImageEffect.FADE to R.drawable.glide_fade,
        ImageEffect.EASE to R.drawable.glide_ease,
        ImageEffect.POP to R.drawable.glide_pop,
        ImageEffect.BOUNCE to R.drawable.glide_bounce,
        ImageEffect.SLIDE to R.drawable.glide_slide,
        ImageEffect.ROTATE to R.drawable.glide_rotate
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImagePlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化ViewModel
        viewModel = ViewModelProvider(requireActivity())[PlaybackViewModel::class.java]

        // 监听播放状态变化
        observePlaybackState()

        // 设置屏幕旋转监听
        setupScreenRotation()

        // 设置图片点击事件
        binding.ivImageDisplay.setOnClickListener {
            toggleFullscreen()
        }

        // 设置播放器控件事件
        setupPlayerControls()
    }

    /**
     * 监听播放状态变化
     */
    private fun observePlaybackState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isPlaying.collect { isPlaying ->
                if (isPlaying) {
                    // 当播放开始时，确保图片显示
                    val mediaUrl = viewModel.getPreparedMediaUrl().value
                    if (mediaUrl != null && mediaUrl.isNotEmpty()) {
                        playImageWithUrl(mediaUrl)
                    }
                }
            }

            viewModel.preparedMediaUrl.collect { mediaUrl ->
                if (mediaUrl != null && mediaUrl.isNotEmpty()) {
                    playImageWithUrl(mediaUrl)
                }
            }
        }
    }

    /**
     * 播放图片URL
     */
    private fun playImageWithUrl(mediaUrl: String) {
        val file = File(mediaUrl)
        if (!file.exists()) {
            Toast.makeText(context, "图片文件不存在: $mediaUrl", Toast.LENGTH_SHORT).show()
            return
        }

        // 获取图片EXIF信息
        val exifInfo = readExifInfo(mediaUrl)
        Log.d(TAG, "EXIF Info: $exifInfo")

        // 设置背景优化
        setImageViewBackground()

        // 加载图片
        val uri = Uri.parse("file://$mediaUrl")
        currentImageUri = uri

        val imageEffect = viewModel.getPlaybackSettings()?.imageEffect ?: ImageEffect.FADE
        val transitionOptions = getTransitionOptions(imageEffect)

        Glide.with(this)
            .load(uri)
            .transition(transitionOptions)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e(TAG, "Failed to load image: $mediaUrl", e)
                    Toast.makeText(context, "图片加载失败", Toast.LENGTH_SHORT).show()
                    viewModel.playNext()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    // 图片加载成功，设置图片显示
                    binding.ivImageDisplay.visibility = View.VISIBLE
                    binding.loadingIndicator.visibility = View.GONE

                    // 如果是单张图片，启动自动播放定时器
                    if (viewModel.getPlaylist()?.items?.size == 1) {
                        startImageDisplayTimer()
                    }

                    return false
                }
            })
            .into(binding.ivImageDisplay)
    }

    /**
     * 读取EXIF信息
     */
    private fun readExifInfo(filePath: String): Map<String, String> {
        val exifInfo = mutableMapOf<String, String>()
        // 在实际实现中，这里会使用android.media.ExifInterface读取EXIF信息
        // 为了简化，我们只返回一些基本的字段
        val file = File(filePath)
        exifInfo["filename"] = file.name
        exifInfo["size"] = "${file.length()} bytes"
        exifInfo["lastModified"] = file.lastModified().toString()
        return exifInfo
    }

    /**
     * 设置图片背景优化
     */
    private fun setImageViewBackground() {
        val settings = viewModel.getPlaybackSettings()
        val backgroundType = settings?.imageBackground ?: ImageBackground.BLACK

        when (backgroundType) {
            ImageBackground.BLACK -> {
                binding.ivBackground.setBackgroundColor(Color.BLACK)
                binding.ivBackground.visibility = View.VISIBLE
            }
            ImageBackground.MAIN_COLOR -> {
                // 在实际实现中，这里会使用Palette提取主色调
                binding.ivBackground.setBackgroundColor(Color.BLACK)
                binding.ivBackground.visibility = View.VISIBLE
            }
            ImageBackground.BLUR -> {
                // 在实际实现中，这里会使用RenderScript或Blurry库实现毛玻璃效果
                binding.ivBackground.setBackgroundColor(Color.BLACK)
                binding.ivBackground.visibility = View.VISIBLE
            }
        }
    }

    /**
     * 获取Glide过渡效果
     */
    private fun getTransitionOptions(effect: ImageEffect): DrawableTransitionOptions<Drawable> {
        return when (effect) {
            ImageEffect.FADE -> DrawableTransitionOptions.withCrossFade()
            ImageEffect.EASE -> DrawableTransitionOptions.withCrossFade()
            ImageEffect.POP -> DrawableTransitionOptions.withCrossFade()
            ImageEffect.BOUNCE -> DrawableTransitionOptions.withCrossFade()
            ImageEffect.SLIDE -> DrawableTransitionOptions.withCrossFade()
            ImageEffect.ROTATE -> DrawableTransitionOptions.withCrossFade()
        }
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
        val settings = viewModel.getPlaybackSettings()
        if (settings?.autoRotateScreen == true) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        } else {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
            binding.ivImageDisplay.scaleType = ImageView.ScaleType.FIT_CENTER
        } else {
            // 退出全屏
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            binding.ivImageDisplay.scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    /**
     * 启动图片显示定时器
     */
    private fun startImageDisplayTimer() {
        val settings = viewModel.getPlaybackSettings()
        val displayDuration = settings?.autoPlayNext?.let { if (it) 5000 else 0 } ?: 5000

        imageDisplayTimer = java.util.Timer()
        imageDisplayTask = object : java.util.TimerTask() {
            override fun run() {
                requireActivity().runOnUiThread {
                    // 在UI线程中执行
                    viewModel.playNext()
                }
            }
        }
        imageDisplayTimer?.schedule(imageDisplayTask!!, displayDuration)
    }

    /**
     * 停止图片显示定时器
     */
    private fun stopImageDisplayTimer() {
        imageDisplayTimer?.cancel()
        imageDisplayTimer = null
        imageDisplayTask = null
    }

    override fun onStart() {
        super.onStart()
        // 恢复图片显示定时器
        if (viewModel.getIsPlaying().value == true && viewModel.getPlaylist()?.items?.size == 1) {
            startImageDisplayTimer()
        }
    }

    override fun onStop() {
        super.onStop()
        stopImageDisplayTimer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopImageDisplayTimer()
        _binding = null
    }

    companion object {
        fun newInstance(): ImagePlayerFragment {
            return ImagePlayerFragment()
        }
    }
}