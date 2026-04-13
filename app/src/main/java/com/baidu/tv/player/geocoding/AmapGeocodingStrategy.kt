package com.baidu.tv.player.geocoding

import android.content.Context
import android.util.Log
import com.baidu.tv.player.config.BaiduConfig
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * 高德地图逆地理编码策略
 *
 * 使用高德地图API进行逆地理编码，支持WGS84到GCJ02坐标转换
 * 高德地图API要求使用GCJ02坐标，而GPS设备通常提供WGS84坐标，因此需要进行转换
 */
class AmapGeocodingStrategy : GeocodingStrategy {
    private val TAG = "AmapGeocodingStrategy"

    // 高德地图API配置
    private val AMAP_API_KEY = BaiduConfig.AMAP_API_KEY
    private val AMAP_API_URL = "https://restapi.amap.com/v3/geocode/regeo"

    // 请求超时时间
    private val CONNECTION_TIMEOUT = 3000 // 3秒连接超时
    private val READ_TIMEOUT = 5000       // 5秒读取超时

    /**
     * 根据经纬度获取地址信息
     *
     * 1. 将WGS84坐标转换为GCJ02坐标（火星坐标）
     * 2. 调用高德地图API获取地址信息
     * 3. 解析JSON响应获取格式化地址
     *
     * @param context 应用上下文
     * @param latitude 纬度（WGS84）
     * @param longitude 经度（WGS84）
     * @return 格式化地址字符串，如果失败则返回null
     */
    override suspend fun getAddress(context: Context, latitude: Double, longitude: Double): String? {
        try {
            // 将WGS84坐标转换为GCJ02坐标（高德地图要求）
            val (gcj02Lon, gcj02Lat) = wgs84ToGcj02(longitude, latitude)

            // 构建API请求URL
            val url = URL("$AMAP_API_URL?location=${gcj02Lon},${gcj02Lat}&key=$AMAP_API_KEY&extensions=all&output=json")

            // 发起HTTP请求
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("User-Agent", "pan.baidu.com")
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "高德地图API请求失败，响应码: $responseCode")
                return null
            }

            // 读取响应内容
            val inputStream = connection.inputStream
            val response = inputStream.bufferedReader().readText()
            inputStream.close()
            connection.disconnect()

            // 解析JSON响应
            // 高德地图API响应格式：{"status":"1","regeocode":{"addressComponent":{},"formatted_address":"北京市朝阳区..."}}
            val json = response

            // 检查状态
            if (json.contains("\"status\":\"1\"")) {
                // 查找格式化地址
                val formattedAddressStart = json.indexOf("\"formatted_address\":\"")
                if (formattedAddressStart != -1) {
                    val formattedAddressEnd = json.indexOf("\"", formattedAddressStart + 20)
                    if (formattedAddressEnd != -1) {
                        val formattedAddress = json.substring(formattedAddressStart + 20, formattedAddressEnd)
                        return formattedAddress
                    }
                }
            }

            Log.d(TAG, "高德地图API返回正常但未找到地址: $response")
            return null

        } catch (e: Exception) {
            Log.e(TAG, "高德地图API调用异常: ${e.message}", e)
            return null
        }
    }

    /**
     * WGS84坐标转GCJ02坐标（火星坐标）
     *
     * 参考：https://github.com/googollee/eviltransform
     *
     * @param lon 经度（WGS84）
     * @param lat 纬度（WGS84）
     * @return 转换后的经度和纬度（GCJ02）
     */
    private fun wgs84ToGcj02(lon: Double, lat: Double): Pair<Double, Double> {
        if (outOfChina(lon, lat)) {
            return Pair(lon, lat)
        }

        val dLat = transformLat(lon - 105.0, lat - 35.0)
        val dLon = transformLon(lon - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * Math.PI
        val magic = Math.sin(radLat)
        val magicSquared = magic * magic
        val sqrtMagic = Math.sqrt(1 - 0.00669342162296594323 * magicSquared)

        val dLatRad = dLat * 180.0 / ((6378245.0 * (1 - 0.00669342162296594323)) / (magicSquared * sqrtMagic) * Math.PI)
        val dLonRad = dLon * 180.0 / (6378245.0 / sqrtMagic * Math.cos(radLat) * Math.PI)

        val mgLat = lat + dLatRad
        val mgLon = lon + dLonRad

        return Pair(mgLon, mgLat)
    }

    /**
     * 判断坐标是否在中国范围内
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 如果坐标在中国范围内返回false，否则返回true
     */
    private fun outOfChina(lon: Double, lat: Double): Boolean {
        return (lon < 72.004 || lon > 137.8347) || (lat < 0.8293 || lat > 55.8271)
    }

    /**
     * 转换纬度
     *
     * @param x 经度偏移量
     * @param y 纬度偏移量
     * @return 转换后的纬度偏移量
     */
    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    /**
     * 转换经度
     *
     * @param x 经度偏移量
     * @param y 纬度偏移量
     * @return 转换后的经度偏移量
     */
    private fun transformLon(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0
        return ret
    }
}