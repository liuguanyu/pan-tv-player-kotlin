package com.baidu.tv.player.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 偏好设置工具类
 *
 * 使用DataStore替代SharedPreferences存储用户设置，
 * 提供类型安全的异步数据访问。
 *
 * 与Java参考项目功能完全一致，使用Kotlin协程和DataStore。
 */
object PreferenceUtils {

    // DataStore实例
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "baidu_tv_player")

    // 认证相关键
    private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    private val KEY_TOKEN_EXPIRES_IN = longPreferencesKey("token_expires_in")
    private val KEY_TOKEN_TIME = longPreferencesKey("token_time")

    // 图片特效相关键
    private val KEY_IMAGE_EFFECT = intPreferencesKey("image_effect")
    private val KEY_IMAGE_DISPLAY_DURATION = intPreferencesKey("image_display_duration")
    private val KEY_IMAGE_TRANSITION_DURATION = intPreferencesKey("image_transition_duration")

    // 地点显示
    private val KEY_SHOW_LOCATION = booleanPreferencesKey("show_location")

    // 文件排序相关
    private val KEY_FILE_SORT_MODE = intPreferencesKey("file_sort_mode")

    // 播放模式相关
    private val KEY_PLAY_MODE = intPreferencesKey("play_mode")

    // 背景模式相关
    private val KEY_BACKGROUND_MODE = intPreferencesKey("background_mode")

    // 默认值
    private const val DEFAULT_IMAGE_EFFECT = 0 // 淡入淡出
    private const val DEFAULT_IMAGE_DISPLAY_DURATION = 10000 // 10秒
    private const val DEFAULT_IMAGE_TRANSITION_DURATION = 1000 // 1秒
    private const val DEFAULT_SHOW_LOCATION = true
    private const val DEFAULT_BACKGROUND_MODE = 1 // 主色调背景

    // ========== 认证相关 ==========

    /**
     * 保存访问令牌
     *
     * @param context 上下文
     * @param accessToken 访问令牌
     */
    suspend fun saveAccessToken(context: Context, accessToken: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCESS_TOKEN] = accessToken
        }
    }

    /**
     * 获取访问令牌
     *
     * @param context 上下文
     * @return 访问令牌，如果未设置则返回空字符串
     */
    suspend fun getAccessToken(context: Context): String {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_ACCESS_TOKEN] ?: ""
        }.first()
    }

    /**
     * 保存刷新令牌
     *
     * @param context 上下文
     * @param refreshToken 刷新令牌
     */
    suspend fun saveRefreshToken(context: Context, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    /**
     * 获取刷新令牌
     *
     * @param context 上下文
     * @return 刷新令牌，如果未设置则返回空字符串
     */
    suspend fun getRefreshToken(context: Context): String {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_REFRESH_TOKEN] ?: ""
        }.first()
    }

    /**
     * 保存令牌过期时间（秒）
     *
     * @param context 上下文
     * @param expiresIn 过期时间（秒）
     */
    suspend fun saveTokenExpiresIn(context: Context, expiresIn: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOKEN_EXPIRES_IN] = expiresIn
        }
    }

    /**
     * 获取令牌过期时间（秒）
     *
     * @param context 上下文
     * @return 过期时间（秒），如果未设置则返回0
     */
    suspend fun getTokenExpiresIn(context: Context): Long {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_TOKEN_EXPIRES_IN] ?: 0L
        }.first()
    }

    /**
     * 保存令牌获取时间（毫秒）
     *
     * @param context 上下文
     * @param time 令牌获取时间（毫秒）
     */
    suspend fun saveTokenTime(context: Context, time: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOKEN_TIME] = time
        }
    }

    /**
     * 获取令牌获取时间（毫秒）
     *
     * @param context 上下文
     * @return 令牌获取时间（毫秒），如果未设置则返回0
     */
    suspend fun getTokenTime(context: Context): Long {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_TOKEN_TIME] ?: 0L
        }.first()
    }

    /**
     * 清除认证信息
     *
     * @param context 上下文
     */
    suspend fun clearAuthInfo(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_ACCESS_TOKEN)
            preferences.remove(KEY_REFRESH_TOKEN)
            preferences.remove(KEY_TOKEN_EXPIRES_IN)
            preferences.remove(KEY_TOKEN_TIME)
        }
    }

    /**
     * 检查令牌是否过期
     *
     * @param context 上下文
     * @return 如果令牌过期则返回true，否则返回false
     */
    suspend fun isTokenExpired(context: Context): Boolean {
        val tokenTime = getTokenTime(context)
        val expiresIn = getTokenExpiresIn(context)

        if (tokenTime == 0L || expiresIn == 0L) {
            return true
        }

        val currentTime = System.currentTimeMillis()
        val expireTime = tokenTime + (expiresIn * 1000)

        // 提前5分钟认为过期
        return currentTime >= (expireTime - 5 * 60 * 1000)
    }

    // ========== 图片特效相关 ==========

    /**
     * 保存图片特效
     *
     * @param context 上下文
     * @param effect 特效类型
     */
    suspend fun saveImageEffect(context: Context, effect: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IMAGE_EFFECT] = effect
        }
    }

    /**
     * 获取图片特效
     *
     * @param context 上下文
     * @return 特效类型，如果未设置则返回默认值
     */
    suspend fun getImageEffect(context: Context): Int {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_IMAGE_EFFECT] ?: DEFAULT_IMAGE_EFFECT
        }.first()
    }

    /**
     * 保存图片展示时长（毫秒）
     *
     * @param context 上下文
     * @param duration 展示时长（毫秒）
     */
    suspend fun saveImageDisplayDuration(context: Context, duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IMAGE_DISPLAY_DURATION] = duration
        }
    }

    /**
     * 获取图片展示时长（毫秒）
     *
     * @param context 上下文
     * @return 展示时长（毫秒），如果未设置则返回默认值
     */
    suspend fun getImageDisplayDuration(context: Context): Int {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_IMAGE_DISPLAY_DURATION] ?: DEFAULT_IMAGE_DISPLAY_DURATION
        }.first()
    }

    /**
     * 保存图片过渡时长（毫秒）
     *
     * @param context 上下文
     * @param duration 过渡时长（毫秒）
     */
    suspend fun saveImageTransitionDuration(context: Context, duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IMAGE_TRANSITION_DURATION] = duration
        }
    }

    /**
     * 获取图片过渡时长（毫秒）
     *
     * @param context 上下文
     * @return 过渡时长（毫秒），如果未设置则返回默认值
     */
    suspend fun getImageTransitionDuration(context: Context): Int {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_IMAGE_TRANSITION_DURATION] ?: DEFAULT_IMAGE_TRANSITION_DURATION
        }.first()
    }

    // ========== 地点显示相关 ==========

    /**
     * 保存是否显示地点
     *
     * @param context 上下文
     * @param show 是否显示地点
     */
    suspend fun saveShowLocation(context: Context, show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_LOCATION] = show
        }
    }

    /**
     * 获取是否显示地点
     *
     * @param context 上下文
     * @return 是否显示地点，如果未设置则返回默认值
     */
    suspend fun getShowLocation(context: Context): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_SHOW_LOCATION] ?: DEFAULT_SHOW_LOCATION
        }.first()
    }

    // ========== 文件排序相关 ==========

    /**
     * 保存文件排序模式
     * 0: NAME_ASC
     * 1: NAME_DESC
     * 2: DATE_ASC
     * 3: DATE_DESC
     *
     * @param context 上下文
     * @param mode 排序模式
     */
    suspend fun saveFileSortMode(context: Context, mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FILE_SORT_MODE] = mode
        }
    }

    /**
     * 获取文件排序模式
     *
     * @param context 上下文
     * @return 排序模式，如果未设置则返回默认值0 (NAME_ASC)
     */
    suspend fun getFileSortMode(context: Context): Int {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_FILE_SORT_MODE] ?: 0
        }.first()
    }

    // ========== 播放模式相关 ==========

    /**
     * 保存播放模式
     * 0: 顺序播放
     * 1: 随机播放
     * 2: 单曲循环
     * 3: 倒序播放
     *
     * @param context 上下文
     * @param mode 播放模式
     */
    suspend fun savePlayMode(context: Context, mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PLAY_MODE] = mode
        }
    }

    /**
     * 获取播放模式
     *
     * @param context 上下文
     * @return 播放模式，如果未设置则返回默认值0 (SEQUENTIAL)
     */
    suspend fun getPlayMode(context: Context): Int {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_PLAY_MODE] ?: 0
        }.first()
    }

    // ========== 背景模式相关 ==========

    /**
     * 保存背景模式
     * 0: 纯黑色背景
     * 1: 主色调背景
     * 2: 毛玻璃背景
     *
     * @param context 上下文
     * @param mode 背景模式
     */
    suspend fun saveBackgroundMode(context: Context, mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BACKGROUND_MODE] = mode
        }
    }

    /**
     * 获取背景模式
     *
     * @param context 上下文
     * @return 背景模式，如果未设置则返回默认值1 (主色调背景)
     */
    suspend fun getBackgroundMode(context: Context): Int {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_BACKGROUND_MODE] ?: DEFAULT_BACKGROUND_MODE
        }.first()
    }
}
