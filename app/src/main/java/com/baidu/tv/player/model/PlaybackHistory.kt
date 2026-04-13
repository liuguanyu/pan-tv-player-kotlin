package com.baidu.tv.player.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 播放历史记录实体
 *
 * 表名: playback_history
 *
 * 记录用户的播放历史，包含文件路径、文件名、播放时间等信息。
 * 用于实现播放历史功能，支持查看最近播放的文件。
 */
@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 文件路径 */
    val filePath: String,

    /** 文件名 */
    val fileName: String,

    /** 播放时间（毫秒） */
    val playedAt: Long,

    /** 缩略图路径 */
    val thumbnailPath: String?,

    /** 媒体类型：1=视频, 2=图片 */
    val mediaType: Int
)