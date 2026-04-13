package com.baidu.tv.player.geocoding

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import java.io.IOException
import java.util.*

/**
 * Android原生Geocoder策略
 *
 * 使用Android系统内置的Geocoder服务进行逆地理编码
 * 优点：无需网络请求，直接使用系统服务
 * 缺点：依赖设备上的地理编码服务，可能不准确或不可用
 */
class AndroidGeocoderStrategy : GeocodingStrategy {
    private val TAG = "AndroidGeocoderStrategy"

    /**
     * 根据经纬度获取地址信息
     *
     * 使用Android系统内置的Geocoder服务
     * 如果系统服务不可用，返回null
     *
     * @param context 应用上下文
     * @param latitude 纬度
     * @param longitude 经度
     * @return 格式化地址字符串，如果失败则返回null
     */
    override suspend fun getAddress(context: Context, latitude: Double, longitude: Double): String? {
        try {
            // 创建Geocoder实例
            val geocoder = Geocoder(context, Locale.getDefault())

            // 获取地址列表
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]

                // 构建格式化地址
                val addressParts = mutableListOf<String>()

                // 添加地址行
                if (address.maxAddressLineIndex >= 0) {
                    addressParts.add(address.getAddressLine(0))
                }

                // 添加城市
                if (!address.locality.isNullOrBlank()) {
                    addressParts.add(address.locality)
                }

                // 添加国家
                if (!address.countryName.isNullOrBlank()) {
                    addressParts.add(address.countryName)
                }

                // 返回格式化地址
                return addressParts.joinToString(", ")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Android Geocoder获取地址失败: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Android Geocoder异常: ${e.message}", e)
        }

        return null
    }
}