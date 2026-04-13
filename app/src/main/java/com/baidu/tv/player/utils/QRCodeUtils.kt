package com.baidu.tv.player.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.util.HashMap

/**
 * 二维码工具类
 *
 * 使用ZXing库生成二维码，
 * 与Java参考项目功能完全一致。
 */
object QRCodeUtils {

    /**
     * 生成二维码Bitmap
     *
     * @param content 二维码内容
     * @param width 宽度（像素）
     * @param height 高度（像素）
     * @return 二维码Bitmap，如果生成失败则返回null
     */
    fun createQRCodeBitmap(content: String, width: Int, height: Int): Bitmap? {
        val writer = QRCodeWriter()
        try {
            // 设置编码提示，减少边距
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.MARGIN] = 0 // 设置边距为0

            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix[x, y]) {
                        pixels[y * width + x] = Color.BLACK
                    } else {
                        pixels[y * width + x] = Color.WHITE
                    }
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            return null
        }
    }
}
