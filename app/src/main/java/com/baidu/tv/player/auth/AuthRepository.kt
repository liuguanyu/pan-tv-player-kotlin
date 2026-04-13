package com.baidu.tv.player.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.baidu.tv.player.model.AuthInfo
import com.baidu.tv.player.model.DeviceCodeResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 认证数据仓库
 *
 * 负责管理认证状态、提供认证数据、处理认证流程
 * 使用Repository模式封装数据源操作
 *
 * @property context 应用上下文
 */
class AuthRepository(private val context: Context) {

    private val authService = AuthService.getInstance(context)
    private val _authStateLiveData = MutableLiveData<AuthState>()
    val authStateLiveData: LiveData<AuthState> = _authStateLiveData

    init {
        // 初始化认证状态检查
        checkAuthStatus()
    }

    /**
     * 检查认证状态
     */
    fun checkAuthStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            val isAuthenticated = authService.isAuthenticated()
            val state = if (isAuthenticated) {
                AuthState(AuthState.Status.AUTHENTICATED, "已认证")
            } else {
                AuthState(AuthState.Status.UNAUTHENTICATED, "未认证")
            }
            _authStateLiveData.postValue(state)
        }
    }

    /**
     * 获取设备码
     */
    fun getDeviceCode() {
        _authStateLiveData.postValue(AuthState(AuthState.Status.LOADING, "正在获取设备码..."))

        authService.getDeviceCode(object : AuthService.AuthCallback<DeviceCodeResponse> {
            override fun onSuccess(result: DeviceCodeResponse) {
                _authStateLiveData.postValue(
                    AuthState(
                        AuthState.Status.DEVICE_CODE_RECEIVED,
                        "设备码获取成功",
                        result
                    )
                )
            }

            override fun onError(error: String) {
                _authStateLiveData.postValue(AuthState(AuthState.Status.ERROR, error))
            }
        })
    }

    /**
     * 轮询设备码状态
     *
     * @param deviceCode 设备码
     */
    fun pollDeviceCodeStatus(deviceCode: String) {
        _authStateLiveData.postValue(AuthState(AuthState.Status.POLLING, "等待授权中..."))

        authService.pollDeviceCodeStatus(deviceCode, object : AuthService.AuthCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                if (result) {
                    _authStateLiveData.postValue(AuthState(AuthState.Status.AUTHENTICATED, "授权成功"))
                } else {
                    _authStateLiveData.postValue(AuthState(AuthState.Status.ERROR, "授权失败"))
                }
            }

            override fun onError(error: String) {
                _authStateLiveData.postValue(AuthState(AuthState.Status.ERROR, error))
            }
        })
    }

    /**
     * 刷新token
     */
    fun refreshToken() {
        _authStateLiveData.postValue(AuthState(AuthState.Status.REFRESHING, "正在刷新令牌..."))

        authService.refreshToken(object : AuthService.AuthCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                if (result) {
                    _authStateLiveData.postValue(AuthState(AuthState.Status.AUTHENTICATED, "令牌刷新成功"))
                } else {
                    _authStateLiveData.postValue(AuthState(AuthState.Status.UNAUTHENTICATED, "令牌刷新失败"))
                }
            }

            override fun onError(error: String) {
                _authStateLiveData.postValue(AuthState(AuthState.Status.ERROR, error))
            }
        })
    }

    /**
     * 退出登录
     */
    fun logout() {
        CoroutineScope(Dispatchers.IO).launch {
            authService.logout()
            _authStateLiveData.postValue(AuthState(AuthState.Status.UNAUTHENTICATED, "已退出登录"))
        }
    }

    /**
     * 获取认证状态LiveData
     */
    fun getAuthStateLiveData(): LiveData<AuthState> = authStateLiveData

    /**
     * 获取认证信息
     */
    suspend fun getAuthInfo(): AuthInfo = authService.getAuthInfo()

    /**
     * 检查是否已认证
     */
    suspend fun isAuthenticated(): Boolean = authService.isAuthenticated()

    /**
     * 获取访问令牌
     */
    suspend fun getAccessToken(): String = authService.getAccessToken()

    /**
     * 认证状态数据类
     *
     * @property status 认证状态
     * @property message 状态消息
     * @property data 附加数据（如DeviceCodeResponse）
     */
    data class AuthState(
        val status: Status,
        val message: String,
        val data: Any? = null
    ) {
        constructor(status: Status, message: String) : this(status, message, null)
    }

    /**
     * 认证状态枚举
     */
    enum class Status {
        LOADING,                    // 加载中
        DEVICE_CODE_RECEIVED,       // 已获取设备码
        POLLING,                    // 轮询中
        AUTHENTICATED,              // 已认证
        UNAUTHENTICATED,            // 未认证
        REFRESHING,                 // 刷新中
        ERROR                       // 错误
    }
}