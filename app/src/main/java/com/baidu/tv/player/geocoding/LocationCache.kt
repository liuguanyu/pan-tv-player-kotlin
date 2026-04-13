package com.baidu.tv.player.geocoding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.baidu.tv.player.utils.LocationUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 双层缓存管理器
 *
 * 实现内存缓存 + 本地持久化缓存的双层缓存策略
 * 内存缓存：ConcurrentHashMap，用于快速访问
 * 本地缓存：DataStore，用于持久化存储
 *
 * 缓存过期时间：30天
 * 缓存大小限制：内存1000条，本地5000条
 */
object LocationCache {
    // 内存缓存（ConcurrentHashMap）
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()
    private const val MAX_MEMORY_CACHE_SIZE = 1000

    // 本地缓存（DataStore）
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "location_cache")
    private const val CACHE_KEY_PREFIX = "loc_"
    private const val MAX_DISK_CACHE_SIZE = 5000
    private const val CACHE_EXPIRY_DAYS = 30L // 缓存30天后过期

    /**
     * 缓存条目
     */
    private data class CacheEntry(
        val value: String,
        val timestamp: Long
    )

    /**
     * 从缓存中获取地址信息
     *
     * 按顺序检查内存缓存和本地缓存
     *
     * @param context 上下文
     * @param cacheKey 缓存键（格式：纬度,经度）
     * @return 缓存的地址信息，如果未找到或过期则返回null
     */
    suspend fun get(context: Context, cacheKey: String): String? {
        // L1: 检查内存缓存
        val memoryEntry = memoryCache[cacheKey]
        if (memoryEntry != null) {
            // 检查是否过期
            val currentTime = System.currentTimeMillis()
            val expiryTime = CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L

            if (currentTime - memoryEntry.timestamp <= expiryTime) {
                return memoryEntry.value
            } else {
                // 过期，从内存中移除
                memoryCache.remove(cacheKey)
                return null
            }
        }

        // L2: 检查本地缓存
        return loadFromDiskCache(context, cacheKey)
    }

    /**
     * 将地址信息保存到双层缓存
     *
     * @param context 上下文
     * @param cacheKey 缓存键（格式：纬度,经度）
     * @param value 地址信息
     */
    suspend fun put(context: Context, cacheKey: String, value: String) {
        // L1: 保存到内存缓存
        if (memoryCache.size >= MAX_MEMORY_CACHE_SIZE) {
            // 简单的LRU：移除第一个元素
            val firstKey = memoryCache.keys.first()
            memoryCache.remove(firstKey)
        }
        memoryCache[cacheKey] = CacheEntry(value, System.currentTimeMillis())

        // L2: 保存到本地缓存
        saveToDiskCache(context, cacheKey, value)
    }

    /**
     * 从本地缓存加载地址信息
     *
     * @param context 上下文
     * @param cacheKey 缓存键
     * @return 缓存的地址信息，如果未找到或过期则返回null
     */
    private suspend fun loadFromDiskCache(context: Context, cacheKey: String): String? {
        try {
            val fullKey = "$CACHE_KEY_PREFIX$cacheKey"

            // 读取地址值
            val location = context.dataStore.data.map { preferences ->
                preferences[stringPreferencesKey(fullKey)]
            }.first()

            if (location != null) {
                // 检查是否过期
                val timestamp = context.dataStore.data.map { preferences ->
                    preferences[longPreferencesKey("${fullKey}_time")]
                }.first()

                if (timestamp != null) {
                    val currentTime = System.currentTimeMillis()
                    val expiryTime = CACHE_EXPIRY_DAYS * 24 * 60 * 60 * 1000L

                    if (currentTime - timestamp > expiryTime) {
                        // 过期，从本地缓存中移除
                        context.dataStore.edit { preferences ->
                            preferences.remove(stringPreferencesKey(fullKey))
                            preferences.remove(longPreferencesKey("${fullKey}_time"))
                        }
                        return null
                    }

                    return location
                }
            }
        } catch (e: Exception) {
            // 如果读取失败，返回null
            return null
        }

        return null
    }

    /**
     * 保存到本地缓存
     *
     * @param context 上下文
     * @param cacheKey 缓存键
     * @param value 地址信息
     */
    private suspend fun saveToDiskCache(context: Context, cacheKey: String, value: String) {
        try {
            val fullKey = "$CACHE_KEY_PREFIX$cacheKey"

            // 检查本地缓存大小
            val currentSize = context.dataStore.data.map { preferences ->
                preferences.keys.count()
            }.first() / 2 // 每个缓存项占用2个key（数据+时间戳）

            if (currentSize >= MAX_DISK_CACHE_SIZE) {
                cleanupOldDiskCache(context)
            }

            // 保存缓存数据和时间戳
            context.dataStore.edit { preferences ->
                preferences[stringPreferencesKey(fullKey)] = value
                preferences[longPreferencesKey("${fullKey}_time")] = System.currentTimeMillis()
            }

        } catch (e: Exception) {
            // 如果写入失败，忽略错误
        }
    }

    /**
     * 清理旧的本地缓存（LRU策略）
     *
     * 移除最旧的20%缓存项
     *
     * @param context 上下文
     */
    private suspend fun cleanupOldDiskCache(context: Context) {
        try {
            // 获取所有缓存项
            val allPreferences = context.dataStore.data.map { preferences ->
                preferences.toList()
            }.first()

            // 收集所有缓存项的时间戳
            val entries = mutableListOf<Pair<String, Long>>()
            for ((key, value) in allPreferences) {
                if (key.name.startsWith(CACHE_KEY_PREFIX) && key.name.endsWith("_time")) {
                    val dataKey = key.name.substring(0, key.name.length - 5) // 移除"_time"后缀
                    if (value is Long) {
                        entries.add(Pair(dataKey, value))
                    }
                }
            }

            // 按时间戳排序（最旧的在前）
            entries.sortBy { it.second }

            // 删除最旧的20%缓存
            val toRemove = (entries.size * 0.2).toInt().coerceAtLeast(1)
            for (i in 0 until toRemove) {
                val key = entries[i].first
                context.dataStore.edit { preferences ->
                    preferences.remove(stringPreferencesKey(key))
                    preferences.remove(longPreferencesKey("${key}_time"))
                }
            }

        } catch (e: Exception) {
            // 如果清理失败，忽略错误
        }
    }

    /**
     * 清空所有缓存
     *
     * @param context 上下文
     */
    suspend fun clearAll(context: Context) {
        // 清空内存缓存
        memoryCache.clear()

        // 清空本地缓存
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息字符串
     */
    fun getCacheStats(): String {
        val memorySize = memoryCache.size
        return "内存缓存: $memorySize/$MAX_MEMORY_CACHE_SIZE"
    }
}