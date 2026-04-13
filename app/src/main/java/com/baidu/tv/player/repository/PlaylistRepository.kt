package com.baidu.tv.player.repository

import com.baidu.tv.player.database.AppDatabase
import com.baidu.tv.player.database.PlaylistDao
import com.baidu.tv.player.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 播放列表仓库
 *
 * 作为数据访问层的抽象，封装了对播放列表数据库的操作。
 * 提供了业务逻辑层与数据持久化层之间的接口。
 */
class PlaylistRepository(private val database: AppDatabase) {

    private val playlistDao: PlaylistDao = database.playlistDao()

    /**
     * 获取所有播放列表
     *
     * @return Flow<List<Playlist>> 流式返回所有播放列表
     */
    fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists()
    }

    /**
     * 根据ID获取播放列表
     *
     * @param id 播放列表ID
     * @return Flow<Playlist> 流式返回指定ID的播放列表
     */
    fun getPlaylistById(id: Long): Flow<Playlist> {
        return playlistDao.getPlaylistById(id)
    }

    /**
     * 插入或更新播放列表
     *
     * @param playlist 要插入或更新的播放列表
     * @return 插入或更新后记录的ID
     */
    suspend fun insertOrUpdatePlaylist(playlist: Playlist): Long {
        return playlistDao.insert(playlist)
    }

    /**
     * 更新播放列表
     *
     * @param playlist 要更新的播放列表
     */
    suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.update(playlist)
    }

    /**
     * 删除播放列表
     *
     * @param playlist 要删除的播放列表
     */
    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.delete(playlist)
    }

    /**
     * 删除所有播放列表
     */
    suspend fun deleteAllPlaylists() {
        playlistDao.deleteAll()
    }
}