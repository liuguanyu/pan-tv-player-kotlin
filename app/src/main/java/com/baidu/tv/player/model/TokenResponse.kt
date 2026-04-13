package com.baidu.tv.player.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Token响应模型
 *
 * @property accessToken 访问令牌
 * @property refreshToken 刷新令牌
 * @property expiresIn 过期时间（秒）
 * @property scope 授权范围
 * @property sessionKey 会话密钥
 * @property sessionSecret 会话密钥
 * @property error 错误码
 * @property errorDescription 错误描述
 */
@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token")
    val accessToken: String? = null,

    @Json(name = "refresh_token")
    val refreshToken: String? = null,

    @Json(name = "expires_in")
    val expiresIn: Long = 0,

    @Json(name = "scope")
    val scope: String? = null,

    @Json(name = "session_key")
    val sessionKey: String? = null,

    @Json(name = "session_secret")
    val sessionSecret: String? = null,

    @Json(name = "error")
    val error: String? = null,

    @Json(name = "error_description")
    val errorDescription: String? = null
) {
    /**
     * 是否有错误
     * @return 如果存在错误码返回true，否则返回false
     */
    fun hasError(): Boolean = !error.isNullOrEmpty()
}