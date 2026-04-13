package com.baidu.tv.player.ui.filebrowser

/**
 * 文件信息模型类
 *
 * 用于表示百度网盘中的文件或文件夹信息
 *
 * @property id 文件唯一标识符
 * @property name 文件名
 * @property path 文件路径
 * @property size 文件大小（字节）
 * @property isDir 是否为文件夹
 * @property modifiedTime 修改时间（毫秒）
 * @property thumbnailUrl 缩略图URL
 * @property videoDuration 视频时长（秒）
 * @property mimeType MIME类型
 * @property isSupported 是否为支持的媒体文件（图片或视频）
 */
data class FileInfo(
    val id: String,
    val name: String,
    val path: String,
    val size: Long,
    val isDir: Boolean,
    val modifiedTime: Long,
    val thumbnailUrl: String? = null,
    val videoDuration: Int? = null,
    val mimeType: String? = null
) {

    /**
     * 判断是否为支持的媒体文件（图片或视频）
     *
     * 支持的图片格式：jpg, jpeg, png, gif, bmp, webp
     * 支持的视频格式：mp4, mkv, avi, mov, flv, ts, m3u8, hevc, h265
     */
    val isSupported: Boolean
        get() = when {
            isDir -> true
            mimeType != null -> {
                val lowerMime = mimeType.lowercase()
                lowerMime.startsWith("image/") ||
                lowerMime.startsWith("video/") ||
                lowerMime.contains("mp4") ||
                lowerMime.contains("mkv") ||
                lowerMime.contains("avi") ||
                lowerMime.contains("mov") ||
                lowerMime.contains("flv") ||
                lowerMime.contains("ts") ||
                lowerMime.contains("m3u8") ||
                lowerMime.contains("hevc") ||
                lowerMime.contains("h265")
            }
            else -> {
                val lowerName = name.lowercase()
                val supportedExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "mp4", "mkv", "avi", "mov", "flv", "ts", "m3u8", "hevc", "h265")
                supportedExtensions.any { lowerName.endsWith(".$it") }
            }
        }

    /**
     * 判断是否为图片文件
     */
    val isImage: Boolean
        get() = !isDir && mimeType?.startsWith("image/") == true

    /**
     * 判断是否为视频文件
     */
    val isVideo: Boolean
        get() = !isDir && mimeType?.startsWith("video/") == true

    /**
     * 获取文件扩展名
     */
    val extension: String
        get() = name.substringAfterLast(".", "")

    /**
     * 格式化文件大小为易读格式
     */
    fun formatSize(): String {
        return when {
            size < 1024 -> "${size} B"
            size < 1024 * 1024 -> "${size / 1024.0:.1f} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024).toDouble():.1f} MB"
            else -> "${size / (1024 * 1024 * 1024).toDouble():.1f} GB"
        }
    }

    /**
     * 格式化修改时间为日期时间字符串
     */
    fun formatModifiedTime(): String {
        val date = java.util.Date(modifiedTime)
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return formatter.format(date)
    }
}