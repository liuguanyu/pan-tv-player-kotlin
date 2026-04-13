package com.baidu.tv.player.auth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.baidu.tv.player.config.BaiduConfig
import com.baidu.tv.player.model.AuthInfo
import com.baidu.tv.player.model.DeviceCodeResponse
import com.baidu.tv.player.model.TokenResponse
import com.baidu.tv.player.network.BaiduPanService
import com.baidu.tv.player.network.RetrofitClient
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * AuthService 单元测试
 *
 * 测试设备码流程、token刷新、认证状态管理等功能
 * 使用 MockK 进行 Kotlin 专用模拟
 * 使用 kotlinx-coroutines-test 进行协程测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthServiceTest {

    @MockK
    private lateinit var mockBaiduPanService: BaiduPanService

    @MockK
    private lateinit var mockRetrofitClient: RetrofitClient

    @MockK
    private lateinit var mockDataStoreManager: DataStoreManager

    private lateinit var authService: AuthService
    private lateinit var context: Context
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()
        Dispatchers.setMain(testDispatcher)

        // Mock RetrofitClient 单例
        mockkObject(RetrofitClient)
        every { RetrofitClient.getOAuthInstance() } returns mockRetrofitClient
        every { mockRetrofitClient.create(any()) } returns mockBaiduPanService

        // Mock DataStoreManager 单例
        mockkObject(DataStoreManager)
        every { DataStoreManager.getInstance(any()) } returns mockDataStoreManager

        // Mock DataStoreManager authPreferences
        val mockAuthPreferences = mockk<Preferences>(relaxUnitFun = true)
        every { mockDataStoreManager.authPreferences } returns mockAuthPreferences

        // Create AuthService instance
        authService = AuthService(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(RetrofitClient)
        unmockkObject(DataStoreManager)
    }

    /**
     * 测试：获取设备ID - 新设备
     */
    @Test
    fun testGetDeviceId_NewDevice() {
        // Given: 模拟SharedPreferences没有设备ID
        val mockSharedPreferences = mockk<android.content.SharedPreferences>(relaxUnitFun = true)
        val mockEditor = mockk<android.content.SharedPreferences.Editor>(relaxUnitFun = true)

        every { context.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.getString(any(), any()) } returns null
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs

        // When: 获取设备ID
        val deviceId = authService.getDeviceId()

        // Then: 验证生成了新的UUID并保存
        assertThat(deviceId).isNotNull()
        assertThat(deviceId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        verify(exactly = 1) { mockSharedPreferences.edit() }
        verify(exactly = 1) { mockEditor.putString(any(), any()) }
        verify(exactly = 1) { mockEditor.apply() }
    }

    /**
     * 测试：获取设备ID - 已有设备
     */
    @Test
    fun testGetDeviceId_ExistingDevice() {
        // Given: 模拟SharedPreferences有设备ID
        val mockSharedPreferences = mockk<android.content.SharedPreferences>(relaxUnitFun = true)
        val deviceId = "existing-device-id"

        every { context.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.getString(any(), any()) } returns deviceId

        // When: 获取设备ID
        val resultDeviceId = authService.getDeviceId()

        // Then: 验证返回了已有的设备ID，没有创建新ID
        assertThat(resultDeviceId).isEqualTo(deviceId)
        verify(exactly = 0) { mockSharedPreferences.edit() }
    }

    /**
     * 测试：检查是否已认证 - 已认证
     */
    @Test
    fun testIsAuthenticated_Authenticated() = runTest {
        // Given: 模拟已认证状态
        val authInfo = AuthInfo(
            accessToken = "test_access_token",
            refreshToken = "test_refresh_token",
            expiresAt = System.currentTimeMillis() + 3600000,
            isLoggedIn = true
        )
        coEvery { mockDataStoreManager.authPreferences } returns mockk<Preferences>()
        coEvery { authService.loadAuthInfo() } returns authInfo
        coEvery { mockDataStoreManager.authPreferences[any()] } returns ""

        // When: 检查是否已认证
        val isAuthenticated = authService.isAuthenticated()

        // Then: 验证返回true
        assertThat(isAuthenticated).isTrue()
        coVerify { authService.loadAuthInfo() }
    }

    /**
     * 测试：检查是否已认证 - 未认证
     */
    @Test
    fun testIsAuthenticated_Unauthenticated() = runTest {
        // Given: 模拟未认证状态
        val authInfo = AuthInfo(
            accessToken = "",
            refreshToken = "",
            expiresAt = 0,
            isLoggedIn = false
        )
        coEvery { authService.loadAuthInfo() } returns authInfo

        // When: 检查是否已认证
        val isAuthenticated = authService.isAuthenticated()

        // Then: 验证返回false
        assertThat(isAuthenticated).isFalse()
        coVerify { authService.loadAuthInfo() }
    }

    /**
     * 测试：检查是否已认证 - token过期并成功刷新
     */
    @Test
    fun testIsAuthenticated_TokenExpiredAndRefreshed() = runTest {
        // Given: 模拟token已过期但刷新成功
        val authInfo = AuthInfo(
            accessToken = "test_access_token",
            refreshToken = "test_refresh_token",
            expiresAt = System.currentTimeMillis() - 3600000, // 过期
            isLoggedIn = true
        )
        coEvery { authService.loadAuthInfo() } returns authInfo
        coEvery { authService.refreshTokenInternal() } returns true
        coEvery { authService.loadAuthInfo() } returns AuthInfo(
            accessToken = "new_access_token",
            refreshToken = "test_refresh_token",
            expiresAt = System.currentTimeMillis() + 3600000,
            isLoggedIn = true
        )

        // When: 检查是否已认证
        val isAuthenticated = authService.isAuthenticated()

        // Then: 验证返回true（因为刷新成功）
        assertThat(isAuthenticated).isTrue()
        coVerify(exactly = 2) { authService.loadAuthInfo() }
        coVerify { authService.refreshTokenInternal() }
    }

    /**
     * 测试：检查是否已认证 - token过期但刷新失败
     */
    @Test
    fun testIsAuthenticated_TokenExpiredAndRefreshFailed() = runTest {
        // Given: 模拟token已过期且刷新失败
        val authInfo = AuthInfo(
            accessToken = "test_access_token",
            refreshToken = "test_refresh_token",
            expiresAt = System.currentTimeMillis() - 3600000, // 过期
            isLoggedIn = true
        )
        coEvery { authService.loadAuthInfo() } returns authInfo
        coEvery { authService.refreshTokenInternal() } returns false

        // When: 检查是否已认证
        val isAuthenticated = authService.isAuthenticated()

        // Then: 验证返回false（因为刷新失败）
        assertThat(isAuthenticated).isFalse()
        coVerify { authService.loadAuthInfo() }
        coVerify { authService.refreshTokenInternal() }
    }

    /**
     * 测试：获取访问令牌
     */
    @Test
    fun testGetAccessToken() = runTest {
        // Given: 模拟认证信息
        val authInfo = AuthInfo(
            accessToken = "test_access_token",
            refreshToken = "test_refresh_token",
            expiresAt = System.currentTimeMillis() + 3600000,
            isLoggedIn = true
        )
        coEvery { authService.loadAuthInfo() } returns authInfo

        // When: 获取访问令牌
        val accessToken = authService.getAccessToken()

        // Then: 验证返回正确的令牌
        assertThat(accessToken).isEqualTo("test_access_token")
        coVerify { authService.loadAuthInfo() }
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
        coEvery { authService.loadAuthInfo() } returns expectedAuthInfo

        // When: 获取认证信息
        val authInfo = authService.getAuthInfo()

        // Then: 验证返回正确的认证信息
        assertThat(authInfo).isEqualTo(expectedAuthInfo)
        coVerify { authService.loadAuthInfo() }
    }

    /**
     * 测试：获取设备码 - 成功
     */
    @Test
    fun testGetDeviceCode_Success() = runTest {
        // Given: 模拟成功响应
        val deviceCodeResponse = DeviceCodeResponse(
            deviceCode = "test_device_code",
            userCode = "ABC123",
            verificationUrl = "https://example.com/verify",
            expiresIn = 600,
            interval = 5
        )

        val mockResponse = mockk<retrofit2.Response<DeviceCodeResponse>>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body() } returns deviceCodeResponse
        every { mockBaiduPanService.getDeviceCode(any(), any(), any()) } returns mockResponse

        // When: 获取设备码
        val capturedCallback = slot<AuthService.AuthCallback<DeviceCodeResponse>>()
        authService.getDeviceCode(capture(capturedCallback))

        // Then: 验证回调被正确调用
        assertThat(capturedCallback.isCaptured).isTrue()
        capturedCallback.captured.onSuccess(deviceCodeResponse)
        verify { mockBaiduPanService.getDeviceCode(BaiduConfig.APP_KEY, BaiduConfig.SCOPE, "device_code") }
    }

    /**
     * 测试：获取设备码 - 失败
     */
    @Test
    fun testGetDeviceCode_Failure() = runTest {
        // Given: 模拟失败响应
        val mockResponse = mockk<retrofit2.Response<DeviceCodeResponse>>()
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code() } returns 500
        every { mockBaiduPanService.getDeviceCode(any(), any(), any()) } returns mockResponse

        // When: 获取设备码
        val capturedCallback = slot<AuthService.AuthCallback<DeviceCodeResponse>>()
        authService.getDeviceCode(capture(capturedCallback))

        // Then: 验证回调被正确调用
        assertThat(capturedCallback.isCaptured).isTrue()
        capturedCallback.captured.onError("获取设备码失败: 500")
        verify { mockBaiduPanService.getDeviceCode(BaiduConfig.APP_KEY, BaiduConfig.SCOPE, "device_code") }
    }

    /**
     * 测试：轮询设备码状态 - 授权成功
     */
    @Test
    fun testPollDeviceCodeStatus_AuthorizationSuccess() = runTest {
        // Given: 模拟授权成功
        val deviceCode = "test_device_code"
        val tokenResponse = TokenResponse(
            accessToken = "new_access_token",
            refreshToken = "new_refresh_token",
            expiresIn = 3600,
            scope = "basic",
            sessionKey = "session_key",
            sessionSecret = "session_secret"
        )

        val mockResponse = mockk<retrofit2.Response<TokenResponse>>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body() } returns tokenResponse
        every { mockBaiduPanService.getTokenByDeviceCode(any(), any(), any(), any()) } returns mockResponse

        // When: 轮询设备码状态
        val capturedCallback = slot<AuthService.AuthCallback<Boolean>>()
        authService.pollDeviceCodeStatus(deviceCode, capture(capturedCallback))

        // Then: 验证回调被正确调用
        assertThat(capturedCallback.isCaptured).isTrue()
        capturedCallback.captured.onSuccess(true)
        verify { mockBaiduPanService.getTokenByDeviceCode("device_token", deviceCode, BaiduConfig.APP_KEY, BaiduConfig.SECRET_KEY) }
    }

    /**
     * 测试：轮询设备码状态 - 授权中
     */
    @Test
    fun testPollDeviceCodeStatus_AuthorizationPending() = runTest {
        // Given: 模拟授权中状态
        val deviceCode = "test_device_code"
        val tokenResponse = TokenResponse(
            accessToken = null,
            refreshToken = null,
            expiresIn = 0,
            scope = "",
            sessionKey = "",
            sessionSecret = "",
            error = "authorization_pending",
            errorDescription = "Authorization pending"
        )

        val mockResponse = mockk<retrofit2.Response<TokenResponse>>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body() } returns tokenResponse
        every { mockBaiduPanService.getTokenByDeviceCode(any(), any(), any(), any()) } returns mockResponse

        // When: 轮询设备码状态
        val capturedCallback = slot<AuthService.AuthCallback<Boolean>>()
        authService.pollDeviceCodeStatus(deviceCode, capture(capturedCallback))

        // Then: 验证回调未被调用，应继续轮询
        assertThat(capturedCallback.isCaptured).isFalse()
        verify { mockBaiduPanService.getTokenByDeviceCode("device_token", deviceCode, BaiduConfig.APP_KEY, BaiduConfig.SECRET_KEY) }
    }

    /**
     * 测试：刷新token - 成功
     */
    @Test
    fun testRefreshToken_Success() = runTest {
        // Given: 模拟刷新成功
        val authInfo = AuthInfo(
            accessToken = "old_access_token",
            refreshToken = "old_refresh_token",
            expiresAt = System.currentTimeMillis() + 3600000,
            isLoggedIn = true
        )
        coEvery { authService.loadAuthInfo() } returns authInfo

        val tokenResponse = TokenResponse(
            accessToken = "new_access_token",
            refreshToken = "new_refresh_token",
            expiresIn = 3600,
            scope = "basic",
            sessionKey = "session_key",
            sessionSecret = "session_secret"
        )

        val mockResponse = mockk<retrofit2.Response<TokenResponse>>()
        every { mockResponse.isSuccessful } returns true
        every { mockResponse.body() } returns tokenResponse
        every { mockBaiduPanService.refreshToken(any(), any(), any(), any()) } returns mockResponse

        // When: 刷新token
        val capturedCallback = slot<AuthService.AuthCallback<Boolean>>()
        authService.refreshToken(capture(capturedCallback))

        // Then: 验证回调被正确调用
        assertThat(capturedCallback.isCaptured).isTrue()
        capturedCallback.captured.onSuccess(true)
        verify { mockBaiduPanService.refreshToken("refresh_token", "old_refresh_token", BaiduConfig.APP_KEY, BaiduConfig.SECRET_KEY) }
    }

    /**
     * 测试：刷新token - 失败
     */
    @Test
    fun testRefreshToken_Failure() = runTest {
        // Given: 模拟刷新失败
        val authInfo = AuthInfo(
            accessToken = "old_access_token",
            refreshToken = "old_refresh_token",
            expiresAt = System.currentTimeMillis() + 3600000,
            isLoggedIn = true
        )
        coEvery { authService.loadAuthInfo() } returns authInfo

        val mockResponse = mockk<retrofit2.Response<TokenResponse>>()
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code() } returns 401
        every { mockBaiduPanService.refreshToken(any(), any(), any(), any()) } returns mockResponse

        // When: 刷新token
        val capturedCallback = slot<AuthService.AuthCallback<Boolean>>()
        authService.refreshToken(capture(capturedCallback))

        // Then: 验证回调被正确调用
        assertThat(capturedCallback.isCaptured).isTrue()
        capturedCallback.captured.onError("刷新令牌失败")
        verify { mockBaiduPanService.refreshToken("refresh_token", "old_refresh_token", BaiduConfig.APP_KEY, BaiduConfig.SECRET_KEY) }
    }
