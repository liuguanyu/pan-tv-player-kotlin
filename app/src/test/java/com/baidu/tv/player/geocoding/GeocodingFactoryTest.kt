package com.baidu.tv.player.geocoding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * GeocodingFactory 单元测试
 *
 * 测试逆地理编码策略优先级选择功能
 * 使用 MockK 进行策略模拟
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GeocodingFactoryTest {

    @MockK
    private lateinit var mockAmapStrategy: AmapGeocodingStrategy

    @MockK
    private lateinit var mockAndroidStrategy: AndroidGeocoderStrategy

    @MockK
    private lateinit var mockNominatimStrategy: NominatimGeocodingStrategy

    private lateinit var context: Context

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()

        // Mock the strategies
        every { mockAmapStrategy.getAddress(any(), any(), any()) } returns null
        every { mockAndroidStrategy.getAddress(any(), any(), any()) } returns null
        every { mockNominatimStrategy.getAddress(any(), any(), any()) } returns null
    }

    /**
     * 测试：获取地址 - 高德地图成功
     */
    @Test
    fun testGetAddress_AmapSuccess() = runTest {
        // Given: 高德地图策略成功返回地址
        val address = "北京市朝阳区"
        every { mockAmapStrategy.getAddress(any(), any(), any()) } returns address

        // When: 获取地址
        val result = GeocodingFactory.getAddress(context, 39.9042, 116.4074)

        // Then: 验证返回高德地图的结果
        assertThat(result).isEqualTo(address)
        verify(exactly = 1) { mockAmapStrategy.getAddress(any(), any(), any()) }
        verify(exactly = 0) { mockAndroidStrategy.getAddress(any(), any(), any()) }
        verify(exactly = 0) { mockNominatimStrategy.getAddress(any(), any(), any()) }
    }

    /**
     * 测试：获取地址 - 高德地图失败，Android Geocoder成功
     */
    @Test
    fun testGetAddress_AmapFail_AndroidSuccess() = runTest {
        // Given: 高德地图失败，Android Geocoder成功
        val address = "北京市朝阳区"
        every { mockAmapStrategy.getAddress(any(), any(), any()) } returns null
        every { mockAndroidStrategy.getAddress(any(), any(), any()) } returns address

        // When: 获取地址
        val result = GeocodingFactory.getAddress(context, 39.9042, 116.4074)

        // Then: 验证返回Android Geocoder的结果
        assertThat(result).isEqualTo(address)
        verify(exactly = 1) { mockAmapStrategy.getAddress(any(), any(), any()) }
        verify(exactly = 1) { mockAndroidStrategy.getAddress(any(), any(), any()) }
        verify(exactly = 0) { mockNominatimStrategy.getAddress(any(), any(), any()) }
    }

    /**
     * 测试：获取地址 - 高德地图和Android Geocoder失败，Nominatim成功
     */
    @Test
    fun testGetAddress_AmapAndroidFail_NominatimSuccess() = runTest {
        // Given: 高德地图和Android Geocoder失败，Nominatim成功
        val address = "北京市朝阳区"
        every { mockAmapStrategy.getAddress(any(), any(), any()) } returns null
        every { mockAndroidStrategy.getAddress(any(), any(), any()) } returns null
        every { mockNominatimStrategy.getAddress(any(), any(), any()) } returns address

        // When: 获取地址
        val result = GeocodingFactory.getAddress(context, 39.9042, 116.4074)

        // Then: 验证返回Nominatim的结果
        assertThat(result).isEqualTo(address)
        verify(exactly = 1) { mockAmapStrategy.getAddress(any(), any(), any()) }
        verify(exactly = 1) { mockAndroidStrategy.getAddress(any(), any(), any()) }
        verify(exactly = 1) { mockNominatimStrategy.getAddress(any(), any(), any()) }
    }

    /**
     * 测试：获取地址 - 所有策略都失败
     */
    @Test
    fun testGetAddress_AllFail() = runTest {
        // Given: 所有策略都失败
        every { mockAmapStrategy.getAddress(any(), any(), any()) } returns null
        every { mockAndroidStrategy.getAddress(any(), any(), any()) } returns null
        every { mockNominatimStrategy.getAddress(any(), any(), any()) } returns null

        // When: 获取地址
        val result = GeocodingFactory.getAddress(context, 39.9042, 116.4074)

        // Then: 验证返回null
        assertThat(result).isNull()
        verify(exactly = 1) { mockAmapStrategy.getAddress(any(), any(), any()) }
        verify(exactly = 1) { mockAndroidStrategy.getAddress(any(), any(), any()) }
        verify(exactly = 1) { mockNominatimStrategy.getAddress(any(), any(), any()) }
    }

    /**
     * 测试：获取地址 - 高德地图抛出异常，Android Geocoder成功
     */
    @Test
    fun testGetAddress_AmapException_AndroidSuccess() = runTest {
        // Given: 高德地图抛出异常，Android Geocoder成功
        val address = "北京市朝阳区"
        every { mockAmapStrategy.getAddress(any(), any(), any()) } throws Exception("Network error")
        every { mockAndroidStrategy.getAddress(any(), any(), any()) } returns address

        // When: 获取地址
        val result = GeocodingFactory.getAddress(context, 39.9042, 116.4074)

        // Then: 验证返回Android Geocoder的结果
        assertThat(result).isEqualTo(address)
        verify(exactly = 1) { mockAmapStrategy.getAddress(any(), any(), any()) }
        verify(exactly = 1) { mockAndroidStrategy.getAddress(any(), any(), any()) }
        verify(exactly = 0) { mockNominatimStrategy.getAddress(any(), any(), any()) }
    }

    /**
     * 测试：获取所有策略名称
     */
    @Test
    fun testGetStrategyNames() {
        // When: 获取策略名称
        val strategyNames = GeocodingFactory.getStrategyNames()

        // Then: 验证返回正确的策略名称
        assertThat(strategyNames).hasSize(3)
        assertThat(strategyNames[0]).isEqualTo("AmapGeocodingStrategy")
        assertThat(strategyNames[1]).isEqualTo("AndroidGeocoderStrategy")
        assertThat(strategyNames[2]).isEqualTo("NominatimGeocodingStrategy")
    }

    /**
     * 测试：测试所有策略的可用性
     */
    @Test
    fun testTestAllStrategies() = runTest {
        // Given: 设置不同策略的返回值
        val address1 = "北京市朝阳区"
        val address2 = "北京市海淀区"
        val address3 = "北京市东城区"
        every { mockAmapStrategy.getAddress(any(), any(), any()) } returns address1
        every { mockAndroidStrategy.getAddress(any(), any(), any()) } returns address2
        every { mockNominatimStrategy.getAddress(any(), any(), any()) } returns address3

        // When: 测试所有策略
        val results = GeocodingFactory.testAllStrategies(context, 39.9042, 116.4074)

        // Then: 验证返回所有策略的结果
        assertThat(results["AmapGeocodingStrategy"]).isEqualTo(address1)
        assertThat(results["AndroidGeocoderStrategy"]).isEqualTo(address2)
        assertThat(results["NominatimGeocodingStrategy"]).isEqualTo(address3)
    }
}
