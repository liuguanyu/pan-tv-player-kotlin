package com.baidu.tv.player.repository

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.baidu.tv.player.database.AppDatabase
import com.baidu.tv.player.database.PlaybackHistoryDao
import com.baidu.tv.player.model.PlaybackHistory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat

/**
 * PlaybackHistoryRepository 单元测试
 *
 * 测试播放历史记录的CRUD操作
 * 使用 Room 数据库进行集成测试
 * 使用 kotlinx-coroutines-test 进行协程测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PlaybackHistoryRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var playbackHistoryDao: PlaybackHistoryDao
    private lateinit var playbackHistoryRepository: PlaybackHistoryRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // 使用内存数据库进行测试
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // 允许主线程查询，简化测试
            .build()
        playbackHistoryDao = database.playbackHistoryDao()
        playbackHistoryRepository = PlaybackHistoryRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * 测试：获取所有播放历史记录
     */
    @Test
    fun testGetAllHistory() = runTest {
        // Given: 插入一些播放历史记录
        val history1 = PlaybackHistory(id = 1L, filePath = "/path/to/video1.mp4", fileName = "Video 1", fileSize = 1024, modifiedTime = System.currentTimeMillis(), duration = 120, playedTime = 30)
        val history2 = PlaybackHistory(id = 2L, filePath = "/path/to/video2.mp4", fileName = "Video 2", fileSize = 2048, modifiedTime = System.currentTimeMillis(), duration = 180, playedTime = 90)
        val history3 = PlaybackHistory(id = 3L, filePath = "/path/to/video3.mp4", fileName = "Video 3", fileSize = 4096, modifiedTime = System.currentTimeMillis(), duration = 240, playedTime = 120)

        playbackHistoryDao.insert(history1)
        playbackHistoryDao.insert(history2)
        playbackHistoryDao.insert(history3)

        // When: 获取所有播放历史记录
        val historyFlow = playbackHistoryRepository.getAllHistory()
        val historyList = historyFlow.first()

        // Then: 验证返回正确的播放历史记录
        assertThat(historyList).hasSize(3)
        assertThat(historyList).contains(history1)
        assertThat(historyList).contains(history2)
        assertThat(historyList).contains(history3)
    }

    /**
     * 测试：获取最近的播放历史记录
     */
    @Test
    fun testGetRecentHistory() = runTest {
        // Given: 插入多个播放历史记录
        val history1 = PlaybackHistory(id = 1L, filePath = "/path/to/video1.mp4", fileName = "Video 1", fileSize = 1024, modifiedTime = System.currentTimeMillis() - 10000, duration = 120, playedTime = 30)
        val history2 = PlaybackHistory(id = 2L, filePath = "/path/to/video2.mp4", fileName = "Video 2", fileSize = 2048, modifiedTime = System.currentTimeMillis() - 5000, duration = 180, playedTime = 90)
        val history3 = PlaybackHistory(id = 3L, filePath = "/path/to/video3.mp4", fileName = "Video 3", fileSize = 4096, modifiedTime = System.currentTimeMillis(), duration = 240, playedTime = 120)

        playbackHistoryDao.insert(history1)
        playbackHistoryDao.insert(history2)
        playbackHistoryDao.insert(history3)

        // When: 获取最近的2条历史记录
        val historyFlow = playbackHistoryRepository.getRecentHistory(2)
        val historyList = historyFlow.first()

        // Then: 验证返回最近的2条历史记录
        assertThat(historyList).hasSize(2)
        assertThat(historyList[0]).isEqualTo(history3)
        assertThat(historyList[1]).isEqualTo(history2)
    }

    /**
     * 测试：获取最新的4条播放历史记录
     */
    @Test
    fun testGetTop4History() = runTest {
        // Given: 插入多个播放历史记录
        val history1 = PlaybackHistory(id = 1L, filePath = "/path/to/video1.mp4", fileName = "Video 1", fileSize = 1024, modifiedTime = System.currentTimeMillis() - 10000, duration = 120, playedTime = 30)
        val history2 = PlaybackHistory(id = 2L, filePath = "/path/to/video2.mp4", fileName = "Video 2", fileSize = 2048, modifiedTime = System.currentTimeMillis() - 5000, duration = 180, playedTime = 90)
        val history3 = PlaybackHistory(id = 3L, filePath = "/path/to/video3.mp4", fileName = "Video 3", fileSize = 4096, modifiedTime = System.currentTimeMillis(), duration = 240, playedTime = 120)

        playbackHistoryDao.insert(history1)
        playbackHistoryDao.insert(history2)
        playbackHistoryDao.insert(history3)

        // When: 获取最新的4条历史记录
        val historyFlow = playbackHistoryRepository.getTop4History()
        val historyList = historyFlow.first()

        // Then: 验证返回所有历史记录（少于4条）
        assertThat(historyList).hasSize(3)
        assertThat(historyList[0]).isEqualTo(history3)
        assertThat(historyList[1]).isEqualTo(history2)
        assertThat(historyList[2]).isEqualTo(history1)
    }

    /**
     * 测试：获取单个播放历史记录
     */
    @Test
    fun testGetHistoryById() = runTest {
        // Given: 插入一个播放历史记录
        val expectedHistory = PlaybackHistory(id = 1L, filePath = "/path/to/video.mp4", fileName = "Video", fileSize = 1024, modifiedTime = System.currentTimeMillis(), duration = 120, playedTime = 30)
        playbackHistoryDao.insert(expectedHistory)

        // When: 获取指定ID的历史记录
        val historyFlow = playbackHistoryRepository.getHistoryById(1L)
        val history = historyFlow.first()

        // Then: 验证返回正确的播放历史记录
        assertThat(history).isEqualTo(expectedHistory)
    }

    /**
     * 测试：根据文件路径获取播放历史记录
     */
    @Test
    fun testGetHistoryByPath() = runTest {
        // Given: 插入一个播放历史记录
        val expectedHistory = PlaybackHistory(id = 1L, filePath = "/path/to/video.mp4", fileName = "Video", fileSize = 1024, modifiedTime = System.currentTimeMillis(), duration = 120, playedTime = 30)
        playbackHistoryDao.insert(expectedHistory)

        // When: 根据文件路径获取历史记录
        val history = playbackHistoryRepository.getHistoryByPath("/path/to/video.mp4")

        // Then: 验证返回正确的播放历史记录
        assertThat(history).isEqualTo(expectedHistory)
    }

    /**
     * 测试：根据文件路径获取不存在的历史记录
     */
    @Test
    fun testGetHistoryByPath_NotFound() = runTest {
        // Given: 数据库为空

        // When: 根据文件路径获取历史记录
        val history = playbackHistoryRepository.getHistoryByPath("/path/to/nonexistent.mp4")

        // Then: 验证返回null
        assertThat(history).isNull()
    }

    /**
     * 测试：插入或更新播放历史记录
     */
    @Test
    fun testInsertOrUpdateHistory() = runTest {
        // Given: 创建一个新的播放历史记录
        val history = PlaybackHistory(filePath = "/path/to/video.mp4", fileName = "Video", fileSize = 1024, modifiedTime = System.currentTimeMillis(), duration = 120, playedTime = 30)

        // When: 插入播放历史记录
        val id = playbackHistoryRepository.insertOrUpdateHistory(history)

        // Then: 验证插入成功并返回了ID
        assertThat(id).isGreaterThan(0L)

        // 验证数据库中确实存在该播放历史记录
        val insertedHistory = playbackHistoryDao.getHistoryById(id).first()
        assertThat(insertedHistory.filePath).isEqualTo(history.filePath)
        assertThat(insertedHistory.fileName).isEqualTo(history.fileName)
        assertThat(insertedHistory.fileSize).isEqualTo(history.fileSize)
        assertThat(insertedHistory.modifiedTime).isEqualTo(history.modifiedTime)
        assertThat(insertedHistory.duration).isEqualTo(history.duration)
        assertThat(insertedHistory.playedTime).isEqualTo(history.playedTime)
    }

    /**
     * 测试：更新播放历史记录
     */
    @Test
    fun testUpdateHistory() = runTest {
        // Given: 插入一个播放历史记录
        val originalHistory = PlaybackHistory(filePath = "/path/to/video.mp4", fileName = "Original Video", fileSize = 1024, modifiedTime = System.currentTimeMillis(), duration = 120, playedTime = 30)
        val id = playbackHistoryDao.insert(originalHistory)

        // When: 更新播放历史记录
        val updatedHistory = originalHistory.copy(id = id, fileName = "Updated Video", playedTime = 60)
        playbackHistoryRepository.updateHistory(updatedHistory)

        // Then: 验证更新成功
        val retrievedHistory = playbackHistoryDao.getHistoryById(id).first()
        assertThat(retrievedHistory.fileName).isEqualTo("Updated Video")
        assertThat(retrievedHistory.playedTime).isEqualTo(60)
    }

    /**
     * 测试：删除播放历史记录
     */
    @Test
    fun testDeleteHistory() = runTest {
        // Given: 插入一个播放历史记录
        val history = PlaybackHistory(filePath = "/path/to/video.mp4", fileName = "To Delete", fileSize = 1024, modifiedTime = System.currentTimeMillis(), duration = 120, playedTime = 30)
        val id = playbackHistoryDao.insert(history)

        // When: 删除播放历史记录
        val toDeleteHistory = history.copy(id = id)
        playbackHistoryRepository.deleteHistory(toDeleteHistory)

        // Then: 验证删除成功
        val retrievedHistory = playbackHistoryDao.getHistoryById(id).firstOrNull()
        assertThat(retrievedHistory).isNull()
    }

    /**
     * 测试：删除所有播放历史记录
     */
    @Test
    fun testDeleteAllHistory() = runTest {
        // Given: 插入多个播放历史记录
        val history1 = PlaybackHistory(filePath = "/path/to/video1.mp4", fileName = "Video 1", fileSize = 1024, modifiedTime = System.currentTimeMillis(), duration = 120, playedTime = 30)
        val history2 = PlaybackHistory(filePath = "/path/to/video2.mp4", fileName = "Video 2", fileSize = 2048, modifiedTime = System.currentTimeMillis(), duration = 180, playedTime = 90)
        val history3 = PlaybackHistory(filePath = "/path/to/video3.mp4", fileName = "Video 3", fileSize = 4096, modifiedTime = System.currentTimeMillis(), duration = 240, playedTime = 120)

        playbackHistoryDao.insert(history1)
        playbackHistoryDao.insert(history2)
        playbackHistoryDao.insert(history3)

        // When: 删除所有播放历史记录
        playbackHistoryRepository.deleteAllHistory()

        // Then: 验证所有播放历史记录都被删除
        val allHistory = playbackHistoryDao.getAllHistory().first()
        assertThat(allHistory).isEmpty()
    }

    /**
     * 测试：获取不存在的历史记录
     */
    @Test
    fun testGetHistoryById_NotFound() = runTest {
        // Given: 数据库为空

        // When: 获取不存在的历史记录
        val historyFlow = playbackHistoryRepository.getHistoryById(999L)
        val history = historyFlow.firstOrNull()

        // Then: 验证返回null
        assertThat(history).isNull()
    }

    /**
     * 测试：插入重复的历史记录进行更新
     */
    @Test
    fun testInsertOrUpdateHistory_Existing() = runTest {
        // Given: 插入一个播放历史记录
        val originalHistory = PlaybackHistory(id = 1L, filePath = "/path/to/video.mp4", fileName = "Existing Video", fileSize = 1024, modifiedTime = System.currentTimeMillis(), duration = 120, playedTime = 30)
        playbackHistoryDao.insert(originalHistory)

        // When: 使用相同ID插入更新
        val updatedHistory = originalHistory.copy(fileName = "Updated Video", playedTime = 60)
        val id = playbackHistoryRepository.insertOrUpdateHistory(updatedHistory)

        // Then: 验证ID保持不变，内容已更新
        assertThat(id).isEqualTo(1L)
        val retrievedHistory = playbackHistoryDao.getHistoryById(id).first()
        assertThat(retrievedHistory.fileName).isEqualTo("Updated Video")
        assertThat(retrievedHistory.playedTime).isEqualTo(60)
    }
}
