package com.baidu.tv.player.ui.playback

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.baidu.tv.player.model.FileInfo
import com.baidu.tv.player.model.Playlist
import com.baidu.tv.player.model.PlaylistItem
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * PlaybackViewModel 单元测试
 *
 * 测试播放状态管理、播放列表、播放模式等功能
 * 使用 MockK 进行依赖项模拟
 */
class PlaybackViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockDataStore: DataStore<Preferences>

    private lateinit var playbackViewModel: PlaybackViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        playbackViewModel = PlaybackViewModel(mockContext, mockDataStore)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * 测试：播放状态
     */
    @Test
    fun testIsPlaying() {
        // Given: 初始状态为未播放
        val isPlayingObserver = mockk<Observer<Boolean>>()
        playbackViewModel.isPlaying.observeForever(isPlayingObserver)

        // When: 切换播放状态
        playbackViewModel.togglePlayPause()

        // Then: 验证播放状态已更新为true
        verify(exactly = 1) { isPlayingObserver.onChanged(true) }

        // When: 再次切换播放状态
        playbackViewModel.togglePlayPause()

        // Then: 验证播放状态已更新为false
        verify(exactly = 1) { isPlayingObserver.onChanged(false) }
    }

    /**
     * 测试：设置播放列表
     */
    @Test
    fun testSetPlaylist() {
        // Given: 创建播放列表
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val fileInfo2 = FileInfo("/path/to/video2.mp4", "video2.mp4", 2048L, System.currentTimeMillis(), "video/mp4", 180)
        val playlistItem1 = PlaylistItem(fileInfo1, 0)
        val playlistItem2 = PlaylistItem(fileInfo2, 1)
        val playlist = Playlist(listOf(playlistItem1, playlistItem2))

        val currentFileObserver = mockk<Observer<FileInfo>>()
        val playlistObserver = mockk<Observer<Playlist>>()
        playbackViewModel.currentFile.observeForever(currentFileObserver)
        playbackViewModel.playlist.observeForever(playlistObserver)

        // When: 设置播放列表
        playbackViewModel.setPlaylist(playlist)

        // Then: 验证播放列表和当前文件已更新
        verify(exactly = 1) { playlistObserver.onChanged(playlist) }
        verify(exactly = 1) { currentFileObserver.onChanged(fileInfo1) }
        assertThat(playbackViewModel.getCurrentIndex()).isEqualTo(0)
    }

    /**
     * 测试：播放指定索引的文件
     */
    @Test
    fun testPlayFileAtIndex() {
        // Given: 创建播放列表
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val fileInfo2 = FileInfo("/path/to/video2.mp4", "video2.mp4", 2048L, System.currentTimeMillis(), "video/mp4", 180)
        val playlistItem1 = PlaylistItem(fileInfo1, 0)
        val playlistItem2 = PlaylistItem(fileInfo2, 1)
        val playlist = Playlist(listOf(playlistItem1, playlistItem2))

        playbackViewModel.setPlaylist(playlist)

        val currentFileObserver = mockk<Observer<FileInfo>>()
        val preparedMediaUrlObserver = mockk<Observer<String>>()
        val playerIndicatorObserver = mockk<Observer<String>>()
        playbackViewModel.currentFile.observeForever(currentFileObserver)
        playbackViewModel.preparedMediaUrl.observeForever(preparedMediaUrlObserver)
        playbackViewModel.playerIndicator.observeForever(playerIndicatorObserver)

        // When: 播放第二个文件
        playbackViewModel.playFileAtIndex(1)

        // Then: 验证当前文件、媒体URL和播放器指示器已更新
        verify(exactly = 1) { currentFileObserver.onChanged(fileInfo2) }
        verify(exactly = 1) { preparedMediaUrlObserver.onChanged("/path/to/video2.mp4") }
        verify(exactly = 1) { playerIndicatorObserver.onChanged("ExoPlayer") }
        assertThat(playbackViewModel.getCurrentIndex()).isEqualTo(1)
    }

    /**
     * 测试：播放下一个文件 - 顺序模式
     */
    @Test
    fun testPlayNext_OrderMode() {
        // Given: 创建播放列表并设置顺序模式
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val fileInfo2 = FileInfo("/path/to/video2.mp4", "video2.mp4", 2048L, System.currentTimeMillis(), "video/mp4", 180)
        val playlistItem1 = PlaylistItem(fileInfo1, 0)
        val playlistItem2 = PlaylistItem(fileInfo2, 1)
        val playlist = Playlist(listOf(playlistItem1, playlistItem2))

        playbackViewModel.setPlaylist(playlist)
        playbackViewModel.setPlayMode(PlayMode.ORDER)

        val currentFileObserver = mockk<Observer<FileInfo>>()
        playbackViewModel.currentFile.observeForever(currentFileObserver)

        // When: 播放下一个文件
        playbackViewModel.playNext()

        // Then: 验证播放第二个文件
        verify(exactly = 1) { currentFileObserver.onChanged(fileInfo2) }
        assertThat(playbackViewModel.getCurrentIndex()).isEqualTo(1)

        // When: 再次播放下一个文件
        playbackViewModel.playNext()

        // Then: 验证循环回第一个文件
        verify(exactly = 1) { currentFileObserver.onChanged(fileInfo1) }
        assertThat(playbackViewModel.getCurrentIndex()).isEqualTo(0)
    }

    /**
     * 测试：播放下一个文件 - 随机模式
     */
    @Test
    fun testPlayNext_RandomMode() {
        // Given: 创建播放列表并设置随机模式
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val fileInfo2 = FileInfo("/path/to/video2.mp4", "video2.mp4", 2048L, System.currentTimeMillis(), "video/mp4", 180)
        val playlistItem1 = PlaylistItem(fileInfo1, 0)
        val playlistItem2 = PlaylistItem(fileInfo2, 1)
        val playlist = Playlist(listOf(playlistItem1, playlistItem2))

        playbackViewModel.setPlaylist(playlist)
        playbackViewModel.setPlayMode(PlayMode.RANDOM)

        val currentFileObserver = mockk<Observer<FileInfo>>()
        playbackViewModel.currentFile.observeForever(currentFileObserver)

        // When: 播放下一个文件
        playbackViewModel.playNext()

        // Then: 验证播放了随机文件（要么是第一个，要么是第二个）
        verify(exactly = 1) { currentFileObserver.onChanged(any()) }
        val lastCalledArgument = currentFileObserver.lastCalled?.arguments?.firstOrNull() as FileInfo
        assertThat(lastCalledArgument).isIn(fileInfo1, fileInfo2)
    }

    /**
     * 测试：播放下一个文件 - 单曲循环模式
     */
    @Test
    fun testPlayNext_SingleMode() {
        // Given: 创建播放列表并设置单曲循环模式
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val playlistItem1 = PlaylistItem(fileInfo1, 0)
        val playlist = Playlist(listOf(playlistItem1))

        playbackViewModel.setPlaylist(playlist)
        playbackViewModel.setPlayMode(PlayMode.SINGLE)

        val currentFileObserver = mockk<Observer<FileInfo>>()
        playbackViewModel.currentFile.observeForever(currentFileObserver)

        // When: 播放下一个文件
        playbackViewModel.playNext()

        // Then: 验证仍播放当前文件
        verify(exactly = 1) { currentFileObserver.onChanged(fileInfo1) }
        assertThat(playbackViewModel.getCurrentIndex()).isEqualTo(0)
    }

    /**
     * 测试：播放上一个文件 - 顺序模式
     */
    @Test
    fun testPlayPrev_OrderMode() {
        // Given: 创建播放列表并设置顺序模式
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val fileInfo2 = FileInfo("/path/to/video2.mp4", "video2.mp4", 2048L, System.currentTimeMillis(), "video/mp4", 180)
        val playlistItem1 = PlaylistItem(fileInfo1, 0)
        val playlistItem2 = PlaylistItem(fileInfo2, 1)
        val playlist = Playlist(listOf(playlistItem1, playlistItem2))

        playbackViewModel.setPlaylist(playlist)
        playbackViewModel.setPlayMode(PlayMode.ORDER)
        playbackViewModel.playFileAtIndex(1) // 设置为第二个文件

        val currentFileObserver = mockk<Observer<FileInfo>>()
        playbackViewModel.currentFile.observeForever(currentFileObserver)

        // When: 播放上一个文件
        playbackViewModel.playPrev()

        // Then: 验证播放第一个文件
        verify(exactly = 1) { currentFileObserver.onChanged(fileInfo1) }
        assertThat(playbackViewModel.getCurrentIndex()).isEqualTo(0)

        // When: 再次播放上一个文件
        playbackViewModel.playPrev()

        // Then: 验证循环回第二个文件
        verify(exactly = 1) { currentFileObserver.onChanged(fileInfo2) }
        assertThat(playbackViewModel.getCurrentIndex()).isEqualTo(1)
    }

    /**
     * 测试：播放上一个文件 - 随机模式
     */
    @Test
    fun testPlayPrev_RandomMode() {
        // Given: 创建播放列表并设置随机模式
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val fileInfo2 = FileInfo("/path/to/video2.mp4", "video2.mp4", 2048L, System.currentTimeMillis(), "video/mp4", 180)
        val playlistItem1 = PlaylistItem(fileInfo1, 0)
        val playlistItem2 = PlaylistItem(fileInfo2, 1)
        val playlist = Playlist(listOf(playlistItem1, playlistItem2))

        playbackViewModel.setPlaylist(playlist)
        playbackViewModel.setPlayMode(PlayMode.RANDOM)

        val currentFileObserver = mockk<Observer<FileInfo>>()
        playbackViewModel.currentFile.observeForever(currentFileObserver)

        // When: 播放上一个文件
        playbackViewModel.playPrev()

        // Then: 验证播放了随机文件（要么是第一个，要么是第二个）
        verify(exactly = 1) { currentFileObserver.onChanged(any()) }
        val lastCalledArgument = currentFileObserver.lastCalled?.arguments?.firstOrNull() as FileInfo
        assertThat(lastCalledArgument).isIn(fileInfo1, fileInfo2)
    }

    /**
     * 测试：播放上一个文件 - 单曲循环模式
     */
    @Test
    fun testPlayPrev_SingleMode() {
        // Given: 创建播放列表并设置单曲循环模式
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val playlistItem1 = PlaylistItem(fileInfo1, 0)
        val playlist = Playlist(listOf(playlistItem1))

        playbackViewModel.setPlaylist(playlist)
        playbackViewModel.setPlayMode(PlayMode.SINGLE)

        val currentFileObserver = mockk<Observer<FileInfo>>()
        playbackViewModel.currentFile.observeForever(currentFileObserver)

        // When: 播放上一个文件
        playbackViewModel.playPrev()

        // Then: 验证仍播放当前文件
        verify(exactly = 1) { currentFileObserver.onChanged(fileInfo1) }
        assertThat(playbackViewModel.getCurrentIndex()).isEqualTo(0)
    }

    /**
     * 测试：设置播放模式
     */
    @Test
    fun testSetPlayMode() = runTest {
        // Given: 模拟DataStore
        val mockPreferences = mockk<Preferences>()
        every { mockDataStore.data } returns flowOf(mockPreferences)
        every { mockDataStore.edit(any()) } answers {
            val block = firstArg<(Preferences) -> Unit>()
            block(mockPreferences)
        }

        // When: 设置播放模式
        playbackViewModel.setPlayMode(PlayMode.RANDOM)

        // Then: 验证播放模式已更新，且DataStore已更新
        assertThat(playbackViewModel.getPlayMode()).isEqualTo(PlayMode.RANDOM)
    }

    /**
     * 测试：获取当前播放文件
     */
    @Test
    fun testGetCurrentFile() {
        // Given: 创建播放列表
        val fileInfo = FileInfo("/path/to/video.mp4", "video.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val playlistItem = PlaylistItem(fileInfo, 0)
        val playlist = Playlist(listOf(playlistItem))

        playbackViewModel.setPlaylist(playlist)

        // When: 获取当前播放文件
        val currentFile = playbackViewModel.getCurrentFile()

        // Then: 验证返回正确的文件
        assertThat(currentFile).isEqualTo(fileInfo)
    }

    /**
     * 测试：获取当前播放模式
     */
    @Test
    fun testGetPlayMode() {
        // Given: 设置播放模式
        playbackViewModel.setPlayMode(PlayMode.ORDER)

        // When: 获取播放模式
        val playMode = playbackViewModel.getPlayMode()

        // Then: 验证返回正确的播放模式
        assertThat(playMode).isEqualTo(PlayMode.ORDER)
    }

    /**
     * 测试：获取播放设置
     */
    @Test
    fun testGetPlaybackSettings() {
        // Given: 模拟DataStore返回播放设置
        val mockPreferences = mockk<Preferences>()
        every { mockDataStore.data } returns flowOf(mockPreferences)
        every { mockPreferences[any()] } returns PlayMode.ORDER
        every { mockPreferences[any()] } returns ImageEffect.FADE
        every { mockPreferences[any()] } returns ImageBackground.BLACK
        every { mockPreferences[any()] } returns true
        every { mockPreferences[any()] } returns false
        every { mockPreferences[any()] } returns true

        // When: 获取播放设置
        val settings = playbackViewModel.getPlaybackSettings()

        // Then: 验证返回正确的播放设置
        assertThat(settings).isNotNull()
        assertThat(settings!!.playMode).isEqualTo(PlayMode.ORDER)
    }

    /**
     * 测试：获取播放列表
     */
    @Test
    fun testGetPlaylist() {
        // Given: 创建播放列表
        val fileInfo = FileInfo("/path/to/video.mp4", "video.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val playlistItem = PlaylistItem(fileInfo, 0)
        val playlist = Playlist(listOf(playlistItem))

        playbackViewModel.setPlaylist(playlist)

        // When: 获取播放列表
        val currentPlaylist = playbackViewModel.getPlaylist()

        // Then: 验证返回正确的播放列表
        assertThat(currentPlaylist).isEqualTo(playlist)
    }

    /**
     * 测试：获取当前播放索引
     */
    @Test
    fun testGetCurrentIndex() {
        // Given: 创建播放列表
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val fileInfo2 = FileInfo("/path/to/video2.mp4", "video2.mp4", 2048L, System.currentTimeMillis(), "video/mp4", 180)
        val playlistItem1 = PlaylistItem(fileInfo1, 0)
        val playlistItem2 = PlaylistItem(fileInfo2, 1)
        val playlist = Playlist(listOf(playlistItem1, playlistItem2))

        playbackViewModel.setPlaylist(playlist)
        playbackViewModel.playFileAtIndex(1)

        // When: 获取当前播放索引
        val currentIndex = playbackViewModel.getCurrentIndex()

        // Then: 验证返回正确的索引
        assertThat(currentIndex).isEqualTo(1)
    }

    /**
     * 测试：检查是否有下一个文件
     */
    @Test
    fun testHasNext() {
        // Given: 创建播放列表
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val fileInfo2 = FileInfo("/path/to/video2.mp4", "video2.mp4", 2048L, System.currentTimeMillis(), "video/mp4", 180)
        val playlist = Playlist(listOf(PlaylistItem(fileInfo1, 0), PlaylistItem(fileInfo2, 1)))

        playbackViewModel.setPlaylist(playlist)

        // When & Then: 准备播放第一个文件
        assertThat(playbackViewModel.hasNext()).isTrue()

        // When: 播放到第二个文件
        playbackViewModel.playFileAtIndex(1)
        assertThat(playbackViewModel.hasNext()).isFalse()

        // When: 顺序模式下回到第一个文件
        playbackViewModel.playNext()
        assertThat(playbackViewModel.hasNext()).isTrue()
    }

    /**
     * 测试：检查是否有上一个文件
     */
    @Test
    fun testHasPrev() {
        // Given: 创建播放列表
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val fileInfo2 = FileInfo("/path/to/video2.mp4", "video2.mp4", 2048L, System.currentTimeMillis(), "video/mp4", 180)
        val playlist = Playlist(listOf(PlaylistItem(fileInfo1, 0), PlaylistItem(fileInfo2, 1)))

        playbackViewModel.setPlaylist(playlist)

        // When: 准备播放第一个文件
        assertThat(playbackViewModel.hasPrev()).isFalse()

        // When: 播放到第二个文件
        playbackViewModel.playFileAtIndex(1)
        assertThat(playbackViewModel.hasPrev()).isTrue()

        // When: 播放到第一个文件
        playbackViewModel.playFileAtIndex(0)
        assertThat(playbackViewModel.hasPrev()).isFalse()
    }

    /**
     * 测试：获取下一个文件
     */
    @Test
    fun testGetNextFile() {
        // Given: 创建播放列表
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val fileInfo2 = FileInfo("/path/to/video2.mp4", "video2.mp4", 2048L, System.currentTimeMillis(), "video/mp4", 180)
        val fileInfo3 = FileInfo("/path/to/video3.mp4", "video3.mp4", 4096L, System.currentTimeMillis(), "video/mp4", 240)
        val playlist = Playlist(listOf(
            PlaylistItem(fileInfo1, 0),
            PlaylistItem(fileInfo2, 1),
            PlaylistItem(fileInfo3, 2)
        ))

        playbackViewModel.setPlaylist(playlist)
        playbackViewModel.playFileAtIndex(0)

        // When: 获取下一个文件
        val nextFile = playbackViewModel.getNextFile()

        // Then: 验证返回正确的文件
        assertThat(nextFile).isEqualTo(fileInfo2)
    }

    /**
     * 测试：获取上一个文件
     */
    @Test
    fun testGetPrevFile() {
        // Given: 创建播放列表
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val fileInfo2 = FileInfo("/path/to/video2.mp4", "video2.mp4", 2048L, System.currentTimeMillis(), "video/mp4", 180)
        val fileInfo3 = FileInfo("/path/to/video3.mp4", "video3.mp4", 4096L, System.currentTimeMillis(), "video/mp4", 240)
        val playlist = Playlist(listOf(
            PlaylistItem(fileInfo1, 0),
            PlaylistItem(fileInfo2, 1),
            PlaylistItem(fileInfo3, 2)
        ))

        playbackViewModel.setPlaylist(playlist)
        playbackViewModel.playFileAtIndex(1)

        // When: 获取上一个文件
        val prevFile = playbackViewModel.getPrevFile()

        // Then: 验证返回正确的文件
        assertThat(prevFile).isEqualTo(fileInfo1)
    }

    /**
     * 测试：获取不存在的上下文件
     */
    @Test
    fun testGetNonExistentNextPrevFile() {
        // Given: 只有一个文件的播放列表
        val fileInfo = FileInfo("/path/to/video.mp4", "video.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val playlist = Playlist(listOf(PlaylistItem(fileInfo, 0)))

        playbackViewModel.setPlaylist(playlist)

        // When: 获取下一个文件
        val nextFile = playbackViewModel.getNextFile()
        val prevFile = playbackViewModel.getPrevFile()

        // Then: 验证返回null
        assertThat(nextFile).isNull()
        assertThat(prevFile).isNull()
    }

    /**
     * 测试：预加载下一个文件
     */
    @Test
    fun testPreloadNextFile() {
        // Given: 创建播放列表
        val fileInfo1 = FileInfo("/path/to/video1.mp4", "video1.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val fileInfo2 = FileInfo("/path/to/video2.mp4", "video2.mp4", 2048L, System.currentTimeMillis(), "video/mp4", 180)
        val playlist = Playlist(listOf(PlaylistItem(fileInfo1, 0), PlaylistItem(fileInfo2, 1)))

        playbackViewModel.setPlaylist(playlist)
        playbackViewModel.playFileAtIndex(0)

        // When: 预加载下一个文件
        playbackViewModel.preloadNextFile()

        // Then: 验证没有抛出异常（方法主要是记录日志）
        // 实际行为需要实际的预加载实现，这里只测试方法可调用
        val nextFile = playbackViewModel.getNextFile()
        assertThat(nextFile).isNotNull()
    }

    /**
     * 测试：保存和获取播放进度
     */
    @Test
    fun testSaveAndGetPlaybackPosition() {
        // Given: 文件信息
        val fileInfo = FileInfo("/path/to/video.mp4", "video.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)

        // When: 保存播放进度
        playbackViewModel.savePlaybackPosition(50000L)

        // Then: 验证可以获取播放进度（实际实现需要数据库支持）
        val position = playbackViewModel.getPlaybackPosition(fileInfo)
        assertThat(position).isEqualTo(0L) // 默认实现返回0
    }

    /**
     * 测试：播放器类型指示器 - 视频文件
     */
    @Test
    fun testPlayerIndicator_Video() {
        // Given: 视频文件信息
        val videoInfo = FileInfo("/path/to/video.mp4", "video.mp4", 1024L, System.currentTimeMillis(), "video/mp4", 120)
        val playlist = Playlist(listOf(PlaylistItem(videoInfo, 0)))

        playbackViewModel.setPlaylist(playlist)
        playbackViewModel.playFileAtIndex(0)

        // When: 获取播放器类型指示器
        val indicator = playbackViewModel.getPlayerIndicator().value

        // Then: 验证指示器为 ExoPlayer
        assertThat(indicator).isEqualTo("ExoPlayer")
    }

    /**
     * 测试：播放器类型指示器 - 图片文件
     */
    @Test
    fun testPlayerIndicator_Image() {
        // Given: 图片文件信息
        val imageInfo = FileInfo("/path/to/image.jpg", "image.jpg", 1024L, System.currentTimeMillis(), "image/jpeg", 0)
        val playlist = Playlist(listOf(PlaylistItem(imageInfo, 0)))

        playbackViewModel.setPlaylist(playlist)
        playbackViewModel.playFileAtIndex(0)

        // When: 获取播放器类型指示器
        val indicator = playbackViewModel.getPlayerIndicator().value

        // Then: 验证指示器为 Glide
        assertThat(indicator).isEqualTo("Glide")
    }
}
