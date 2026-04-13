package com.baidu.tv.player.utils

import java.util.Random

/**
 * 随机数工具类
 *
 * 实现Fisher-Yates洗牌算法，
 * 与Java参考项目功能完全一致。
 */
object RandomUtils {
    private val random = Random()

    /**
     * Fisher-Yates洗牌算法
     *
     * 对数组进行随机打乱，确保每个排列的概率相等。
     * 时间复杂度：O(n)
     * 空间复杂度：O(1)
     *
     * @param array 要打乱的数组
     */
    fun shuffle(array: IntArray) {
        for (i in array.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            // 交换元素
            val temp = array[i]
            array[i] = array[j]
            array[j] = temp
        }
    }

    /**
     * Fisher-Yates洗牌算法（泛型版本）
     *
     * 对任意类型的数组进行随机打乱
     *
     * @param array 要打乱的数组
     */
    fun <T> shuffle(array: Array<T>) {
        for (i in array.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            // 交换元素
            val temp = array[i]
            array[i] = array[j]
            array[j] = temp
        }
    }

    /**
     * 生成指定范围内的随机整数
     *
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @return 随机整数
     */
    fun nextInt(min: Int, max: Int): Int {
        return random.nextInt(max - min + 1) + min
    }

    /**
     * 生成0到1之间的随机浮点数
     *
     * @return 0到1之间的随机浮点数
     */
    fun nextDouble(): Double {
        return random.nextDouble()
    }
}
