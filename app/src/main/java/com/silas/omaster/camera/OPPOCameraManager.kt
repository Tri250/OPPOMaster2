package com.silas.omaster.camera

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.SoftLightMode

/**
 * OPPO Find X9 哈苏大师模式相机参数管理器
 *
 * 实现真实的相机参数应用，支持以下四种应用方式（按优先级降序）：
 * 1. ContentProvider：写入 OPPO 相机大师模式 ContentProvider
 * 2. System Settings：通过 Settings.System/Global 写入 OPPO 专属设置键
 * 3. Camera Intent：启动 OPPO 相机大师模式并传递参数
 * 4. Clipboard Fallback：将参数复制到剪贴板供手动输入
 *
 * 单例模式，通过 [getInstance] 获取实例。
 */
class OPPOCameraManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "OPPOCameraManager"

        // OPPO 大师模式 ContentProvider URI
        private const val MASTER_PROVIDER_AUTHORITY = "com.oppo.camera.master.provider"
        private const val MASTER_PARAMS_PATH = "params"
        val MASTER_PROVIDER_URI: Uri = Uri.parse("content://$MASTER_PROVIDER_AUTHORITY/$MASTER_PARAMS_PATH")

        // OPPO 相机大师模式 Intent Action
        private const val ACTION_MASTER_MODE = "com.oppo.camera.action.MASTER_MODE"
        private const val OPPO_CAMERA_PACKAGE = "com.oppo.camera"

        // OPPO 专属 Settings 键名前缀
        private const val SETTINGS_PREFIX = "oppo_camera_master_"

        // 参数范围常量
        private val RANGE_SATURATION = -100 to 100
        private val RANGE_CONTRAST = -100 to 100
        private val RANGE_TONE = -100 to 100
        private val RANGE_WARMTH = -100 to 100
        private val RANGE_SHARPNESS = 0 to 100
        private val RANGE_CLARITY = 0 to 100
        private val RANGE_ISO = 100 to 12800
        private val RANGE_WHITE_BALANCE = 2000 to 8000
        private val RANGE_EXPOSURE_COMPENSATION = -3.0 to 3.0
        private val RANGE_VIGNETTE = -100 to 100
        private val RANGE_CYAN_MAGENTA = -100 to 100
        private val RANGE_BRIGHTNESS = -100 to 100

        // ContentProvider 列名
        private const val COL_PARAM_KEY = "param_key"
        private const val COL_PARAM_VALUE = "param_value"
        private const val COL_PARAM_TYPE = "param_type"   // int / float / string

        // Intent Extra 键名
        private const val EXTRA_SATURATION = "master_saturation"
        private const val EXTRA_CONTRAST = "master_contrast"
        private const val EXTRA_TONE = "master_tone"
        private const val EXTRA_WARMTH = "master_warmth"
        private const val EXTRA_SHARPNESS = "master_sharpness"
        private const val EXTRA_CLARITY = "master_clarity"
        private const val EXTRA_BRIGHTNESS = "master_brightness"
        private const val EXTRA_ISO = "master_iso"
        private const val EXTRA_SHUTTER_SPEED = "master_shutter_speed"
        private const val EXTRA_WHITE_BALANCE = "master_white_balance"
        private const val EXTRA_EXPOSURE_COMPENSATION = "master_exposure_compensation"
        private const val EXTRA_VIGNETTE = "master_vignette"
        private const val EXTRA_CYAN_MAGENTA = "master_cyan_magenta"
        private const val EXTRA_SOFT_LIGHT = "master_soft_light"
        private const val EXTRA_FILTER = "master_filter"

        // OPPO/Realme/OnePlus 品牌标识
        private val OPPO_FAMILY_MANUFACTURERS = setOf("oppo", "realme", "oneplus", "one plus")
        private val OPPO_FAMILY_BRANDS = setOf("oppo", "realme", "oneplus", "one plus")

        // Find X 系列型号前缀
        private val FIND_X_MODEL_PREFIXES = listOf("CPH", "PFM", "PFT", "PDS", "RMX", "CPH3", "PFHM")

        /**
         * 参数白名单：允许校验的参数键名集合。
         * 不在白名单中的键会记录警告日志，但不会作为校验错误处理。
         */
        private val ALLOWED_PARAM_KEYS: Set<String> = setOf(
            "saturation", "contrast", "tone", "warmth", "sharpness", "clarity",
            "iso", "whiteBalance", "exposureCompensation", "vignette", "cyanMagenta",
            "brightness", "highlights", "shadows",
            "softLight", "filter", "shutterSpeed"
        )

        /**
         * 设备能力缓存 TTL（毫秒），默认 5 分钟。
         * ContentProvider 可能在 OPPO 相机更新后出现或消失，因此需要定期刷新。
         */
        private const val DEVICE_CAPABILITY_CACHE_TTL_MS = 5 * 60 * 1000L

        @Volatile
        private var instance: OPPOCameraManager? = null

        fun getInstance(context: Context): OPPOCameraManager {
            return instance ?: synchronized(this) {
                instance ?: OPPOCameraManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // 缓存设备能力检测结果
    private var cachedDeviceCapability: DeviceCapability? = null

    /**
     * 缓存创建时间戳（毫秒），用于 TTL 过期检测。
     */
    private var cachedDeviceCapabilityTimestamp: Long = 0L

    // ==================== 公共 API ====================

    /**
     * 检测当前设备能力
     *
     * 通过 Build.MANUFACTURER、Build.BRAND、Build.MODEL 判断设备类型，
     * 并尝试探测 OPPO 大师模式 ContentProvider 是否存在。
     *
     * 结果会被缓存，但会在 [DEVICE_CAPABILITY_CACHE_TTL_MS] 后自动过期。
     * ContentProvider 可能在 OPPO 相机更新后出现或消失，因此需要定期刷新。
     *
     * @return 设备能力信息
     */
    fun detectDeviceCapability(): DeviceCapability {
        val now = System.currentTimeMillis()
        cachedDeviceCapability?.let { cached ->
            if (now - cachedDeviceCapabilityTimestamp < DEVICE_CAPABILITY_CACHE_TTL_MS) {
                return cached
            }
            Log.d(TAG, "设备能力缓存已过期（TTL=${DEVICE_CAPABILITY_CACHE_TTL_MS}ms），重新检测")
        }

        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL ?: ""
        val modelLower = model.lowercase()

        val isOppoDevice = manufacturer in OPPO_FAMILY_MANUFACTURERS ||
                brand in OPPO_FAMILY_BRANDS

        val isFindXSeries = isOppoDevice && (
                modelLower.contains("find x") ||
                modelLower.contains("findx") ||
                FIND_X_MODEL_PREFIXES.any { model.startsWith(it, ignoreCase = true) } ||
                modelLower.contains("rmx") // Realme X 系列
                )

        val supportsContentProvider = probeContentProvider()

        val supportsMasterMode = isOppoDevice && (
                isFindXSeries ||
                modelLower.contains("find") ||
                modelLower.contains("reno") ||
                modelLower.contains("ace") ||
                modelLower.contains("gt") // Realme GT 系列
                )

        val capability = DeviceCapability(
            manufacturer = Build.MANUFACTURER,
            model = model,
            isOppoDevice = isOppoDevice,
            isFindXSeries = isFindXSeries,
            supportsMasterMode = supportsMasterMode,
            supportsContentProvider = supportsContentProvider
        )

        Log.d(TAG, "设备能力检测: manufacturer=${capability.manufacturer}, " +
                "model=${capability.model}, isOppo=${capability.isOppoDevice}, " +
                "isFindX=${capability.isFindXSeries}, masterMode=${capability.supportsMasterMode}, " +
                "contentProvider=${capability.supportsContentProvider}")

        cachedDeviceCapability = capability
        cachedDeviceCapabilityTimestamp = now
        return capability
    }

    /**
     * 应用 MasterPreset 参数到 OPPO 相机大师模式
     *
     * 按优先级依次尝试：ContentProvider → System Settings → Camera Intent → Clipboard
     * 任何一种方式成功即返回对应结果。
     *
     * @param preset 大师预设
     * @return 应用结果
     */
    fun applyPreset(preset: MasterPreset): CameraApplyResult {
        Log.d(TAG, "开始应用预设: ${preset.name}")
        val params = buildParamMap(preset)
        return applyParams(params)
    }

    /**
     * 应用 HasselbladParams 参数到 OPPO 相机大师模式
     *
     * @param params 哈苏参数
     * @param iso ISO 值（可选）
     * @param shutterSpeed 快门速度字符串（可选，如 "1/125"）
     * @param whiteBalanceK 白平衡色温值（可选，2000-8000）
     * @param exposureCompensation 曝光补偿（可选，-3.0 ~ +3.0）
     * @return 应用结果
     */
    fun applyHasselbladParams(
        params: HasselbladParams,
        iso: Int? = null,
        shutterSpeed: String? = null,
        whiteBalanceK: Int? = null,
        exposureCompensation: Double? = null
    ): CameraApplyResult {
        Log.d(TAG, "开始应用哈苏参数: tone=${params.tone}, saturation=${params.saturation}, " +
                "contrast=${params.contrast}, colorTemp=${params.colorTemp}")
        val paramMap = buildParamMapFromHasselblad(params, iso, shutterSpeed, whiteBalanceK, exposureCompensation)
        return applyParams(paramMap)
    }

    /**
     * 应用原始参数 Map 到 OPPO 相机大师模式
     *
     * @param params 参数键值对，键名参考 PARAM_KEY_* 常量
     * @return 应用结果
     */
    fun applyParams(params: Map<String, Any>): CameraApplyResult {
        Log.d(TAG, "开始应用参数: ${params.keys}")

        // 参数校验
        val validationResult = validateParams(params)
        if (validationResult.isInvalid) {
            Log.w(TAG, "参数校验失败: ${validationResult.errors}")
            return CameraApplyResult.Failed(
                reason = "参数校验失败: ${validationResult.errors.joinToString("; ")}",
                suggestion = "请检查参数范围：饱和度/对比度/影调/冷暖/暗角/青品调 -100~100，" +
                        "锐度/清晰度 0~100，ISO 100~12800，白平衡 2000K~8000K，曝光补偿 -3.0~+3.0"
            )
        }

        val capability = detectDeviceCapability()

        // 优先级 1：ContentProvider
        if (capability.supportsContentProvider) {
            val result = applyViaContentProvider(params)
            if (result != null) {
                Log.d(TAG, "ContentProvider 方式应用成功")
                return result
            }
            Log.w(TAG, "ContentProvider 方式失败，尝试下一方式")
        }

        // 优先级 2：System Settings
        if (capability.isOppoDevice) {
            val result = applyViaSystemSettings(params)
            if (result != null) {
                Log.d(TAG, "System Settings 方式应用成功")
                return result
            }
            Log.w(TAG, "System Settings 方式失败，尝试下一方式")
        }

        // 优先级 3：Camera Intent
        if (capability.supportsMasterMode) {
            val result = applyViaCameraIntent(params)
            if (result != null) {
                Log.d(TAG, "Camera Intent 方式应用成功")
                return result
            }
            Log.w(TAG, "Camera Intent 方式失败，尝试下一方式")
        }

        // 优先级 4：Clipboard Fallback
        val result = applyViaClipboard(params)
        Log.d(TAG, "Clipboard Fallback 方式应用完成")
        return result
    }

    /**
     * 清除设备能力缓存，强制下次重新检测
     */
    fun invalidateDeviceCache() {
        cachedDeviceCapability = null
        cachedDeviceCapabilityTimestamp = 0L
        Log.d(TAG, "设备能力缓存已清除")
    }

    // ==================== ContentProvider 应用 ====================

    /**
     * 通过 OPPO 大师模式 ContentProvider 写入参数
     *
     * 向 content://com.oppo.camera.master.provider/params 写入 ContentValues，
     * 每个参数为一行记录，包含 param_key、param_value、param_type 三列。
     *
     * @return 成功返回 CameraApplyResult，失败返回 null（应继续尝试下一方式）
     */
    private fun applyViaContentProvider(params: Map<String, Any>): CameraApplyResult? {
        return try {
            val contentResolver = context.contentResolver
            val appliedParams = mutableMapOf<String, Any>()
            val failedParams = mutableListOf<String>()

            // 先清除旧参数
            try {
                contentResolver.delete(MASTER_PROVIDER_URI, null, null)
                Log.d(TAG, "ContentProvider: 已清除旧参数")
            } catch (e: Exception) {
                Log.w(TAG, "ContentProvider: 清除旧参数失败: ${e.message}")
            }

            // 逐条写入新参数
            for ((key, value) in params) {
                val contentValues = ContentValues().apply {
                    put(COL_PARAM_KEY, key)
                    put(COL_PARAM_TYPE, inferParamType(value))
                    when (value) {
                        is Int -> put(COL_PARAM_VALUE, value.toString())
                        is Float -> put(COL_PARAM_VALUE, value.toString())
                        is Double -> put(COL_PARAM_VALUE, value.toString())
                        is Long -> put(COL_PARAM_VALUE, value.toString())
                        is String -> put(COL_PARAM_VALUE, value)
                        else -> put(COL_PARAM_VALUE, value.toString())
                    }
                }

                try {
                    val resultUri = contentResolver.insert(MASTER_PROVIDER_URI, contentValues)
                    if (resultUri != null) {
                        appliedParams[key] = value
                        Log.d(TAG, "ContentProvider: 写入 $key=$value 成功, uri=$resultUri")
                    } else {
                        failedParams.add(key)
                        Log.w(TAG, "ContentProvider: 写入 $key=$value 失败, insert 返回 null")
                    }
                } catch (e: Exception) {
                    failedParams.add(key)
                    Log.w(TAG, "ContentProvider: 写入 $key=$value 异常: ${e.message}")
                }
            }

            // 验证写入结果：回读确认
            val verifiedParams = verifyContentProviderWrite(params)
            val trulyApplied = appliedParams.filterKeys { it in verifiedParams }

            when {
                trulyApplied.isEmpty() -> {
                    Log.w(TAG, "ContentProvider: 无参数成功应用")
                    null
                }
                failedParams.isEmpty() -> {
                    CameraApplyResult.Success(ApplyMethod.CONTENT_PROVIDER, trulyApplied)
                }
                else -> {
                    CameraApplyResult.PartialSuccess(
                        ApplyMethod.CONTENT_PROVIDER,
                        trulyApplied,
                        failedParams.filter { it !in trulyApplied }
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "ContentProvider: 权限不足: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "ContentProvider: 未知异常: ${e.message}", e)
            null
        }
    }

    /**
     * 回读 ContentProvider 验证写入是否成功
     */
    private fun verifyContentProviderWrite(expectedParams: Map<String, Any>): Map<String, Any> {
        val verified = mutableMapOf<String, Any>()
        try {
            val cursor = context.contentResolver.query(
                MASTER_PROVIDER_URI,
                arrayOf(COL_PARAM_KEY, COL_PARAM_VALUE, COL_PARAM_TYPE),
                null, null, null
            )
            cursor?.use {
                val keyIndex = it.getColumnIndex(COL_PARAM_KEY)
                val valueIndex = it.getColumnIndex(COL_PARAM_VALUE)
                val typeIndex = it.getColumnIndex(COL_PARAM_TYPE)

                while (it.moveToNext()) {
                    if (keyIndex >= 0 && valueIndex >= 0) {
                        val key = it.getString(keyIndex)
                        val valueStr = it.getString(valueIndex)
                        val type = if (typeIndex >= 0) it.getString(typeIndex) else "string"

                        if (key != null && valueStr != null && key in expectedParams) {
                            val parsedValue = parseParamValue(valueStr, type)
                            if (parsedValue != null) {
                                verified[key] = parsedValue
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ContentProvider: 回读验证失败: ${e.message}")
        }
        return verified
    }

    /**
     * 根据 type 字符串解析参数值
     */
    private fun parseParamValue(valueStr: String, type: String): Any? {
        return try {
            when (type) {
                "int" -> valueStr.toInt()
                "float" -> valueStr.toFloat()
                "long" -> valueStr.toLong()
                "double" -> valueStr.toDouble()
                else -> valueStr
            }
        } catch (e: NumberFormatException) {
            Log.w(TAG, "参数值解析失败: $valueStr (type=$type)")
            null
        }
    }

    /**
     * 推断参数类型字符串
     */
    private fun inferParamType(value: Any): String {
        return when (value) {
            is Int -> "int"
            is Long -> "long"
            is Float -> "float"
            is Double -> "double"
            is String -> "string"
            else -> "string"
        }
    }

    // ==================== System Settings 应用 ====================

    /**
     * 通过 Settings.System / Settings.Global 写入 OPPO 专属设置键
     *
     * OPPO 相机大师模式参数通过 oppo_camera_master_* 前缀的系统设置键存储。
     * 在非 OPPO 设备上此方法会因无法写入而返回 null。
     *
     * 注意：Settings.System 写入需要 WRITE_SETTINGS 权限（Android 6.0+）。
     * 如果权限未授予，将跳过此方式并返回 null。
     *
     * @return 成功返回 CameraApplyResult，失败返回 null
     */
    private fun applyViaSystemSettings(params: Map<String, Any>): CameraApplyResult? {
        // 检查是否有 WRITE_SETTINGS 权限（Android 6.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                Log.w(TAG, "Settings: 缺少 WRITE_SETTINGS 权限，跳过 System Settings 方式")
                return null
            }
        }

        val appliedParams = mutableMapOf<String, Any>()
        val failedParams = mutableListOf<String>()

        for ((key, value) in params) {
            val settingsKey = SETTINGS_PREFIX + key
            try {
                val success = when (value) {
                    is Int -> Settings.System.putInt(context.contentResolver, settingsKey, value)
                    is Float -> Settings.System.putFloat(context.contentResolver, settingsKey, value)
                    is Double -> Settings.System.putFloat(context.contentResolver, settingsKey, value.toFloat())
                    is Long -> Settings.System.putLong(context.contentResolver, settingsKey, value)
                    is String -> Settings.System.putString(context.contentResolver, settingsKey, value)
                    else -> {
                        val written = Settings.System.putString(context.contentResolver, settingsKey, value.toString())
                        written
                    }
                }

                if (success) {
                    appliedParams[key] = value
                    Log.d(TAG, "Settings: 写入 $settingsKey=$value 成功")
                } else {
                    failedParams.add(key)
                    Log.w(TAG, "Settings: 写入 $settingsKey=$value 失败 (put 返回 false)")
                }
            } catch (e: SecurityException) {
                // 尝试 Settings.Global 作为后备
                try {
                    val globalSuccess = writeViaSettingsGlobal(settingsKey, value)
                    if (globalSuccess) {
                        appliedParams[key] = value
                        Log.d(TAG, "Settings.Global: 写入 $settingsKey=$value 成功")
                    } else {
                        failedParams.add(key)
                        Log.w(TAG, "Settings.Global: 写入 $settingsKey=$value 也失败")
                    }
                } catch (ge: Exception) {
                    failedParams.add(key)
                    Log.w(TAG, "Settings: 写入 $settingsKey 异常: ${e.message}, Global 也失败: ${ge.message}")
                }
            } catch (e: Exception) {
                failedParams.add(key)
                Log.w(TAG, "Settings: 写入 $settingsKey 异常: ${e.message}")
            }
        }

        if (appliedParams.isEmpty()) {
            Log.w(TAG, "Settings: 无参数成功应用")
            return null
        }

        return if (failedParams.isEmpty()) {
            CameraApplyResult.Success(ApplyMethod.SYSTEM_SETTINGS, appliedParams)
        } else {
            CameraApplyResult.PartialSuccess(ApplyMethod.SYSTEM_SETTINGS, appliedParams, failedParams)
        }
    }

    /**
     * 尝试通过 Settings.Global 写入
     */
    private fun writeViaSettingsGlobal(key: String, value: Any): Boolean {
        return try {
            when (value) {
                is Int -> Settings.Global.putInt(context.contentResolver, key, value)
                is Float -> Settings.Global.putFloat(context.contentResolver, key, value)
                is Double -> Settings.Global.putFloat(context.contentResolver, key, value.toFloat())
                is Long -> Settings.Global.putLong(context.contentResolver, key, value)
                is String -> Settings.Global.putString(context.contentResolver, key, value)
                else -> Settings.Global.putString(context.contentResolver, key, value.toString())
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Settings.Global: 无权限写入 $key: ${e.message}")
            false
        }
    }

    // ==================== Camera Intent 应用 ====================

    /**
     * 通过 Intent 启动 OPPO 相机大师模式并传递参数
     *
     * 使用 com.oppo.camera.action.MASTER_MODE action 启动相机，
     * 参数通过 Intent extras 传递。
     *
     * **注意**：Intent extras 的传递有以下限制：
     * - 如果相机应用已在后台运行，新的 Intent extras 可能不会被接收，
     *   因为 onNewIntent 的实现取决于目标应用是否主动读取 extras。
     * - 如果相机应用不在前台，extras 可能被忽略，参数可能无法生效。
     * - 建议在启动 Intent 前确保相机应用未被后台运行，或使用 FLAG_ACTIVITY_CLEAR_TOP
     *   和 FLAG_ACTIVITY_NEW_TASK 标志来增加参数被接收的可能性。
     * - 如果此方式未能传递参数，将自动回退到 Clipboard 方式。
     *
     * @return 成功返回 CameraApplyResult，失败返回 null
     */
    private fun applyViaCameraIntent(params: Map<String, Any>): CameraApplyResult? {
        return try {
            val intent = Intent(ACTION_MASTER_MODE).apply {
                setPackage(OPPO_CAMERA_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                // 写入所有参数为 Intent extras
                for ((key, value) in params) {
                    when (value) {
                        is Int -> putExtra(key, value)
                        is Float -> putExtra(key, value)
                        is Double -> putExtra(key, value)
                        is Long -> putExtra(key, value)
                        is String -> putExtra(key, value)
                        else -> putExtra(key, value.toString())
                    }
                }

                // 写入标准化的 Intent Extra 键名（OPPO 相机识别的键名）
                putStandardizedExtras(params)
            }

            // 验证 Intent 是否可解析
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            if (resolveInfo == null) {
                Log.w(TAG, "Camera Intent: 无法解析 $ACTION_MASTER_MODE，尝试通用相机 Intent")
                return tryFallbackCameraIntent(params)
            }

            context.startActivity(intent)
            Log.d(TAG, "Camera Intent: 成功启动 OPPO 相机大师模式" +
                    "（注意：如果相机应用已在后台运行，extras 可能不会被接收）")
            CameraApplyResult.Success(ApplyMethod.CAMERA_INTENT, params)

        } catch (e: Exception) {
            Log.w(TAG, "Camera Intent: 启动失败: ${e.message}")
            tryFallbackCameraIntent(params)
        }
    }

    /**
     * 将参数写入 OPPO 相机识别的标准化 Intent Extra 键名
     */
    private fun Intent.putStandardizedExtras(params: Map<String, Any>) {
        params["saturation"]?.let { putExtra(EXTRA_SATURATION, it.toString().toIntOrNull() ?: 0) }
        params["contrast"]?.let { putExtra(EXTRA_CONTRAST, it.toString().toIntOrNull() ?: 0) }
        params["tone"]?.let { putExtra(EXTRA_TONE, it.toString().toIntOrNull() ?: 0) }
        params["warmth"]?.let { putExtra(EXTRA_WARMTH, it.toString().toIntOrNull() ?: 0) }
        params["sharpness"]?.let { putExtra(EXTRA_SHARPNESS, it.toString().toIntOrNull() ?: 0) }
        params["clarity"]?.let { putExtra(EXTRA_CLARITY, it.toString().toIntOrNull() ?: 0) }
        params["brightness"]?.let { putExtra(EXTRA_BRIGHTNESS, it.toString().toIntOrNull() ?: 0) }
        params["iso"]?.let { putExtra(EXTRA_ISO, it.toString().toIntOrNull() ?: 0) }
        params["shutterSpeed"]?.let { putExtra(EXTRA_SHUTTER_SPEED, it.toString()) }
        params["whiteBalance"]?.let { putExtra(EXTRA_WHITE_BALANCE, it.toString().toIntOrNull() ?: 5500) }
        params["exposureCompensation"]?.let {
            val doubleVal = it.toString().toDoubleOrNull() ?: 0.0
            putExtra(EXTRA_EXPOSURE_COMPENSATION, doubleVal.toFloat())
        }
        params["vignette"]?.let { putExtra(EXTRA_VIGNETTE, it.toString().toIntOrNull() ?: 0) }
        params["cyanMagenta"]?.let { putExtra(EXTRA_CYAN_MAGENTA, it.toString().toIntOrNull() ?: 0) }
        params["softLight"]?.let { putExtra(EXTRA_SOFT_LIGHT, it.toString()) }
        params["filter"]?.let { putExtra(EXTRA_FILTER, it.toString()) }
    }

    /**
     * 后备：尝试使用标准 ACTION_IMAGE_CAPTURE 或 MediaStore Intent 启动相机
     */
    private fun tryFallbackCameraIntent(params: Map<String, Any>): CameraApplyResult? {
        return try {
            // 尝试直接启动 OPPO 相机包
            val launchIntent = context.packageManager.getLaunchIntentForPackage(OPPO_CAMERA_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // 将参数作为 extras 附加
                for ((key, value) in params) {
                    when (value) {
                        is Int -> launchIntent.putExtra(key, value)
                        is Float -> launchIntent.putExtra(key, value)
                        is Double -> launchIntent.putExtra(key, value)
                        is Long -> launchIntent.putExtra(key, value)
                        is String -> launchIntent.putExtra(key, value)
                        else -> launchIntent.putExtra(key, value.toString())
                    }
                }
                launchIntent.putStandardizedExtras(params)
                context.startActivity(launchIntent)
                Log.d(TAG, "Camera Intent (fallback): 成功启动 OPPO 相机（launch intent）")
                CameraApplyResult.PartialSuccess(
                    ApplyMethod.CAMERA_INTENT,
                    params,
                    listOf("master_mode_not_confirmed")
                )
            } else {
                // 最后尝试标准相机 Intent
                val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    for ((key, value) in params) {
                        when (value) {
                            is Int -> putExtra(key, value)
                            is Float -> putExtra(key, value)
                            is Double -> putExtra(key, value)
                            is Long -> putExtra(key, value)
                            is String -> putExtra(key, value)
                            else -> putExtra(key, value.toString())
                        }
                    }
                }
                context.startActivity(cameraIntent)
                Log.d(TAG, "Camera Intent (fallback): 启动了标准相机应用")
                CameraApplyResult.PartialSuccess(
                    ApplyMethod.CAMERA_INTENT,
                    emptyMap(),
                    params.keys.toList()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Camera Intent (fallback): 也失败了: ${e.message}")
            null
        }
    }

    // ==================== Clipboard Fallback ====================

    /**
     * 将参数格式化后复制到剪贴板
     *
     * 格式: "OMaster 预设参数: 饱和度=+10, 对比度=-5, ..."
     * 同时显示 Toast 通知用户。
     *
     * 注意：Android 12+（API 31+）系统会在剪贴板写入时自动显示 toast 提示，
     * 因此跳过手动 Toast 以避免重复提示。
     *
     * @return 始终返回 CameraApplyResult（Success 或 Failed）
     */
    private fun applyViaClipboard(params: Map<String, Any>): CameraApplyResult {
        return try {
            val formattedString = formatParamsForClipboard(params)

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("OMaster 预设参数", formattedString)
            clipboard.setPrimaryClip(clip)

            // Android 12+（API 31+）系统自动显示剪贴板 toast，跳过手动 Toast 以避免重复
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Toast.makeText(
                    context,
                    "参数已复制到剪贴板，请在 OPPO 相机大师模式中手动输入",
                    Toast.LENGTH_LONG
                ).show()
            }

            Log.d(TAG, "Clipboard: 参数已复制到剪贴板")
            CameraApplyResult.Success(ApplyMethod.CLIPBOARD_FALLBACK, params)
        } catch (e: Exception) {
            Log.e(TAG, "Clipboard: 复制失败: ${e.message}", e)
            CameraApplyResult.Failed(
                reason = "剪贴板写入失败: ${e.message}",
                suggestion = "请手动记录以下参数并在 OPPO 相机大师模式中输入"
            )
        }
    }

    /**
     * 格式化参数为中文可读字符串
     *
     * 格式: "OMaster 预设参数: 饱和度=+10, 对比度=-5, ..."
     */
    private fun formatParamsForClipboard(params: Map<String, Any>): String {
        val paramLabels = mapOf(
            "saturation" to "饱和度",
            "contrast" to "对比度",
            "tone" to "影调",
            "warmth" to "冷暖",
            "sharpness" to "锐度",
            "clarity" to "清晰度",
            "brightness" to "亮度",
            "iso" to "ISO",
            "shutterSpeed" to "快门速度",
            "whiteBalance" to "白平衡",
            "exposureCompensation" to "曝光补偿",
            "vignette" to "暗角",
            "cyanMagenta" to "青品调",
            "softLight" to "柔光",
            "filter" to "滤镜"
        )

        val paramParts = params.map { (key, value) ->
            val label = paramLabels[key] ?: key
            val displayValue = when {
                value is Int && key in listOf("saturation", "contrast", "tone", "warmth",
                    "vignette", "cyanMagenta", "brightness", "exposureCompensation") -> {
                    if (value >= 0) "+$value" else "$value"
                }
                value is Double && key == "exposureCompensation" -> {
                    val formatted = String.format("%.1f", value)
                    if (value >= 0) "+$formatted" else formatted
                }
                key == "whiteBalance" -> "${value}K"
                key == "iso" -> "ISO $value"
                key == "shutterSpeed" -> value.toString()
                else -> value.toString()
            }
            "$label=$displayValue"
        }

        return "OMaster 预设参数: ${paramParts.joinToString(", ")}"
    }

    // ==================== 参数构建 ====================

    /**
     * 从 MasterPreset 构建参数 Map
     */
    private fun buildParamMap(preset: MasterPreset): Map<String, Any> {
        val params = mutableMapOf<String, Any>()

        // 色彩参数
        preset.saturation?.let { params["saturation"] = it }
        preset.tone?.let { params["tone"] = it }
        preset.warmCool?.let { params["warmth"] = it }
        preset.cyanMagenta?.let { params["cyanMagenta"] = it }
        preset.sharpness?.let { params["sharpness"] = it }
        preset.vignette?.let {
            // 暗角参数：MasterPreset 中为 "开"/"关"，转为数值
            params["vignette"] = when (it) {
                "开", "on", "1" -> 30
                "关", "off", "0" -> 0
                else -> it.toIntOrNull() ?: 0
            }
        }
        preset.softLight?.let {
            params["softLight"] = when (it) {
                "梦幻" -> "dreamy"
                "柔" -> "soft"
                else -> it
            }
        }
        preset.filter?.let { params["filter"] = it }

        // Pro 模式参数
        preset.iso?.let {
            it.toIntOrNull()?.let { iso -> params["iso"] = iso }
        }
        preset.shutterSpeed?.let { params["shutterSpeed"] = it }
        preset.exposureCompensation?.let {
            it.toDoubleOrNull()?.let { ec -> params["exposureCompensation"] = ec }
        }
        preset.colorTemperature?.let { params["whiteBalance"] = it }
        preset.colorHue?.let { params["cyanMagenta"] = it }

        // 从 colorGradingParams 扩展参数
        preset.colorGradingParams?.forEach { (key, value) ->
            when (key) {
                "contrast" -> value.toIntOrNull()?.let { params["contrast"] = it }
                "clarity" -> value.toIntOrNull()?.let { params["clarity"] = it }
                "brightness" -> value.toIntOrNull()?.let { params["brightness"] = it }
            }
        }

        // 从 params 扩展参数
        preset.params?.forEach { (key, value) ->
            when (key) {
                "iso" -> value.toIntOrNull()?.let { params["iso"] = it }
                "shutterSpeed" -> { params["shutterSpeed"] = value }
                "whiteBalance" -> value.toIntOrNull()?.let { params["whiteBalance"] = it }
                "exposureCompensation" -> value.toDoubleOrNull()?.let { params["exposureCompensation"] = it }
            }
        }

        return params
    }

    /**
     * 从 HasselbladParams 构建参数 Map
     *
     * HasselbladParams 范围为 -30 ~ +30，需映射到 OPPO 大师模式 -100 ~ +100 范围。
     * 映射公式: oppeValue = hasselbladValue * (100 / 30)
     */
    private fun buildParamMapFromHasselblad(
        params: HasselbladParams,
        iso: Int?,
        shutterSpeed: String?,
        whiteBalanceK: Int?,
        exposureCompensation: Double?
    ): Map<String, Any> {
        val paramMap = mutableMapOf<String, Any>()
        val scale = 100.0 / 30.0  // -30~+30 → -100~+100

        // 映射哈苏参数到 OPPO 大师模式参数
        if (params.saturation != 0) paramMap["saturation"] = (params.saturation * scale).toInt()
        if (params.contrast != 0) paramMap["contrast"] = (params.contrast * scale).toInt()
        if (params.tone != 0) paramMap["tone"] = (params.tone * scale).toInt()
        if (params.colorTemp != 0) paramMap["warmth"] = (params.colorTemp * scale).toInt()
        if (params.sharpness != 0) paramMap["sharpness"] = (params.sharpness * scale).toInt().coerceAtLeast(0)
        if (params.vignette != 0) paramMap["vignette"] = (params.vignette * scale).toInt()
        if (params.cyanMagenta != 0) paramMap["cyanMagenta"] = (params.cyanMagenta * scale).toInt()
        if (params.clarity != 0) paramMap["clarity"] = (params.clarity * scale).toInt().coerceAtLeast(0)
        if (params.highlights != 0) paramMap["highlights"] = (params.highlights * scale).toInt()
        if (params.shadows != 0) paramMap["shadows"] = (params.shadows * scale).toInt()

        // 柔光模式
        if (params.softLight != SoftLightMode.NONE) {
            paramMap["softLight"] = when (params.softLight) {
                SoftLightMode.SOFT -> "soft"
                SoftLightMode.DREAMY -> "dreamy"
                SoftLightMode.NONE -> ""
            }
        }

        // Pro 模式参数
        iso?.let { paramMap["iso"] = it }
        shutterSpeed?.let { paramMap["shutterSpeed"] = it }
        whiteBalanceK?.let { paramMap["whiteBalance"] = it }
        exposureCompensation?.let { paramMap["exposureCompensation"] = it }

        return paramMap
    }

    // ==================== 参数校验 ====================

    /**
     * 参数校验结果
     */
    private data class ValidationResult(
        val errors: List<String>,
        val isInvalid: Boolean
    )

    /**
     * 校验所有参数范围
     *
     * 使用白名单机制：仅校验 [ALLOWED_PARAM_KEYS] 中定义的参数键。
     * 不在白名单中的键会记录警告日志，但不会被视为校验错误。
     *
     * 校验规则：
     * - 饱和度: -100 ~ 100
     * - 对比度/影调: -100 ~ 100
     * - 冷暖: -100 ~ 100
     * - 锐度: 0 ~ 100
     * - 清晰度: 0 ~ 100
     * - ISO: 100 ~ 12800
     * - 白平衡: 2000K ~ 8000K
     * - 曝光补偿: -3.0 ~ +3.0
     * - 暗角: -100 ~ 100
     * - 青品调: -100 ~ 100
     * - 亮度: -100 ~ 100
     */
    private fun validateParams(params: Map<String, Any>): ValidationResult {
        val errors = mutableListOf<String>()

        for ((key, value) in params) {
            // 白名单检查：只校验已知参数，未知参数仅记录警告
            if (key !in ALLOWED_PARAM_KEYS) {
                Log.w(TAG, "参数校验: 未知参数键 '$key'，不在白名单中，跳过范围校验")
                continue
            }

            val error = validateSingleParam(key, value)
            if (error != null) {
                errors.add(error)
            }
        }

        return ValidationResult(errors = errors, isInvalid = errors.isNotEmpty())
    }

    /**
     * 校验单个参数
     */
    private fun validateSingleParam(key: String, value: Any): String? {
        return when (key) {
            "saturation" -> validateIntRange(key, value, RANGE_SATURATION)
            "contrast" -> validateIntRange(key, value, RANGE_CONTRAST)
            "tone" -> validateIntRange(key, value, RANGE_TONE)
            "warmth" -> validateIntRange(key, value, RANGE_WARMTH)
            "sharpness" -> validateIntRange(key, value, RANGE_SHARPNESS)
            "clarity" -> validateIntRange(key, value, RANGE_CLARITY)
            "iso" -> validateIntRange(key, value, RANGE_ISO)
            "whiteBalance" -> validateIntRange(key, value, RANGE_WHITE_BALANCE)
            "exposureCompensation" -> validateDoubleRange(key, value, RANGE_EXPOSURE_COMPENSATION)
            "vignette" -> validateIntRange(key, value, RANGE_VIGNETTE)
            "cyanMagenta" -> validateIntRange(key, value, RANGE_CYAN_MAGENTA)
            "brightness" -> validateIntRange(key, value, RANGE_BRIGHTNESS)
            "highlights" -> validateIntRange(key, value, -100 to 100)
            "shadows" -> validateIntRange(key, value, -100 to 100)
            // 字符串类型参数不做范围校验
            "softLight", "filter", "shutterSpeed" -> null
            else -> {
                // 此分支理论上不会到达（因为 validateParams 已做白名单过滤），
                // 但保留作为防御性编程
                Log.w(TAG, "未知参数键: $key")
                null
            }
        }
    }

    /**
     * 校验 Int 类型参数范围
     */
    private fun validateIntRange(key: String, value: Any, range: Pair<Int, Int>): String? {
        val intValue = when (value) {
            is Int -> value
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }

        if (intValue == null) {
            return "$key: 值 '$value' 无法解析为整数"
        }

        if (intValue !in range.first..range.second) {
            return "$key: 值 $intValue 超出范围 [${range.first}, ${range.second}]"
        }

        return null
    }

    /**
     * 校验 Double 类型参数范围
     */
    private fun validateDoubleRange(key: String, value: Any, range: Pair<Double, Double>): String? {
        val doubleValue = when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }

        if (doubleValue == null) {
            return "$key: 值 '$value' 无法解析为浮点数"
        }

        if (doubleValue < range.first || doubleValue > range.second) {
            return "$key: 值 $doubleValue 超出范围 [${range.first}, ${range.second}]"
        }

        return null
    }

    // ==================== ContentProvider 探测 ====================

    /**
     * 探测 OPPO 大师模式 ContentProvider 是否存在
     *
     * 通过 PackageManager 检查 ContentProvider 对应的包是否存在，
     * 并尝试查询 ContentProvider 是否可访问。
     *
     * 区分预期异常和意外异常：
     * - [PackageManager.NameNotFoundException]：预期异常，表示 OPPO 相机包未安装
     * - [SecurityException]：预期异常，表示无权限访问 ContentProvider
     * - 其他异常：记录为错误日志，可能是意外问题需要排查
     */
    private fun probeContentProvider(): Boolean {
        // 方法 1：通过 PackageManager 检查 OPPO 相机包是否包含对应 Provider
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                OPPO_CAMERA_PACKAGE,
                android.content.pm.PackageManager.GET_PROVIDERS
            )
            val providers = packageInfo.providers
            if (providers != null) {
                val found = providers.any { it.authority == MASTER_PROVIDER_AUTHORITY }
                if (found) {
                    Log.d(TAG, "ContentProvider 探测: 在 OPPO 相机包中找到 $MASTER_PROVIDER_AUTHORITY")
                    return true
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // 预期异常：OPPO 相机包未安装
            Log.d(TAG, "ContentProvider 探测: OPPO 相机包未安装")
        } catch (e: SecurityException) {
            // 预期异常：无权限获取包信息
            Log.d(TAG, "ContentProvider 探测: 无权限获取 OPPO 相机包信息")
        } catch (e: Exception) {
            // 意外异常：记录为错误以便排查
            Log.e(TAG, "ContentProvider 探测: 获取 OPPO 相机包信息时发生意外异常: ${e.message}", e)
        }

        // 方法 2：尝试直接查询 ContentProvider
        try {
            val cursor = context.contentResolver.query(
                MASTER_PROVIDER_URI,
                arrayOf(COL_PARAM_KEY),
                null, null, null
            )
            cursor?.close()
            Log.d(TAG, "ContentProvider 探测: 直接查询成功")
            return true
        } catch (e: SecurityException) {
            // 预期异常：无权限访问 ContentProvider
            Log.d(TAG, "ContentProvider 探测: 无权限访问 ContentProvider: ${e.message}")
        } catch (e: IllegalArgumentException) {
            // 预期异常：URI 无效或 Provider 不存在
            Log.d(TAG, "ContentProvider 探测: Provider 不存在或 URI 无效: ${e.message}")
        } catch (e: Exception) {
            // 意外异常：记录为错误以便排查
            Log.e(TAG, "ContentProvider 探测: 直接查询时发生意外异常: ${e.message}", e)
        }

        return false
    }
}