package com.baidu.tv.player.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.baidu.tv.player.model.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * 播放列表DAO接口
 *
 * 定义了对播放列表数据的所有操作方法。
 * 所有方法都返回Flow或suspend函数，以支持Kotlin协程和响应式编程。
 */
@Dao
interface PlaylistDao {

    /**
     * 插入或替换播放列表
     *
     * @param playlist 要插入或替换的播放列表
     * @return 插入记录的ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: Playlist): Long

    /**
     * 更新播放列表
     *
     * @param playlist 要更新的播放列表
     */
    @Update
    suspend fun update(playlist: Playlist)

    /**
     * 删除播放列表
     *
     * @param playlist 要删除的播放列表
     */
    @Delete
    suspend fun delete(playlist: Playlist)

    /**
     * 获取所有播放列表，按排序顺序和创建时间降序排列
     *
     * @return Flow<List<Playlist>> 流式返回所有播放列表
     */
    @Query("SELECT * FROM playlists ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    /**
     * 根据ID获取播放列表
     *
     * @param id 播放列表ID
     * @return Flow<Playlist> 流式返回指定ID的播放列表
     */
    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistById(id: Long): Flow<Playlist>

    /**
     * 删除所有播放列表
     */
    @Query("DELETE FROM playlists")
    suspend fun deleteAll()
}