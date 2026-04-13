package com.baidu.tv.player.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.baidu.tv.player.model.PlaybackHistory
import com.baidu.tv.player.model.Playlist
import com.baidu.tv.player.model.PlaylistItem

/**
 * 应用程序数据库类
 *
 * Room数据库的主类，定义了数据库的实体、版本和DAO接口。
 * 使用单例模式确保数据库实例的唯一性。
 *
 * 数据库版本从1开始，支持迁移策略。
 * 注意：当前实现使用fallbackToDestructiveMigration()，在生产环境中应实现具体的迁移策略。
 */
@Database(
    entities = [Playlist::class, PlaylistItem::class, PlaybackHistory::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * 播放列表DAO
     */
    abstract fun playlistDao(): PlaylistDao

    /**
     * 播放列表项DAO
     */
    abstract fun playlistItemDao(): PlaylistItemDao

    /**
     * 播放历史记录DAO
     */
    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库实例（单例模式）
         *
         * @param context Android Context
         * @return AppDatabase实例
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * 构建数据库实例
         *
         * @param context Android Context
         * @return AppDatabase实例
         */
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "baidu_tv_player.db"
            )
                .fallbackToDestructiveMigration() // 开发阶段使用，生产环境应实现迁移策略
                .build()
        }
    }
}