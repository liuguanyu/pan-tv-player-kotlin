package com.baidu.tv.player.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 播放列表实体类
 *
 * 表名: playlists
 *
 * 用于存储播放列表的基本信息，包括名称、创建时间、最后播放信息等。
 * 与PlaylistItem通过playlistId建立一对多关系。
 */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 播放列表名称 */
    val name: String,

    /** 创建时间（毫秒） */
    val createdAt: Long,

    /** 最后播放时间（毫秒） */
    val lastPlayedAt: Long,

    /** 最后播放的文件索引 */
    val lastPlayedIndex: Int = 0,

    /** 媒体类型：0=混合, 1=视频, 2=图片 */
    val mediaType: Int,

    /** 封面图片路径 */
    val coverImagePath: String?,

    /** 总文件数 */
    val totalItems: Int,

    /** 总时长（毫秒，仅视频） */
    val totalDuration: Long = 0,

    /** 排序顺序 */
    val sortOrder: Int = 0,

    /** 源目录路径列表 (JSON格式字符串)，用于刷新内容 */
    val sourcePaths: String
)