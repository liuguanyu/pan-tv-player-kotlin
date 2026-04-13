package com.baidu.tv.player.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.baidu.tv.player.database.AppDatabase
import com.baidu.tv.player.model.Playlist
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat

/**
 * PlaylistRepository 集成测试
 *
 * 测试播放列表模块在真实Android环境中的数据库操作
 * 使用AndroidX Test框架进行集成测试
 */
@RunWith(AndroidJUnit4::class)
class PlaylistRepositoryIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // 使用内存数据库进行测试
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .build()
        playlistRepository = PlaylistRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * 测试：获取所有播放列表 - 实际环境
     */
    @Test
    fun testGetAllPlaylists_RealEnvironment() {
        // Given: 插入一些播放列表
        val playlist1 = Playlist(name = "Playlist 1", description = "Description 1", createdAt = System.currentTimeMillis())
        val playlist2 = Playlist(name = "Playlist 2", description = "Description 2", createdAt = System.currentTimeMillis())
        val playlist3 = Playlist(name = "Playlist 3", description = "Description 3", createdAt = System.currentTimeMillis())

        database.playlistDao().insert(playlist1)
        database.playlistDao().insert(playlist2)
        database.playlistDao().insert(playlist3)

        // When: 获取所有播放列表
        val playlists = playlistRepository.getAllPlaylists().first()

        // Then: 验证返回正确的播放列表
        assertThat(playlists).hasSize(3)
        assertThat(playlists).contains(playlist1)
        assertThat(playlists).contains(playlist2)
        assertThat(playlists).contains(playlist3)
    }

    /**
     * 测试：获取单个播放列表 - 实际环境
     */
    @Test
    fun testGetPlaylistById_RealEnvironment() {
        // Given: 插入一个播放列表
        val expectedPlaylist = Playlist(name = "My Playlist", description = "My Description", createdAt = System.currentTimeMillis())
        val id = database.playlistDao().insert(expectedPlaylist)

        // When: 获取指定ID的播放列表
        val playlist = playlistRepository.getPlaylistById(id).first()

        // Then: 验证返回正确的播放列表
        assertThat(playlist).isEqualTo(expectedPlaylist)
    }

    /**
     * 测试：插入或更新播放列表 - 实际环境
     */
    @Test
    fun testInsertOrUpdatePlaylist_RealEnvironment() {
        // Given: 创建一个新的播放列表
        val playlist = Playlist(name = "New Playlist", description = "New Description", createdAt = System.currentTimeMillis())

        // When: 插入播放列表
        val id = playlistRepository.insertOrUpdatePlaylist(playlist)

        // Then: 验证插入成功并返回了ID
        assertThat(id).isGreaterThan(0L)

        // 验证数据库中确实存在该播放列表
        val insertedPlaylist = database.playlistDao().getPlaylistById(id).first()
        assertThat(insertedPlaylist.name).isEqualTo(playlist.name)
        assertThat(insertedPlaylist.description).isEqualTo(playlist.description)
    }

    /**
     * 测试：更新播放列表 - 实际环境
     */
    @Test
    fun testUpdatePlaylist_RealEnvironment() {
        // Given: 插入一个播放列表
        val originalPlaylist = Playlist(name = "Original Playlist", description = "Original Description", createdAt = System.currentTimeMillis())
        val id = database.playlistDao().insert(originalPlaylist)

        // When: 更新播放列表
        val updatedPlaylist = originalPlaylist.copy(name = "Updated Playlist", description = "Updated Description")
        playlistRepository.updatePlaylist(updatedPlaylist)

        // Then: 验证更新成功
        val retrievedPlaylist = database.playlistDao().getPlaylistById(id).first()
        assertThat(retrievedPlaylist.name).isEqualTo("Updated Playlist")
        assertThat(retrievedPlaylist.description).isEqualTo("Updated Description")
    }

    /**
     * 测试：删除播放列表 - 实际环境
     */
    @Test
    fun testDeletePlaylist_RealEnvironment() {
        // Given: 插入一个播放列表
        val playlist = Playlist(name = "ToDelete Playlist", description = "To be deleted", createdAt = System.currentTimeMillis())
        val id = database.playlistDao().insert(playlist)

        // When: 删除播放列表
        playlistRepository.deletePlaylist(playlist.copy(id = id))

        // Then: 验证删除成功
        val retrievedPlaylist = database.playlistDao().getPlaylistById(id).firstOrNull()
        assertThat(retrievedPlaylist).isNull()
    }

    /**
     * 测试：删除所有播放列表 - 实际环境
     */
    @Test
    fun testDeleteAllPlaylists_RealEnvironment() {
        // Given: 插入多个播放列表
        val playlist1 = Playlist(name = "Playlist 1", description = "Description 1", createdAt = System.currentTimeMillis())
        val playlist2 = Playlist(name = "Playlist 2", description = "Description 2", createdAt = System.currentTimeMillis())
        val playlist3 = Playlist(name = "Playlist 3", description = "Description 3", createdAt = System.currentTimeMillis())

        database.playlistDao().insert(playlist1)
        database.playlistDao().insert(playlist2)
        database.playlistDao().insert(playlist3)

        // When: 删除所有播放列表
        playlistRepository.deleteAllPlaylists()

        // Then: 验证所有播放列表都被删除
        val allPlaylists = database.playlistDao().getAllPlaylists().first()
        assertThat(allPlaylists).isEmpty()
    }
}
