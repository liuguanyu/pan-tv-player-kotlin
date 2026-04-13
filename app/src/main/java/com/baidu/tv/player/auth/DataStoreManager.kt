package com.baidu.tv.player.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore管理器，用于持久化认证信息
 *
 * 使用Jetpack DataStore替代SharedPreferences
 * 提供类型安全的键值对存储
 *
 * @property context 应用上下文
 */
class DataStoreManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: DataStoreManager? = null

        /**
         * 获取DataStoreManager单例实例
         */
        fun getInstance(context: Context): DataStoreManager {
            return instance ?: synchronized(this) {
                instance ?: DataStoreManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val Context.authPreferences: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")

    // DataStore键定义
    object PreferenceKeys {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_SCOPE = "scope"
        const val KEY_SESSION_KEY = "session_key"
        const val KEY_SESSION_SECRET = "session_secret"
        const val KEY_SESSION_EXPIRES_AT = "session_expires_at"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_IS_LOGGED_IN = "is_logged_in"

        // 设备ID单独存储在另一个DataStore（或SharedPreferences）
        const val KEY_DEVICE_ID = "device_id"
    }

    val authPreferences: DataStore<Preferences>
        get() = context.authPreferences

    /**
     * 获取字符串偏好
     */
    suspend fun getString(key: String, defaultValue: String = ""): String {
        return context.authPreferences.data
            .collect { preferences ->
                return@collect preferences[stringPreferencesKey(key)] ?: defaultValue
            }
    }

    /**
     * 获取Long偏好
     */
    suspend fun getLong(key: String, defaultValue: Long = 0L): Long {
        return context.authPreferences.data
            .collect { preferences ->
                return@collect preferences[longPreferencesKey(key)] ?: defaultValue
            }
    }

    /**
     * 获取Boolean偏好
     */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return context.authPreferences.data
            .collect { preferences ->
                return@collect preferences[booleanPreferencesKey(key)] ?: defaultValue
            }
    }

    /**
     * 获取字符串Flow
     */
    fun getStringFlow(key: String, defaultValue: String = ""): Flow<String> {
        return context.authPreferences.data.map { preferences ->
            preferences[stringPreferencesKey(key)] ?: defaultValue
        }
    }

    /**
     * 获取Long Flow
     */
    fun getLongFlow(key: String, defaultValue: Long = 0L): Flow<Long> {
        return context.authPreferences.data.map { preferences ->
            preferences[longPreferencesKey(key)] ?: defaultValue
        }
    }

    /**
     * 获取Boolean Flow
     */
    fun getBooleanFlow(key: String, defaultValue: Boolean = false): Flow<Boolean> {
        return context.authPreferences.data.map { preferences ->
            preferences[booleanPreferencesKey(key)] ?: defaultValue
        }
    }

    /**
     * 存储字符串
     */
    suspend fun putString(key: String, value: String) {
        context.authPreferences.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    /**
     * 存储Long
     */
    suspend fun putLong(key: String, value: Long) {
        context.authPreferences.edit { preferences ->
            preferences[longPreferencesKey(key)] = value
        }
    }

    /**
     * 存储Boolean
     */
    suspend fun putBoolean(key: String, value: Boolean) {
        context.authPreferences.edit { preferences ->
            preferences[booleanPreferencesKey(key)] = value
        }
    }

    /**
     * 删除指定键
     */
    suspend fun remove(key: String) {
        context.authPreferences.edit { preferences ->
            preferences.remove(stringPreferencesKey(key))
        }
    }

    /**
     * 清空所有偏好
     */
    suspend fun clear() {
        context.authPreferences.edit { preferences ->
            preferences.clear()
        }
    }
}