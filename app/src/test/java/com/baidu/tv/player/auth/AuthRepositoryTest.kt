package com.baidu.tv.player.auth

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.baidu.tv.player.model.AuthInfo
import com.baidu.tv.player.model.DeviceCodeResponse
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * AuthRepository 单元测试
 *
 * 测试认证状态管理、登录流程、token存储/读取等功能
 * 使用 MockK 进行 Kotlin 专用模拟
 * 使用 kotlinx-coroutines-test 进行协程测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var mockAuthService: AuthService

    private lateinit var authRepository: AuthRepository
    private lateinit var context: Context
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()
        Dispatchers.setMain(testDispatcher)

        // Mock AuthService 单例
        mockkObject(AuthService)
        every { AuthService.getInstance(any()) } returns mockAuthService

        authRepository = AuthRepository(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(AuthService)
    }

    /**
     * 测试：检查认证状态 - 已认证
     */
    @Test
    fun testCheckAuthStatus_Authenticated() = runTest {
        // Given: 模拟已认证状态
        coEvery { mockAuthService.isAuthenticated() } returns true

        // When: 检查认证状态
        authRepository.checkAuthStatus()

        // Then: 验证状态已更新为已认证
        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isEqualTo(AuthRepository.Status.AUTHENTICATED)
        assertThat(authState.message).isEqualTo("已认证")

        coVerify { mockAuthService.isAuthenticated() }
    }

    /**
     * 测试：检查认证状态 - 未认证
     */
    @Test
    fun testCheckAuthStatus_Unauthenticated() = runTest {
        // Given: 模拟未认证状态
        coEvery { mockAuthService.isAuthenticated() } returns false

        // When: 检查认证状态
        authRepository.checkAuthStatus()

        // Then: 验证状态已更新为未认证
        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isEqualTo(AuthRepository.Status.UNAUTHENTICATED)
        assertThat(authState.message).isEqualTo("未认证")

        coVerify { mockAuthService.isAuthenticated() }
    }

    /**
     * 测试：获取设备码 - 成功
     */
    @Test
    fun testGetDeviceCode_Success() = runTest {
        // Given: 模拟设备码响应
        val deviceCodeResponse = DeviceCodeResponse(
            deviceCode = "test_device_code",
            userCode = "ABC123",
            verificationUrl = "https://example.com/verify",
            expiresIn = 600,
            interval = 5
        )

        every { mockAuthService.getDeviceCode(any()) } answers {
            val callback = firstArg<AuthService.AuthCallback<DeviceCodeResponse>>()
            callback.onSuccess(deviceCodeResponse)
        }

        // When: 获取设备码
        authRepository.getDeviceCode()

        // Then: 验证状态已更新为设备码已接收
        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isEqualTo(AuthRepository.Status.DEVICE_CODE_RECEIVED)
        assertThat(authState.message).isEqualTo("设备码获取成功")
        assertThat(authState.data).isEqualTo(deviceCodeResponse)

        verify { mockAuthService.getDeviceCode(any()) }
    }

    /**
     * 测试：获取设备码 - 失败
     */
    @Test
    fun testGetDeviceCode_Error() = runTest {
        // Given: 模拟获取设备码失败
        every { mockAuthService.getDeviceCode(any()) } answers {
            val callback = firstArg<AuthService.AuthCallback<DeviceCodeResponse>>()
            callback.onError("网络请求失败")
        }

        // When: 获取设备码
        authRepository.getDeviceCode()

        // Then: 验证状态已更新为错误
        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isEqualTo(AuthRepository.Status.ERROR)
        assertThat(authState.message).isEqualTo("网络请求失败")

        verify { mockAuthService.getDeviceCode(any()) }
    }

    /**
     * 测试：轮询设备码状态 - 成功
     */
    @Test
    fun testPollDeviceCodeStatus_Success() = runTest {
        // Given: 模拟轮询成功
        val deviceCode = "test_device_code"
        every { mockAuthService.pollDeviceCodeStatus(deviceCode, any()) } answers {
            val callback = secondArg<AuthService.AuthCallback<Boolean>>()
            callback.onSuccess(true)
        }

        // When: 轮询设备码状态
        authRepository.pollDeviceCodeStatus(deviceCode)

        // Then: 验证状态已更新为已认证
        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isEqualTo(AuthRepository.Status.AUTHENTICATED)
        assertThat(authState.message).isEqualTo("授权成功")

        verify { mockAuthService.pollDeviceCodeStatus(deviceCode, any()) }
    }

    /**
     * 测试：轮询设备码状态 - 失败
     */
    @Test
    fun testPollDeviceCodeStatus_Error() = runTest {
        // Given: 模拟轮询失败
        val deviceCode = "test_device_code"
        every { mockAuthService.pollDeviceCodeStatus(deviceCode, any()) } answers {
            val callback = secondArg<AuthService.AuthCallback<Boolean>>()
            callback.onError("授权超时")
        }

        // When: 轮询设备码状态
        authRepository.pollDeviceCodeStatus(deviceCode)

        // Then: 验证状态已更新为错误
        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isEqualTo(AuthRepository.Status.ERROR)
        assertThat(authState.message).isEqualTo("授权超时")

        verify { mockAuthService.pollDeviceCodeStatus(deviceCode, any()) }
    }

    /**
     * 测试：刷新token - 成功
     */
    @Test
    fun testRefreshToken_Success() = runTest {
        // Given: 模拟刷新token成功
        every { mockAuthService.refreshToken(any()) } answers {
            val callback = firstArg<AuthService.AuthCallback<Boolean>>()
            callback.onSuccess(true)
        }

        // When: 刷新token
        authRepository.refreshToken()

        // Then: 验证状态已更新为已认证
        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isEqualTo(AuthRepository.Status.AUTHENTICATED)
        assertThat(authState.message).isEqualTo("令牌刷新成功")

        verify { mockAuthService.refreshToken(any()) }
    }

    /**
     * 测试：刷新token - 失败
     */
    @Test
    fun testRefreshToken_Error() = runTest {
        // Given: 模拟刷新token失败
        every { mockAuthService.refreshToken(any()) } answers {
            val callback = firstArg<AuthService.AuthCallback<Boolean>>()
            callback.onError("刷新令牌失败")
        }

        // When: 刷新token
        authRepository.refreshToken()

        // Then: 验证状态已更新为未认证
        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isEqualTo(AuthRepository.Status.UNAUTHENTICATED)
        assertThat(authState.message).isEqualTo("令牌刷新失败")

        verify { mockAuthService.refreshToken(any()) }
    }

    /**
     * 测试：退出登录
     */
    @Test
    fun testLogout() = runTest {
        // Given: 模拟logout方法
        coEvery { mockAuthService.logout() } just Runs

        // When: 退出登录
        authRepository.logout()

        // Then: 验证logout被调用，状态更新为未认证
        coVerify { mockAuthService.logout() }

        val authState = authRepository.authStateLiveData.value
        assertThat(authState).isNotNull()
        assertThat(authState!!.status).isEqualTo(AuthRepository.Status.UNAUTHENTICATED)
        assertThat(authState.message).isEqualTo("已退出登录")
    }

    /**
     * 测试：获取认证信息
     */
    @Test
    fun testGetAuthInfo() = runTest {
        // Given: 模拟认证信息
        val expectedAuthInfo = AuthInfo(
            accessToken = "test_access_token",
            refreshToken = "test_refresh_token",
            expiresAt = System.currentTimeMillis() + 3600000,
            isLoggedIn = true
        )
        coEvery { mockAuthService.getAuthInfo() } returns expectedAuthInfo

        // When: 获取认证信息
        val authInfo = authRepository.getAuthInfo()

        // Then: 验证返回正确的认证信息
        assertThat(authInfo).isEqualTo(expectedAuthInfo)
        coVerify { mockAuthService.getAuthInfo() }
    }

    /**
     * 测试：检查是否已认证
     */
    @Test
    fun testIsAuthenticated() = runTest {
        // Given: 模拟已认证
        coEvery { mockAuthService.isAuthenticated() } returns true

        // When: 检查是否已认证
        val isAuthenticated = authRepository.isAuthenticated()

        // Then: 验证返回true
        assertThat(isAuthenticated).isTrue()
        coVerify { mockAuthService.isAuthenticated() }
    }

    /**
     * 测试：获取访问令牌
     */
    @Test
    fun testGetAccessToken() = runTest {
        // Given: 模拟访问令牌
        val expectedToken = "test_access_token"
        coEvery { mockAuthService.getAccessToken() } returns expectedToken

        // When: 获取访问令牌
        val accessToken = authRepository.getAccessToken()

        // Then: 验证返回正确的令牌
        assertThat(accessToken).isEqualTo(expectedToken)
        coVerify { mockAuthService.getAccessToken() }
    }
}
