package com.baidu.tv.player.utils

import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * RandomUtils 单元测试
 *
 * 测试Fisher-Yates洗牌算法和随机数生成功能
 */
class RandomUtilsTest {

    /**
     * 测试：Fisher-Yates洗牌算法 - 整数数组
     */
    @Test
    fun testShuffle_IntArray() {
        // Given: 一个有序的整数数组
        val array = intArrayOf(1, 2, 3, 4, 5)
        val originalArray = array.copyOf()

        // When: 执行洗牌
        RandomUtils.shuffle(array)

        // Then: 验证数组长度不变
        assertThat(array.size).isEqualTo(originalArray.size)

        // 验证所有元素都存在（洗牌不会丢失元素）
        val originalSet = originalArray.toSet()
        val shuffledSet = array.toSet()
        assertThat(shuffledSet).isEqualTo(originalSet)

        // 验证数组不是原顺序（洗牌应该改变顺序）
        // 由于是随机算法，不能保证每次都不一样，但多次运行后应该有变化
        // 这里我们验证数组不是完全相同的
        val isSameOrder = array.zip(originalArray).all { (a, b) -> a == b }
        assertThat(isSameOrder).isFalse()
    }

    /**
     * 测试：Fisher-Yates洗牌算法 - 字符串数组
     */
    @Test
    fun testShuffle_StringArray() {
        // Given: 一个有序的字符串数组
        val array = arrayOf("apple", "banana", "cherry", "date", "elderberry")
        val originalArray = array.copyOf()

        // When: 执行洗牌
        RandomUtils.shuffle(array)

        // Then: 验证数组长度不变
        assertThat(array.size).isEqualTo(originalArray.size)

        // 验证所有元素都存在
        val originalSet = originalArray.toSet()
        val shuffledSet = array.toSet()
        assertThat(shuffledSet).isEqualTo(originalSet)

        // 验证数组不是原顺序
        val isSameOrder = array.zip(originalArray).all { (a, b) -> a == b }
        assertThat(isSameOrder).isFalse()
    }

    /**
     * 测试：Fisher-Yates洗牌算法 - 单个元素数组
     */
    @Test
    fun testShuffle_SingleElement() {
        // Given: 只有一个元素的数组
        val array = intArrayOf(42)
        val originalArray = array.copyOf()

        // When: 执行洗牌
        RandomUtils.shuffle(array)

        // Then: 验证数组不变（单个元素无法洗牌）
        assertThat(array).isEqualTo(originalArray)
    }

    /**
     * 测试：Fisher-Yates洗牌算法 - 空数组
     */
    @Test
    fun testShuffle_EmptyArray() {
        // Given: 空数组
        val array = intArrayOf()
        val originalArray = array.copyOf()

        // When: 执行洗牌
        RandomUtils.shuffle(array)

        // Then: 验证数组不变
        assertThat(array).isEqualTo(originalArray)
    }

    /**
     * 测试：生成指定范围内的随机整数
     */
    @Test
    fun testNextInt_Range() {
        // Given: 指定范围
        val min = 10
        val max = 20

        // When: 生成100个随机数
        val randomNumbers = List(100) { RandomUtils.nextInt(min, max) }

        // Then: 验证所有随机数都在指定范围内
        assertThat(randomNumbers).allSatisfy { number ->
            assertThat(number).isAtLeast(min)
            assertThat(number).isAtMost(max)
        }

        // 验证至少有一些数字是min，一些是max
        val hasMin = randomNumbers.any { it == min }
        val hasMax = randomNumbers.any { it == max }
        assertThat(hasMin).isTrue()
        assertThat(hasMax).isTrue()
    }

    /**
     * 测试：生成0到1之间的随机浮点数
     */
    @Test
    fun testNextDouble() {
        // When: 生成100个随机浮点数
        val randomNumbers = List(100) { RandomUtils.nextDouble() }

        // Then: 验证所有随机数都在[0,1)范围内
        assertThat(randomNumbers).allSatisfy { number ->
            assertThat(number).isAtLeast(0.0)
            assertThat(number).isLessThan(1.0)
        }
    }
}
