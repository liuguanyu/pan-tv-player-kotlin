package com.baidu.tv.player.geocoding

import android.content.Context

/**
 * 逆地理编码策略接口
 *
 * 定义了获取地理位置信息的统一接口，支持多种地理编码服务
 */
interface GeocodingStrategy {
    /**
     * 根据经纬度获取地址信息
     *
     * @param context 应用上下文
     * @param latitude 纬度
     * @param longitude 经度
     * @return 地址字符串，如果失败则返回null
     */
    suspend fun getAddress(context: Context, latitude: Double, longitude: Double): String?
}