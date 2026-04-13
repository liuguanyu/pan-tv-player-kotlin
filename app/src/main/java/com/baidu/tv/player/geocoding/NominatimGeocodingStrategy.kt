package com.baidu.tv.player.geocoding

import android.content.Context
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Nominatim逆地理编码策略
 *
 * 使用OpenStreetMap的Nominatim服务进行逆地理编码
 * 免费、开源，但有使用限制（每秒最多1次请求）
 * 适用于不需要高精度或高频率的场景
 */
class NominatimGeocodingStrategy : GeocodingStrategy {
    private val TAG = "NominatimGeocodingStrategy"

    // Nominatim API配置
    private val NOMINATIM_API_URL = "https://nominatim.openstreetmap.org/reverse"

    // 请求超时时间
    private val CONNECTION_TIMEOUT = 5000 // 5秒连接超时
    private val READ_TIMEOUT = 10000      // 10秒读取超时

    /**
     * 根据经纬度获取地址信息
     *
     * 使用Nominatim API进行逆地理编码
     * Nominatim API要求提供格式化地址的详细程度
     *
     * @param context 应用上下文
     * @param latitude 纬度
     * @param longitude 经度
     * @return 格式化地址字符串，如果失败则返回null
     */
    override suspend fun getAddress(context: Context, latitude: Double, longitude: Double): String? {
        try {
            // 构建API请求URL
            val url = URL("$NOMINATIM_API_URL?lat=$latitude&lon=$longitude&format=json&addressdetails=1&accept-language=zh-CN")

            // 发起HTTP请求
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            // 设置User-Agent以符合Nominatim使用条款
            connection.setRequestProperty("User-Agent", "pan-tv-player/1.0 (https://github.com/baidu/pan-tv-player)")
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Nominatim API请求失败，响应码: $responseCode")
                return null
            }

            // 读取响应内容
            val inputStream = connection.inputStream
            val response = inputStream.bufferedReader().readText()
            inputStream.close()
            connection.disconnect()

            // 解析JSON响应
            // Nominatim响应格式：{"display_name":"北京市朝阳区...","address":{"country":"中国","state":"北京市","city":"朝阳区"}}
            val json = response

            // 查找display_name字段
            val displayNameStart = json.indexOf("\"display_name\":\"")
            if (displayNameStart != -1) {
                val displayNameEnd = json.indexOf("\"", displayNameStart + 16)
                if (displayNameEnd != -1) {
                    val displayName = json.substring(displayNameStart + 16, displayNameEnd)
                    return displayName
                }
            }

            Log.d(TAG, "Nominatim API返回正常但未找到地址: $response")
            return null

        } catch (e: Exception) {
            Log.e(TAG, "Nominatim API调用异常: ${e.message}", e)
            return null
        }
    }
}