package com.baidu.tv.player.model

/**
 * 认证信息模型
 *
 * @property accessToken 访问令牌
 * @property refreshToken 刷新令牌
 * @property expiresAt 令牌过期时间（毫秒时间戳）
 * @property scope 授权范围
 * @property sessionSecret 会话密钥
 * @property sessionKey 会话键
 * @property sessionExpiresAt 会话过期时间
 * @property userId 用户ID
 * @property username 用户名
 * @property isLoggedIn 是否已登录
 */
data class AuthInfo(
    var accessToken: String = "",
    var refreshToken: String = "",
    var expiresAt: Long = 0,
    var scope: String = "",
    var sessionSecret: String = "",
    var sessionKey: String = "",
    var sessionExpiresAt: Long = 0,
    var userId: String = "",
    var username: String = "",
    var isLoggedIn: Boolean = false
) {
    /**
     * 检查token是否过期
     * @return 如果当前时间超过过期时间返回true，否则返回false
     */
    fun isTokenExpired(): Boolean = System.currentTimeMillis() >= expiresAt
}