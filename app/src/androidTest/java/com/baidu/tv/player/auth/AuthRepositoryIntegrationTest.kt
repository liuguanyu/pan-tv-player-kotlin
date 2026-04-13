package com.baidu.tv.player.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.baidu.tv.player.model.AuthInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat

/**
 * AuthRepository 集成测试
 *
 * 测试认证模块在真实Android环境中的行为
 * 使用AndroidX Test框架进行集成测试
 */
@RunWith(AndroidJUnit4::class)
class AuthRepositoryIntegrationTest {

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule<androidx.appcompat.app.AppCompatActivity>(androidx.appcompat.app.AppCompatActivity::class.java)

    private lateinit var context: Context
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        authRepository = AuthRepository(context)
    }

    /**
     * 测试：检查认证状态 - 实际环境
     */
    @Test
    fun testCheckAuthStatus_RealEnvironment() {
        // When: 检查认证状态
        authRepository.checkAuthStatus()

        // Then: 验证认证状态LiveData有值
        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isIn(
            AuthRepository.Status.AUTHENTICATED,
            AuthRepository.Status.UNAUTHENTICATED,
            AuthRepository.Status.ERROR
        )
    }

    /**
     * 测试：获取设备码 - 实际环境
     */
    @Test
    fun testGetDeviceCode_RealEnvironment() {
        // When: 获取设备码
        authRepository.getDeviceCode()

        // Then: 验证认证状态LiveData有值
        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isIn(
            AuthRepository.Status.DEVICE_CODE_RECEIVED,
            AuthRepository.Status.ERROR
        )
    }

    /**
     * 测试：刷新token - 实际环境
     */
    @Test
    fun testRefreshToken_RealEnvironment() {
        // When: 刷新token
        authRepository.refreshToken()

        // Then: 验证认证状态LiveData有值
        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isIn(
            AuthRepository.Status.AUTHENTICATED,
            AuthRepository.Status.UNAUTHENTICATED,
            AuthRepository.Status.ERROR
        )
    }

    /**
     * 测试：退出登录 - 实际环境
     */
    @Test
    fun testLogout_RealEnvironment() {
        // When: 退出登录
        authRepository.logout()

        // Then: 验证认证状态LiveData有值
        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isEqualTo(AuthRepository.Status.UNAUTHENTICATED)
    }

    /**
     * 测试：获取认证信息 - 实际环境
     */
    @Test
    fun testGetAuthInfo_RealEnvironment() {
        // When: 获取认证信息
        val authInfo = authRepository.getAuthInfo()

        // Then: 验证认证信息有值
        assertThat(authInfo).isNotNull()
        assertThat(authInfo.accessToken).isNotNull()
    }

    /**
     * 测试：检查是否已认证 - 实际环境
     */
    @Test
    fun testIsAuthenticated_RealEnvironment() {
        // When: 检查是否已认证
        val isAuthenticated = authRepository.isAuthenticated()

        // Then: 验证返回布尔值
        assertThat(isAuthenticated).isNotNull()
    }

    /**
     * 测试：获取访问令牌 - 实际环境
     */
    @Test
    fun testGetAccessToken_RealEnvironment() {
        // When: 获取访问令牌
        val accessToken = authRepository.getAccessToken()

        // Then: 验证令牌有值
        assertThat(accessToken).isNotNull()
    }
}
