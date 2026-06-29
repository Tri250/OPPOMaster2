package com.silas.omaster.engine

import android.graphics.Bitmap
import kotlin.math.pow

/**
 * 16 位半精度浮点处理管线
 *
 * 参照 AlcedoStudio 的 32-bit 浮点处理管线和 RapidRAW 的 GPU 加速管线。
 * AlcedoStudio 使用 32 位 float 处理，Android 端内存有限，使用 16 位半精度
 * 作为平衡方案：比 8 位精度高 256 倍，内存仅为 32 位的一半。
 *
 * 核心思路：
 * - 所有像素操作在 Float16Buffer 中进行，避免 8 位量化损失
 * - 中间结果保持 16 位精度，只在最终输出时转换回 8 位
 * - 溢出/截断使用 Film-like sigmoid 过渡，避免硬裁剪
 *
 * 操作链路：
 * 1. Bitmap → Float16Buffer（8bit→16bit 扩展）
 * 2. 在 Float16Buffer 上执行全部调整
 * 3. Float16Buffer → Bitmap（16bit→8bit 压缩，可选色调映射）
 */
class Float16Pipeline {

    /** 16 位浮点像素缓冲区 */
    class Float16Buffer(
        val width: Int,
        val height: Int
    ) {
        // 使用 ShortArray 存储 half-float（每个通道 16 位 = 2 字节）
        // 布局：RGB 交替 [R0, G0, B0, R1, G1, B1, ...]
        val data: ShortArray = ShortArray(width * height * 3)

        /** 获取像素 RGB */
        fun getPixel(x: Int, y: Int): FloatArray {
            val idx = (y * width + x) * 3
            return floatArrayOf(
                halfToFloat(data[idx]),
                halfToFloat(data[idx + 1]),
                halfToFloat(data[idx + 2])
            )
        }

        /** 设置像素 RGB */
        fun setPixel(x: Int, y: Int, r: Float, g: Float, b: Float) {
            val idx = (y * width + x) * 3
            data[idx] = floatToHalf(r)
            data[idx + 1] = floatToHalf(g)
            data[idx + 2] = floatToHalf(b)
        }

        /** 获取通道值 */
        fun getChannel(x: Int, y: Int, channel: Int): Float {
            val idx = (y * width + x) * 3 + channel
            return halfToFloat(data[idx])
        }

        /** 设置通道值 */
        fun setChannel(x: Int, y: Int, channel: Int, value: Float) {
            val idx = (y * width + x) * 3 + channel
            data[idx] = floatToHalf(value)
        }
    }

    // ============ 类型转换 ============

    /**
     * Bitmap → Float16Buffer
     * 将 8-bit sRGB 像素转换为线性 16-bit 浮点
     */
    fun bitmapToFloat16(bitmap: Bitmap): Float16Buffer {
        val w = bitmap.width
        val h = bitmap.height
        val buffer = Float16Buffer(w, h)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val p = pixels[i]
            // sRGB → 线性（逆伽马）
            val r = srgbToLinear(((p shr 16) and 0xFF) / 255f)
            val g = srgbToLinear(((p shr 8) and 0xFF) / 255f)
            val b = srgbToLinear((p and 0xFF) / 255f)

            val x = i % w
            val y = i / w
            buffer.setPixel(x, y, r, g, b)
        }

        return buffer
    }

    /**
     * Float16Buffer → Bitmap
     * 将线性 16-bit 浮点转换回 8-bit sRGB
     * 使用 Film-like sigmoid 过渡避免硬裁剪
     * @param toneMap 色调映射模式
     */
    fun float16ToBitmap(
        buffer: Float16Buffer,
        toneMap: ToneMapMode = ToneMapMode.SIGMOID
    ): Bitmap {
        val w = buffer.width
        val h = buffer.height
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)

        for (y in 0 until h) {
            for (x in 0 until w) {
                val rgb = buffer.getPixel(x, y)

                // 色调映射（处理超亮/超暗值）
                val mapped = when (toneMap) {
                    ToneMapMode.CLIP -> floatArrayOf(
                        rgb[0].coerceIn(0f, 1f),
                        rgb[1].coerceIn(0f, 1f),
                        rgb[2].coerceIn(0f, 1f)
                    )
                    ToneMapMode.SIGMOID -> floatArrayOf(
                        sigmoidToneMap(rgb[0]),
                        sigmoidToneMap(rgb[1]),
                        sigmoidToneMap(rgb[2])
                    )
                    ToneMapMode.REINHARD -> floatArrayOf(
                        reinhardToneMap(rgb[0]),
                        reinhardToneMap(rgb[1]),
                        reinhardToneMap(rgb[2])
                    )
                }

                // 线性 → sRGB（伽马校正）
                val r = linearToSrgb(mapped[0])
                val g = linearToSrgb(mapped[1])
                val b = linearToSrgb(mapped[2])

                pixels[y * w + x] = (0xFF shl 24) or
                        ((r * 255f).toInt().coerceIn(0, 255) shl 16) or
                        ((g * 255f).toInt().coerceIn(0, 255) shl 8) or
                        ((b * 255f).toInt().coerceIn(0, 255))
            }
        }

        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return bitmap
    }

    enum class ToneMapMode {
        CLIP,       // 硬裁剪
        SIGMOID,    // S 曲线过渡（Film-like，AlcedoStudio 同款）
        REINHARD    // Reinhard 色调映射
    }

    // ============ 16-bit 浮点操作 ============

    /**
     * 曝光调整（16-bit 浮点精度，无量化损失）
     * 2^exposure 乘法
     */
    fun adjustExposure(buffer: Float16Buffer, exposure: Float) {
        val factor = 2f.pow(exposure)
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                for (ch in 0..2) {
                    val v = buffer.getChannel(x, y, ch)
                    buffer.setChannel(x, y, ch, v * factor)
                }
            }
        }
    }

    /**
     * 白平衡调整（16-bit 精度，R/G/B 通道独立增益）
     */
    fun adjustWhiteBalance(buffer: Float16Buffer, rGain: Float, gGain: Float, bGain: Float) {
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                buffer.setChannel(x, y, 0, buffer.getChannel(x, y, 0) * rGain)
                buffer.setChannel(x, y, 1, buffer.getChannel(x, y, 1) * gGain)
                buffer.setChannel(x, y, 2, buffer.getChannel(x, y, 2) * bGain)
            }
        }
    }

    /**
     * 对比度调整（16-bit 浮点）
     * contrast > 1: 增强，< 1: 降低
     */
    fun adjustContrast(buffer: Float16Buffer, contrast: Float) {
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                for (ch in 0..2) {
                    val v = buffer.getChannel(x, y, ch)
                    buffer.setChannel(x, y, ch, 0.5f + (v - 0.5f) * contrast)
                }
            }
        }
    }

    /**
     * 高光恢复（Film-like sigmoid 过渡，避免硬裁剪）
     * 仅影响亮度 > threshold 的像素
     */
    fun recoverHighlights(buffer: Float16Buffer, amount: Float) {
        val threshold = 0.75f
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                val rgb = buffer.getPixel(x, y)
                val lum = 0.299f * rgb[0] + 0.587f * rgb[1] + 0.114f * rgb[2]

                if (lum > threshold) {
                    val excess = (lum - threshold) / (1f - threshold)
                    val recovery = 1f - excess * amount * 0.5f
                    buffer.setPixel(x, y,
                        rgb[0] * recovery,
                        rgb[1] * recovery,
                        rgb[2] * recovery
                    )
                }
            }
        }
    }

    // ============ 辅助函数 ============

    /** sRGB → 线性（逆伽马） */
    private fun srgbToLinear(srgb: Float): Float {
        return if (srgb <= 0.04045f) {
            srgb / 12.92f
        } else {
            ((srgb + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    /** 线性 → sRGB（伽马校正） */
    private fun linearToSrgb(linear: Float): Float {
        return if (linear <= 0.0031308f) {
            12.92f * linear
        } else {
            1.055f * linear.pow(1f / 2.4f) - 0.055f
        }
    }

    /** Sigmoid 色调映射（Film-like highlight transition） */
    private fun sigmoidToneMap(v: Float): Float {
        // S 曲线：暗部/亮部过渡柔和
        val x = v.coerceIn(-6f, 6f)
        return 1f / (1f + kotlin.math.exp(-x * 2f))
    }

    /** Reinhard 色调映射 */
    private fun reinhardToneMap(v: Float): Float {
        return v / (1f + v)
    }

    /**
     * Float → Half (IEEE 754 半精度)
     * 简化实现，精度足够用于图片处理
     */
    private fun floatToHalf(f: Float): Short {
        val bits = java.lang.Float.floatToIntBits(f)
        val sign = (bits ushr 16) and 0x8000
        val exponent = (bits ushr 23) and 0xFF
        val mantissa = bits and 0x7FFFFF

        val halfExp = exponent - 112  // 127 - 15
        val halfMant = mantissa ushr 13

        return if (exponent == 0) {
            // 零或非规格化数
            sign.toShort()
        } else if (exponent == 0xFF) {
            // 无穷或NaN
            (sign or 0x7C00 or (if (mantissa != 0) 1 else 0)).toShort()
        } else if (halfExp > 30) {
            // 溢出 → 最大半精度值
            (sign or 0x7BFF).toShort()
        } else if (halfExp <= 0) {
            // 下溢 → 零
            sign.toShort()
        } else {
            (sign or (halfExp shl 10) or halfMant).toShort()
        }
    }

    /**
     * Half → Float
     */
    private fun halfToFloat(h: Short): Float {
        val bits = h.toInt() and 0xFFFF
        val sign = (bits ushr 15) and 1
        val exponent = (bits ushr 10) and 0x1F
        val mantissa = bits and 0x3FF

        val floatBits = when {
            exponent == 0 && mantissa == 0 -> (sign shl 31)
            exponent == 0 -> {
                // 非规格化数 → 规格化
                val normalizedExponent = 1
                val normalizedMantissa = mantissa
                ((sign shl 31) or
                        ((normalizedExponent + 112) shl 23) or
                        (normalizedMantissa shl 13))
            }
            exponent == 31 -> {
                // 无穷或NaN
                (sign shl 31) or 0x7F800000 or (mantissa shl 13)
            }
            else -> {
                ((sign shl 31) or ((exponent + 112) shl 23) or (mantissa shl 13))
            }
        }

        return java.lang.Float.intBitsToFloat(floatBits)
    }

    private fun Float.pow(x: Float): Float = kotlin.math.pow(this.toDouble(), x.toDouble()).toFloat()
}
