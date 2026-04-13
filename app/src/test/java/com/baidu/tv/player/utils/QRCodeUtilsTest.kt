package com.baidu.tv.player.utils

import android.graphics.Bitmap
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * QRCodeUtils 单元测试
 *
 * 测试二维码生成功能
 */
class QRCodeUtilsTest {

    /**
     * 测试：生成二维码 - 成功
     */
    @Test
    fun testCreateQRCodeBitmap_Success() {
        // Given: 二维码内容和尺寸
        val content = "https://example.com"
        val width = 300
        val height = 300

        // When: 生成二维码
        val bitmap = QRCodeUtils.createQRCodeBitmap(content, width, height)

        // Then: 验证生成了位图
        assertThat(bitmap).isNotNull()
        assertThat(bitmap!!.width).isEqualTo(width)
        assertThat(bitmap.height).isEqualTo(height)
    }

    /**
     * 测试：生成二维码 - 空内容
     */
    @Test
    fun testCreateQRCodeBitmap_EmptyContent() {
        // Given: 空的二维码内容
        val content = ""
        val width = 300
        val height = 300

        // When: 生成二维码
        val bitmap = QRCodeUtils.createQRCodeBitmap(content, width, height)

        // Then: 验证生成了位图
        assertThat(bitmap).isNotNull()
        assertThat(bitmap!!.width).isEqualTo(width)
        assertThat(bitmap.height).isEqualTo(height)
    }

    /**
     * 测试：生成二维码 - 非法尺寸
     */
    @Test
    fun testCreateQRCodeBitmap_InvalidSize() {
        // Given: 非法尺寸
        val content = "https://example.com"
        val width = 0
        val height = 0

        // When: 生成二维码
        val bitmap = QRCodeUtils.createQRCodeBitmap(content, width, height)

        // Then: 验证生成了位图
        assertThat(bitmap).isNotNull()
        assertThat(bitmap!!.width).isEqualTo(width)
        assertThat(bitmap.height).isEqualTo(height)
    }

    /**
     * 测试：生成二维码 - 大尺寸
     */
    @Test
    fun testCreateQRCodeBitmap_LargeSize() {
        // Given: 大尺寸
        val content = "https://example.com/very/long/url/that/needs/a/large/qr/code"
        val width = 500
        val height = 500

        // When: 生成二维码
        val bitmap = QRCodeUtils.createQRCodeBitmap(content, width, height)

        // Then: 验证生成了位图
        assertThat(bitmap).isNotNull()
        assertThat(bitmap!!.width).isEqualTo(width)
        assertThat(bitmap.height).isEqualTo(height)
    }
}
