package com.baidu.tv.player.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 用户信息响应模型
 *
 * @property errno 错误码，0表示成功
 * @property errmsg 错误信息
 * @property baiduName 百度账号名
 * @property netdiskName 网盘昵称
 * @property avatarUrl 头像URL
 * @property vipType 会员类型
 * @property uk 用户唯一标识
 */
@JsonClass(generateAdapter = true)
data class UserInfoResponse(
    @Json(name = "errno")
    val errno: Int = 0,

    val errmsg: String = "",

    @Json(name = "baidu_name")
    val baiduName: String = "",

    @Json(name = "netdisk_name")
    val netdiskName: String = "",

    @Json(name = "avatar_url")
    val avatarUrl: String = "",

    @Json(name = "vip_type")
    val vipType: Int = 0,

    @Json(name = "uk")
    val uk: Long = 0L
) {
    /**
     * 是否成功
     * @return 如果errno为0返回true，否则返回false
     */
    fun isSuccess(): Boolean = errno == 0
}