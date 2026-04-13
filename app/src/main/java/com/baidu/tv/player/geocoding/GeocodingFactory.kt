package com.baidu.tv.player.geocoding

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking

/**
 * 逆地理编码策略工厂
 *
 * 单例模式，管理所有可用的地理编码策略
 * 按优先级自动选择最佳的逆地理编码服务
 * 优先级：1-高德地图，2-Android Geocoder，3-Nominatim
 */
object GeocodingFactory {
    private const val TAG = "GeocodingFactory"

    // 策略列表，按优先级排序
    private val strategies by lazy {
        listOf(
            AmapGeocodingStrategy(),
            AndroidGeocoderStrategy(),
            NominatimGeocodingStrategy()
        )
    }

    /**
     * 获取地址信息
     *
     * 按优先级依次尝试不同的策略，直到成功获取地址
     * 如果所有策略都失败，返回null
     *
     * @param context 应用上下文
     * @param latitude 纬度
     * @param longitude 经度
     * @return 格式化地址字符串，如果所有策略都失败则返回null
     */
    suspend fun getAddress(context: Context, latitude: Double, longitude: Double): String? {
        for ((index, strategy) in strategies.withIndex()) {
            try {
                Log.d(TAG, "尝试使用策略 ${index + 1}/${strategies.size}: ${strategy::class.simpleName}")
                val address = strategy.getAddress(context, latitude, longitude)

                if (address != null && address.isNotBlank()) {
                    Log.d(TAG, "策略 ${strategy::class.simpleName} 成功获取地址: $address")
                    return address
                } else {
                    Log.d(TAG, "策略 ${strategy::class.simpleName} 返回null或空，尝试下一个策略")
                }
            } catch (e: Exception) {
                Log.e(TAG, "策略 ${strategy::class.simpleName} 执行异常: ${e.message}", e)
            }
        }

        Log.w(TAG, "所有逆地理编码策略均失败，无法获取地址")
        return null
    }

    /**
     * 获取所有策略的名称列表（用于调试）
     */
    fun getStrategyNames(): List<String> {
        return strategies.map { it::class.simpleName ?: "Unknown" }
    }

    /**
     * 测试所有策略的可用性
     */
    fun testAllStrategies(context: Context, latitude: Double, longitude: Double): Map<String, String?> {
        val results = mutableMapOf<String, String?>()

        runBlocking {
            for (strategy in strategies) {
                try {
                    val result = strategy.getAddress(context, latitude, longitude)
                    results[strategy::class.simpleName ?: "Unknown"] = result
                } catch (e: Exception) {
                    results[strategy::class.simpleName ?: "Unknown"] = null
                }
            }
        }

        return results
    }
}