package com.baidu.tv.player.model

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 文件信息模型
 *
 * @property fsId 文件ID
 * @property path 文件路径
 * @property serverFilename 服务器文件名
 * @property size 文件大小（字节）
 * @property serverMtime 服务器修改时间
 * @property serverCtime 服务器创建时间
 * @property localMtime 本地修改时间
 * @property localCtime 本地创建时间
 * @property isdir 是否是目录（1=目录，0=文件）
 * @property category 文件分类
 * @property md5 文件MD5
 * @property dirEmpty 目录是否为空
 * @property thumbs 缩略图信息
 * @property dlink 下载链接
 */
@JsonClass(generateAdapter = true)
data class FileInfo(
    @Json(name = "fs_id")
    val fsId: Long = 0L,

    val path: String = "",

    @Json(name = "server_filename")
    val serverFilename: String = "",

    val size: Long = 0L,

    @Json(name = "server_mtime")
    val serverMtime: Long = 0L,

    @Json(name = "server_ctime")
    val serverCtime: Long = 0L,

    @Json(name = "local_mtime")
    val localMtime: Long = 0L,

    @Json(name = "local_ctime")
    val localCtime: Long = 0L,

    val isdir: Int = 0,

    val category: Int = 0,

    val md5: String = "",

    @Json(name = "dir_empty")
    val dirEmpty: Int = 0,

    val thumbs: Thumbs? = null,

    val dlink: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readParcelable(Thumbs::class.java.classLoader),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(fsId)
        parcel.writeString(path)
        parcel.writeString(serverFilename)
        parcel.writeLong(size)
        parcel.writeLong(serverMtime)
        parcel.writeLong(serverCtime)
        parcel.writeLong(localMtime)
        parcel.writeLong(localCtime)
        parcel.writeInt(isdir)
        parcel.writeInt(category)
        parcel.writeString(md5)
        parcel.writeInt(dirEmpty)
        parcel.writeParcelable(thumbs, flags)
        parcel.writeString(dlink)
    }

    override fun describeContents(): Int = 0

    /**
     * 是否是目录
     */
    fun isDirectory(): Boolean = isdir == 1

    /**
     * 是否是图片
     * 优先使用百度网盘API的category字段(3=图片)，其次检查文件扩展名
     */
    fun isImage(): Boolean {
        // 优先使用百度API的category字段判断
        if (category == 3) return true

        // 如果category不是图片，再通过扩展名判断
        val ext = extension.lowercase()
        return ext in setOf("jpg", "jpeg", "png", "avif", "webp", "heic", "heif", "bmp", "gif", "tiff", "tif")
    }

    /**
     * 是否是视频
     * 优先使用百度网盘API的category字段(1=视频)，其次检查文件扩展名
     */
    fun isVideo(): Boolean {
        // 优先使用百度API的category字段判断
        if (category == 1) return true

        // 如果category不是视频，再通过扩展名判断
        val ext = extension.lowercase()
        return ext in setOf("mp4", "mov", "3gp", "mkv", "avi", "m4v", "flv", "wmv", "webm")
    }

    /**
     * 获取文件扩展名
     */
    val extension: String
        get() = if (serverFilename.contains(".")) {
            serverFilename.substringAfterLast(".")
        } else {
            ""
        }

    /**
     * 缩略图信息
     */
    @JsonClass(generateAdapter = true)
    data class Thumbs(
        val icon: String = "",
        val url1: String = "",
        val url2: String = "",
        val url3: String = ""
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: ""
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(icon)
            parcel.writeString(url1)
            parcel.writeString(url2)
            parcel.writeString(url3)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<Thumbs> {
            override fun createFromParcel(parcel: Parcel): Thumbs = Thumbs(parcel)
            override fun newArray(size: Int): Array<Thumbs?> = arrayOfNulls(size)
        }
    }

    companion object CREATOR : Parcelable.Creator<FileInfo> {
        override fun createFromParcel(parcel: Parcel): FileInfo = FileInfo(parcel)
        override fun newArray(size: Int): Array<FileInfo?> = arrayOfNulls(size)
    }
}