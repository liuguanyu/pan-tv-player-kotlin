package com.baidu.tv.player.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.baidu.tv.player.database.AppDatabase
import com.baidu.tv.player.model.PlaybackHistory
import java.util.List

/**
 * 主界面视图模型
 *
 * 负责管理主界面的UI状态，特别是最近播放记录的获取
 * 使用LiveData来观察数据库中最近4条播放历史记录的变化
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val recentHistory: LiveData<List<PlaybackHistory>> = database.playbackHistoryDao().getTop4History()

    /**
     * 获取最近播放记录的LiveData
     *
     * @return 最近4条播放历史记录的LiveData
     */
    fun getRecentHistory(): LiveData<List<PlaybackHistory>> = recentHistory
}