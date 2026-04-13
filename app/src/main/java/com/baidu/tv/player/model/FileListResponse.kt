package com.baidu.tv.player.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 文件列表响应模型
 *
 * @property errno 错误码，0表示成功
 * @property errmsg 错误信息
 * @property list 文件列表
 * @property guidInfo GUID信息
 * @property requestId 请求ID
 * @property hasMore 是否还有更多文件
 * @property cursor 分页游标
 */
@JsonClass(generateAdapter = true)
data class FileListResponse(
    @Json(name = "errno")
    val errno: Int = 0,

    val errmsg: String = "",

    @Json(name = "list")
    val list: List<FileInfo> = emptyList(),

    @Json(name = "guid_info")
    val guidInfo: String = "",

    @Json(name = "request_id")
    val requestId: Long = 0L,

    @Json(name = "has_more")
    val hasMore: Int = 0,

    val cursor: String = ""
) {
    /**
     * 是否成功
     * @return 如果errno为0返回true，否则返回false
     */
    fun isSuccess(): Boolean = errno == 0
}