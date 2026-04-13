package com.baidu.tv.player.ui.playback

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout
import kotlinx.coroutines.launch
import java.io.File

/**
 * 播放器Activity
 * 默认使用 ExoPlayer (主力播放器)，失败时自动切换到 VLC (备用播放器)
 *
 * 播放器策略：
 * 1. 主力使用 ExoPlayer - Google 官方推荐，性能更好，适合 Android TV
 * 2. ExoPlayer 失败时自动切换到 VLC - 支持更多格式（HEVC/H.265 等）
 * 3. 两者都失败则跳过当前文件
 */
class PlaybackActivity : AppCompatActivity() {

    private val TAG = "PlaybackActivity"

    private lateinit var viewModel: PlaybackViewModel
    private lateinit var exoPlayer: ExoPlayer
    private lateinit var libVLC: LibVLC
    private lateinit var vlcMediaPlayer: MediaPlayer
    private var useVlc = false
    private var vlcErrorCount = 0
    private var exoErrorCount = 0
    private val MAX_VLC_RETRIES = 2
    private val MAX_EXO_RETRIES = 1
    private var currentMediaUrl: String? = null
    private val PERMISSION_REQUEST_CODE = 1001

    // UI组件
    private lateinit var surfaceView: View
    private lateinit var playerView: View
    private lateinit var ivImageDisplay: View
    private lateinit var ivBackground: View
    private lateinit var layoutControls: View
    private lateinit var tvFileName: View
    private lateinit var tvLocation: View
    private lateinit var tvPlayerIndicator: View
    private lateinit var tvCurrentTime: View
    private lateinit var tvTotalTime: View
    private lateinit var seekbarProgress: View
    private lateinit var ivPlayMode: View
    private lateinit var ivPrev: View
    private lateinit var ivPlayPause: View
    private lateinit var ivNext: View
    private lateinit var loadingIndicator: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏和沉浸式模式
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        setContentView(R.layout.activity_playback)

        initViews()
        initVLC()
        initExoPlayer()
        initViewModel()
        initData()
        setupClickListeners()
    }

    private fun initViews() {
        surfaceView = findViewById(R.id.surface_view)
        playerView = findViewById(R.id.player_view)
        ivImageDisplay = findViewById(R.id.iv_image_display)
        ivBackground = findViewById(R.id.iv_background)
        layoutControls = findViewById(R.id.layout_controls)
        tvFileName = findViewById(R.id.tv_file_name)
        tvLocation = findViewById(R.id.tv_location)
        tvPlayerIndicator = findViewById(R.id.tv_player_indicator)
        tvCurrentTime = findViewById(R.id.tv_current_time)
        tvTotalTime = findViewById(R.id.tv_total_time)
        seekbarProgress = findViewById(R.id.seekbar_progress)
        ivPlayMode = findViewById(R.id.iv_play_mode)
        ivPrev = findViewById(R.id.iv_prev)
        ivPlayPause = findViewById(R.id.iv_play_pause)
        ivNext = findViewById(R.id.iv_next)
        loadingIndicator = findViewById(R.id.loading_indicator)

        // 隐藏控制栏
        hideControls()
    }

    private fun initVLC() {
        try {
            val options = mutableListOf<String>()
            // 启用详细日志
            options.add("-vvv")
            // 使用硬件加速，默认设置（让VLC自动选择最佳解码器）
            options.add("--avcodec-hw=any")
            // 增加网络缓存以提高稳定性
            options.add("--network-caching=2000")

            libVLC = LibVLC(this, options)
            vlcMediaPlayer = MediaPlayer(libVLC)

            val vout = vlcMediaPlayer.vlcVout
            vout.videoView = surfaceView
            vout.setWindowSize(surfaceView.width, surfaceView.height)

            vout.addCallback(object : IVLCVout.Callback {
                override fun onSurfacesCreated(vout: IVLCVout) {
                    Log.d(TAG, "VLC Surface created")
                }

                override fun onSurfacesDestroyed(vout: IVLCVout) {
                    Log.d(TAG, "VLC Surface destroyed")
                }

                override fun onNewLayout(vout: IVLCVout, width: Int, height: Int, visibleWidth: Int, visibleHeight: Int, sarNum: Int, sarDen: Int) {
                    if (width * height == 0) return

                    // 计算视频实际宽高比（考虑SAR）
                    val videoRatio = width.toDouble() / height
                    val finalVideoRatio = if (sarNum > 0 && sarDen > 0) videoRatio * sarNum / sarDen else videoRatio

                    Log.d(TAG, "视频源尺寸: video=$width×$height, sar=$sarNum/$sarDen, ratio=$finalVideoRatio")

                    runOnUiThread {
                        val dm = android.util.DisplayMetrics()
                        windowManager.defaultDisplay.getRealMetrics(dm)
                        val screenWidth = dm.widthPixels
                        val screenHeight = dm.heightPixels
                        val screenRatio = screenWidth.toDouble() / screenHeight

                        val isLandscape = finalVideoRatio >= 1.0

                        val lp = surfaceView.layoutParams as android.widget.FrameLayout.LayoutParams

                        if (isLandscape) {
                            // 横屏视频：CenterCrop模式
                            val surfaceWidth = if (finalVideoRatio > screenRatio) {
                                (screenHeight * finalVideoRatio).toInt()
                            } else {
                                screenWidth
                            }
                            val surfaceHeight = if (finalVideoRatio > screenRatio) {
                                screenHeight
                            } else {
                                (screenWidth / finalVideoRatio).toInt()
                            }

                            lp.width = surfaceWidth
                            lp.height = surfaceHeight
                            lp.gravity = android.view.Gravity.CENTER
                            surfaceView.layoutParams = lp

                            vlcMediaPlayer.setAspectRatio(null)
                            vlcMediaPlayer.setScale(0f)

                            Log.d(TAG, "横屏视频 - CenterCrop模式: SurfaceView=$surfaceWidth×$surfaceHeight")
                        } else {
                            // 竖屏视频：FitCenter模式
                            lp.width = screenWidth
                            lp.height = screenHeight
                            lp.gravity = android.view.Gravity.CENTER
                            surfaceView.layoutParams = lp

                            val targetAspectRatio = if (finalVideoRatio > screenRatio) {
                                "$screenWidth:${(screenWidth / finalVideoRatio).toInt()}"
                            } else {
                                "${(screenHeight * finalVideoRatio).toInt()}:$screenHeight"
                            }

                            vlcMediaPlayer.setAspectRatio(targetAspectRatio)
                            vlcMediaPlayer.setScale(0f)

                            Log.d(TAG, "竖屏视频 - FitCenter模式: AspectRatio=$targetAspectRatio")
                        }
                    }
                }
            })

            surfaceView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
                override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                    Log.d(TAG, "SurfaceHolder created")
                    if (!vlcMediaPlayer.vlcVout.areViewsAttached()) {
                        vlcMediaPlayer.vlcVout.attachViews()
                    }
                }

                override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {
                    Log.d(TAG, "SurfaceHolder changed: $width×$height")
                    if (vlcMediaPlayer.vlcVout.areViewsAttached()) {
                        vlcMediaPlayer.vlcVout.setWindowSize(width, height)
                    }
                }

                override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                    Log.d(TAG, "SurfaceHolder destroyed")
                }
            })

            vlcMediaPlayer.eventListener = MediaPlayer.EventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Buffering -> {
                        if (event.buffering == 100f) {
                            loadingIndicator.visibility = View.GONE
                        } else {
                            if (loadingIndicator.visibility != View.VISIBLE) {
                                loadingIndicator.visibility = View.VISIBLE
                            }
                        }
                    }
                    MediaPlayer.Event.Playing -> {
                        loadingIndicator.visibility = View.GONE
                        updatePlayPauseButton(true)
                        startProgressUpdate()
                    }
                    MediaPlayer.Event.Paused -> {
                        updatePlayPauseButton(false)
                        stopProgressUpdate()
                    }
                    MediaPlayer.Event.Stopped -> {
                        updatePlayPauseButton(false)
                        stopProgressUpdate()
                    }
                    MediaPlayer.Event.EndReached -> {
                        stopProgressUpdate()
                        playNext()
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        loadingIndicator.visibility = View.GONE
                        handleVlcError()
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "VLC初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            useVlc = false
        }
    }

    private fun initExoPlayer() {
        val context = this
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

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.message}")
                handleExoPlayerError(error)
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        loadingIndicator.visibility = View.VISIBLE
                    }
                    Player.STATE_READY -> {
                        loadingIndicator.visibility = View.GONE
                        // 开始播放时，预加载下一个文件
                        viewModel.preloadNextFile()
                    }
                    Player.STATE_ENDED -> {
                        playNext()
                    }
                }
            }
        })

        // 绑定播放器到View
        playerView.player = exoPlayer
    }

    private fun initViewModel() {
        // 在实际实现中，这里会初始化DataStore
        // 为了简化，我们使用默认构造函数
        viewModel = ViewModelProvider(this)[PlaybackViewModel::class.java]
    }

    private fun initData() {
        val intent = intent
        if (intent.hasExtra("playlist")) {
            val playlist = intent.getSerializableExtra("playlist") as Playlist
            val startIndex = intent.getIntExtra("startIndex", 0)
            viewModel.setPlaylist(playlist, startIndex)
        }
    }

    private fun setupClickListeners() {
        ivPlayPause.setOnClickListener {
            viewModel.togglePlayPause()
        }

        ivPrev.setOnClickListener {
            viewModel.playPrev()
        }

        ivNext.setOnClickListener {
            viewModel.playNext()
        }

        ivPlayMode.setOnClickListener {
            val currentMode = viewModel.getPlayMode() ?: PlayMode.ORDER
            val nextMode = when (currentMode) {
                PlayMode.ORDER -> PlayMode.RANDOM
                PlayMode.RANDOM -> PlayMode.SINGLE
                PlayMode.SINGLE -> PlayMode.ORDER
            }
            viewModel.setPlayMode(nextMode)
            updatePlayModeIcon(nextMode)
        }

        // 监听播放状态变化
        lifecycleScope.launch {
            viewModel.isPlaying.collect { isPlaying ->
                updatePlayPauseButton(isPlaying)
            }

            viewModel.currentFile.collect { fileInfo ->
                if (fileInfo != null) {
                    tvFileName.text = fileInfo.fileName
                    tvPlayerIndicator.text = viewModel.getPlayerIndicator().value

                    // 显示文件路径（用于调试）
                    Log.d(TAG, "Current file: ${fileInfo.absolutePath}")
                }
            }

            viewModel.playerIndicator.collect { indicator ->
                tvPlayerIndicator.text = indicator
            }
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        ivPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun updatePlayModeIcon(mode: PlayMode) {
        val iconRes = when (mode) {
            PlayMode.ORDER -> R.drawable.ic_play_order
            PlayMode.RANDOM -> R.drawable.ic_play_random
            PlayMode.SINGLE -> R.drawable.ic_play_single
        }
        ivPlayMode.setImageResource(iconRes)
    }

    private fun hideControls() {
        layoutControls.visibility = View.GONE
    }

    private fun showControls() {
        layoutControls.visibility = View.VISIBLE
    }

    private fun startProgressUpdate() {
        // 在实际实现中，这里会启动进度更新定时器
        // 为了简化，我们只做日志记录
    }

    private fun stopProgressUpdate() {
        // 在实际实现中，这里会停止进度更新定时器
        // 为了简化，我们只做日志记录
    }

    private fun handleVlcError() {
        vlcErrorCount++
        if (vlcErrorCount <= MAX_VLC_RETRIES) {
            Log.w(TAG, "VLC播放失败，重试第${vlcErrorCount}次")
            // 尝试重新播放
            val currentFile = viewModel.getCurrentFile()
            if (currentFile != null) {
                val media = Media(libVLC, Uri.parse("file://${currentFile.absolutePath}").toString())
                vlcMediaPlayer.setMedia(media)
                vlcMediaPlayer.play()
            }
        } else {
            Log.e(TAG, "VLC播放失败，尝试切换到ExoPlayer")
            useVlc = false
            // 切换到ExoPlayer
            if (currentMediaUrl != null) {
                playWithExoPlayer(currentMediaUrl!!)
            }
        }
    }

    private fun handleExoPlayerError(error: PlaybackException) {
        exoErrorCount++
        if (exoErrorCount <= MAX_EXO_RETRIES) {
            Log.w(TAG, "ExoPlayer播放失败，重试第${exoErrorCount}次")
            // 尝试重新播放
            val currentFile = viewModel.getCurrentFile()
            if (currentFile != null) {
                playWithExoPlayer(currentFile.absolutePath)
            }
        } else {
            Log.e(TAG, "ExoPlayer播放失败，尝试切换到VLC")
            useVlc = true
            // 切换到VLC
            if (currentMediaUrl != null) {
                playWithVlc(currentMediaUrl!!)
            }
        }
    }

    private fun playWithExoPlayer(mediaUrl: String) {
        currentMediaUrl = mediaUrl
        val file = File(mediaUrl)
        if (!file.exists()) {
            Toast.makeText(this, "文件不存在: $mediaUrl", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse("file://$mediaUrl")
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(getMimeType(mediaUrl))
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()

        // 隐藏VLC，显示ExoPlayer
        surfaceView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        ivImageDisplay.visibility = View.GONE
    }

    private fun playWithVlc(mediaUrl: String) {
        currentMediaUrl = mediaUrl
        val file = File(mediaUrl)
        if (!file.exists()) {
            Toast.makeText(this, "文件不存在: $mediaUrl", Toast.LENGTH_SHORT).show()
            return
        }

        val media = Media(libVLC, Uri.parse("file://${file.absolutePath}").toString())
        vlcMediaPlayer.setMedia(media)
        vlcMediaPlayer.play()

        // 隐藏ExoPlayer，显示VLC
        surfaceView.visibility = View.VISIBLE
        playerView.visibility = View