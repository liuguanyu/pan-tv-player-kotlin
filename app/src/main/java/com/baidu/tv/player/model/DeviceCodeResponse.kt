package com.baidu.tv.player.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 设备码响应模型
 *
 * @property deviceCode 设备码
 * @property userCode 用户码
 * @property verificationUrl 验证URL
 * @property expiresIn 过期时间（秒）
 * @property interval 轮询间隔（秒）
 * @property error 错误码
 * @property errorDescription 错误描述
 */
@JsonClass(generateAdapter = true)
data class DeviceCodeResponse(
    @Json(name = "device_code")
    val deviceCode: String? = null,

    @Json(name = "user_code")
    val userCode: String? = null,

    @Json(name = "verification_url")
    val verificationUrl: String? = null,

    @Json(name = "expires_in")
    val expiresIn: Long = 0,

    @Json(name = "interval")
    val interval: Int = 0,

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

    /**
     * 获取完整的验证URL
     * @return 包含用户码的完整验证URL，如果缺少必要信息则返回null
     */
    fun getFullVerificationUrl(): String? {
        return if (!verificationUrl.isNullOrEmpty() && !userCode.isNullOrEmpty()) {
            "$verificationUrl?code=$userCode"
        } else {
            null
        }
    }
}