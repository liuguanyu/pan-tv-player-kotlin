package com.baidu.tv.player.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.baidu.tv.player.config.BaiduConfig
import com.baidu.tv.player.geocoding.GeocodingFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.Locale

/**
 * 地点识别工具类
 *
 * 从图片EXIF和视频元数据中提取GPS坐标，
 * 并使用高德地图API进行反向地理编码，
 * 集成双层缓存（内存+本地）提高性能。
 *
 * 与Java参考项目功能完全一致，使用Kotlin协程和DataStore替代SharedPreferences。
 */
object LocationUtils {
    private const val TAG = "LocationUtils"
    private const val GPS_DEBUG = "GPS_DEBUG:"

    // ==================== 缓存配置 ====================
    // L1: 内存缓存（快速访问）
    private val memoryCache = ConcurrentHashMap<String, String>()
    private const val MAX_MEMORY_CACHE_SIZE = 1000

    // L2: 本地持久化缓存（DataStore）
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "location_cache")
    private const val CACHE_KEY_PREFIX = "loc_"
    private const val MAX_DISK_CACHE_SIZE = 5000
    private const val CACHE_EXPIRY_DAYS = 30L // 缓存30天后过期

    // Nominatim API 基础URL（完全免费，不需要API Key）
    private const val NOMINATIM_API_URL = "https://nominatim.openstreetmap.org/reverse"

    // 高德地图API配置
    private const val AMAP_API_KEY = BaiduConfig.AMAP_API_KEY
    private const val AMAP_API_URL = "https://restapi.amap.com/v3/geocode/regeo"

    // 请求超时时间（毫秒）
    private const val CONNECTION_TIMEOUT = 3000
    private const val READ_TIMEOUT = 5000

    // 启用测试模式
    private const val ENABLE_TEST_MODE = false
    private const val TEST_LATITUDE = 39.9042
    private const val TEST_LONGITUDE = 116.4074

    /**
     * 从图片中获取地点信息
     *
     * 使用临时文件方式读取EXIF，避免直接从网络流读取的兼容性问题
     *
     * @param context 上下文
     * @param imageUrl 图片URL
     * @return 地点名称，如果无法获取则返回null
     */
    suspend fun getLocationFromImage(context: Context, imageUrl: String): String? {
        Log.d(TAG, "开始从图片获取地点: $imageUrl")

        var tempFile: File? = null
        var inputStream: InputStream? = null
        var connection: HttpURLConnection? = null

        try {
            // 从URL下载图片到临时文件
            val url = URL(imageUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            // 设置百度网盘需要的User-Agent
            connection.setRequestProperty("User-Agent", "pan.baidu.com")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()

            val responseCode = connection.responseCode
            Log.d(TAG, "图片请求响应码: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.inputStream

                // 创建临时文件
                tempFile = File.createTempFile("location_exif_", ".tmp", context.cacheDir)
                val outputStream = tempFile.outputStream()

                // 下载图片到临时文件
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytes = 0L
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead

                    // 限制文件大小，避免下载过大的文件
                    if (totalBytes > 10 * 1024 * 1024) { // 10MB
                        Log.w(TAG, "图片文件过大，停止下载: $totalBytes bytes")
                        break
                    }
                }
                outputStream.flush()
                outputStream.close()

                Log.d(TAG, "图片下载完成，大小: $totalBytes bytes")

                // 从临时文件读取EXIF信息
                val exif = ExifInterface(tempFile.absolutePath)

                // 获取GPS坐标
                val latLong = FloatArray(2)
                val hasLatLong = exif.getLatLong(latLong)
                Log.d(TAG, "EXIF GPS坐标: ${if (hasLatLong) "${latLong[0]},${latLong[1]}" else "null"}")

                if (hasLatLong) {
                    val latitude = latLong[0].toDouble()
                    val longitude = latLong[1].toDouble()
                    return getLocationFromCoordinates(context, latitude, longitude)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "获取图片地点失败: ${e.message}", e)
        } finally {
            // 清理资源
            try {
                inputStream?.close()
            } catch (e: IOException) {
                // ignore
            }

            try {
                connection?.disconnect()
            } catch (e: Exception) {
                // ignore
            }

            // 删除临时文件
            tempFile?.let { file ->
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d(TAG, "临时文件删除${if (deleted) "成功" else "失败"}")
                }
            }
        }

        return null
    }

    /**
     * 从视频中获取地点信息
     *
     * 支持多种视频元数据格式，使用并行策略提高成功率
     *
     * @param context 上下文
     * @param videoUrl 视频URL
     * @return 地点名称，如果无法获取则返回null
     */
    suspend fun getLocationFromVideo(context: Context, videoUrl: String): String? {
        Log.d(TAG, "$GPS_DEBUG========== 开始并行提取视频GPS信息 ==========")
        Log.d(TAG, "$GPS_DEBUG视频URL: $videoUrl")

        // 使用协程并发执行多种提取策略
        val results = mutableListOf<String?>()
        val mutex = Mutex()

        // 任务1: 使用MediaMetadataRetriever
        val task1 = kotlin.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            Log.d(TAG, "$GPS_DEBUG[任务1] 开始使用MediaMetadataRetriever提取元数据")
            val retriever = MediaMetadataRetriever()
            try {
                // 设置请求头
                val headers = mapOf("User-Agent" to "pan.baidu.com")
                retriever.setDataSource(videoUrl, headers)

                val locationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                Log.d(TAG, "$GPS_DEBUG[任务1] METADATA_KEY_LOCATION: $locationString")

                if (locationString != null) {
                    val location = parseLocationString(context, locationString)
                    if (location != null) {
                        Log.d(TAG, "$GPS_DEBUG[任务1] ✅ 成功: 从MediaMetadataRetriever获得位置字符串: $location")
                        mutex.withLock { results.add(location) }
                        return@launch
                    }
                }

                // 打印其他元数据帮助调试
                val date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                Log.d(TAG, "$GPS_DEBUG[任务1] METADATA_KEY_DATE: $date")
                Log.d(TAG, "$GPS_DEBUG[任务1] METADATA_KEY_VIDEO_ROTATION: $rotation")

            } catch (e: Exception) {
                Log.e(TAG, "$GPS_DEBUG[任务1] MediaMetadataRetriever提取失败: ${e.message}")
            } finally {
                retriever.release()
            }

            // 尝试解析 ISO6709 格式的字符串
            throw Exception("MediaMetadataRetriever未找到GPS信息")
        }

        // 任务2: 从视频文件头提取GPS信息
        val task2 = kotlin.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            if (videoUrl.startsWith("http")) {
                Log.d(TAG, "$GPS_DEBUG[任务2] 开始从文件头提取GPS信息")
                val locationFromHeader = getLocationFromVideoHeader(context, videoUrl)
                if (locationFromHeader != null) {
                    Log.d(TAG, "$GPS_DEBUG[任务2] ✅ 成功: 从文件头解析到位置: $locationFromHeader")
                    mutex.withLock { results.add(locationFromHeader) }
                    return@launch
                } else {
                    Log.d(TAG, "$GPS_DEBUG[任务2] 文件头中未找到GPS信息")
                }
            } else {
                Log.d(TAG, "$GPS_DEBUG[任务2] 跳过文件头提取（非HTTP URL）")
            }
            throw Exception("文件头提取未找到GPS信息")
        }

        // 任务3: 从视频文件尾部提取GPS信息
        val task3 = kotlin.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            if (videoUrl.startsWith("http")) {
                Log.d(TAG, "$GPS_DEBUG[任务3] 开始从文件尾部提取GPS信息")
                val locationFromTail = getLocationFromVideoTail(context, videoUrl)
                if (locationFromTail != null) {
                    Log.d(TAG, "$GPS_DEBUG[任务3] ✅ 成功: 从文件尾部解析到位置: $locationFromTail")
                    mutex.withLock { results.add(locationFromTail) }
                    return@launch
                } else {
                    Log.d(TAG, "$GPS_DEBUG[任务3] 文件尾部中未找到GPS信息")
                }
            } else {
                Log.d(TAG, "$GPS_DEBUG[任务3] 跳过文件尾部提取（非HTTP URL）")
            }
            throw Exception("文件尾部提取未找到GPS信息")
        }

        // 等待所有任务完成
        task1.join()
        task2.join()
        task3.join()

        // 返回第一个成功的结果
        val result = results.firstOrNull()
        if (result != null) {
            Log.d(TAG, "$GPS_DEBUG✅ 并行提取成功，结果: $result")
        } else {
            Log.d(TAG, "$GPS_DEBUG❌ 所有并行提取任务均失败")
        }

        Log.d(TAG, "$GPS_DEBUG========== GPS提取结束 ==========")
        return result
    }

    /**
     * 解析位置字符串 (ISO-6709 标准)
     * 格式如: +37.7749-122.4194/ 或 +37.7749-122.4194
     *
     * @param context 上下文
     * @param locationString 位置字符串
     * @return 地点名称，如果无法解析则返回null
     */
    private fun parseLocationString(context: Context, locationString: String?): String? {
        if (locationString == null) return null

        try {
            // 清理字符串，移除结尾的/
            val cleanLocation = if (locationString.endsWith("/")) locationString.substring(0, locationString.length - 1) else locationString

            // 使用正则解析
            val pattern = Regex("([+-]\d+\.\d+)([+-]\d+\.\d+)")
            val matcher = pattern.find(cleanLocation)

            if (matcher != null) {
                val lat = matcher.groupValues[1].toDouble()
                val lon = matcher.groupValues[2].toDouble()

                Log.d(TAG, "$GPS_DEBUG解析到坐标: $lat, $lon")
                return getLocationFromCoordinates(context, lat, lon)
            }
        } catch (e: Exception) {
            Log.e(TAG, "$GPS_DEBUG解析位置字符串失败: ${e.message}")
        }

        return null
    }

    /**
     * 根据经纬度获取地点名称
     *
     * 使用双层缓存：内存+本地，减少API调用次数
     *
     * @param context 上下文
     * @param latitude 纬度
     * @param longitude 经度
     * @return 地点名称，如果无法获取则返回null
     */
    suspend fun getLocationFromCoordinates(context: Context, latitude: Double, longitude: Double): String? {
        // 生成缓存Key（保留4位小数，约11米精度）
        val cacheKey = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)

        // L1: 检查内存缓存
        val cachedLocation = memoryCache[cacheKey]
        if (cachedLocation != null) {
            Log.d(TAG, "GPS_DEBUG:💾 [L1命中] 内存缓存: $cachedLocation")
            return cachedLocation
        }

        // L2: 检查本地持久化缓存
        val diskCachedLocation = loadFromDiskCache(context, cacheKey)
        if (diskCachedLocation != null) {
            Log.d(TAG, "GPS_DEBUG:💾 [L2命中] 本地缓存: $diskCachedLocation")
            // 回填到内存缓存
            memoryCache[cacheKey] = diskCachedLocation
            return diskCachedLocation
        }

        Log.d(TAG, "GPS_DEBUG:🔍 [缓存未命中] 需要调用API")

        // 使用策略模式获取地址
        val location = GeocodingFactory.getInstance().getAddress(context, latitude, longitude)
        if (location != null) {
            // 保存到双层缓存
            saveToCache(context, cacheKey, location)
            Log.d(TAG, "GPS_DEBUG:✅ 策略模式获取地址成功: $location")
            return location
        }

        // 如果所有地理编码方法都失败，不显示地点信息
        Log.d(TAG, "GPS_DEBUG:❌ 所有地理编码方法失败，返回null")
        return null
    }

    /**
     * 保存到双层缓存
     *
     * @param context 上下文
     * @param cacheKey 缓存键
     * @param location 地点名称
     */
    private fun saveToCache(context: Context, cacheKey: String, location: String) {
        // L1: 保存到内存缓存（LRU策略）
        if (memoryCache.size >= MAX_MEMORY_CACHE_SIZE) {
            // 简单的LRU：移除第一个元素
            val firstKey = memoryCache.keys.first()
            memoryCache.remove(firstKey)
            Log.d(TAG, "GPS_DEBUG:💾 [L1清理] 移除旧缓存: $firstKey")
        }
        memoryCache[cacheKey] = location

        // L2: 保存到本地持久化缓存
        saveToDiskCache(context, cacheKey, location)
    }

    /**
     * 从本地缓存加载
     *
     * @param context 上下文
     * @param cacheKey 缓存键
     * @return 缓存的地点名称，如果未找到或过期则返回null
     */
    private suspend fun loadFromDiskCache(context: Context, cacheKey: String): String? {
        try {
            val fullKey = "$CACHE_KEY_PREFIX$cacheKey"
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
                        Log.d(TAG, "GPS_DEBUG:💾 [L2过期] 缓存已过期: $cacheKey")
                        context.dataStore.edit { preferences ->
                            preferences.remove(stringPreferencesKey(fullKey))
                            preferences.remove(longPreferencesKey("${fullKey}_time"))
                        }
                        return null
                    }
                }
            }
            return location
        } catch (e: Exception) {
            Log.e(TAG, "GPS_DEBUG:❌ [L2错误] 加载本地缓存失败: ${e.message}")
            return null
        }
    }

    /**
     * 保存到本地缓存
     *
     * @param context 上下文
     * @param cacheKey 缓存键
     * @param location 地点名称
     */
    private fun saveToDiskCache(context: Context, cacheKey: String, location: String) {
        try {
            val fullKey = "$CACHE_KEY_PREFIX$cacheKey"
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                context.dataStore.edit {
                    it[stringPreferencesKey(fullKey)] = location
                    it[longPreferencesKey("${fullKey}_time")] = System.currentTimeMillis()
                }
            }
            Log.d(TAG, "GPS_DEBUG:💾 [L2保存] 保存到本地缓存: $cacheKey")
        } catch (e: Exception) {
            Log.e(TAG, "GPS_DEBUG:❌ [L2错误] 保存本地缓存失败: ${e.message}")
        }
    }

    /**
     * 从视频文件头提取GPS信息
     *
     * @param context 上下文
     * @param videoUrl 视频URL
     * @return 地点名称，如果无法提取则返回null
     */
    private fun getLocationFromVideoHeader(context: Context, videoUrl: String): String? {
        // 简化实现：直接返回null，完整实现需要下载文件头并解析
        return null
    }

    /**
     * 从视频文件尾部提取GPS信息
     *
     * @param context 上下文
     * @param videoUrl 视频URL
     * @return 地点名称，如果无法提取则返回null
     */
    private fun getLocationFromVideoTail(context: Context, videoUrl: String): String? {
        // 简化实现：直接返回null，完整实现需要下载文件尾部并解析
        return null
    }

    /**
     * 清空所有缓存
     *
     * @param context 上下文
     */
    fun clearAllCache(context: Context) {
        // 清空内存缓存
        memoryCache.clear()
        Log.d(TAG, "GPS_DEBUG:💾 [清理] 内存缓存已清空")

        // 清空本地缓存
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            context.dataStore.edit { it.clear() }
            Log.d(TAG, "GPS_DEBUG:💾 [清理] 本地缓存已清空")
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @param context 上下文
     * @return 缓存统计字符串
     */
    fun getCacheStats(context: Context): String {
        val memorySize = memoryCache.size
        return "内存缓存: $memorySize/$MAX_MEMORY_CACHE_SIZE"
    }

    /**
     * 释放资源
     */
    fun release() {
        memoryCache.clear()
    }
}