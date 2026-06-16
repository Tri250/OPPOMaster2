package com.silas.omaster.ai

/**
 * 胶片调整参数配置表
 * 每种胶片定义其独特的参数调整映射
 */
object FilmAdjustments {

    /**
     * 胶片参数调整表
     * key: 胶片ID
     * value: 参数调整映射 (paramName -> adjustmentValue)
     */
    private val adjustmentsMap: Map<String, Map<String, Int>> = mapOf(
        // Portra 400 - 柔和人像风格
        "portra" to mapOf(
            "saturation" to -5,
            "contrast" to -10,
            "colorTemp" to 5,
            "tone" to -3,
            "sharpness" to -5,
            "vignette" to 10
        ),

        // RDP3 正片 - 高饱和反转片风格
        "rdp3" to mapOf(
            "saturation" to 15,
            "contrast" to 5,
            "sharpness" to 10,
            "colorTemp" to 0,
            "tone" to 5,
            "vignette" to -5
        ),

        // TX400 黑白 - 高对比黑白颗粒
        "tx400" to mapOf(
            "saturation" to -30,
            "contrast" to 20,
            "tone" to -10,
            "sharpness" to 15,
            "vignette" to 10,
            "clarity" to 10
        ),

        // CC 经典负片 - 经典胶片质感
        "cc" to mapOf(
            "saturation" to 0,
            "contrast" to 0,
            "colorTemp" to 0,
            "tone" to 0,
            "sharpness" to 0,
            "vignette" to 0
        ),

        // NC 自然 - 自然柔和
        "nc" to mapOf(
            "saturation" to -5,
            "contrast" to -5,
            "colorTemp" to 0,
            "tone" to 2,
            "sharpness" to -3,
            "vignette" to -5
        ),

        // NH 浓郁 - 高对比浓郁色彩
        "nh" to mapOf(
            "saturation" to 10,
            "contrast" to 15,
            "colorTemp" to 0,
            "tone" to -5,
            "sharpness" to 8,
            "vignette" to 5,
            "clarity" to 5
        ),

        // 800T 夜景 - 电影感夜景
        "800t" to mapOf(
            "saturation" to 5,
            "contrast" to 15,
            "colorTemp" to -10,
            "tone" to -15,
            "sharpness" to 10,
            "vignette" to 20,
            "cyanMagenta" to -10
        ),

        // 冷 CCD - 清冷数字质感
        "ccd_cool" to mapOf(
            "saturation" to -3,
            "contrast" to 5,
            "colorTemp" to -15,
            "tone" to 5,
            "sharpness" to 5,
            "cyanMagenta" to -5
        ),

        // 暖 CCD - 温馨数字质感
        "ccd_warm" to mapOf(
            "saturation" to 8,
            "contrast" to -3,
            "colorTemp" to 15,
            "tone" to 5,
            "sharpness" to -3,
            "cyanMagenta" to 5
        )
    )

    /**
     * 获取指定胶片的参数调整
     * @param filmId 胶片ID
     * @return 参数调整映射，如果未找到则返回空映射
     */
    fun getAdjustments(filmId: String): Map<String, Int> {
        return adjustmentsMap[filmId] ?: emptyMap()
    }

    /**
     * 获取所有支持的胶片ID列表
     */
    fun getSupportedFilmIds(): List<String> {
        return adjustmentsMap.keys.toList()
    }

    /**
     * 检查胶片是否支持参数调整
     */
    fun hasAdjustments(filmId: String): Boolean {
        return adjustmentsMap.containsKey(filmId)
    }

    /**
     * 获取胶片的特定参数调整值
     * @param filmId 胶片ID
     * @param paramName 参数名
     * @return 调整值，如果未找到则返回0
     */
    fun getParamAdjustment(filmId: String, paramName: String): Int {
        return adjustmentsMap[filmId]?.get(paramName) ?: 0
    }
}