package com.baidu.tv.player.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 播放列表项实体类
 *
 * 表名: playlist_items
 *
 * 表示播放列表中的单个媒体文件项，与Playlist通过playlistId建立外键关系。
 * 使用级联删除，当播放列表被删除时，其所有项也会被自动删除。
 */
@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE  // 删除播放列表时级联删除所有项
        )
    ],
    indices = [
        Index("playlistId")   // 为playlistId建立索引提高查询速度
    ]
)
data class PlaylistItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 所属播放列表ID */
    val playlistId: Long,

    /** 百度网盘文件fsId（用于获取dlink） */
    val fsId: Long,

    /** 文件路径（相对路径） */
    val filePath: String,

    /** 文件名 */
    val fileName: String,

    /** 媒体类型：1=视频, 2=图片 */
    val mediaType: Int,

    /** 排序顺序 */
    val sortOrder: Int,

    /** 时长（毫秒，仅视频） */
    val duration: Long = 0,

    /** 文件大小（字节） */
    val fileSize: Long
)