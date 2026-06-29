package com.silas.omaster.renderer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.UUID

/**
 * 预设管理器
 *
 * 功能：
 * 1. 预设强度滑块：0%~200%，通过 lerp 实现任意强度
 * 2. 预设导入：从 JSON 文件导入
 * 3. 预设导出：导出为 JSON 文件
 * 4. 预设库管理：保存/删除/重命名
 */
class PresetManager(context: Context) {

    data class PresetEntry(
        val id: String,
        val name: String,
        val category: String,
        val params: RenderParameters,
        val createdAt: Long = System.currentTimeMillis(),
        val isBuiltIn: Boolean = false
    )

    private val presetsDir = File(context.filesDir, "presets").apply { mkdirs() }
    private val _presets = mutableListOf<PresetEntry>()
    val presets: List<PresetEntry> get() = _presets.toList()

    /**
     * 应用预设（支持强度 0%~200%）
     *
     * @param preset 目标预设
     * @param intensity 强度百分比（0.0~2.0，1.0 = 100%原始效果）
     * @return 调整强度后的 RenderParameters
     */
    fun applyWithIntensity(preset: PresetEntry, intensity: Float): RenderParameters {
        val t = intensity.coerceIn(0f, 2f)
        val identity = RenderParameters()
        if (t <= 1f) {
            return identity.lerp(preset.params, t)
        } else {
            // 100-200%: lerp from preset to 2x preset (extrapolation)
            val doubled = extrapolateParams(preset.params, 2f)
            return preset.params.lerp(doubled, t - 1f)
        }
    }

    /**
     * 保存预设到文件
     */
    suspend fun savePreset(preset: PresetEntry): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(presetsDir, "${preset.id}$PRESET_EXTENSION")
            val json = presetToJson(preset)
            FileOutputStream(file).use { fos ->
                fos.write(json.toString(2).toByteArray(Charsets.UTF_8))
            }
            // 同步到内存列表
            val idx = _presets.indexOfFirst { it.id == preset.id }
            if (idx >= 0) {
                _presets[idx] = preset
            } else {
                _presets.add(preset)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save preset: ${preset.name}", e)
            false
        }
    }

    /**
     * 从文件导入预设
     */
    suspend fun importPreset(file: File): PresetEntry? = withContext(Dispatchers.IO) {
        try {
            val content = BufferedReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8)).use { br ->
                br.readText()
            }
            val json = JSONObject(content)
            val preset = jsonToPreset(json)
            // 重新生成 ID 避免冲突
            val imported = preset.copy(
                id = UUID.randomUUID().toString(),
                isBuiltIn = false
            )
            // 保存到预设目录
            val saveResult = savePreset(imported)
            if (saveResult) imported else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import preset from: ${file.absolutePath}", e)
            null
        }
    }

    /**
     * 导出预设到文件
     */
    suspend fun exportPreset(preset: PresetEntry, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = presetToJson(preset)
            FileOutputStream(outputFile).use { fos ->
                fos.write(json.toString(2).toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export preset: ${preset.name}", e)
            false
        }
    }

    /**
     * 删除预设
     */
    fun deletePreset(presetId: String) {
        _presets.removeAll { it.id == presetId && !it.isBuiltIn }
        val file = File(presetsDir, "${presetId}$PRESET_EXTENSION")
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * 重命名预设
     */
    fun renamePreset(presetId: String, newName: String): Boolean {
        val idx = _presets.indexOfFirst { it.id == presetId }
        if (idx < 0) return false
        val preset = _presets[idx]
        if (preset.isBuiltIn) return false

        val renamed = preset.copy(name = newName)
        _presets[idx] = renamed

        // 持久化
        val file = File(presetsDir, "${presetId}$PRESET_EXTENSION")
        if (file.exists()) {
            try {
                val json = presetToJson(renamed)
                FileOutputStream(file).use { fos ->
                    fos.write(json.toString(2).toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist rename", e)
            }
        }
        return true
    }

    /**
     * RenderParameters 转 JSON
     */
    fun paramsToJson(params: RenderParameters): JSONObject {
        val json = JSONObject()

        // 基础调整参数
        json.put("saturation", params.saturation)
        json.put("contrast", params.contrast)
        json.put("brightness", params.brightness)
        json.put("warmth", params.warmth)

        // 细节增强参数
        json.put("sharpness", params.sharpness)
        json.put("clarity", params.clarity)
        json.put("texture", params.texture)

        // 色彩调整参数
        json.put("vibrance", params.vibrance)

        // 光影调整参数
        json.put("highlights", params.highlights)
        json.put("shadows", params.shadows)
        json.put("whites", params.whites)
        json.put("blacks", params.blacks)
        json.put("exposure", params.exposure)

        // 效果参数
        json.put("grain", params.grain)
        json.put("fade", params.fade)
        json.put("dehaze", params.dehaze)

        // 降噪与平滑参数
        json.put("denoise", params.denoise)
        json.put("skinSmooth", params.skinSmooth)

        // HSL 8 通道
        json.put("hslRedHue", params.hslRedHue)
        json.put("hslRedSaturation", params.hslRedSaturation)
        json.put("hslRedLuminance", params.hslRedLuminance)
        json.put("hslOrangeHue", params.hslOrangeHue)
        json.put("hslOrangeSaturation", params.hslOrangeSaturation)
        json.put("hslOrangeLuminance", params.hslOrangeLuminance)
        json.put("hslYellowHue", params.hslYellowHue)
        json.put("hslYellowSaturation", params.hslYellowSaturation)
        json.put("hslYellowLuminance", params.hslYellowLuminance)
        json.put("hslGreenHue", params.hslGreenHue)
        json.put("hslGreenSaturation", params.hslGreenSaturation)
        json.put("hslGreenLuminance", params.hslGreenLuminance)
        json.put("hslCyanHue", params.hslCyanHue)
        json.put("hslCyanSaturation", params.hslCyanSaturation)
        json.put("hslCyanLuminance", params.hslCyanLuminance)
        json.put("hslBlueHue", params.hslBlueHue)
        json.put("hslBlueSaturation", params.hslBlueSaturation)
        json.put("hslBlueLuminance", params.hslBlueLuminance)
        json.put("hslPurpleHue", params.hslPurpleHue)
        json.put("hslPurpleSaturation", params.hslPurpleSaturation)
        json.put("hslPurpleLuminance", params.hslPurpleLuminance)
        json.put("hslMagentaHue", params.hslMagentaHue)
        json.put("hslMagentaSaturation", params.hslMagentaSaturation)
        json.put("hslMagentaLuminance", params.hslMagentaLuminance)

        // 曲线 LUT（4 通道 x 256 点）
        json.put("curveRgbLut", floatArrayToJson(params.curveRgbLut))
        json.put("curveRedLut", floatArrayToJson(params.curveRedLut))
        json.put("curveGreenLut", floatArrayToJson(params.curveGreenLut))
        json.put("curveBlueLut", floatArrayToJson(params.curveBlueLut))

        // 3D LUT 状态（仅保存强度，纹理 ID 为运行时状态）
        json.put("lutStrength", params.lutStrength)
        json.put("lutEnabled", params.lutEnabled)
        json.put("lutSize", params.lutSize)

        return json
    }

    /**
     * JSON 转 RenderParameters
     */
    fun jsonToParams(json: JSONObject): RenderParameters {
        return RenderParameters(
            saturation = json.optDouble("saturation", 0.0).toFloat(),
            contrast = json.optDouble("contrast", 0.0).toFloat(),
            brightness = json.optDouble("brightness", 0.0).toFloat(),
            warmth = json.optDouble("warmth", 0.0).toFloat(),
            sharpness = json.optDouble("sharpness", 0.0).toFloat(),
            clarity = json.optDouble("clarity", 0.0).toFloat(),
            texture = json.optDouble("texture", 0.0).toFloat(),
            vibrance = json.optDouble("vibrance", 0.0).toFloat(),
            highlights = json.optDouble("highlights", 0.0).toFloat(),
            shadows = json.optDouble("shadows", 0.0).toFloat(),
            whites = json.optDouble("whites", 0.0).toFloat(),
            blacks = json.optDouble("blacks", 0.0).toFloat(),
            exposure = json.optDouble("exposure", 0.0).toFloat(),
            grain = json.optDouble("grain", 0.0).toFloat(),
            fade = json.optDouble("fade", 0.0).toFloat(),
            dehaze = json.optDouble("dehaze", 0.0).toFloat(),
            denoise = json.optDouble("denoise", 0.0).toFloat(),
            skinSmooth = json.optDouble("skinSmooth", 0.0).toFloat(),

            // HSL 8 通道
            hslRedHue = json.optDouble("hslRedHue", 0.0).toFloat(),
            hslRedSaturation = json.optDouble("hslRedSaturation", 0.0).toFloat(),
            hslRedLuminance = json.optDouble("hslRedLuminance", 0.0).toFloat(),
            hslOrangeHue = json.optDouble("hslOrangeHue", 0.0).toFloat(),
            hslOrangeSaturation = json.optDouble("hslOrangeSaturation", 0.0).toFloat(),
            hslOrangeLuminance = json.optDouble("hslOrangeLuminance", 0.0).toFloat(),
            hslYellowHue = json.optDouble("hslYellowHue", 0.0).toFloat(),
            hslYellowSaturation = json.optDouble("hslYellowSaturation", 0.0).toFloat(),
            hslYellowLuminance = json.optDouble("hslYellowLuminance", 0.0).toFloat(),
            hslGreenHue = json.optDouble("hslGreenHue", 0.0).toFloat(),
            hslGreenSaturation = json.optDouble("hslGreenSaturation", 0.0).toFloat(),
            hslGreenLuminance = json.optDouble("hslGreenLuminance", 0.0).toFloat(),
            hslCyanHue = json.optDouble("hslCyanHue", 0.0).toFloat(),
            hslCyanSaturation = json.optDouble("hslCyanSaturation", 0.0).toFloat(),
            hslCyanLuminance = json.optDouble("hslCyanLuminance", 0.0).toFloat(),
            hslBlueHue = json.optDouble("hslBlueHue", 0.0).toFloat(),
            hslBlueSaturation = json.optDouble("hslBlueSaturation", 0.0).toFloat(),
            hslBlueLuminance = json.optDouble("hslBlueLuminance", 0.0).toFloat(),
            hslPurpleHue = json.optDouble("hslPurpleHue", 0.0).toFloat(),
            hslPurpleSaturation = json.optDouble("hslPurpleSaturation", 0.0).toFloat(),
            hslPurpleLuminance = json.optDouble("hslPurpleLuminance", 0.0).toFloat(),
            hslMagentaHue = json.optDouble("hslMagentaHue", 0.0).toFloat(),
            hslMagentaSaturation = json.optDouble("hslMagentaSaturation", 0.0).toFloat(),
            hslMagentaLuminance = json.optDouble("hslMagentaLuminance", 0.0).toFloat(),

            // 曲线 LUT
            curveRgbLut = jsonToFloatArray(json.optJSONArray("curveRgbLut"), RenderParameters.IDENTITY_CURVE.copyOf()),
            curveRedLut = jsonToFloatArray(json.optJSONArray("curveRedLut"), RenderParameters.IDENTITY_CURVE.copyOf()),
            curveGreenLut = jsonToFloatArray(json.optJSONArray("curveGreenLut"), RenderParameters.IDENTITY_CURVE.copyOf()),
            curveBlueLut = jsonToFloatArray(json.optJSONArray("curveBlueLut"), RenderParameters.IDENTITY_CURVE.copyOf()),

            // 3D LUT 状态
            lutStrength = json.optDouble("lutStrength", 0.0).toFloat(),
            lutEnabled = json.optBoolean("lutEnabled", false),
            lutSize = json.optInt("lutSize", 0)
        )
    }

    /**
     * 加载所有已保存的预设
     */
    suspend fun loadAllPresets() = withContext(Dispatchers.IO) {
        _presets.clear()

        val files = presetsDir.listFiles { _, name -> name.endsWith(PRESET_EXTENSION) } ?: return@withContext

        for (file in files) {
            try {
                val content = BufferedReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8)).use { br ->
                    br.readText()
                }
                val json = JSONObject(content)
                val preset = jsonToPreset(json)
                _presets.add(preset)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load preset from: ${file.name}", e)
            }
        }

        // 按创建时间排序
        _presets.sortBy { it.createdAt }
    }

    /**
     * 参数外推（用于 > 100% 强度）
     * 将所有调整参数乘以 factor，使其可以用于 lerp 外推
     */
    private fun extrapolateParams(params: RenderParameters, factor: Float): RenderParameters {
        return RenderParameters(
            saturation = (params.saturation * factor).coerceIn(-100f, 100f),
            contrast = (params.contrast * factor).coerceIn(-100f, 100f),
            brightness = (params.brightness * factor).coerceIn(-100f, 100f),
            warmth = (params.warmth * factor).coerceIn(-100f, 100f),
            sharpness = (params.sharpness * factor).coerceIn(0f, 100f),
            clarity = (params.clarity * factor).coerceIn(0f, 100f),
            texture = (params.texture * factor).coerceIn(-100f, 100f),
            vibrance = (params.vibrance * factor).coerceIn(-100f, 100f),
            highlights = (params.highlights * factor).coerceIn(-100f, 100f),
            shadows = (params.shadows * factor).coerceIn(-100f, 100f),
            whites = (params.whites * factor).coerceIn(-100f, 100f),
            blacks = (params.blacks * factor).coerceIn(-100f, 100f),
            exposure = (params.exposure * factor).coerceIn(-100f, 100f),
            grain = (params.grain * factor).coerceIn(0f, 100f),
            fade = (params.fade * factor).coerceIn(0f, 100f),
            dehaze = (params.dehaze * factor).coerceIn(0f, 100f),
            denoise = (params.denoise * factor).coerceIn(0f, 100f),
            skinSmooth = (params.skinSmooth * factor).coerceIn(0f, 100f),

            // HSL 8 通道
            hslRedHue = (params.hslRedHue * factor).coerceIn(-180f, 180f),
            hslRedSaturation = (params.hslRedSaturation * factor).coerceIn(-100f, 100f),
            hslRedLuminance = (params.hslRedLuminance * factor).coerceIn(-100f, 100f),
            hslOrangeHue = (params.hslOrangeHue * factor).coerceIn(-180f, 180f),
            hslOrangeSaturation = (params.hslOrangeSaturation * factor).coerceIn(-100f, 100f),
            hslOrangeLuminance = (params.hslOrangeLuminance * factor).coerceIn(-100f, 100f),
            hslYellowHue = (params.hslYellowHue * factor).coerceIn(-180f, 180f),
            hslYellowSaturation = (params.hslYellowSaturation * factor).coerceIn(-100f, 100f),
            hslYellowLuminance = (params.hslYellowLuminance * factor).coerceIn(-100f, 100f),
            hslGreenHue = (params.hslGreenHue * factor).coerceIn(-180f, 180f),
            hslGreenSaturation = (params.hslGreenSaturation * factor).coerceIn(-100f, 100f),
            hslGreenLuminance = (params.hslGreenLuminance * factor).coerceIn(-100f, 100f),
            hslCyanHue = (params.hslCyanHue * factor).coerceIn(-180f, 180f),
            hslCyanSaturation = (params.hslCyanSaturation * factor).coerceIn(-100f, 100f),
            hslCyanLuminance = (params.hslCyanLuminance * factor).coerceIn(-100f, 100f),
            hslBlueHue = (params.hslBlueHue * factor).coerceIn(-180f, 180f),
            hslBlueSaturation = (params.hslBlueSaturation * factor).coerceIn(-100f, 100f),
            hslBlueLuminance = (params.hslBlueLuminance * factor).coerceIn(-100f, 100f),
            hslPurpleHue = (params.hslPurpleHue * factor).coerceIn(-180f, 180f),
            hslPurpleSaturation = (params.hslPurpleSaturation * factor).coerceIn(-100f, 100f),
            hslPurpleLuminance = (params.hslPurpleLuminance * factor).coerceIn(-100f, 100f),
            hslMagentaHue = (params.hslMagentaHue * factor).coerceIn(-180f, 180f),
            hslMagentaSaturation = (params.hslMagentaSaturation * factor).coerceIn(-100f, 100f),
            hslMagentaLuminance = (params.hslMagentaLuminance * factor).coerceIn(-100f, 100f),

            // 曲线和 LUT 保持不变（外推不适用于查找表）
            curveRgbLut = params.curveRgbLut,
            curveRedLut = params.curveRedLut,
            curveGreenLut = params.curveGreenLut,
            curveBlueLut = params.curveBlueLut,
            lutStrength = (params.lutStrength * factor).coerceIn(0f, 1f),
            lutSize = params.lutSize,
            lutEnabled = params.lutEnabled
        )
    }

    /**
     * 将 PresetEntry 转为 JSON
     */
    private fun presetToJson(preset: PresetEntry): JSONObject {
        val json = JSONObject()
        json.put("id", preset.id)
        json.put("name", preset.name)
        json.put("category", preset.category)
        json.put("createdAt", preset.createdAt)
        json.put("isBuiltIn", preset.isBuiltIn)
        json.put("params", paramsToJson(preset.params))
        return json
    }

    /**
     * 将 JSON 转为 PresetEntry
     */
    private fun jsonToPreset(json: JSONObject): PresetEntry {
        val paramsJson = json.optJSONObject("params") ?: JSONObject()
        return PresetEntry(
            id = json.optString("id", UUID.randomUUID().toString()),
            name = json.optString("name", "Unnamed"),
            category = json.optString("category", "Custom"),
            params = jsonToParams(paramsJson),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            isBuiltIn = json.optBoolean("isBuiltIn", false)
        )
    }

    /**
     * FloatArray 转 JSONArray
     */
    private fun floatArrayToJson(array: FloatArray): JSONArray {
        val jsonArray = JSONArray()
        for (value in array) {
            jsonArray.put(value.toDouble())
        }
        return jsonArray
    }

    /**
     * JSONArray 转 FloatArray
     */
    private fun jsonToFloatArray(jsonArray: JSONArray?, default: FloatArray): FloatArray {
        if (jsonArray == null || jsonArray.length() != 256) return default
        val result = FloatArray(256)
        for (i in 0 until 256) {
            result[i] = jsonArray.optDouble(i, i / 255.0).toFloat()
        }
        return result
    }

    companion object {
        private const val TAG = "PresetManager"
        private const val PRESET_EXTENSION = ".omaster_preset"
    }
}
