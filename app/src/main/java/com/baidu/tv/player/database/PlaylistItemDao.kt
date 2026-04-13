package com.baidu.tv.player.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.baidu.tv.player.model.PlaylistItem
import kotlinx.coroutines.flow.Flow

/**
 * 播放列表项DAO接口
 *
 * 定义了对播放列表项数据的所有操作方法。
 * 所有方法都返回Flow或suspend函数，以支持Kotlin协程和响应式编程。
 */
@Dao
interface PlaylistItemDao {

    /**
     * 插入或替换单个播放列表项
     *
     * @param item 要插入或替换的播放列表项
     * @return 插入记录的ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PlaylistItem): Long

    /**
     * 批量插入播放列表项
     *
     * @param items 要插入的播放列表项列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PlaylistItem>)

    /**
     * 删除播放列表项
     *
     * @param item 要删除的播放列表项
     */
    @Delete
    suspend fun delete(item: PlaylistItem)

    /**
     * 根据播放列表ID获取所有项，按排序顺序升序排列
     *
     * @param playlistId 播放列表ID
     * @return Flow<List<PlaylistItem>> 流式返回播放列表中的所有项
     */
    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    fun getItemsByPlaylistId(playlistId: Long): Flow<List<PlaylistItem>>

    /**
     * 根据播放列表ID删除所有项
     *
     * @param playlistId 播放列表ID
     */
    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylistId(playlistId: Long)

    /**
     * 获取指定播放列表的项目数量
     *
     * @param playlistId 播放列表ID
     * @return 项目数量
     */
    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getItemCount(playlistId: Long): Int
}