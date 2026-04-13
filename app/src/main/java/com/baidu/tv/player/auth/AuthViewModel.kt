package com.baidu.tv.player.auth

import android.app.Application
import androidx.annotation.NonNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.baidu.tv.player.model.AuthInfo

/**
 * 认证视图模型
 *
 * 为UI层提供认证相关的数据和操作
 * 使用ViewModel模式管理生命周期
 *
 * @property application Android应用实例
 */
class AuthViewModel(@NonNull application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    /**
     * 获取认证状态LiveData
     */
    fun getAuthState(): LiveData<AuthRepository.AuthState> = authRepository.getAuthStateLiveData()

    /**
     * 开始登录流程（获取设备码）
     */
    fun startLogin() {
        authRepository.getDeviceCode()
    }

    /**
     * 开始轮询设备码状态
     *
     * @param deviceCode 设备码
     */
    fun startPolling(deviceCode: String) {
        authRepository.pollDeviceCodeStatus(deviceCode)
    }

    /**
     * 退出登录
     */
    fun logout() {
        authRepository.logout()
    }

    /**
     * 刷新token
     */
    fun refreshToken() {
        authRepository.refreshToken()
    }

    /**
     * 检查是否已认证
     */
    suspend fun isAuthenticated(): Boolean = authRepository.isAuthenticated()

    /**
     * 获取认证信息
     */
    suspend fun getAuthInfo(): AuthInfo = authRepository.getAuthInfo()

    /**
     * 获取访问令牌
     */
    suspend fun getAccessToken(): String = authRepository.getAccessToken()

    /**
     * 手动检查认证状态
     */
    fun checkAuthStatus() {
        authRepository.checkAuthStatus()
    }
}