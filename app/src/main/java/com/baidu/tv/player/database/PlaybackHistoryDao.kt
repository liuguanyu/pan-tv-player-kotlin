package com.baidu.tv.player.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.baidu.tv.player.model.PlaybackHistory
import kotlinx.coroutines.flow.Flow

/**
 * 播放历史记录DAO接口
 *
 * 定义了对播放历史数据的所有操作方法。
 * 所有方法都返回Flow或suspend函数，以支持Kotlin协程和响应式编程。
 */
@Dao
interface PlaybackHistoryDao {

    /**
     * 插入或替换播放历史记录
     *
     * @param history 要插入或替换的历史记录
     * @return 插入记录的ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PlaybackHistory): Long

    /**
     * 更新播放历史记录
     *
     * @param history 要更新的历史记录
     */
    @Update
    suspend fun update(history: PlaybackHistory)

    /**
     * 删除播放历史记录
     *
     * @param history 要删除的历史记录
     */
    @Delete
    suspend fun delete(history: PlaybackHistory)

    /**
     * 获取所有播放历史，按播放时间降序排列
     *
     * @return Flow<List<PlaybackHistory>> 流式返回所有历史记录
     */
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC")
    fun getAllHistory(): Flow<List<PlaybackHistory>>

    /**
     * 获取指定数量的最近播放历史，按播放时间降序排列
     *
     * @param limit 限制返回的数量
     * @return Flow<List<PlaybackHistory>> 流式返回最近的历史记录
     */
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<PlaybackHistory>>

    /**
     * 获取最新4条播放历史，按播放时间降序排列
     *
     * @return Flow<List<PlaybackHistory>> 流式返回最新4条历史记录
     */
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT 4")
    fun getTop4History(): Flow<List<PlaybackHistory>>

    /**
     * 根据ID获取播放历史
     *
     * @param id 历史记录ID
     * @return Flow<PlaybackHistory> 流式返回指定ID的历史记录
     */
    @Query("SELECT * FROM playback_history WHERE id = :id")
    fun getHistoryById(id: Long): Flow<PlaybackHistory>

    /**
     * 根据文件路径获取播放历史（返回单条记录）
     *
     * @param path 文件路径
     * @return PlaybackHistory? 如果存在则返回历史记录，否则返回null
     */
    @Query("SELECT * FROM playback_history WHERE filePath = :path LIMIT 1")
    suspend fun getHistoryByPath(path: String): PlaybackHistory?

    /**
     * 删除所有播放历史
     */
    @Query("DELETE FROM playback_history")
    suspend fun deleteAll()
}