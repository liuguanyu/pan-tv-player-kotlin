package com.baidu.tv.player.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.launch
import com.baidu.tv.player.R
import com.baidu.tv.player.model.BackgroundMode
import com.baidu.tv.player.model.H265Quality
import com.baidu.tv.player.model.ImageEffect

/**
 * 设置Fragment
 *
 * 使用AndroidX Preference库实现的设置界面，支持TV遥控器D-pad操作。
 * 所有设置项都通过PreferenceScreen定义，便于管理和维护。
 *
 * 功能：
 * - 图片特效设置（支持多选和随机模式）
 * - 动画时长设置
 * - 展示时长设置
 * - 背景模式选择
 * - 地点显示开关
 * - H.265播放设置
 */
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var viewModel: SettingsViewModel

    // 图片特效偏好
    private lateinit var effectFade: CheckBoxPreference
    private lateinit var effectEase: CheckBoxPreference
    private lateinit var effectFloat: CheckBoxPreference
    private lateinit var effectBounce: CheckBoxPreference
    private lateinit var effectBlinds: CheckBoxPreference
    private lateinit var effectZoom: CheckBoxPreference
    private lateinit var effectRotate: CheckBoxPreference
    private lateinit var effectSlide: CheckBoxPreference
    private lateinit var effectRandom: CheckBoxPreference

    // 时长设置偏好
    private lateinit var transitionDuration: SeekBarPreference
    private lateinit var displayDuration: SeekBarPreference

    // 背景模式偏好
    private lateinit var backgroundMode: ListPreference

    // 地点显示偏好
    private lateinit var showLocation: SwitchPreferenceCompat

    // H.265设置偏好
    private lateinit var h265Enabled: SwitchPreferenceCompat
    private lateinit var h265AutoTranscode: SwitchPreferenceCompat
    private lateinit var h265RequireCharging: SwitchPreferenceCompat
    private lateinit var h265Quality: ListPreference

    // 标记是否正在更新偏好设置，防止递归触发
    private var isUpdatingPreferences = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 观察设置状态变化
        observeSettings()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // 从XML加载偏好设置
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        // 初始化偏好设置引用
        initPreferences()

        // 设置偏好改变监听器
        setupPreferenceListeners()
    }

    /**
     * 初始化偏好设置引用
     */
    private fun initPreferences() {
        // 图片特效
        effectFade = findPreference<CheckBoxPreference>("effect_fade")!!
        effectEase = findPreference<CheckBoxPreference>("effect_ease")!!
        effectFloat = findPreference<CheckBoxPreference>("effect_float")!!
        effectBounce = findPreference<CheckBoxPreference>("effect_bounce")!!
        effectBlinds = findPreference<CheckBoxPreference>("effect_blinds")!!
        effectZoom = findPreference<CheckBoxPreference>("effect_zoom")!!
        effectRotate = findPreference<CheckBoxPreference>("effect_rotate")!!
        effectSlide = findPreference<CheckBoxPreference>("effect_slide")!!
        effectRandom = findPreference<CheckBoxPreference>("effect_random")!!

        // 时长设置
        transitionDuration = findPreference<SeekBarPreference>("transition_duration")!!
        displayDuration = findPreference<SeekBarPreference>("display_duration")!!

        // 背景模式
        backgroundMode = findPreference<ListPreference>("background_mode")!!

        // 地点显示
        showLocation = findPreference<SwitchPreferenceCompat>("show_location")!!

        // H.265设置
        h265Enabled = findPreference<SwitchPreferenceCompat>("h265_enabled")!!
        h265AutoTranscode = findPreference<SwitchPreferenceCompat>("h265_auto_transcode")!!
        h265RequireCharging = findPreference<SwitchPreferenceCompat>("h265_require_charging")!!
        h265Quality = findPreference<ListPreference>("h265_quality")!!
    }

    /**
     * 设置偏好改变监听器
     */
    private fun setupPreferenceListeners() {
        // 图片特效监听器
        val effectChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            if (!isUpdatingPreferences) {
                val isChecked = newValue as Boolean
                val effect = getEffectFromPreferenceKey(preference.key)
                updateEffectSelection(effect, isChecked)
            }
            true
        }

        effectFade.onPreferenceChangeListener = effectChangeListener
        effectEase.onPreferenceChangeListener = effectChangeListener
        effectFloat.onPreferenceChangeListener = effectChangeListener
        effectBounce.onPreferenceChangeListener = effectChangeListener
        effectBlinds.onPreferenceChangeListener = effectChangeListener
        effectZoom.onPreferenceChangeListener = effectChangeListener
        effectRotate.onPreferenceChangeListener = effectChangeListener
        effectSlide.onPreferenceChangeListener = effectChangeListener
        effectRandom.onPreferenceChangeListener = effectChangeListener

        // 时长设置监听器
        transitionDuration.setOnPreferenceChangeListener { _, newValue ->
            val duration = (newValue as Int) * 1000 // 转换为毫秒
            viewModel.saveTransitionDuration(duration)
            true
        }

        displayDuration.setOnPreferenceChangeListener { _, newValue ->
            val duration = (newValue as Int) * 1000 // 转换为毫秒
            viewModel.saveDisplayDuration(duration)
            true
        }

        // 背景模式监听器
        backgroundMode.setOnPreferenceChangeListener { _, newValue ->
            val mode = BackgroundMode.fromValue((newValue as String).toInt())
            viewModel.saveBackgroundMode(mode)
            true
        }

        // 地点显示监听器
        showLocation.setOnPreferenceChangeListener { _, newValue ->
            viewModel.saveShowLocation(newValue as Boolean)
            true
        }

        // H.265设置监听器
        h265Enabled.setOnPreferenceChangeListener { _, newValue ->
            viewModel.saveH265Enabled(newValue as Boolean)
            true
        }

        h265AutoTranscode.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                viewModel.saveH265Settings(
                    enabled = viewModel.settings.value.h265Settings.enabled,
                    autoTranscodeOnCharging = newValue as Boolean,
                    requireCharging = viewModel.settings.value.h265Settings.requireCharging,
                    quality = viewModel.settings.value.h265Settings.quality
                )
            }
            true
        }

        h265RequireCharging.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                viewModel.saveH265Settings(
                    enabled = viewModel.settings.value.h265Settings.enabled,
                    autoTranscodeOnCharging = viewModel.settings.value.h265Settings.autoTranscodeOnCharging,
                    requireCharging = newValue as Boolean,
                    quality = viewModel.settings.value.h265Settings.quality
                )
            }
            true
        }

        h265Quality.setOnPreferenceChangeListener { _, newValue ->
            val quality = H265Quality.fromValue((newValue as String).toInt())
            lifecycleScope.launch {
                viewModel.saveH265Settings(
                    enabled = viewModel.settings.value.h265Settings.enabled,
                    autoTranscodeOnCharging = viewModel.settings.value.h265Settings.autoTranscodeOnCharging,
                    requireCharging = viewModel.settings.value.h265Settings.requireCharging,
                    quality = quality
                )
            }
            true
        }
    }

    /**
     * 观察设置状态变化
     */
    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                isUpdatingPreferences = true

                // 更新图片特效选择
                updateEffectPreferences(settings.imageEffectSettings.effects)

                // 更新时长设置
                transitionDuration.value = settings.imageEffectSettings.transitionDuration / 1000
                displayDuration.value = settings.imageEffectSettings.displayDuration / 1000

                // 更新背景模式
                backgroundMode.value = settings.backgroundMode.value.toString()

                // 更新地点显示
                showLocation.isChecked = settings.showLocation

                // 更新H.265设置
                h265Enabled.isChecked = settings.h265Settings.enabled
                h265AutoTranscode.isChecked = settings.h265Settings.autoTranscodeOnCharging
                h265RequireCharging.isChecked = settings.h265Settings.requireCharging
                h265Quality.value = settings.h265Settings.quality.value.toString()

                isUpdatingPreferences = false
            }
        }
    }

    /**
     * 根据偏好设置键获取对应的图片特效
     *
     * @param key 偏好设置键
     * @return 对应的ImageEffect
     */
    private fun getEffectFromPreferenceKey(key: String): ImageEffect {
        return when (key) {
            "effect_fade" -> ImageEffect.FADE
            "effect_ease" -> ImageEffect.EASE
            "effect_float" -> ImageEffect.FLOAT
            "effect_bounce" -> ImageEffect.BOUNCE
            "effect_blinds" -> ImageEffect.BLINDS
            "effect_zoom" -> ImageEffect.ZOOM
            "effect_rotate" -> ImageEffect.ROTATE
            "effect_slide" -> ImageEffect.SLIDE
            "effect_random" -> ImageEffect.RANDOM
            else -> ImageEffect.FADE
        }
    }

    /**
     * 更新图片特效选择
     *
     * @param effects 已选择的特效列表
     */
    private fun updateEffectPreferences(effects: List<ImageEffect>) {
        effectFade.isChecked = effects.contains(ImageEffect.FADE)
        effectEase.isChecked = effects.contains(ImageEffect.EASE)
        effectFloat.isChecked = effects.contains(ImageEffect.FLOAT)
        effectBounce.isChecked = effects.contains(ImageEffect.BOUNCE)
        effectBlinds.isChecked = effects.contains(ImageEffect.BLINDS)
        effectZoom.isChecked = effects.contains(ImageEffect.ZOOM)
        effectRotate.isChecked = effects.contains(ImageEffect.ROTATE)
        effectSlide.isChecked = effects.contains(ImageEffect.SLIDE)
        effectRandom.isChecked = effects.contains(ImageEffect.RANDOM)
    }

    /**
     * 更新特效选择
     *
     * @param effect 选中的特效
     * @param isChecked 是否选中
     */
    private fun updateEffectSelection(effect: ImageEffect, isChecked: Boolean) {
        val currentEffects = viewModel.settings.value.imageEffectSettings.effects.toMutableList()

        if (effect == ImageEffect.RANDOM && isChecked) {
            // 如果选择了随机模式，清除其他选择
            viewModel.saveImageEffect(ImageEffect.RANDOM)
        } else if (effect == ImageEffect.RANDOM && !isChecked) {
            // 取消随机模式，默认选择淡入淡出
            viewModel.saveImageEffect(ImageEffect.FADE)
        } else {
            // 处理其他特效
            if (isChecked) {
                if (!currentEffects.contains(effect)) {
                    currentEffects.add(effect)
                }
                // 移除随机模式
                currentEffects.remove(ImageEffect.RANDOM)
            } else {
                currentEffects.remove(effect)
            }

            // 至少保留一个特效
            if (currentEffects.isEmpty()) {
                currentEffects.add(ImageEffect.FADE)
            }

            viewModel.saveImageEffects(currentEffects)
        }
    }
}