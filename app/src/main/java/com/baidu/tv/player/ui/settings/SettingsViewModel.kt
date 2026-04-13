package com.baidu.tv.player.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.baidu.tv.player.model.BackgroundMode
import com.baidu.tv.player.model.H265Quality
import com.baidu.tv.player.model.ImageEffect
import com.baidu.tv.player.model.Settings

/**
 * 设置视图模型
 *
 * 负责管理设置相关的UI状态和业务逻辑。
 * 使用StateFlow暴露设置状态，确保UI层能够实时观察到设置的变化。
 *
 * @property application Application实例
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    // 设置状态流
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 保存状态
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // 初始化时加载设置
        loadSettings()
    }

    /**
     * 加载设置
     *
     * 从DataStore中加载保存的设置。
     */
    fun loadSettings() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getSettings().collect { settings ->
                _settings.value = settings
                _isLoading.value = false
            }
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
    fun saveImageEffectSettings(
        effects: List<ImageEffect>,
        transitionDuration: Int,
        displayDuration: Int,
        isRandomMode: Boolean
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.saveImageEffectSettings(
                    effects = effects,
                    transitionDuration = transitionDuration,
                    displayDuration = displayDuration,
                    isRandomMode = isRandomMode
                )
                // 更新本地状态
                _settings.value = _settings.value.copy(
                    imageEffectSettings = _settings.value.imageEffectSettings.copy(
                        effects = effects,
                        transitionDuration = transitionDuration,
                        displayDuration = displayDuration,
                        isRandomMode = isRandomMode
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "保存图片特效设置失败: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 保存图片特效列表
     *
     * @param effects 图片特效列表
     */
    fun saveImageEffects(effects: List<ImageEffect>) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.saveImageEffectSettings(
                    effects = effects,
                    transitionDuration = _settings.value.imageEffectSettings.transitionDuration,
                    displayDuration = _settings.value.imageEffectSettings.displayDuration,
                    isRandomMode = _settings.value.imageEffectSettings.isRandomMode
                )
                // 更新本地状态
                _settings.value = _settings.value.copy(
                    imageEffectSettings = _settings.value.imageEffectSettings.copy(
                        effects = effects
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "保存图片特效失败: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 保存图片特效（单个）
     *
     * @param effect 图片特效
     */
    fun saveImageEffect(effect: ImageEffect) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.saveImageEffectSettings(
                    effects = listOf(effect),
                    transitionDuration = _settings.value.imageEffectSettings.transitionDuration,
                    displayDuration = _settings.value.imageEffectSettings.displayDuration,
                    isRandomMode = effect == ImageEffect.RANDOM
                )
                // 更新本地状态
                _settings.value = _settings.value.copy(
                    imageEffectSettings = _settings.value.imageEffectSettings.copy(
                        effects = listOf(effect),
                        isRandomMode = effect == ImageEffect.RANDOM
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "保存图片特效失败: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 保存动画时长
     *
     * @param transitionDuration 过渡时长（毫秒）
     */
    fun saveTransitionDuration(transitionDuration: Int) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.saveImageEffectSettings(
                    effects = _settings.value.imageEffectSettings.effects,
                    transitionDuration = transitionDuration,
                    displayDuration = _settings.value.imageEffectSettings.displayDuration,
                    isRandomMode = _settings.value.imageEffectSettings.isRandomMode
                )
                // 更新本地状态
                _settings.value = _settings.value.copy(
                    imageEffectSettings = _settings.value.imageEffectSettings.copy(
                        transitionDuration = transitionDuration
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "保存动画时长失败: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 保存展示时长
     *
     * @param displayDuration 展示时长（毫秒）
     */
    fun saveDisplayDuration(displayDuration: Int) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.saveImageEffectSettings(
                    effects = _settings.value.imageEffectSettings.effects,
                    transitionDuration = _settings.value.imageEffectSettings.transitionDuration,
                    displayDuration = displayDuration,
                    isRandomMode = _settings.value.imageEffectSettings.isRandomMode
                )
                // 更新本地状态
                _settings.value = _settings.value.copy(
                    imageEffectSettings = _settings.value.imageEffectSettings.copy(
                        displayDuration = displayDuration
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "保存展示时长失败: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 保存背景模式
     *
     * @param backgroundMode 背景模式
     */
    fun saveBackgroundMode(backgroundMode: BackgroundMode) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.saveBackgroundMode(backgroundMode)
                // 更新本地状态
                _settings.value = _settings.value.copy(backgroundMode = backgroundMode)
            } catch (e: Exception) {
                _errorMessage.value = "保存背景模式失败: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 保存地点显示设置
     *
     * @param showLocation 是否显示地点
     */
    fun saveShowLocation(showLocation: Boolean) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.saveShowLocation(showLocation)
                // 更新本地状态
                _settings.value = _settings.value.copy(showLocation = showLocation)
            } catch (e: Exception) {
                _errorMessage.value = "保存地点显示设置失败: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 保存H.265设置
     *
     * @param enabled 是否启用H.265优化
     * @param autoTranscodeOnCharging 是否在充电时自动转码
     * @param requireCharging 是否要求充电
     * @param quality 转码质量
     */
    fun saveH265Settings(
        enabled: Boolean,
        autoTranscodeOnCharging: Boolean,
        requireCharging: Boolean,
        quality: H265Quality
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.saveH265Settings(
                    enabled = enabled,
                    autoTranscodeOnCharging = autoTranscodeOnCharging,
                    requireCharging = requireCharging,
                    quality = quality
                )
                // 更新本地状态
                _settings.value = _settings.value.copy(
                    h265Settings = _settings.value.h265Settings.copy(
                        enabled = enabled,
                        autoTranscodeOnCharging = autoTranscodeOnCharging,
                        requireCharging = requireCharging,
                        quality = quality
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = "保存H.265设置失败: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 保存H.265启用状态
     *
     * @param enabled 是否启用H.265优化
     */
    fun saveH265Enabled(enabled: Boolean) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.saveH265Settings(
                    enabled = enabled,
                    autoTranscodeOnCharging = _settings.value.h265Settings.autoTranscodeOnCharging,
                    requireCharging = _settings.value.h265Settings.requireCharging,
                    quality = _settings.value.h265Settings.quality
                )
                // 更新本地状态
                _settings.value = _settings.value.copy(
                    h265Settings = _settings.value.h265Settings.copy(enabled = enabled)
                )
            } catch (e: Exception) {
                _errorMessage.value = "保存H.265启用状态失败: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}