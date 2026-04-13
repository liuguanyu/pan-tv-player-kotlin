package com.baidu.tv.player.ui.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 播放器视图模型
 * 管理播放状态、播放列表、播放设置和播放历史
 * 使用DataStore保存播放设置，使用Kotlin协程处理异步操作
 */
class PlaybackViewModel(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val TAG = "PlaybackViewModel"

    // 播放状态
    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    // 当前播放的文件
    private val _currentFile = MutableLiveData<FileInfo>()
    val currentFile: LiveData<FileInfo> = _currentFile

    // 当前准备的媒体URL
    private val _preparedMediaUrl = MutableLiveData<String>()
    val preparedMediaUrl: LiveData<String> = _preparedMediaUrl

    // 播放模式
    private val _playMode = MutableLiveData<PlayMode>()
    val playMode: LiveData<PlayMode> = _playMode

    // 播放列表
    private val _playlist = MutableLiveData<Playlist>()
    val playlist: LiveData<Playlist> = _playlist

    // 播放历史
    private val _playbackHistory = MutableLiveData<PlaybackHistory>()
    val playbackHistory: LiveData<PlaybackHistory> = _playbackHistory

    // 播放设置
    private val _playbackSettings = MutableLiveData<PlaybackSettings>()
    val playbackSettings: LiveData<PlaybackSettings> = _playbackSettings

    // 播放器类型指示器
    private val _playerIndicator = MutableLiveData<String>()
    val playerIndicator: LiveData<String> = _playerIndicator

    // 当前播放索引
    private var currentIndex = 0

    // 播放状态标志
    private var isPlayingState = false
    private var isPlaylistReady = false
    private val isPreparing = AtomicBoolean(false)

    // 初始化播放设置
    init {
        loadSettings()
        loadPlaybackHistory()
    }

    /**
     * 加载播放设置
     * 从DataStore读取用户偏好设置
     */
    private fun loadSettings() {
        viewModelScope.launch {
            dataStore.data
                .catch { exception ->
                    Log.e(TAG, "Error reading dataStore", exception)
                }
                .map { preferences ->
                    PlaybackSettings(
                        playMode = preferences[PreferencesKeys.PLAY_MODE] ?: PlayMode.ORDER,
                        imageEffect = preferences[PreferencesKeys.IMAGE_EFFECT] ?: ImageEffect.FADE,
                        imageBackground = preferences[PreferencesKeys.IMAGE_BACKGROUND] ?: ImageBackground.BLACK,
                        autoPlayNext = preferences[PreferencesKeys.AUTO_PLAY_NEXT] ?: true,
                        enableBackgroundBlur = preferences[PreferencesKeys.ENABLE_BACKGROUND_BLUR] ?: false,
                        autoRotateScreen = preferences[PreferencesKeys.AUTO_ROTATE_SCREEN] ?: true
                    )
                }
                .collect { settings ->
                    _playbackSettings.value = settings
                    _playMode.value = settings.playMode
                }
        }
    }

    /**
     * 加载播放历史
     * 从本地数据库加载最近播放的文件
     */
    private fun loadPlaybackHistory() {
        // 在实际实现中，这里会从数据库加载播放历史
        // 为了简化，我们初始化一个空的历史记录
        _playbackHistory.value = PlaybackHistory(listOf())
    }

    /**
     * 重新加载设置
     * 当从设置页面返回时调用
     */
    fun reloadSettings() {
        loadSettings()
    }

    /**
     * 设置播放列表
     * @param playlist 播放列表
     * @param startIndex 开始播放的索引
     */
    fun setPlaylist(playlist: Playlist, startIndex: Int = 0) {
        _playlist.value = playlist
        currentIndex = startIndex
        isPlaylistReady = true
        playFileAtIndex(currentIndex)
    }

    /**
     * 播放指定索引的文件
     * @param index 文件索引
     */
    fun playFileAtIndex(index: Int) {
        if (!isPlaylistReady || playlist.value?.items.isNullOrEmpty() || index < 0 || index >= playlist.value!!.items.size) {
            Log.w(TAG, "Invalid index for playback: $index")
            return
        }

        currentIndex = index
        val item = playlist.value!!.items[index]
        _currentFile.value = item.fileInfo
        _preparedMediaUrl.value = item.fileInfo.absolutePath
        _playerIndicator.value = ""

        // 保存播放历史
        savePlaybackHistory(item.fileInfo)

        // 检查文件类型
        if (item.fileInfo.isVideo()) {
            // 视频播放
            _playerIndicator.value = "ExoPlayer"
        } else if (item.fileInfo.isImage()) {
            // 图片播放
            _playerIndicator.value = "Glide"
        }
    }

    /**
     * 播放下一个文件
     */
    fun playNext() {
        val playlistSize = playlist.value?.items?.size ?: 0
        if (playlistSize <= 1) return

        val nextIndex = when (_playMode.value) {
            PlayMode.ORDER -> {
                if (currentIndex < playlistSize - 1) currentIndex + 1 else 0
            }
            PlayMode.RANDOM -> {
                (0 until playlistSize).random()
            }
            PlayMode.SINGLE -> {
                currentIndex
            }
        }

        playFileAtIndex(nextIndex)
    }

    /**
     * 播放上一个文件
     */
    fun playPrev() {
        val playlistSize = playlist.value?.items?.size ?: 0
        if (playlistSize <= 1) return

        val prevIndex = when (_playMode.value) {
            PlayMode.ORDER -> {
                if (currentIndex > 0) currentIndex - 1 else playlistSize - 1
            }
            PlayMode.RANDOM -> {
                (0 until playlistSize).random()
            }
            PlayMode.SINGLE -> {
                currentIndex
            }
        }

        playFileAtIndex(prevIndex)
    }

    /**
     * 播放/暂停
     */
    fun togglePlayPause() {
        isPlayingState = !isPlayingState
        _isPlaying.value = isPlayingState
    }

    /**
     * 设置播放模式
     */
    fun setPlayMode(playMode: PlayMode) {
        _playMode.value = playMode
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.PLAY_MODE] = playMode
            }
        }
    }

    /**
     * 保存播放历史
     */
    private fun savePlaybackHistory(fileInfo: FileInfo) {
        // 在实际实现中，这里会保存到数据库
        // 为了简化，我们只记录日志
        Log.d(TAG, "Saved playback history: ${fileInfo.absolutePath}")
    }

    /**
     * 获取当前播放的文件信息
     */
    fun getCurrentFile(): FileInfo? {
        return _currentFile.value
    }

    /**
     * 获取当前准备的媒体URL
     */
    fun getPreparedMediaUrl(): LiveData<String> {
        return _preparedMediaUrl
    }

    /**
     * 获取当前播放模式
     */
    fun getPlayMode(): PlayMode? {
        return _playMode.value
    }

    /**
     * 获取播放设置
     */
    fun getPlaybackSettings(): PlaybackSettings? {
        return _playbackSettings.value
    }

    /**
     * 获取播放列表
     */
    fun getPlaylist(): Playlist? {
        return _playlist.value
    }

    /**
     * 获取当前播放索引
     */
    fun getCurrentIndex(): Int {
        return currentIndex
    }

    /**
     * 是否正在播放
     */
    fun getIsPlaying(): LiveData<Boolean> {
        return _isPlaying
    }

    /**
     * 播放器类型指示器
     */
    fun getPlayerIndicator(): LiveData<String> {
        return _playerIndicator
    }

    /**
     * 检查是否还有下一个文件
     */
    fun hasNext(): Boolean {
        val playlistSize = playlist.value?.items?.size ?: 0
        return currentIndex < playlistSize - 1
    }

    /**
     * 检查是否还有上一个文件
     */
    fun hasPrev(): Boolean {
        return currentIndex > 0
    }

    /**
     * 获取下一个文件信息
     */
    fun getNextFile(): FileInfo? {
        if (!hasNext()) return null
        return playlist.value?.items?.get(currentIndex + 1)?.fileInfo
    }

    /**
     * 获取上一个文件信息
     */
    fun getPrevFile(): FileInfo? {
        if (!hasPrev()) return null
        return playlist.value?.items?.get(currentIndex - 1)?.fileInfo
    }

    /**
     * 预加载下一个文件
     * 在播放当前文件时，提前加载下一个文件的元数据
     */
    fun preloadNextFile() {
        val nextFile = getNextFile()
        if (nextFile != null) {
            // 预加载视频元数据（在实际实现中会加载分辨率、编码等信息）
            Log.d(TAG, "Preloading next file: ${nextFile.absolutePath}")
            // 在实际实现中，这里会使用MediaMetadataRetriever或ExoPlayer预加载
        }
    }

    /**
     * 保存播放进度
     * @param position 当前播放位置（毫秒）
     */
    fun savePlaybackPosition(position: Long) {
        // 在实际实现中，这里会保存到数据库
        Log.d(TAG, "Saved playback position: $position")
    }

    /**
     * 获取播放进度
     * @param fileInfo 文件信息
     */
    fun getPlaybackPosition(fileInfo: FileInfo): Long {
        // 在实际实现中，这里会从数据库读取
        return 0L
    }

    /**
     * 退出播放器
     */
    override fun onCleared() {
        super.onCleared()
        // 清理资源
        Log.d(TAG, "PlaybackViewModel cleared")
    }
}

/**
 * 播放文件信息
 */
data class FileInfo(
    val absolutePath: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val mimeType: String,
    val duration: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val hasGps: Boolean = false,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null
) {
    fun isVideo(): Boolean {
        return mimeType.startsWith("video/")
    }

    fun isImage(): Boolean {
        return mimeType.startsWith("image/")
    }
}

/**
 * 播放列表项
 */
data class PlaylistItem(
    val fileInfo: FileInfo,
    val position: Int = 0
)

/**
 * 播放列表
 */
data class Playlist(
    val items: List<PlaylistItem> = emptyList()
)

/**
 * 播放历史
 */
data class PlaybackHistory(
    val history: List<FileInfo>
)

/**
 * 播放设置
 */
data class PlaybackSettings(
    val playMode: PlayMode = PlayMode.ORDER,
    val imageEffect: ImageEffect = ImageEffect.FADE,
    val imageBackground: ImageBackground = ImageBackground.BLACK,
    val autoPlayNext: Boolean = true,
    val enableBackgroundBlur: Boolean = false,
    val autoRotateScreen: Boolean = true
)

/**
 * 播放模式
 */
enum class PlayMode {
    ORDER, RANDOM, SINGLE
}

/**
 * 图片切换特效
 */
enum class ImageEffect {
    FADE, EASE, POP, BOUNCE, SLIDE, ROTATE
}

/**
 * 图片背景
 */
enum class ImageBackground {
    BLACK, MAIN_COLOR, BLUR
}

/**
 * 数据存储键
 */
object PreferencesKeys {
    val PLAY_MODE = stringPreferencesKey("play_mode")
    val IMAGE_EFFECT = stringPreferencesKey("image_effect")
    val IMAGE_BACKGROUND = stringPreferencesKey("image_background")
    val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
    val ENABLE_BACKGROUND_BLUR = booleanPreferencesKey("enable_background_blur")
    val AUTO_ROTATE_SCREEN = booleanPreferencesKey("auto_rotate_screen")
}

/**
 * 媒体文件类型枚举
 */
enum class MediaType {
    VIDEO, IMAGE
}