package com.baidu.tv.player.model

import java.util.Random

/**
 * 图片展示特效枚举
 *
 * 定义了所有支持的图片特效，包括：
 * - FADE: 淡入淡出
 * - EASE: 缓动
 * - FLOAT: 浮现
 * - BOUNCE: 跳动
 * - BLINDS: 百叶窗
 * - ZOOM: 放大
 * - ROTATE: 旋转
 * - SLIDE: 两侧划入
 * - RANDOM: 随机（从其他特效中随机选择）
 *
 * 每个特效都有一个整数值用于序列化存储，以及一个本地化显示名称。
 */
enum class ImageEffect(
    val value: Int,
    val name: String
) {
    FADE(0, "淡入淡出"),
    EASE(1, "缓动"),
    FLOAT(2, "浮现"),
    BOUNCE(3, "跳动"),
    BLINDS(5, "百叶窗"),
    ZOOM(6, "放大"),
    ROTATE(7, "旋转"),
    SLIDE(8, "两侧划入"),
    RANDOM(4, "随机");

    companion object {
        private val random = Random()
        private val nonRandomEffects = values().filter { it != RANDOM }

        /**
         * 获取随机特效（从所有非随机特效中随机选择）
         *
         * @return 随机选择的特效
         */
        fun getRandomEffect(): ImageEffect {
            return nonRandomEffects[random.nextInt(nonRandomEffects.size)]
        }

        /**
         * 根据整数值获取对应的ImageEffect枚举
         *
         * @param value 整数值
         * @return 对应的ImageEffect，如果未找到则返回FADE
         */
        fun fromValue(value: Int): ImageEffect {
            return values().firstOrNull { it.value == value } ?: FADE
        }
    }

    /**
     * 如果当前特效是RANDOM，返回随机特效；否则返回当前特效
     *
     * @return 实际使用的特效
     */
    fun getActualEffect(): ImageEffect {
        return if (this == RANDOM) getRandomEffect() else this
    }
}

/**
 * 背景模式枚举
 *
 * 定义了所有支持的背景模式：
 * - BLACK: 纯黑色背景
 * - PRIMARY_COLOR: 主色调背景
 * - BLUR: 毛玻璃背景
 *
 * 每个模式都有一个整数值用于序列化存储。
 */
enum class BackgroundMode(
    val value: Int
) {
    BLACK(0),
    PRIMARY_COLOR(1),
    BLUR(2);

    /**
     * 根据整数值获取对应的BackgroundMode枚举
     *
     * @param value 整数值
     * @return 对应的BackgroundMode，如果未找到则返回PRIMARY_COLOR
     */
    companion object {
        fun fromValue(value: Int): BackgroundMode {
            return values().firstOrNull { it.value == value } ?: PRIMARY_COLOR
        }
    }
}

/**
 * H.265转码质量枚举
 *
 * 定义了H.265转码的两种质量选项：
 * - HD_720P: 720p分辨率
 * - HD_1080P: 1080p分辨率
 */
enum class H265Quality(
    val value: Int,
    val resolution: String
) {
    HD_720P(0, "720p"),
    HD_1080P(1, "1080p");

    /**
     * 根据整数值获取对应的H265Quality枚举
     *
     * @param value 整数值
     * @return 对应的H265Quality，如果未找到则返回HD_720P
     */
    companion object {
        fun fromValue(value: Int): H265Quality {
            return values().firstOrNull { it.value == value } ?: HD_720P
        }
    }
}

/**
 * H.265播放设置选项
 *
 * 定义了H.265播放的所有设置项
 */
data class H265Settings(
    val enabled: Boolean = true,
    val autoTranscodeOnCharging: Boolean = true,
    val requireCharging: Boolean = true,
    val quality: H265Quality = H265Quality.HD_720P
)

/**
 * 图片特效设置选项
 *
 * 定义了图片特效的所有设置项
 */
data class ImageEffectSettings(
    val effects: List<ImageEffect> = listOf(ImageEffect.FADE),
    val transitionDuration: Int = 1000, // 1秒
    val displayDuration: Int = 10000, // 10秒
    val isRandomMode: Boolean = false
)

/**
 * 设置项数据类
 *
 * 封装了所有设置项，用于在ViewModel中管理状态
 */
data class Settings(
    val imageEffectSettings: ImageEffectSettings = ImageEffectSettings(),
    val backgroundMode: BackgroundMode = BackgroundMode.PRIMARY_COLOR,
    val showLocation: Boolean = true,
    val h265Settings: H265Settings = H265Settings()
)