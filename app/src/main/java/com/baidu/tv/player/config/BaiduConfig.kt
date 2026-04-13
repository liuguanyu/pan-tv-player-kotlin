package com.baidu.tv.player.config

/**
 * 百度网盘配置
 * 这是一个示例配置文件
 * 请将此文件复制为 BaiduConfig.kt 并填入您的实际凭据
 * 注意：不要将包含真实凭据的文件提交到版本控制系统
 */
object BaiduConfig {

    // 百度网盘应用配置
    const val APP_ID = "YOUR_APP_ID"
    const val APP_KEY = "YOUR_APP_KEY"
    const val SECRET_KEY = "YOUR_SECRET_KEY"
    const val SIGN_KEY = "YOUR_SIGN_KEY"

    // 高德地图API配置
    const val AMAP_API_KEY = "YOUR_AMAP_API_KEY"

    // OAuth配置
    const val REDIRECT_URI = "oob"
    const val SCOPE = "basic,netdisk"
    const val DEVICE_NAME = "百度网盘TV播放器"

    // API地址
    const val API_BASE_URL = "https://pan.baidu.com/rest/2.0/"
    const val OAUTH_URL = "https://openapi.baidu.com/oauth/2.0/"
    const val PCS_URL = "https://d.pcs.baidu.com/rest/2.0/pcs/"
}