package com.baidu.tv.player.geocoding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.test.core.app.ApplicationProvider
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * LocationCache 单元测试
 *
 * 测试双层缓存（内存+本地）的存取功能
 * 使用 MockK 进行DataStore模拟
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationCacheTest {

    @MockK
    private lateinit var mockDataStore: DataStore<Preferences>

    @MockK
    private lateinit var mockPreferences: Preferences

    private lateinit var context: Context

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        context = ApplicationProvider.getApplicationContext()

        // Mock DataStore
        every { mockDataStore.data } returns flowOf(mockPreferences)
        every { mockDataStore.edit(any()) } answers {
            val block = firstArg<(Preferences) -> Unit>()
            block(mockPreferences)
        }
    }

    @After
    fun tearDown() {
        // 清空缓存
        LocationCache.memoryCache.clear()
    }

    /**
     * 测试：从内存缓存获取地址
     */
    @Test
    fun testGet_FromMemoryCache() = runTest {
        // Given: 内存缓存中存在地址
        val cacheKey = "39.9042,116.4074"
        val address = "北京市朝阳区"
        LocationCache.memoryCache[cacheKey] = LocationCache.CacheEntry(address, System.currentTimeMillis())

        // When: 从缓存获取地址
        val result = LocationCache.get(context, cacheKey)

        // Then: 验证返回内存缓存的地址
        assertThat(result).isEqualTo(address)
    }

    /**
     * 测试：从内存缓存获取过期地址
     */
    @Test
    fun testGet_FromMemoryCache_Expired() = runTest {
        // Given: 内存缓存中存在过期地址
        val cacheKey = "39.9042,116.4074"
        val address = "北京市朝阳区"
        val oldTimestamp = System.currentTimeMillis() - (LocationCache.CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L) - 1000
        LocationCache.memoryCache[cacheKey] = LocationCache.CacheEntry(address, oldTimestamp)

        // When: 从缓存获取地址
        val result = LocationCache.get(context, cacheKey)

        // Then: 验证返回null（地址已过期）
        assertThat(result).isNull()
        // 验证地址已从内存缓存中移除
        assertThat(LocationCache.memoryCache.containsKey(cacheKey)).isFalse()
    }

    /**
     * 测试：从本地缓存获取地址
     */
    @Test
    fun testGet_FromDiskCache() = runTest {
        // Given: 本地缓存中存在地址
        val cacheKey = "39.9042,116.4074"
        val address = "北京市朝阳区"
        val timestamp = System.currentTimeMillis()
        val fullKey = "loc_$cacheKey"

        every { mockPreferences[stringPreferencesKey(fullKey)] } returns address
        every { mockPreferences[longPreferencesKey("${fullKey}_time")] } returns timestamp

        // When: 从缓存获取地址
        val result = LocationCache.get(context, cacheKey)

        // Then: 验证返回本地缓存的地址
        assertThat(result).isEqualTo(address)
        // 验证地址已存入内存缓存
        assertThat(LocationCache.memoryCache[cacheKey]?.value).isEqualTo(address)
    }

    /**
     * 测试：从本地缓存获取过期地址
     */
    @Test
    fun testGet_FromDiskCache_Expired() = runTest {
        // Given: 本地缓存中存在过期地址
        val cacheKey = "39.9042,116.4074"
        val address = "北京市朝阳区"
        val oldTimestamp = System.currentTimeMillis() - (LocationCache.CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L) - 1000
        val fullKey = "loc_$cacheKey"

        every { mockPreferences[stringPreferencesKey(fullKey)] } returns address
        every { mockPreferences[longPreferencesKey("${fullKey}_time")] } returns oldTimestamp

        // When: 从缓存获取地址
        val result = LocationCache.get(context, cacheKey)

        // Then: 验证返回null（地址已过期）
        assertThat(result).isNull()
        // 验证地址已从本地缓存中移除
        verify(exactly = 1) { mockDataStore.edit(any()) }
    }

    /**
     * 测试：保存地址到双层缓存
     */
    @Test
    fun testPut_ToBothCaches() = runTest {
        // Given: 缓存键和地址
        val cacheKey = "39.9042,116.4074"
        val address = "北京市朝阳区"
        val fullKey = "loc_$cacheKey"

        // When: 保存地址
        LocationCache.put(context, cacheKey, address)

        // Then: 验证地址已存入内存缓存
        assertThat(LocationCache.memoryCache[cacheKey]?.value).isEqualTo(address)
        // 验证地址已存入本地缓存
        verify(exactly = 1) { mockDataStore.edit(any()) }
    }

    /**
     * 测试：内存缓存达到最大容量时的LRU清理
     */
    @Test
    fun testPut_MemoryCacheFull_LRU() = runTest {
        // Given: 内存缓存已满
        val cacheKeys = (1..LocationCache.MAX_MEMORY_CACHE_SIZE).map { "${39.9042 + it},${116.4074 + it}" }
        cacheKeys.forEach { key ->
            LocationCache.memoryCache[key] = LocationCache.CacheEntry("Address $key", System.currentTimeMillis())
        }

        val newCacheKey = "39.9042,116.4074"
        val newAddress = "北京市朝阳区"

        // When: 添加新地址
        LocationCache.put(context, newCacheKey, newAddress)

        // Then: 验证第一个键被移除
        assertThat(LocationCache.memoryCache.containsKey(cacheKeys.first())).isFalse()
        // 验证新地址被添加
        assertThat(LocationCache.memoryCache.containsKey(newCacheKey)).isTrue()
    }

    /**
     * 测试：清除所有缓存
     */
    @Test
    fun testClearAll() = runTest {
        // Given: 内存和本地缓存都有数据
        val cacheKey = "39.9042,116.4074"
        val address = "北京市朝阳区"
        LocationCache.memoryCache[cacheKey] = LocationCache.CacheEntry(address, System.currentTimeMillis())
        val fullKey = "loc_$cacheKey"
        every { mockPreferences[stringPreferencesKey(fullKey)] } returns address
        every { mockPreferences[longPreferencesKey("${fullKey}_time")] } returns System.currentTimeMillis()

        // When: 清除所有缓存
        LocationCache.clearAll(context)

        // Then: 验证内存缓存被清空
        assertThat(LocationCache.memoryCache.isEmpty()).isTrue()
        // 验证本地缓存被清空
        verify(exactly = 1) { mockDataStore.edit { it.clear() } }
    }

    /**
     * 测试：获取缓存统计信息
     */
    @Test
    fun testGetCacheStats() {
        // Given: 内存缓存中有数据
        val cacheKey = "39.9042,116.4074"
        val address = "北京市朝阳区"
        LocationCache.memoryCache[cacheKey] = LocationCache.CacheEntry(address, System.currentTimeMillis())

        // When: 获取缓存统计信息
        val stats = LocationCache.getCacheStats()

        // Then: 验证返回正确的统计信息
        assertThat(stats).isEqualTo("内存缓存: 1/1000")
    }
}
