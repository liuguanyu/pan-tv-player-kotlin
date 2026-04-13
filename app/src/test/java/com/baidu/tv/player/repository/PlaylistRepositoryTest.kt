package com.baidu.tv.player.repository

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.baidu.tv.player.database.AppDatabase
import com.baidu.tv.player.database.PlaylistDao
import com.baidu.tv.player.model.Playlist
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat

/**
 * PlaylistRepository 单元测试
 *
 * 测试播放列表的CRUD操作
 * 使用 Room 数据库进行集成测试
 * 使用 kotlinx-coroutines-test 进行协程测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PlaylistRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var playlistDao: PlaylistDao
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // 使用内存数据库进行测试
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // 允许主线程查询，简化测试
            .build()
        playlistDao = database.playlistDao()
        playlistRepository = PlaylistRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * 测试：获取所有播放列表
     */
    @Test
    fun testGetAllPlaylists() = runTest {
        // Given: 插入一些播放列表
        val playlist1 = Playlist(id = 1L, name = "Playlist 1", description = "Description 1", createdAt = System.currentTimeMillis())
        val playlist2 = Playlist(id = 2L, name = "Playlist 2", description = "Description 2", createdAt = System.currentTimeMillis())
        val playlist3 = Playlist(id = 3L, name = "Playlist 3", description = "Description 3", createdAt = System.currentTimeMillis())

        playlistDao.insert(playlist1)
        playlistDao.insert(playlist2)
        playlistDao.insert(playlist3)

        // When: 获取所有播放列表
        val playlistsFlow = playlistRepository.getAllPlaylists()
        val playlists = playlistsFlow.first()

        // Then: 验证返回正确的播放列表
        assertThat(playlists).hasSize(3)
        assertThat(playlists).contains(playlist1)
        assertThat(playlists).contains(playlist2)
        assertThat(playlists).contains(playlist3)
    }

    /**
     * 测试：获取单个播放列表
     */
    @Test
    fun testGetPlaylistById() = runTest {
        // Given: 插入一个播放列表
        val expectedPlaylist = Playlist(id = 1L, name = "My Playlist", description = "My Description", createdAt = System.currentTimeMillis())
        playlistDao.insert(expectedPlaylist)

        // When: 获取指定ID的播放列表
        val playlistFlow = playlistRepository.getPlaylistById(1L)
        val playlist = playlistFlow.first()

        // Then: 验证返回正确的播放列表
        assertThat(playlist).isEqualTo(expectedPlaylist)
    }

    /**
     * 测试：插入或更新播放列表
     */
    @Test
    fun testInsertOrUpdatePlaylist() = runTest {
        // Given: 创建一个新的播放列表
        val playlist = Playlist(name = "New Playlist", description = "New Description", createdAt = System.currentTimeMillis())

        // When: 插入播放列表
        val id = playlistRepository.insertOrUpdatePlaylist(playlist)

        // Then: 验证插入成功并返回了ID
        assertThat(id).isGreaterThan(0L)

        // 验证数据库中确实存在该播放列表
        val insertedPlaylist = playlistDao.getPlaylistById(id).first()
        assertThat(insertedPlaylist).isEqualTo(playlist.copy(id = id))
    }

    /**
     * 测试：更新播放列表
     */
    @Test
    fun testUpdatePlaylist() = runTest {
        // Given: 插入一个播放列表
        val originalPlaylist = Playlist(name = "Original Playlist", description = "Original Description", createdAt = System.currentTimeMillis())
        val id = playlistDao.insert(originalPlaylist).toLong()

        // When: 更新播放列表
        val updatedPlaylist = originalPlaylist.copy(id = id, name = "Updated Playlist", description = "Updated Description")
        playlistRepository.updatePlaylist(updatedPlaylist)

        // Then: 验证更新成功
        val retrievedPlaylist = playlistDao.getPlaylistById(id).first()
        assertThat(retrievedPlaylist.name).isEqualTo("Updated Playlist")
        assertThat(retrievedPlaylist.description).isEqualTo("Updated Description")
    }

    /**
     * 测试：删除播放列表
     */
    @Test
    fun testDeletePlaylist() = runTest {
        // Given: 插入一个播放列表
        val playlist = Playlist(name = "ToDelete Playlist", description = "To be deleted", createdAt = System.currentTimeMillis())
        val id = playlistDao.insert(playlist)

        // When: 删除播放列表
        val toDeletePlaylist = playlist.copy(id = id)
        playlistRepository.deletePlaylist(toDeletePlaylist)

        // Then: 验证删除成功
        val retrievedPlaylist = playlistDao.getPlaylistById(id).firstOrNull()
        assertThat(retrievedPlaylist).isNull()
    }

    /**
     * 测试：删除所有播放列表
     */
    @Test
    fun testDeleteAllPlaylists() = runTest {
        // Given: 插入多个播放列表
        val playlist1 = Playlist(name = "Playlist 1", description = "Description 1", createdAt = System.currentTimeMillis())
        val playlist2 = Playlist(name = "Playlist 2", description = "Description 2", createdAt = System.currentTimeMillis())
        val playlist3 = Playlist(name = "Playlist 3", description = "Description 3", createdAt = System.currentTimeMillis())

        playlistDao.insert(playlist1)
        playlistDao.insert(playlist2)
        playlistDao.insert(playlist3)

        // When: 删除所有播放列表
        playlistRepository.deleteAllPlaylists()

        // Then: 验证所有播放列表都被删除
        val allPlaylists = playlistDao.getAllPlaylists().first()
        assertThat(allPlaylists).isEmpty()
    }

    /**
     * 测试：获取不存在的播放列表
     */
    @Test
    fun testGetPlaylistById_NotFound() = runTest {
        // Given: 数据库为空

        // When: 获取不存在的播放列表
        val playlistFlow = playlistRepository.getPlaylistById(999L)
        val playlist = playlistFlow.firstOrNull()

        // Then: 验证返回null
        assertThat(playlist).isNull()
    }

    /**
     * 测试：插入重复的播放列表进行更新
     */
    @Test
    fun testInsertOrUpdatePlaylist_Existing() = runTest {
        // Given: 插入一个播放列表
        val originalPlaylist = Playlist(id = 1L, name = "Existing Playlist", description = "Original Description", createdAt = System.currentTimeMillis())
        playlistDao.insert(originalPlaylist)

        // When: 使用相同ID插入更新
        val updatedPlaylist = originalPlaylist.copy(name = "Updated Playlist", description = "Updated Description")
        val id = playlistRepository.insertOrUpdatePlaylist(updatedPlaylist)

        // Then: 验证ID保持不变，内容已更新
        assertThat(id).isEqualTo(1L)
        val retrievedPlaylist = playlistDao.getPlaylistById(id).first()
        assertThat(retrievedPlaylist.name).isEqualTo("Updated Playlist")
        assertThat(retrievedPlaylist.description).isEqualTo("Updated Description")
    }
}
