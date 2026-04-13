package com.baidu.tv.player.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.baidu.tv.player.model.BackgroundMode
import com.baidu.tv.player.model.H265Quality
import com.baidu.tv.player.model.ImageEffect
import com.baidu.tv.player.model.Settings
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * SettingsRepository 单元测试
 *
 * 测试设置存储和读取功能
 * 使用 MockK 进行DataStore模拟
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockDataStore: DataStore<Preferences>

    @MockK
    private lateinit var mockPreferences: Preferences

    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockContext = ApplicationProvider.getApplicationContext()

        // Mock DataStore
        every { mockDataStore.data } returns flowOf(mockPreferences)
        every { mockDataStore.edit(any()) } answers {
            val block = firstArg<(Preferences) -> Unit>()
            block(mockPreferences)
        }

        settingsRepository = SettingsRepository(mockContext)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * 测试：获取设置 - 默认值
     */
    @Test
    fun testGetSettings_DefaultValues() = runTest {
        // Given: DataStore中没有设置
        every { mockPreferences[stringPreferencesKey("image_effects")] } returns ""
        every { mockPreferences[intPreferencesKey("transition_duration")] } returns null
        every { mockPreferences[intPreferencesKey("display_duration")] } returns null
        every { mockPreferences[booleanPreferencesKey("is_random_mode")] } returns null
        every { mockPreferences[intPreferencesKey("background_mode")] } returns null
        every { mockPreferences[booleanPreferencesKey("show_location")] } returns null
        every { mockPreferences[booleanPreferencesKey("h265_enabled")] } returns null
        every { mockPreferences[booleanPreferencesKey("h265_auto_transcode")] } returns null
        every { mockPreferences[booleanPreferencesKey("h265_require_charging")] } returns null
        every { mockPreferences[intPreferencesKey("h265_quality")] } returns null

        // When: 获取设置
        val settingsFlow = settingsRepository.getSettings()
        val settings = settingsFlow.first()

        // Then: 验证返回默认设置
        assertThat(settings.imageEffectSettings.effects).isEqualTo(listOf(ImageEffect.FADE))
        assertThat(settings.imageEffectSettings.transitionDuration).isEqualTo(1000)
        assertThat(settings.imageEffectSettings.displayDuration).isEqualTo(10000)
        assertThat(settings.imageEffectSettings.isRandomMode).isFalse()
        assertThat(settings.backgroundMode).isEqualTo(BackgroundMode.DEFAULT)
        assertThat(settings.showLocation).isTrue()
        assertThat(settings.h265Settings.enabled).isTrue()
        assertThat(settings.h265Settings.autoTranscodeOnCharging).isTrue()
        assertThat(settings.h265Settings.requireCharging).isTrue()
        assertThat(settings.h265Settings.quality).isEqualTo(H265Quality.MEDIUM)
    }

    /**
     * 测试：获取设置 - 自定义值
     */
    @Test
    fun testGetSettings_CustomValues() = runTest {
        // Given: DataStore中有自定义设置
        val effects = "1,2,3"
        val transitionDuration = 2000
        val displayDuration = 15000
        val isRandomMode = true
        val backgroundMode = 2
        val showLocation = false
        val h265Enabled = false
        val h265AutoTranscode = false
        val h265RequireCharging = false
        val h265Quality = 2

        every { mockPreferences[stringPreferencesKey("image_effects")] } returns effects
        every { mockPreferences[intPreferencesKey("transition_duration")] } returns transitionDuration
        every { mockPreferences[intPreferencesKey("display_duration")] } returns displayDuration
        every { mockPreferences[booleanPreferencesKey("is_random_mode")] } returns isRandomMode
        every { mockPreferences[intPreferencesKey("background_mode")] } returns backgroundMode
        every { mockPreferences[booleanPreferencesKey("show_location")] } returns showLocation
        every { mockPreferences[booleanPreferencesKey("h265_enabled")] } returns h265Enabled
        every { mockPreferences[booleanPreferencesKey("h265_auto_transcode")] } returns h265AutoTranscode
        every { mockPreferences[booleanPreferencesKey("h265_require_charging")] } returns h265RequireCharging
        every { mockPreferences[intPreferencesKey("h265_quality")] } returns h265Quality

        // When: 获取设置
        val settingsFlow = settingsRepository.getSettings()
        val settings = settingsFlow.first()

        // Then: 验证返回自定义设置
        assertThat(settings.imageEffectSettings.effects).isEqualTo(listOf(ImageEffect.FADE, ImageEffect.EASE, ImageEffect.POP))
        assertThat(settings.imageEffectSettings.transitionDuration).isEqualTo(transitionDuration)
        assertThat(settings.imageEffectSettings.displayDuration).isEqualTo(displayDuration)
        assertThat(settings.imageEffectSettings.isRandomMode).isEqualTo(isRandomMode)
        assertThat(settings.backgroundMode).isEqualTo(BackgroundMode.BLUR)
        assertThat(settings.showLocation).isEqualTo(showLocation)
        assertThat(settings.h265Settings.enabled).isEqualTo(h265Enabled)
        assertThat(settings.h265Settings.autoTranscodeOnCharging).isEqualTo(h265AutoTranscode)
        assertThat(settings.h265Settings.requireCharging).isEqualTo(h265RequireCharging)
        assertThat(settings.h265Settings.quality).isEqualTo(H265Quality.HIGH)
    }

    /**
     * 测试：保存设置
     */
    @Test
    fun testSaveSettings() = runTest {
        // Given: 创建设置对象
        val settings = Settings(
            imageEffectSettings = ImageEffectSettings(
                effects = listOf(ImageEffect.FADE, ImageEffect.SLIDE),
                transitionDuration = 1500,
                displayDuration = 8000,
                isRandomMode = true
            ),
            backgroundMode = BackgroundMode.MAIN_COLOR,
            showLocation = false,
            h265Settings = H265Settings(
                enabled = false,
                autoTranscodeOnCharging = false,
                requireCharging = true,
                quality = H265Quality.HIGH
            )
        )

        // When: 保存设置
        settingsRepository.saveSettings(settings)

        // Then: 验证DataStore被正确更新
        verify(exactly = 1) { mockDataStore.edit(any()) }
        val capturedBlock = capture<Block<Preferences>>()
        every { mockDataStore.edit(capture(capturedBlock)) } returns Unit
        capturedBlock.captured[any()] // 触发验证

        // 验证每个设置项都被正确保存
        assertThat(capturedBlock.captured[stringPreferencesKey("image_effects")]).isEqualTo("1,4")
        assertThat(capturedBlock.captured[intPreferencesKey("transition_duration")]).isEqualTo(1500)
        assertThat(capturedBlock.captured[intPreferencesKey("display_duration")]).isEqualTo(8000)
        assertThat(capturedBlock.captured[booleanPreferencesKey("is_random_mode")]).isEqualTo(true)
        assertThat(capturedBlock.captured[intPreferencesKey("background_mode")]).isEqualTo(3)
        assertThat(capturedBlock.captured[booleanPreferencesKey("show_location")]).isEqualTo(false)
        assertThat(capturedBlock.captured[booleanPreferencesKey("h265_enabled")]).isEqualTo(false)
        assertThat(capturedBlock.captured[booleanPreferencesKey("h265_auto_transcode")]).isEqualTo(false)
        assertThat(capturedBlock.captured[booleanPreferencesKey("h265_require_charging")]).isEqualTo(true)
        assertThat(capturedBlock.captured[intPreferencesKey("h265_quality")]).isEqualTo(2)
    }

    /**
     * 测试：保存图片特效设置
     */
    @Test
    fun testSaveImageEffectSettings() = runTest {
        // Given: 创建图片特效设置
        val effects = listOf(ImageEffect.FADE, ImageEffect.BOUNCE)
        val transitionDuration = 1200
        val displayDuration = 10000
        val isRandomMode = true

        // Mock getSettings to return a base settings object
        val baseSettings = Settings(
            imageEffectSettings = ImageEffectSettings(listOf(ImageEffect.FADE), 1000, 10000, false),
            backgroundMode = BackgroundMode.DEFAULT,
            showLocation = true,
            h265Settings = H265Settings(true, true, true, H265Quality.MEDIUM)
        )
        every { settingsRepository.getSettings().first() } returns baseSettings

        // When: 保存图片特效设置
        settingsRepository.saveImageEffectSettings(effects, transitionDuration, displayDuration, isRandomMode)

        // Then: 验证保存了正确的设置
        verify(exactly = 1) { settingsRepository.saveSettings(any()) }
        val capturedSettings = capture<Settings>()
        every { settingsRepository.saveSettings(capture(capturedSettings)) } returns Unit

        assertThat(capturedSettings.captured.imageEffectSettings.effects).isEqualTo(effects)
        assertThat(capturedSettings.captured.imageEffectSettings.transitionDuration).isEqualTo(transitionDuration)
        assertThat(capturedSettings.captured.imageEffectSettings.displayDuration).isEqualTo(displayDuration)
        assertThat(capturedSettings.captured.imageEffectSettings.isRandomMode).isEqualTo(isRandomMode)
    }

    /**
     * 测试：保存背景模式
     */
    @Test
    fun testSaveBackgroundMode() = runTest {
        // Given: 创建背景模式
        val backgroundMode = BackgroundMode.BLUR

        // Mock getSettings to return a base settings object
        val baseSettings = Settings(
            imageEffectSettings = ImageEffectSettings(listOf(ImageEffect.FADE), 1000, 10000, false),
            backgroundMode = BackgroundMode.DEFAULT,
            showLocation = true,
            h265Settings = H265Settings(true, true, true, H265Quality.MEDIUM)
        )
        every { settingsRepository.getSettings().first() } returns baseSettings

        // When: 保存背景模式
        settingsRepository.saveBackgroundMode(backgroundMode)

        // Then: 验证保存了正确的设置
        verify(exactly = 1) { settingsRepository.saveSettings(any()) }
        val capturedSettings = capture<Settings>()
        every { settingsRepository.saveSettings(capture(capturedSettings)) } returns Unit

        assertThat(capturedSettings.captured.backgroundMode).isEqualTo(backgroundMode)
    }

    /**
     * 测试：保存地点显示设置
     */
    @Test
    fun testSaveShowLocation() = runTest {
        // Given: 创建地点显示设置
        val showLocation = false

        // Mock getSettings to return a base settings object
        val baseSettings = Settings(
            imageEffectSettings = ImageEffectSettings(listOf(ImageEffect.FADE), 1000, 10000, false),
            backgroundMode = BackgroundMode.DEFAULT,
            showLocation = true,
            h265Settings = H265Settings(true, true, true, H265Quality.MEDIUM)
        )
        every { settingsRepository.getSettings().first() } returns baseSettings

        // When: 保存地点显示设置
        settingsRepository.saveShowLocation(showLocation)

        // Then: 验证保存了正确的设置
        verify(exactly = 1) { settingsRepository.saveSettings(any()) }
        val capturedSettings = capture<Settings>()
        every { settingsRepository.saveSettings(capture(capturedSettings)) } returns Unit

        assertThat(capturedSettings.captured.showLocation).isEqualTo(showLocation)
    }

    /**
     * 测试：保存H.265设置
     */
    @Test
    fun testSaveH265Settings() = runTest {
        // Given: 创建H.265设置
        val enabled = false
        val autoTranscodeOnCharging = false
        val requireCharging = true
        val quality = H265Quality.HIGH

        // Mock getSettings to return a base settings object
        val baseSettings = Settings(
            imageEffectSettings = ImageEffectSettings(listOf(ImageEffect.FADE), 1000, 10000, false),
            backgroundMode = BackgroundMode.DEFAULT,
            showLocation = true,
            h265Settings = H265Settings(true, true, true, H265Quality.MEDIUM)
        )
        every { settingsRepository.getSettings().first() } returns baseSettings

        // When: 保存H.265设置
        settingsRepository.saveH265Settings(enabled, autoTranscodeOnCharging, requireCharging, quality)

        // Then: 验证保存了正确的设置
        verify(exactly = 1) { settingsRepository.saveSettings(any()) }
        val capturedSettings = capture<Settings>()
        every { settingsRepository.saveSettings(capture(capturedSettings)) } returns Unit

        assertThat(capturedSettings.captured.h265Settings.enabled).isEqualTo(enabled)
        assertThat(capturedSettings.captured.h265Settings.autoTranscodeOnCharging).isEqualTo(autoTranscodeOnCharging)
        assertThat(capturedSettings.captured.h265Settings.requireCharging).isEqualTo(requireCharging)
        assertThat(capturedSettings.captured.h265Settings.quality).isEqualTo(quality)
    }
}
