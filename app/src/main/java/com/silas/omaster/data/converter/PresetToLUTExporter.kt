package com.silas.omaster.data.converter

import com.silas.omaster.data.model.LUTParams
import java.io.File
import java.io.FileWriter

/**
 * 预设到LUT导出器
 * 将预设参数导出为.cube文件
 */
object PresetToLUTExporter {
    
    /**
     * 导出预设参数为LUT文件
     * @param params 预设参数
     * @param size LUT尺寸（33或64）
     * @param outputFile 输出文件
     */
    fun export(params: LUTParams, size: Int = 33, outputFile: File): File {
        val writer = FileWriter(outputFile)
        
        try {
            // CUBE文件头
            writer.write("TITLE \"OMaster Exported LUT\"\n")
            writer.write("# Exported from OMaster Hasselblad Master System\n")
            writer.write("# Generated: ${System.currentTimeMillis()}\n")
            writer.write("\n")
            writer.write("LUT_3D_SIZE $size\n")
            writer.write("\n")
            writer.write("DOMAIN_MIN 0.0 0.0 0.0\n")
            writer.write("DOMAIN_MAX 1.0 1.0 1.0\n")
            writer.write("\n")
            
            // 生成LUT数据
            for (b in 0 until size) {
                for (g in 0 until size) {
                    for (r in 0 until size) {
                        val inputR = r.toFloat() / (size - 1)
                        val inputG = g.toFloat() / (size - 1)
                        val inputB = b.toFloat() / (size - 1)
                        
                        // 应用参数变换
                        val output = applyParams(inputR, inputG, inputB, params)
                        
                        writer.write("${output[0]} ${output[1]} ${output[2]}\n")
                    }
                }
            }
        } finally {
            writer.close()
        }
        
        return outputFile
    }
    
    /**
     * 应用参数变换
     */
    private fun applyParams(r: Float, g: Float, b: Float, params: LUTParams): FloatArray {
        var outR = r
        var outG = g
        var outB = b
        
        // 1. 亮度调整
        val brightness = params.brightness
        outR = (outR + brightness).coerceIn(0f, 1f)
        outG = (outG + brightness).coerceIn(0f, 1f)
        outB = (outB + brightness).coerceIn(0f, 1f)
        
        // 2. 对比度调整
        val contrast = params.contrast + 1f
        outR = ((outR - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
        outG = ((outG - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
        outB = ((outB - 0.5f) * contrast + 0.5f).coerceIn(0f, 1f)
        
        // 3. 饱和度调整
        val saturation = params.saturation + 1f
        val gray = (outR + outG + outB) / 3f
        outR = (gray + (outR - gray) * saturation).coerceIn(0f, 1f)
        outG = (gray + (outG - gray) * saturation).coerceIn(0f, 1f)
        outB = (gray + (outB - gray) * saturation).coerceIn(0f, 1f)
        
        // 4. 色温调整
        val tempShift = params.colorTemperature
        if (tempShift > 0) {
            // 暖调：增加红色，减少蓝色
            outR = (outR + tempShift * 0.1f).coerceIn(0f, 1f)
            outB = (outB - tempShift * 0.1f).coerceIn(0f, 1f)
        } else {
            // 冷调：减少红色，增加蓝色
            outR = (outR + tempShift * 0.1f).coerceIn(0f, 1f)
            outB = (outB - tempShift * 0.1f).coerceIn(0f, 1f)
        }
        
        // 5. 色调调整
        val tintShift = params.tint
        outG = (outG + tintShift * 0.1f).coerceIn(0f, 1f)
        
        // 6. 高光衰减
        val highlightRolloff = params.highlightRolloff
        if (highlightRolloff > 0 && outR > 0.8f) {
            val rolloff = 1f - highlightRolloff * (outR - 0.8f) / 0.2f
            outR = outR * rolloff.coerceIn(0f, 1f)
        }
        if (highlightRolloff > 0 && outG > 0.8f) {
            val rolloff = 1f - highlightRolloff * (outG - 0.8f) / 0.2f
            outG = outG * rolloff.coerceIn(0f, 1f)
        }
        if (highlightRolloff > 0 && outB > 0.8f) {
            val rolloff = 1f - highlightRolloff * (outB - 0.8f) / 0.2f
            outB = outB * rolloff.coerceIn(0f, 1f)
        }
        
        // 7. 阴影提升
        val shadowLift = params.shadowLift
        if (shadowLift > 0 && outR < 0.2f) {
            outR = (outR + shadowLift * (0.2f - outR) / 0.2f).coerceIn(0f, 1f)
        }
        if (shadowLift > 0 && outG < 0.2f) {
            outG = (outG + shadowLift * (0.2f - outG) / 0.2f).coerceIn(0f, 1f)
        }
        if (shadowLift > 0 && outB < 0.2f) {
            outB = (outB + shadowLift * (0.2f - outB) / 0.2f).coerceIn(0f, 1f)
        }
        
        // 8. 肤色保护（简化实现）
        if (params.skinProtection) {
            // 检测是否接近肤色范围
            val isSkinTone = outR > 0.5f && outR < 0.9f &&
                           outG > 0.3f && outG < 0.7f &&
                           outB > 0.2f && outB < 0.5f &&
                           outR > outG && outG > outB
            
            if (isSkinTone) {
                // 保护肤色，减少饱和度变化
                val skinGray = (outR + outG + outB) / 3f
                outR = (skinGray + (outR - skinGray) * 0.8f).coerceIn(0f, 1f)
                outG = (skinGray + (outG - skinGray) * 0.8f).coerceIn(0f, 1f)
                outB = (skinGray + (outB - skinGray) * 0.8f).coerceIn(0f, 1f)
            }
        }
        
        return floatArrayOf(outR, outG, outB)
    }
}