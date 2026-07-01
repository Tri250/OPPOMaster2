package com.silas.omaster.ai.antipattern

import com.silas.omaster.ai.scene.RealtimeSceneResult

/**
 * 拍摄反模式实时检测引擎
 * 基于场景识别结果、拍摄参数和传感器数据，检测常见拍摄错误
 */
object AntiPatternDetector {

    enum class AlertLevel {
        GREEN,   // 无问题
        ORANGE,  // 警告（可优化）
        RED      // 严重（影响成片）
    }

    data class AntiPatternAlert(
        val level: AlertLevel,
        val type: String,
        val title: String,
        val description: String,
        val fixAction: FixAction? = null
    )

    data class FixAction(
        val label: String,
        val actionType: String,   // "switch_zoom", "enable_mode", "adjust_param"
        val actionValue: String   // 具体参数值
    )

    data class ShootingParams(
        val zoomRatio: Float = 1.0f,
        val iso: Int? = null,
        val shutterSpeedNs: Long? = null,
        val exposureCompensation: Float = 0f
    )

    /**
     * 检测所有反模式，返回按严重程度排序的提示列表
     */
    fun detect(
        sceneResult: RealtimeSceneResult,
        shootingParams: ShootingParams = ShootingParams(),
        gyroscopeStable: Boolean = true,
        upperBrightnessRatio: Float = 0f,
        faceRatio: Float = 0f
    ): List<AntiPatternAlert> {
        val alerts = mutableListOf<AntiPatternAlert>()
        val sceneId = sceneResult.sceneProfile.id
        val hasFace = sceneResult.confidenceMap.containsKey("face")

        // 1. 顶光人像
        if (hasFace && upperBrightnessRatio > 0.55f) {
            alerts.add(AntiPatternAlert(
                level = AlertLevel.ORANGE,
                type = "top_light_portrait",
                title = "检测到顶光",
                description = "顶光会在眼窝产生阴影，建议调整角度或寻找遮挡",
                fixAction = FixAction("启用柔光模式", "enable_mode", "soft_light")
            ))
        }

        // 2. 广角近距人像变形
        if (hasFace && shootingParams.zoomRatio < 1.5f && faceRatio > 0.25f) {
            alerts.add(AntiPatternAlert(
                level = AlertLevel.RED,
                type = "wide_angle_portrait",
                title = "广角近距面部变形",
                description = "广角近拍会产生面部畸变（鼻子变大），建议切换至 2x 或 3x",
                fixAction = FixAction("切换到 3x", "switch_zoom", "3.0")
            ))
        }

        // 3. 高 ISO 夜景
        if (sceneId.startsWith("night") && shootingParams.iso != null && shootingParams.iso > 3200) {
            alerts.add(AntiPatternAlert(
                level = AlertLevel.ORANGE,
                type = "high_iso_night",
                title = "高 ISO 噪点风险",
                description = "ISO 超过 3200 可能引入明显噪点，建议使用三脚架或夜景模式",
                fixAction = FixAction("启用夜景模式", "enable_mode", "night_mode")
            ))
        }

        // 4. 手持慢门
        val slowShutter = shootingParams.shutterSpeedNs != null && shootingParams.shutterSpeedNs > 33_000_000L
        if (slowShutter && !gyroscopeStable) {
            alerts.add(AntiPatternAlert(
                level = AlertLevel.RED,
                type = "handheld_slow_shutter",
                title = "手持慢门模糊风险",
                description = "当前快门较慢，手持易模糊，建议寻找支撑点或提高 ISO",
                fixAction = null
            ))
        }

        // 5. 美食顶光
        if (sceneId.startsWith("food") && upperBrightnessRatio > 0.45f) {
            alerts.add(AntiPatternAlert(
                level = AlertLevel.ORANGE,
                type = "top_light_food",
                title = "美食顶光阴影",
                description = "顶光会在食物上产生难看的阴影，建议改用 45° 侧光",
                fixAction = FixAction("调整角度", "adjust_param", "exposure_compensation:+0.3")
            ))
        }

        // 6. 风景正午平光
        if ((sceneId.startsWith("landscape") || sceneId.startsWith("urban"))
            && upperBrightnessRatio > 0.7f) {
            alerts.add(AntiPatternAlert(
                level = AlertLevel.ORANGE,
                type = "harsh_midday_light",
                title = "正午光线较平",
                description = "正午光线较平，阴影生硬，建议等待黄金时刻",
                fixAction = null
            ))
        }

        // 7. 过曝高光
        if (upperBrightnessRatio > 0.65f) {
            alerts.add(AntiPatternAlert(
                level = AlertLevel.ORANGE,
                type = "overexposed_highlights",
                title = "高光过曝",
                description = "高光区域可能过曝，建议降低曝光补偿",
                fixAction = FixAction("降低曝光", "adjust_param", "exposure_compensation:-0.5")
            ))
        }

        // 8. 暗部死黑
        if (upperBrightnessRatio < 0.15f && !sceneId.startsWith("night")) {
            alerts.add(AntiPatternAlert(
                level = AlertLevel.ORANGE,
                type = "crushed_shadows",
                title = "暗部死黑",
                description = "暗部区域可能丢失细节，建议提升曝光补偿",
                fixAction = FixAction("提升曝光", "adjust_param", "exposure_compensation:+0.5")
            ))
        }

        // 9. 微距风天抖动
        if (sceneId.startsWith("macro") && !gyroscopeStable) {
            alerts.add(AntiPatternAlert(
                level = AlertLevel.RED,
                type = "macro_shake",
                title = "微距抖动",
                description = "微距拍摄对稳定性要求极高，建议使用三脚架",
                fixAction = null
            ))
        }

        // 10. 街拍中心构图
        if (sceneId.startsWith("urban") && hasFace && shootingParams.zoomRatio < 2.0f) {
            alerts.add(AntiPatternAlert(
                level = AlertLevel.GREEN,
                type = "centered_street",
                title = "构图建议",
                description = "街拍可尝试三分法或对角线构图增加动感",
                fixAction = FixAction("切换 AR 引导线", "enable_mode", "ar_guide:diagonal")
            ))
        }

        // 按严重程度排序，RED > ORANGE > GREEN
        return alerts.sortedBy { it.level.ordinal }
    }

    /**
     * 仅返回需要用户注意的非 GREEN 级别提示
     */
    fun detectImportant(
        sceneResult: RealtimeSceneResult,
        shootingParams: ShootingParams = ShootingParams(),
        gyroscopeStable: Boolean = true,
        upperBrightnessRatio: Float = 0f,
        faceRatio: Float = 0f
    ): List<AntiPatternAlert> {
        return detect(sceneResult, shootingParams, gyroscopeStable, upperBrightnessRatio, faceRatio)
            .filter { it.level != AlertLevel.GREEN }
    }
}
