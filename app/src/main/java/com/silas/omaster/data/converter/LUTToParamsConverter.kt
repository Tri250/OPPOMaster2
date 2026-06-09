package com.silas.omaster.data.converter

import com.silas.omaster.data.model.LUTParams
import java.io.File
import kotlin.math.sqrt
import kotlin.math.pow

/**
 * LUT到参数转换器
 * 从LUT文件反推近似参数
 */
object LUTToParamsConverter {
    
    /**
     * 从 LUT 文件反推近似参数
     * 策略：采样关键色彩点（肤色/天空/草地/中性灰），
     *       计算 RGB 偏移量，映射到预设参数空间
     */
    fun approximateParams(lutFile: File): LUTParams {
        val lutData = parseCUBEFile(lutFile)
        
        // 采样关键色彩点
        val skinSample = lutData.sample(0.7f, 0.5f, 0.4f)    // 典型肤色
        val skySample = lutData.sample(0.3f, 0.5f, 0.7f)     // 天空蓝
        val neutralSample = lutData.sample(0.5f, 0.5f, 0.5f) // 中性灰
        
        return LUTParams(
            saturation = calculateSaturationShift(neutralSample),
            contrast = calculateContrastShift(lutData),
            brightness = calculateBrightnessShift(neutralSample),
            colorTemperature = calculateTempShift(skinSample),
            tint = calculateTintShift(skinSample),
            highlightRolloff = calculateHighlightRolloff(lutData),
            shadowLift = calculateShadowLift(lutData),
            skinProtection = evaluateSkinProtection(skinSample)
        )
    }
    
    /**
     * 解析CUBE文件
     */
    private fun parseCUBEFile(file: File): LUTData {
        val lines = file.readLines()
        var size = 33
        val data = mutableListOf<FloatArray>()
        
        for (line in lines) {
            if (line.startsWith("LUT_3D_SIZE")) {
                size = line.split(" ").last().toInt()
            } else if (line.matches(Regex("^[\\d.]+ [\\d.]+ [\\d.]+"))) {
                val values = line.split(" ").map { it.toFloat() }
                data.add(floatArrayOf(values[0], values[1], values[2]))
            }
        }
        
        return LUTData(size, data)
    }
    
    /**
     * 计算饱和度偏移
     */
    private fun calculateSaturationShift(sample: FloatArray): Float {
        val r = sample[0]
        val g = sample[1]
        val b = sample[2]
        
        // 计算输入饱和度（中性灰应为0）
        val inputSat = sqrt(pow(r - 0.5f, 2) + pow(g - 0.5f, 2) + pow(b - 0.5f, 2))
        
        // 输入是中性灰，输出偏离中性灰说明饱和度变化
        return (inputSat - 0f).coerceIn(-1f, 1f)
    }
    
    /**
     * 计算对比度偏移
     */
    private fun calculateContrastShift(lutData: LUTData): Float {
        // 采样最亮和最暗点
        val brightest = lutData.sample(1f, 1f, 1f)
        val darkest = lutData.sample(0f, 0f, 0f)
        
        val brightLum = (brightest[0] + brightest[1] + brightest[2]) / 3f
        val darkLum = (darkest[0] + darkest[1] + darkest[2]) / 3f
        
        // 对比度 = 亮部亮度 - 暗部亮度
        val contrast = brightLum - darkLum
        
        // 标准对比度约1，偏离表示对比度变化
        return (contrast - 1f).coerceIn(-1f, 1f)
    }
    
    /**
     * 计算亮度偏移
     */
    private fun calculateBrightnessShift(sample: FloatArray): Float {
        val lum = (sample[0] + sample[1] + sample[2]) / 3f
        return (lum - 0.5f).coerceIn(-1f, 1f)
    }
    
    /**
     * 计算色温偏移
     */
    private fun calculateTempShift(sample: FloatArray): Float {
        // 肤色偏暖表示正色温偏移
        val r = sample[0]
        val b = sample[2]
        return (r - b).coerceIn(-1f, 1f)
    }
    
    /**
     * 计算色调偏移
     */
    private fun calculateTintShift(sample: FloatArray): Float {
        val g = sample[1]
        val r = sample[0]
        return (g - r).coerceIn(-1f, 1f)
    }
    
    /**
     * 计算高光衰减
     */
    private fun calculateHighlightRolloff(lutData: LUTData): Float {
        val bright = lutData.sample(0.9f, 0.9f, 0.9f)
        val expected = 0.9f
        
        val diff = (bright[0] + bright[1] + bright[2]) / 3f - expected
        return diff.coerceIn(-1f, 1f)
    }
    
    /**
     * 计算阴影提升
     */
    private fun calculateShadowLift(lutData: LUTData): Float {
        val dark = lutData.sample(0.1f, 0.1f, 0.1f)
        val expected = 0.1f
        
        val diff = (dark[0] + dark[1] + dark[2]) / 3f - expected
        return diff.coerceIn(-1f, 1f)
    }
    
    /**
     * 评估肤色保护
     */
    private fun evaluateSkinProtection(sample: FloatArray): Boolean {
        // 肤色偏离较小表示保护开启
        val r = sample[0]
        val g = sample[1]
        val b = sample[2]
        
        val deviation = sqrt(pow(r - 0.7f, 2) + pow(g - 0.5f, 2) + pow(b - 0.4f, 2))
        return deviation < 0.1f
    }
}

/**
 * LUT数据结构
 */
data class LUTData(
    val size: Int,
    val data: List<FloatArray>
) {
    /**
     * 采样指定RGB值
     */
    fun sample(r: Float, g: Float, b: Float): FloatArray {
        // 将RGB映射到LUT网格索引
        val ri = (r * (size - 1)).toInt().coerceIn(0, size - 1)
        val gi = (g * (size - 1)).toInt().coerceIn(0, size - 1)
        val bi = (b * (size - 1)).toInt().coerceIn(0, size - 1)
        
        val index = ri + gi * size + bi * size * size
        return if (index < data.size) data[index] else floatArrayOf(r, g, b)
    }
}