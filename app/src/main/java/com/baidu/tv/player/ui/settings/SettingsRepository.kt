package com.baidu.tv.player.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.baidu.tv.player.model.BackgroundMode
import com.baidu.tv.player.model.H265Quality
import com.baidu.tv.player.model.ImageEffect
import com.baidu.tv.player.model.Settings
import java.io.IOException

/**
 * 设置仓库
 *
 * 使用DataStore持久化存储设置项，支持异步读写操作。
 * 所有设置项都通过Kotlin协程进行异步处理，确保主线程不会被阻塞。
 *
 * @property dataStore DataStore实例
 */
class SettingsRepository(private val context: Context) {

    companion object {
        // DataStore的名称
        private const val SETTINGS_DATASTORE_NAME = "settings_datastore"

        // 使用属性委托创建DataStore实例
        private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = SETTINGS_DATASTORE_NAME)
    }

    private val dataStore: DataStore<Preferences> = context.settingsDataStore

    // 设置项的键
    private val IMAGE_EFFECTS_KEY = stringPreferencesKey("image_effects")
    private val TRANSITION_DURATION_KEY = intPreferencesKey("transition_duration")
    private val DISPLAY_DURATION_KEY = intPreferencesKey("display_duration")
    private val IS_RANDOM_MODE_KEY = booleanPreferencesKey("is_random_mode")
    private val BACKGROUND_MODE_KEY = intPreferencesKey("background_mode")
    private val SHOW_LOCATION_KEY = booleanPreferencesKey("show_location")
    private val H265_ENABLED_KEY = booleanPreferencesKey("h265_enabled")
    private val H265_AUTO_TRANSCODE_KEY = booleanPreferencesKey("h265_auto_transcode")
    private val H265_REQUIRE_CHARGING_KEY = booleanPreferencesKey("h265_require_charging")
    private val H265_QUALITY_KEY = intPreferencesKey("h265_quality")

    /**
     * 从DataStore读取设置
     *
     * 返回一个Flow，当设置发生变化时会发出新的设置值。
     * 使用map转换为Settings对象，便于在ViewModel中使用。
     *
     * @return Flow<Settings> 包含当前设置的Flow
     */
    fun getSettings(): Flow<Settings> {
        return dataStore.data
            .catch { exception ->
                // 如果读取失败，返回默认设置
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                // 解析图片特效设置
                val effectsString = preferences[IMAGE_EFFECTS_KEY] ?: ""
                val effects = if (effectsString.isNotEmpty()) {
                    effectsString.split(",").mapNotNull { effectId ->
                        ImageEffect.values().firstOrNull { it.value == effectId.toInt() }
                    }
                } else {
                    listOf(ImageEffect.FADE)
                }

                val transitionDuration = preferences[TRANSITION_DURATION_KEY] ?: 1000
                val displayDuration = preferences[DISPLAY_DURATION_KEY] ?: 10000
                val isRandomMode = preferences[IS_RANDOM_MODE_KEY] ?: false

                val imageEffectSettings = ImageEffectSettings(
                    effects = effects,
                    transitionDuration = transitionDuration,
                    displayDuration = displayDuration,
                    isRandomMode = isRandomMode
                )

                // 解析背景模式
                val backgroundModeValue = preferences[BACKGROUND_MODE_KEY] ?: 1
                val backgroundMode = BackgroundMode.fromValue(backgroundModeValue)

                // 解析地点显示设置
                val showLocation = preferences[SHOW_LOCATION_KEY] ?: true

                // 解析H.265设置
                val h265Enabled = preferences[H265_ENABLED_KEY] ?: true
                val h265AutoTranscode = preferences[H265_AUTO_TRANSCODE_KEY] ?: true
                val h265RequireCharging = preferences[H265_REQUIRE_CHARGING_KEY] ?: true
                val h265QualityValue = preferences[H265_QUALITY_KEY] ?: 0
                val h265Quality = H265Quality.fromValue(h265QualityValue)

                val h265Settings = H265Settings(
                    enabled = h265Enabled,
                    autoTranscodeOnCharging = h265AutoTranscode,
                    requireCharging = h265RequireCharging,
                    quality = h265Quality
                )

                // 返回完整的Settings对象
                Settings(
                    imageEffectSettings = imageEffectSettings,
                    backgroundMode = backgroundMode,
                    showLocation = showLocation,
                    h265Settings = h265Settings
                )
            }
    }

    /**
     * 保存设置
     *
     * 使用DataStore的edit方法异步保存设置。
     *
     * @param settings 要保存的设置
     */
    suspend fun saveSettings(settings: Settings) {
        dataStore.edit { preferences ->
            // 保存图片特效设置
            preferences[IMAGE_EFFECTS_KEY] = settings.imageEffectSettings.effects.joinToString(",") { it.value.toString() }
            preferences[TRANSITION_DURATION_KEY] = settings.imageEffectSettings.transitionDuration
            preferences[DISPLAY_DURATION_KEY] = settings.imageEffectSettings.displayDuration
            preferences[IS_RANDOM_MODE_KEY] = settings.imageEffectSettings.isRandomMode

            // 保存背景模式
            preferences[BACKGROUND_MODE_KEY] = settings.backgroundMode.value

            // 保存地点显示
            preferences[SHOW_LOCATION_KEY] = settings.showLocation

            // 保存H.265设置
            preferences[H265_ENABLED_KEY] = settings.h265Settings.enabled
            preferences[H265_AUTO_TRANSCODE_KEY] = settings.h265Settings.autoTranscodeOnCharging
            preferences[H265_REQUIRE_CHARGING_KEY] = settings.h265Settings.requireCharging
            preferences[H265_QUALITY_KEY] = settings.h265Settings.quality.value
        }
    }

    /**
     * 保存图片特效设置
     *
     * @param effects 图片特效列表
     * @param transitionDuration 过渡时长（毫秒）
     * @param displayDuration 展示时长（毫秒）
     * @param isRandomMode 是否启用随机模式
     */
    suspend fun saveImageEffectSettings(
        effects: List<ImageEffect>,
        transitionDuration: Int,
        displayDuration: Int,
        isRandomMode: Boolean
    ) {
        val settings = getSettings().first()
        saveSettings(
            settings.copy(
                imageEffectSettings = ImageEffectSettings(
                    effects = effects,
                    transitionDuration = transitionDuration,
                    displayDuration = displayDuration,
                    isRandomMode = isRandomMode
                )
            )
        )
    }

    /**
     * 保存背景模式
     *
     * @param backgroundMode 背景模式
     */
    suspend fun saveBackgroundMode(backgroundMode: BackgroundMode) {
        val settings = getSettings().first()
        saveSettings(
            settings.copy(backgroundMode = backgroundMode)
        )
    }

    /**
     * 保存地点显示设置
     *
     * @param showLocation 是否显示地点
     */
    suspend fun saveShowLocation(showLocation: Boolean) {
        val settings = getSettings().first()
        saveSettings(
            settings.copy(showLocation = showLocation)
        )
    }

    /**
     * 保存H.265设置
     *
     * @param enabled 是否启用H.265优化
     * @param autoTranscodeOnCharging 是否在充电时自动转码
     * @param requireCharging 是否要求充电
     * @param quality 转码质量
     */
    suspend fun saveH265Settings(
        enabled: Boolean,
        autoTranscodeOnCharging: Boolean,
        requireCharging: Boolean,
        quality: H265Quality
    ) {
        val settings = getSettings().first()
        saveSettings(
            settings.copy(
                h265Settings = H265Settings(
                    enabled = enabled,
                    autoTranscodeOnCharging = autoTranscodeOnCharging,
                    requireCharging = requireCharging,
                    quality = quality
                )
            )
        )
    }
}