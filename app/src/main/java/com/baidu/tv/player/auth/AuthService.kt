package com.baidu.tv.player.auth

import android.content.Context
import com.baidu.tv.player.config.BaiduConfig
import com.baidu.tv.player.model.AuthInfo
import com.baidu.tv.player.model.DeviceCodeResponse
import com.baidu.tv.player.model.TokenResponse
import com.baidu.tv.player.network.ApiConstants
import com.baidu.tv.player.network.BaiduPanService
import com.baidu.tv.player.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 百度网盘认证服务
 *
 * 使用单例模式，支持OAuth 2.0设备码流程
 * - 获取device_code
 * - 轮询token状态
 * - 刷新access_token
 * - 自动token存储和管理
 *
 * @property context Android应用上下文
 */
class AuthService private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: AuthService? = null

        /**
         * 获取AuthService单例实例
         */
        fun getInstance(context: Context): AuthService {
            return instance ?: synchronized(this) {
                instance ?: AuthService(context.applicationContext).also { instance = it }
            }
        }
    }

    private val dataStoreManager = DataStoreManager.getInstance(context)
    private val oauthService: BaiduPanService by lazy {
        RetrofitClient.getOAuthInstance().create(BaiduPanService::class.java)
    }
    private val mainScope = CoroutineScope(Dispatchers.Main)

    /**
     * 从DataStore加载认证信息
     */
    private suspend fun loadAuthInfo(): AuthInfo = withContext(Dispatchers.IO) {
        val prefs = dataStoreManager.authPreferences
        AuthInfo(
            accessToken = prefs[DataStoreManager.KEY_ACCESS_TOKEN] ?: "",
            refreshToken = prefs[DataStoreManager.KEY_REFRESH_TOKEN] ?: "",
            expiresAt = prefs[DataStoreManager.KEY_EXPIRES_AT] ?: 0L,
            scope = prefs[DataStoreManager.KEY_SCOPE] ?: "",
            sessionKey = prefs[DataStoreManager.KEY_SESSION_KEY] ?: "",
            sessionSecret = prefs[DataStoreManager.KEY_SESSION_SECRET] ?: "",
            sessionExpiresAt = prefs[DataStoreManager.KEY_SESSION_EXPIRES_AT] ?: 0L,
            userId = prefs[DataStoreManager.KEY_USER_ID] ?: "",
            username = prefs[DataStoreManager.KEY_USERNAME] ?: "",
            isLoggedIn = prefs[DataStoreManager.KEY_IS_LOGGED_IN] ?: false
        )
    }

    /**
     * 保存认证信息到DataStore
     */
    private suspend fun saveAuthInfo(authInfo: AuthInfo) = withContext(Dispatchers.IO) {
        val prefs = dataStoreManager.authPreferences.edit()
        prefs[DataStoreManager.KEY_ACCESS_TOKEN] = authInfo.accessToken
        prefs[DataStoreManager.KEY_REFRESH_TOKEN] = authInfo.refreshToken
        prefs[DataStoreManager.KEY_EXPIRES_AT] = authInfo.expiresAt
        prefs[DataStoreManager.KEY_SCOPE] = authInfo.scope
        prefs[DataStoreManager.KEY_SESSION_KEY] = authInfo.sessionKey
        prefs[DataStoreManager.KEY_SESSION_SECRET] = authInfo.sessionSecret
        prefs[DataStoreManager.KEY_SESSION_EXPIRES_AT] = authInfo.sessionExpiresAt
        prefs[DataStoreManager.KEY_USER_ID] = authInfo.userId
        prefs[DataStoreManager.KEY_USERNAME] = authInfo.username
        prefs[DataStoreManager.KEY_IS_LOGGED_IN] = authInfo.isLoggedIn
        prefs.apply()
    }

    /**
     * 获取或生成设备ID
     * 使用SharedPreferences存储（设备ID不需要DataStore的升级迁移）
     */
    fun getDeviceId(): String {
        val prefs = context.getSharedPreferences("baidu_auth_device", Context.MODE_PRIVATE)
        var deviceId = prefs.getString(DataStoreManager.KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(DataStoreManager.KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    /**
     * 检查是否已认证
     * 会根据token有效期自动判断，如过期会尝试自动刷新
     */
    suspend fun isAuthenticated(): Boolean = withContext(Dispatchers.IO) {
        val authInfo = loadAuthInfo()

        if (!authInfo.isLoggedIn) return@withContext false
        if (authInfo.accessToken.isEmpty()) return@withContext false

        // 检查token是否过期
        if (authInfo.isTokenExpired()) {
            // 尝试自动刷新token
            return@withContext try {
                val success = refreshTokenInternal()
                if (success) {
                    val newAuthInfo = loadAuthInfo()
                    newAuthInfo.isLoggedIn && newAuthInfo.accessToken.isNotEmpty() && !newAuthInfo.isTokenExpired()
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }

        return@withContext authInfo.isLoggedIn &&
                authInfo.accessToken.isNotEmpty() &&
                !authInfo.isTokenExpired()
    }

    /**
     * 获取访问令牌
     */
    suspend fun getAccessToken(): String = withContext(Dispatchers.IO) {
        val authInfo = loadAuthInfo()
        authInfo.accessToken
    }

    /**
     * 获取认证信息
     */
    suspend fun getAuthInfo(): AuthInfo = withContext(Dispatchers.IO) {
        loadAuthInfo()
    }

    /**
     * 获取设备码（OAuth设备码流程第一步）
     *
     * @param callback 回调接口，返回设备码响应或错误信息
     */
    fun getDeviceCode(callback: AuthCallback<DeviceCodeResponse>) {
        mainScope.launch(Dispatchers.IO) {
            try {
                val response = oauthService.getDeviceCode(
                    BaiduConfig.APP_KEY,
                    BaiduConfig.SCOPE,
                    "device_code"
                ).execute()

                if (response.isSuccessful && response.body() != null) {
                    val deviceCodeResponse = response.body()!!
                    if (deviceCodeResponse.hasError()) {
                        callback.onError(deviceCodeResponse.error ?: "获取设备码失败")
                    } else {
                        callback.onSuccess(deviceCodeResponse)
                    }
                } else {
                    callback.onError("获取设备码失败: ${response.code()}")
                }
            } catch (e: Exception) {
                callback.onError(e.message ?: "网络请求异常")
            }
        }
    }

    /**
     * 轮询设备码状态（OAuth设备码流程第二步）
     *
     * @param deviceCode 设备码
     * @param callback 回调接口，返回是否授权成功
     * @param pollingInterval 轮询间隔（毫秒，可选，默认使用ApiConstants.POLLING_INTERVAL）
     * @param maxCount 最大轮询次数（可选，默认使用ApiConstants.MAX_POLLING_COUNT）
     */
    fun pollDeviceCodeStatus(
        deviceCode: String,
        callback: AuthCallback<Boolean>,
        pollingInterval: Long = ApiConstants.POLLING_INTERVAL,
        maxCount: Int = ApiConstants.MAX_POLLING_COUNT
    ) {
        pollDeviceCodeStatusRecursive(deviceCode, 0, callback, pollingInterval, maxCount)
    }

    /**
     * 递归轮询设备码状态
     */
    private fun pollDeviceCodeStatusRecursive(
        deviceCode: String,
        count: Int,
        callback: AuthCallback<Boolean>,
        pollingInterval: Long,
        maxCount: Int
    ) {
        if (count >= maxCount) {
            callback.onError("授权超时，请重新尝试")
            return
        }

        mainScope.launch(Dispatchers.IO) {
            try {
                val response = oauthService.getTokenByDeviceCode(
                    "device_token",
                    deviceCode,
                    BaiduConfig.APP_KEY,
                    BaiduConfig.SECRET_KEY
                ).execute()

                if (response.isSuccessful && response.body() != null) {
                    val tokenResponse = response.body()!!

                    // 授权成功
                    if (!tokenResponse.accessToken.isNullOrEmpty()) {
                        val authInfo = AuthInfo(
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken ?: "",
                            expiresAt = System.currentTimeMillis() + tokenResponse.expiresIn * 1000,
                            scope = tokenResponse.scope ?: "",
                            sessionKey = tokenResponse.sessionKey ?: "",
                            sessionSecret = tokenResponse.sessionSecret ?: "",
                            isLoggedIn = true
                        )
                        saveAuthInfo(authInfo)
                        withContext(Dispatchers.Main) {
                            callback.onSuccess(true)
                        }
                    } else {
                        // 处理错误
                        handleAuthError(
                            tokenResponse.error,
                            tokenResponse.errorDescription,
                            deviceCode,
                            count,
                            callback,
                            pollingInterval,
                            maxCount
                        )
                    }
                } else {
                    // 处理HTTP错误
                    handleHttpError(response.code(), deviceCode, count, callback, pollingInterval, maxCount)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e.message ?: "网络请求异常")
                }
            }
        }
    }

    /**
     * 处理HTTP错误
     */
    private fun handleHttpError(
        errorCode: Int,
        deviceCode: String,
        count: Int,
        callback: AuthCallback<Boolean>,
        pollingInterval: Long,
        maxCount: Int
    ) {
        when {
            errorCode == 400 -> {
                // 授权中状态，继续轮询
                delayAndPoll(deviceCode, count + 1, callback, pollingInterval, maxCount)
            }
            errorCode == 401 -> {
                callback.onError("授权凭证无效")
            }
            errorCode == 403 -> {
                callback.onError("授权被拒绝")
            }
            else -> {
                callback.onError("授权失败: HTTP $errorCode")
            }
        }
    }

    /**
     * 处理授权错误
     */
    private fun handleAuthError(
        error: String?,
        description: String?,
        deviceCode: String,
        count: Int,
        callback: AuthCallback<Boolean>,
        pollingInterval: Long,
        maxCount: Int
    ) {
        when (error) {
            "authorization_pending" -> {
                // 授权中，继续轮询
                delayAndPoll(deviceCode, count + 1, callback, pollingInterval, maxCount)
            }
            "expired_token" -> {
                callback.onError("授权已过期，请重新获取设备码")
            }
            "slow_down" -> {
                // 请求过快，增加轮询间隔
                delayAndPoll(deviceCode, count + 1, callback, pollingInterval * 2, maxCount)
            }
            else -> {
                callback.onError(description ?: "授权失败")
            }
        }
    }

    /**
     * 延迟并继续轮询
     */
    private fun delayAndPoll(
        deviceCode: String,
        count: Int,
        callback: AuthCallback<Boolean>,
        delayTime: Long,
        maxCount: Int
    ) {
        mainScope.launch {
            delay(delayTime)
            pollDeviceCodeStatusRecursive(deviceCode, count, callback, delayTime, maxCount)
        }
    }

    /**
     * 刷新token（内部实现，IO线程）
     */
    private suspend fun refreshTokenInternal(): Boolean = withContext(Dispatchers.IO) {
        val authInfo = loadAuthInfo()
        val refreshToken = authInfo.refreshToken

        if (refreshToken.isEmpty()) {
            return@withContext false
        }

        try {
            val response = oauthService.refreshToken(
                "refresh_token",
                refreshToken,
                BaiduConfig.APP_KEY,
                BaiduConfig.SECRET_KEY
            ).execute()

            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!

                if (!tokenResponse.accessToken.isNullOrEmpty()) {
                    val newAuthInfo = AuthInfo(
                        accessToken = tokenResponse.accessToken,
                        refreshToken = tokenResponse.refreshToken ?: refreshToken,
                        expiresAt = System.currentTimeMillis() + tokenResponse.expiresIn * 1000,
                        scope = tokenResponse.scope ?: authInfo.scope,
                        sessionKey = tokenResponse.sessionKey ?: authInfo.sessionKey,
                        sessionSecret = tokenResponse.sessionSecret ?: authInfo.sessionSecret,
                        isLoggedIn = true
                    )
                    saveAuthInfo(newAuthInfo)
                    return@withContext true
                } else {
                    return@withContext false
                }
            } else {
                return@withContext false
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * 刷新token（公开方法）
     *
     * @param callback 回调接口，返回是否刷新成功
     */
    fun refreshToken(callback: AuthCallback<Boolean>) {
        mainScope.launch {
            try {
                val success = refreshTokenInternal()
                if (success) {
                    val newAuthInfo = loadAuthInfo()
                    withContext(Dispatchers.Main) {
                        callback.onSuccess(true)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onError("刷新令牌失败")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e.message ?: "刷新令牌异常")
                }
            }
        }
    }

    /**
     * 退出登录：清除所有认证信息
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        val prefs = dataStoreManager.authPreferences.edit()
        prefs.remove(DataStoreManager.KEY_ACCESS_TOKEN)
        prefs.remove(DataStoreManager.KEY_REFRESH_TOKEN)
        prefs.remove(DataStoreManager.KEY_EXPIRES_AT)
        prefs.remove(DataStoreManager.KEY_SCOPE)
        prefs.remove(DataStoreManager.KEY_SESSION_KEY)
        prefs.remove(DataStoreManager.KEY_SESSION_SECRET)
        prefs.remove(DataStoreManager.KEY_SESSION_EXPIRES_AT)
        prefs.remove(DataStoreManager.KEY_USER_ID)
        prefs.remove(DataStoreManager.KEY_USERNAME)
        prefs.remove(DataStoreManager.KEY_IS_LOGGED_IN)
        prefs.apply()
    }

    /**
     * 认证回调接口
     */
    interface AuthCallback<T> {
        fun onSuccess(result: T)
        fun onError(error: String)
    }
}