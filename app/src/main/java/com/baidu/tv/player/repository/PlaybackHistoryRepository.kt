package com.baidu.tv.player.repository

import com.baidu.tv.player.database.AppDatabase
import com.baidu.tv.player.database.PlaybackHistoryDao
import com.baidu.tv.player.model.PlaybackHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 播放历史记录仓库
 *
 * 作为数据访问层的抽象，封装了对播放历史记录数据库的操作。
 * 提供了业务逻辑层与数据持久化层之间的接口。
 */
class PlaybackHistoryRepository(private val database: AppDatabase) {

    private val playbackHistoryDao: PlaybackHistoryDao = database.playbackHistoryDao()

    /**
     * 获取所有播放历史记录
     *
     * @return Flow<List<PlaybackHistory>> 流式返回所有历史记录
     */
    fun getAllHistory(): Flow<List<PlaybackHistory>> {
        return playbackHistoryDao.getAllHistory()
    }

    /**
     * 获取最近的播放历史记录（指定数量）
     *
     * @param limit 限制返回的数量
     * @return Flow<List<PlaybackHistory>> 流式返回最近的历史记录
     */
    fun getRecentHistory(limit: Int): Flow<List<PlaybackHistory>> {
        return playbackHistoryDao.getRecentHistory(limit)
    }

    /**
     * 获取最新的4条播放历史记录
     *
     * @return Flow<List<PlaybackHistory>> 流式返回最新4条历史记录
     */
    fun getTop4History(): Flow<List<PlaybackHistory>> {
        return playbackHistoryDao.getTop4History()
    }

    /**
     * 根据ID获取播放历史记录
     *
     * @param id 历史记录ID
     * @return Flow<PlaybackHistory> 流式返回指定ID的历史记录
     */
    fun getHistoryById(id: Long): Flow<PlaybackHistory> {
        return playbackHistoryDao.getHistoryById(id)
    }

    /**
     * 根据文件路径获取播放历史记录
     *
     * @param path 文件路径
     * @return PlaybackHistory? 如果存在则返回历史记录，否则返回null
     */
    suspend fun getHistoryByPath(path: String): PlaybackHistory? {
        return playbackHistoryDao.getHistoryByPath(path)
    }

    /**
     * 插入或更新播放历史记录
     *
     * @param history 要插入或更新的历史记录
     * @return 插入或更新后记录的ID
     */
    suspend fun insertOrUpdateHistory(history: PlaybackHistory): Long {
        return playbackHistoryDao.insert(history)
    }

    /**
     * 更新播放历史记录
     *
     * @param history 要更新的历史记录
     */
    suspend fun updateHistory(history: PlaybackHistory) {
        playbackHistoryDao.update(history)
    }

    /**
     * 删除播放历史记录
     *
     * @param history 要删除的历史记录
     */
    suspend fun deleteHistory(history: PlaybackHistory) {
        playbackHistoryDao.delete(history)
    }

    /**
     * 删除所有播放历史记录
     */
    suspend fun deleteAllHistory() {
        playbackHistoryDao.deleteAll()
    }
}