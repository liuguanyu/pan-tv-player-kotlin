package com.baidu.tv.player.network

/**
 * API常量定义
 */
object ApiConstants {

    // 百度网盘API基础URL
    const val PAN_API_BASE_URL = "https://pan.baidu.com/rest/2.0/"
    const val OAUTH_BASE_URL = "https://openapi.baidu.com/oauth/2.0/"
    const val PCS_BASE_URL = "https://d.pcs.baidu.com/rest/2.0/pcs/"

    // API端点
    const val ENDPOINT_FILE = "xpan/file"
    const val ENDPOINT_MULTIMEDIA = "xpan/multimedia"
    const val ENDPOINT_NAS = "xpan/nas"

    // OAuth端点
    const val ENDPOINT_AUTHORIZE = "authorize"
    const val ENDPOINT_TOKEN = "token"
    const val ENDPOINT_DEVICE_CODE = "device/code"
    const val ENDPOINT_REVOKE = "revoke"

    // 请求超时时间（毫秒）
    const val CONNECT_TIMEOUT = 30000L
    const val READ_TIMEOUT = 30000L
    const val WRITE_TIMEOUT = 30000L

    // 轮询间隔（毫秒）
    const val POLLING_INTERVAL = 5000L
    const val MAX_POLLING_COUNT = 60
}