package com.silas.omaster.renderer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.silas.omaster.data.local.LUTDownloadManager
import com.silas.omaster.data.model.LUTResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * LUT 应用器
 * 
 * 功能：
 * - 加载 LUT 数据
 * - 应用 LUT 到 Bitmap
 * - GPU 加速渲染（通过 OpenGL ES）
 * - 强度调节
 */
object LUTApplier {

    private const val TAG = "LUTApplier"

    /**
     * 应用 LUT 到 Bitmap
     * 
     * @param context 上下文
     * @param lut LUT 资源
     * @param bitmap 原始图片
     * @param intensity LUT 强度（0.0 - 1.0），默认 1.0（完全应用）
     * @return 处理后的图片
     */
    suspend fun applyLUT(
        context: Context,
        lut: LUTResource,
        bitmap: Bitmap,
        intensity: Float = 1.0f
    ): Bitmap = withContext(Dispatchers.Default) {
        
        // 1. 检查 LUT 是否已下载
        if (!lut.isDownloaded(context)) {
            Log.w(TAG, "LUT ${lut.id} 未下载，返回原图")
            return@withContext bitmap
        }
        
        // 2. 检查文件完整性
        if (!lut.verifyIntegrity(context)) {
            Log.w(TAG, "LUT ${lut.id} 文件损坏，返回原图")
            return@withContext bitmap
        }
        
        // 3. 解析 CUBE 文件
        val lutFile = lut.getLocalPath(context)
        val lutData = LUTDownloadManager.parseCubeFile(lutFile)
        
        if (lutData == null) {
            Log.w(TAG, "解析 LUT ${lut.id} 失败，返回原图")
            return@withContext bitmap
        }
        
        // 4. 应用 LUT（CPU 方式，后续可优化为 GPU）
        applyLUTData(bitmap, lutData, lut.cubeSize ?: 33, intensity)
    }

    /**
     * 应用 LUT 数据到 Bitmap（CPU 实现）
     * 
     * @param bitmap 原始图片
     * @param lutData LUT 数据（RGB 值数组）
     * @param cubeSize LUT 立方体尺寸（默认 33）
     * @param intensity 应用强度
     * @return 处理后的图片
     */
    private fun applyLUTData(
        bitmap: Bitmap,
        lutData: FloatArray,
        cubeSize: Int,
        intensity: Float
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // 创建输出 Bitmap
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // 获取原始像素数据
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // 处理每个像素
        for (i in pixels.indices) {
            val pixel = pixels[i]
            
            // 提取 RGB 值（0-255）
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            // 归一化到 0.0 - 1.0
            val normalizedR = r / 255.0f
            val normalizedG = g / 255.0f
            val normalizedB = b / 255.0f
            
            // 在 3D LUT 中查找对应的颜色值
            val lutColor = lookupLUT(lutData, cubeSize, normalizedR, normalizedG, normalizedB)
            
            // 混合原始颜色和 LUT 颜色（根据强度）
            val finalR = (normalizedR * (1 - intensity) + lutColor[0] * intensity) * 255
            val finalG = (normalizedG * (1 - intensity) + lutColor[1] * intensity) * 255
            val finalB = (normalizedB * (1 - intensity) + lutColor[2] * intensity) * 255
            
            // 转换回 0-255 并写入输出
            val outputR = finalR.toInt().coerceIn(0, 255)
            val outputG = finalG.toInt().coerceIn(0, 255)
            val outputB = finalB.toInt().coerceIn(0, 255)
            
            // 保留原始 Alpha
            val alpha = (pixel shr 24) and 0xFF
            
            pixels[i] = (alpha shl 24) or (outputR shl 16) or (outputG shl 8) or outputB
        }
        
        // 写入输出 Bitmap
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        
        return output
    }

    /**
     * 在 3D LUT 中查找颜色值（三线性插值）
     * 
     * @param lutData LUT 数据
     * @param cubeSize LUT 立方体尺寸
     * @param r 红色值（0.0 - 1.0）
     * @param g 绿色值（0.0 - 1.0）
     * @param b 蓝色值（0.0 - 1.0）
     * @return 查找到的 RGB 值数组
     */
    private fun lookupLUT(
        lutData: FloatArray,
        cubeSize: Int,
        r: Float,
        g: Float,
        b: Float
    ): FloatArray {
        // 将颜色值映射到 LUT 立方体坐标
        val x = r * (cubeSize - 1)
        val y = g * (cubeSize - 1)
        val z = b * (cubeSize - 1)
        
        // 获取整数坐标和分数部分（用于插值）
        val x0 = x.toInt().coerceIn(0, cubeSize - 2)
        val y0 = y.toInt().coerceIn(0, cubeSize - 2)
        val z0 = z.toInt().coerceIn(0, cubeSize - 2)
        
        val x1 = x0 + 1
        val y1 = y0 + 1
        val z1 = z0 + 1
        
        val dx = x - x0
        val dy = y - y0
        val dz = z - z0
        
        // 三线性插值：获取 8 个邻近点的颜色值
        val c000 = getLUTValue(lutData, cubeSize, x0, y0, z0)
        val c001 = getLUTValue(lutData, cubeSize, x0, y0, z1)
        val c010 = getLUTValue(lutData, cubeSize, x0, y1, z0)
        val c011 = getLUTValue(lutData, cubeSize, x0, y1, z1)
        val c100 = getLUTValue(lutData, cubeSize, x1, y0, z0)
        val c101 = getLUTValue(lutData, cubeSize, x1, y0, z1)
        val c110 = getLUTValue(lutData, cubeSize, x1, y1, z0)
        val c111 = getLUTValue(lutData, cubeSize, x1, y1, z1)
        
        // 三线性插值公式
        val c00 = interpolate(c000, c100, dx)
        val c01 = interpolate(c001, c101, dx)
        val c10 = interpolate(c010, c110, dx)
        val c11 = interpolate(c011, c111, dx)
        
        val c0 = interpolate(c00, c10, dy)
        val c1 = interpolate(c01, c11, dy)
        
        return interpolate(c0, c1, dz)
    }

    /**
     * 从 LUT 数据中获取指定坐标的颜色值
     * 
     * CUBE 文件的坐标顺序：B -> G -> R（蓝-绿-红）
     */
    private fun getLUTValue(
        lutData: FloatArray,
        cubeSize: Int,
        x: Int,
        y: Int,
        z: Int
    ): FloatArray {
        // 计算索引（CUBE 文件格式：B 为主索引）
        val index = (z * cubeSize * cubeSize + y * cubeSize + x) * 3
        
        return FloatArray(3) {
            lutData[index + it]
        }
    }

    /**
     * 线性插值
     */
    private fun interpolate(a: FloatArray, b: FloatArray, t: Float): FloatArray {
        return FloatArray(3) {
            a[it] + (b[it] - a[it]) * t
        }
    }

    /**
     * 批量应用多个 LUT（叠加效果）
     */
    suspend fun applyMultipleLUTs(
        context: Context,
        luts: List<LUTResource>,
        bitmap: Bitmap,
        intensities: List<Float>
    ): Bitmap {
        var currentBitmap = bitmap
        
        luts.forEachIndexed { index, lut ->
            val intensity = intensities.getOrElse(index) { 1.0f }
            currentBitmap = applyLUT(context, lut, currentBitmap, intensity)
        }
        
        return currentBitmap
    }

    /**
     * 创建 LUT 预览（快速应用，用于 UI 展示）
     */
    fun createLUTPreview(
        lutData: FloatArray,
        cubeSize: Int,
        width: Int = 100,
        height: Int = 100
    ): Bitmap {
        val preview = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        
        // 创建渐变预览图
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = x / width.toFloat()
                val g = y / height.toFloat()
                val b = 0.5f
                
                val lutColor = lookupLUT(lutData, cubeSize, r, g, b)
                
                val outputR = (lutColor[0] * 255).toInt().coerceIn(0, 255)
                val outputG = (lutColor[1] * 255).toInt().coerceIn(0, 255)
                val outputB = (lutColor[2] * 255).toInt().coerceIn(0, 255)
                
                pixels[y * width + x] = (0xFF shl 24) or (outputR shl 16) or (outputG shl 8) or outputB
            }
        }
        
        preview.setPixels(pixels, 0, width, 0, 0, width, height)
        return preview
    }
}