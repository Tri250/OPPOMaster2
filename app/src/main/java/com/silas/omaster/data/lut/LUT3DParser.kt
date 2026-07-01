package com.silas.omaster.data.lut

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * 3D LUT (.cube) 文件解析器
 *
 * 支持标准 Adobe Cube LUT 格式规范：
 * - TITLE 可选标题行
 * - LUT_3D_SIZE 定义 LUT 尺寸（如 33 表示 33x33x33）
 * - LUT_3D_INPUT_RANGE 定义输入范围（默认 0.0 1.0）
 * - 数据行：每行 3 个浮点数（R G B），按 Red→Green→Blue 顺序排列
 *   即外层循环 Blue，中层 Green，内层 Red
 *
 * 解析结果为 [LUT3DData]，可直接上传为 OpenGL 3D 纹理或 2D 编码纹理。
 */
object LUT3DParser {

    private const val TAG = "LUT3DParser"

    // 限制 LUT 最大尺寸，防止解析恶意/超大文件导致 OOM
    // 33/65 是常见尺寸，128 已覆盖绝大多数实际用例
    private const val MAX_LUT_SIZE = 128

    /**
     * 解析 .cube 文件
     *
     * @param file .cube 文件
     * @return 解析结果，失败返回 null
     */
    fun parse(file: File): LUT3DData? {
        return try {
            BufferedReader(FileReader(file)).use { reader ->
                parseFromReader(reader, file.name)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse .cube file: ${file.name}", e)
            null
        }
    }

    /**
     * 解析 .cube 文件输入流
     *
     * @param inputStream .cube 文件输入流
     * @param fileName 文件名（用于日志）
     * @return 解析结果，失败返回 null
     */
    fun parse(inputStream: InputStream, fileName: String = "unknown"): LUT3DData? {
        return try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                parseFromReader(reader, fileName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse .cube stream: $fileName", e)
            null
        }
    }

    /**
     * 核心解析逻辑
     */
    private fun parseFromReader(reader: BufferedReader, fileName: String): LUT3DData? {
        var title = fileName
        var lutSize = -1
        var inputRangeStart = 0.0f
        var inputRangeEnd = 1.0f
        val dataPoints = mutableListOf<Float>()

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val currentLine = line ?: continue
            val trimmed = currentLine.trim()

            // 跳过空行和注释
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            when {
                trimmed.startsWith("TITLE", ignoreCase = true) -> {
                    // TITLE "Kodak Portra 400"
                    title = trimmed.removePrefix("TITLE")
                        .removePrefix("title")
                        .trim()
                        .removeSurrounding("\"")
                        .ifBlank { fileName }
                }
                trimmed.startsWith("LUT_3D_SIZE", ignoreCase = true) -> {
                    // LUT_3D_SIZE 33
                    val parts = trimmed.split(Regex("\\s+"))
                    if (parts.size >= 2) {
                        lutSize = parts[1].toIntOrNull() ?: -1
                    }
                }
                trimmed.startsWith("LUT_3D_INPUT_RANGE", ignoreCase = true) -> {
                    // LUT_3D_INPUT_RANGE 0.0 1.0
                    val parts = trimmed.split(Regex("\\s+"))
                    if (parts.size >= 3) {
                        inputRangeStart = parts[1].toFloatOrNull() ?: 0.0f
                        inputRangeEnd = parts[2].toFloatOrNull() ?: 1.0f
                    }
                }
                trimmed.startsWith("DOMAIN_MIN", ignoreCase = true) -> {
                    // DOMAIN_MIN 0.0 0.0 0.0（忽略，使用 LUT_3D_INPUT_RANGE）
                }
                trimmed.startsWith("DOMAIN_MAX", ignoreCase = true) -> {
                    // DOMAIN_MAX 1.0 1.0 1.0（忽略）
                }
                else -> {
                    // 数据行：R G B
                    val values = trimmed.split(Regex("\\s+"))
                    if (values.size >= 3) {
                        val r = values[0].toFloatOrNull()
                        val g = values[1].toFloatOrNull()
                        val b = values[2].toFloatOrNull()
                        if (r != null && g != null && b != null) {
                            dataPoints.add(r)
                            dataPoints.add(g)
                            dataPoints.add(b)
                        }
                    }
                }
            }
        }

        // 验证解析结果
        if (lutSize <= 0 || lutSize > MAX_LUT_SIZE) {
            Log.e(TAG, "Missing or invalid LUT_3D_SIZE in $fileName: size=$lutSize")
            return null
        }

        val expectedPoints = lutSize * lutSize * lutSize * 3
        if (dataPoints.size != expectedPoints) {
            Log.e(
                TAG,
                "Data points mismatch in $fileName: expected $expectedPoints, got ${dataPoints.size}"
            )
            return null
        }

        // 转换为 FloatArray
        val lutData = FloatArray(dataPoints.size) { dataPoints[it] }

        // 如果输入范围不是 [0,1]，需要归一化
        if (inputRangeStart != 0.0f || inputRangeEnd != 1.0f) {
            val range = inputRangeEnd - inputRangeStart
            if (range > 0) {
                for (i in lutData.indices) {
                    lutData[i] = (lutData[i] - inputRangeStart) / range
                }
            }
        }

        Log.d(TAG, "Parsed LUT '$title': size=${lutSize}x${lutSize}x${lutSize}, points=${dataPoints.size / 3}")

        return LUT3DData(
            title = title,
            size = lutSize,
            data = lutData
        )
    }

    /**
     * 将 3D LUT 数据编码为 2D 纹理布局
     *
     * OpenGL ES 3.0 支持 sampler3D，但部分设备兼容性不佳。
     * 将 3D LUT 编码为 2D 纹理是更通用的方案：
     * - 宽度 = lutSize * lutSize
     * - 高度 = lutSize
     * - 布局：每行是一个 Blue 切片（Red 水平排列，Green 垂直排列）
     * - Blue 切片从上到下排列
     *
     * @param lutData 3D LUT 数据（R G B 交错排列，Red→Green→Blue 顺序）
     * @param lutSize LUT 尺寸（如 33）
     * @return RGBA FloatArray，宽度=lutSize*lutSize，高度=lutSize
     */
    fun encodeTo2DTexture(lutData: FloatArray, lutSize: Int): FloatArray {
        if (lutSize <= 0) return FloatArray(0)
        val expectedPoints = lutSize * lutSize * lutSize * 3
        if (lutData.size < expectedPoints) {
            Log.e(TAG, "encodeTo2DTexture: LUT data too small, expected $expectedPoints, got ${lutData.size}")
            return FloatArray(0)
        }

        val width = lutSize * lutSize
        val height = lutSize
        val result = FloatArray(width * height * 4) // RGBA

        for (b in 0 until lutSize) {
            for (g in 0 until lutSize) {
                for (r in 0 until lutSize) {
                    // .cube 文件数据顺序：Red（内层）→ Green（中层）→ Blue（外层）
                    val srcIndex = (b * lutSize * lutSize + g * lutSize + r) * 3

                    // 2D 纹理坐标：x = r + g * lutSize, y = b
                    val x = r + g * lutSize
                    val y = b
                    val dstIndex = (y * width + x) * 4

                    result[dstIndex] = lutData[srcIndex]         // R
                    result[dstIndex + 1] = lutData[srcIndex + 1] // G
                    result[dstIndex + 2] = lutData[srcIndex + 2] // B
                    result[dstIndex + 3] = 1.0f                  // A
                }
            }
        }

        return result
    }

    /**
     * 将 3D LUT 数据转换为 Android Bitmap（用于 CPU 预览）
     *
     * @param lutData 3D LUT 数据
     * @param lutSize LUT 尺寸
     * @return 编码为 2D 纹理的 Bitmap
     */
    fun encodeToBitmap(lutData: FloatArray, lutSize: Int): android.graphics.Bitmap {
        if (lutSize <= 0) {
            return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        }
        val expectedPoints = lutSize * lutSize * lutSize * 3
        if (lutData.size < expectedPoints) {
            Log.e(TAG, "encodeToBitmap: LUT data too small, expected $expectedPoints, got ${lutData.size}")
            return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        }

        val width = lutSize * lutSize
        val height = lutSize
        val textureData = encodeTo2DTexture(lutData, lutSize)

        // encodeTo2DTexture 已校验数据，此处做二次保护
        if (textureData.isEmpty()) {
            return android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        }

        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            val base = i * 4
            val r = (textureData[base].coerceIn(0f, 1f) * 255).toInt()
            val g = (textureData[base + 1].coerceIn(0f, 1f) * 255).toInt()
            val b = (textureData[base + 2].coerceIn(0f, 1f) * 255).toInt()
            val a = (textureData[base + 3].coerceIn(0f, 1f) * 255).toInt()
            pixels[i] = android.graphics.Color.argb(a, r, g, b)
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}

/**
 * 3D LUT 解析结果
 *
 * @param title LUT 标题
 * @param size LUT 尺寸（如 33 表示 33x33x33）
 * @param data LUT 数据，长度 = size^3 * 3，每 3 个浮点数一组（R G B），
 *             按 Red（内层）→ Green（中层）→ Blue（外层）顺序排列
 */
data class LUT3DData(
    val title: String,
    val size: Int,
    val data: FloatArray
) {
    /** LUT 总点数 */
    val totalPoints: Int get() = size * size * size

    /** 获取指定 R/G/B 索引处的颜色值 */
    fun get(r: Int, g: Int, b: Int): FloatArray {
        if (size <= 0 ||
            r < 0 || r >= size ||
            g < 0 || g >= size ||
            b < 0 || b >= size ||
            data.size < size * size * size * 3
        ) {
            return floatArrayOf(0f, 0f, 0f)
        }
        val index = (b * size * size + g * size + r) * 3
        return floatArrayOf(data[index], data[index + 1], data[index + 2])
    }

    /**
     * 三线性插值采样
     *
     * @param r 归一化 R 值 [0, 1]
     * @param g 归一化 G 值 [0, 1]
     * @param b 归一化 B 值 [0, 1]
     * @return 插值后的 RGB 浮点数组
     */
    fun sampleTrilinear(r: Float, g: Float, b: Float): FloatArray {
        if (size <= 0 || data.size < size * size * size * 3) {
            return floatArrayOf(0f, 0f, 0f)
        }
        val maxIndex = size - 1

        // 映射到 LUT 索引空间
        val ri = r.coerceIn(0f, 1f) * maxIndex
        val gi = g.coerceIn(0f, 1f) * maxIndex
        val bi = b.coerceIn(0f, 1f) * maxIndex

        val r0 = ri.toInt().coerceIn(0, maxIndex)
        val g0 = gi.toInt().coerceIn(0, maxIndex)
        val b0 = bi.toInt().coerceIn(0, maxIndex)
        val r1 = (r0 + 1).coerceIn(0, maxIndex)
        val g1 = (g0 + 1).coerceIn(0, maxIndex)
        val b1 = (b0 + 1).coerceIn(0, maxIndex)

        val rf = ri - r0
        val gf = gi - g0
        val bf = bi - b0

        // 8 个角的采样
        val c000 = get(r0, g0, b0)
        val c100 = get(r1, g0, b0)
        val c010 = get(r0, g1, b0)
        val c110 = get(r1, g1, b0)
        val c001 = get(r0, g0, b1)
        val c101 = get(r1, g0, b1)
        val c011 = get(r0, g1, b1)
        val c111 = get(r1, g1, b1)

        // 三线性插值
        val result = FloatArray(3)
        for (i in 0..2) {
            val c00 = c000[i] * (1 - rf) + c100[i] * rf
            val c10 = c010[i] * (1 - rf) + c110[i] * rf
            val c01 = c001[i] * (1 - rf) + c101[i] * rf
            val c11 = c011[i] * (1 - rf) + c111[i] * rf

            val c0 = c00 * (1 - gf) + c10 * gf
            val c1 = c01 * (1 - gf) + c11 * gf

            result[i] = c0 * (1 - bf) + c1 * bf
        }

        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LUT3DData
        if (title != other.title) return false
        if (size != other.size) return false
        if (!data.contentEquals(other.data)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + size
        result = 31 * result + data.contentHashCode()
        return result
    }
}
