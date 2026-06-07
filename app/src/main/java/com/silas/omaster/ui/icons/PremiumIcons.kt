package com.silas.omaster.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.theme.*

/**
 * =====================================================
 * OMaster 精致高端图标系统
 * =====================================================
 * 设计理念：简约、精致、高端、有深度
 * 线条风格：极细线条，圆润端点
 * 视觉权重：统一、平衡、和谐
 */

object PremiumIcons {

    // ==================== 导航图标 ====================
    
    /**
     * 主页图标 - 简约相机
     * 设计：极简相机轮廓，体现专业与纯粹
     */
    val Home: ImageVector = ImageVector.Builder(
        name = "Home",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(TextPrimary),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 8f)
            lineTo(4f, 17f)
            curveTo(4f, 17.5f, 4.5f, 18f, 5f, 18f)
            lineTo(19f, 18f)
            curveTo(19.5f, 18f, 20f, 17.5f, 20f, 17f)
            lineTo(20f, 8f)
            curveTo(20f, 7.5f, 19.5f, 7f, 19f, 7f)
            lineTo(16f, 7f)
            lineTo(15f, 5f)
            lineTo(9f, 5f)
            lineTo(8f, 7f)
            lineTo(5f, 7f)
            curveTo(4.5f, 7f, 4f, 7.5f, 4f, 8f)
        }
    }.build()

    /**
     * AI图标 - 智能星环
     * 设计：环形节点，体现智能与连接
     */
    val AI: ImageVector = ImageVector.Builder(
        name = "AI",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(TextPrimary),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 2f)
            curveTo(17.5f, 2f, 22f, 6.5f, 22f, 12f)
            curveTo(22f, 17.5f, 17.5f, 22f, 12f, 22f)
            curveTo(6.5f, 22f, 2f, 17.5f, 2f, 12f)
            curveTo(2f, 6.5f, 6.5f, 2f, 12f, 2f)
            moveTo(12f, 8f)
            curveTo(12f, 8f, 12f, 12f, 12f, 12f)
            curveTo(12f, 12f, 12f, 16f, 12f, 16f)
            moveTo(8f, 12f)
            curveTo(8f, 12f, 12f, 12f, 12f, 12f)
            curveTo(12f, 12f, 16f, 12f, 16f, 12f)
        }
        path(
            fill = SolidColor(TextPrimary),
            stroke = null
        ) {
            moveTo(12f, 12f)
            arcTo(1.5f, 1.5f, 0f, false, true, 1.5f, 1.5f)
            arcTo(1.5f, 1.5f, 0f, false, true, -1.5f, 1.5f)
            arcTo(1.5f, 1.5f, 0f, false, true, -1.5f, -1.5f)
            arcTo(1.5f, 1.5f, 0f, false, true, 1.5f, -1.5f)
            close()
        }
    }.build()

    /**
     * 水印图标 - 精致印章
     * 设计：印章造型，体现认证与品质
     */
    val Watermark: ImageVector = ImageVector.Builder(
        name = "Watermark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(TextPrimary),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 3f)
            lineTo(21f, 8f)
            lineTo(21f, 16f)
            lineTo(12f, 21f)
            lineTo(3f, 16f)
            lineTo(3f, 8f)
            close()
        }
        path(
            fill = SolidColor(AmberGold.copy(alpha = 0.2f)),
            stroke = SolidColor(AmberGold),
            strokeLineWidth = 1f
        ) {
            moveTo(12f, 7f)
            lineTo(17f, 10f)
            lineTo(17f, 14f)
            lineTo(12f, 17f)
            lineTo(7f, 14f)
            lineTo(7f, 10f)
            close()
        }
    }.build()

    /**
     * 设置图标 - 精密齿轮
     * 设计：精细齿轮轮廓，体现精密与控制
     */
    val Settings: ImageVector = ImageVector.Builder(
        name = "Settings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(TextPrimary),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 15f)
            arcTo(3f, 3f, 0f, false, true, 3f, 3f)
            arcTo(3f, 3f, 0f, false, true, -3f, 3f)
            arcTo(3f, 3f, 0f, false, true, -3f, -3f)
            arcTo(3f, 3f, 0f, false, true, 3f, -3f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(TextPrimary),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 2f)
            lineTo(12f, 4f)
            moveTo(12f, 20f)
            lineTo(12f, 22f)
            moveTo(2f, 12f)
            lineTo(4f, 12f)
            moveTo(20f, 12f)
            lineTo(22f, 12f)
        }
    }.build()

    // ==================== 功能图标 ====================

    /**
     * 认证徽章
     */
    val Certified: ImageVector = ImageVector.Builder(
        name = "Certified",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(AmberGold),
            stroke = null
        ) {
            moveTo(12f, 2f)
            lineTo(22f, 7f)
            lineTo(22f, 14f)
            curveTo(22f, 19f, 17.5f, 22f, 12f, 22f)
            curveTo(6.5f, 22f, 2f, 19f, 2f, 14f)
            lineTo(2f, 7f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8f, 12f)
            lineTo(11f, 15f)
            lineTo(16f, 9f)
        }
    }.build()

    /**
     * 评分星星
     */
    val Star: ImageVector = ImageVector.Builder(
        name = "Star",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(AmberGold),
            stroke = null
        ) {
            moveTo(12f, 2f)
            lineTo(14.5f, 9f)
            lineTo(22f, 9.5f)
            lineTo(16.5f, 14.5f)
            lineTo(18.5f, 22f)
            lineTo(12f, 18f)
            lineTo(5.5f, 22f)
            lineTo(7.5f, 14.5f)
            lineTo(2f, 9.5f)
            lineTo(9.5f, 9f)
            close()
        }
    }.build()

    /**
     * 下载
     */
    val Download: ImageVector = ImageVector.Builder(
        name = "Download",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(JadeGreen),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 3f)
            lineTo(12f, 15f)
            moveTo(8f, 11f)
            lineTo(12f, 15f)
            lineTo(16f, 11f)
            moveTo(4f, 17f)
            lineTo(4f, 19f)
            curveTo(4f, 20f, 5f, 21f, 6f, 21f)
            lineTo(18f, 21f)
            curveTo(19f, 21f, 20f, 20f, 20f, 19f)
            lineTo(20f, 17f)
        }
    }.build()

    // ==================== 相机参数图标 ====================

    /**
     * ISO
     */
    val ISO: ImageVector = ImageVector.Builder(
        name = "ISO",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(AmberGold),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 12f)
            lineTo(12f, 5f)
            moveTo(12f, 12f)
            lineTo(19f, 12f)
            moveTo(12f, 12f)
            lineTo(17f, 7f)
            moveTo(12f, 12f)
            lineTo(7f, 17f)
        }
        path(
            fill = SolidColor(AmberGold),
            stroke = null
        ) {
            moveTo(12f, 12f)
            arcTo(2f, 2f, 0f, false, true, 2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, -2f)
            arcTo(2f, 2f, 0f, false, true, 2f, -2f)
            close()
        }
    }.build()

    /**
     * 快门
     */
    val Shutter: ImageVector = ImageVector.Builder(
        name = "Shutter",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(JadeGreen),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 12f)
            arcTo(8f, 8f, 0f, false, true, 8f, 8f)
            arcTo(8f, 8f, 0f, false, true, -8f, 8f)
            arcTo(8f, 8f, 0f, false, true, -8f, -8f)
            arcTo(8f, 8f, 0f, false, true, 8f, -8f)
            close()
        }
        path(
            fill = SolidColor(JadeGreen),
            stroke = null
        ) {
            moveTo(12f, 12f)
            arcTo(3f, 3f, 0f, false, true, 3f, 3f)
            arcTo(3f, 3f, 0f, false, true, -3f, 3f)
            arcTo(3f, 3f, 0f, false, true, -3f, -3f)
            arcTo(3f, 3f, 0f, false, true, 3f, -3f)
            close()
        }
    }.build()

    /**
     * 光圈
     */
    val Aperture: ImageVector = ImageVector.Builder(
        name = "Aperture",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(InfoSapphire),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 12f)
            arcTo(9f, 9f, 0f, false, true, 9f, 9f)
            arcTo(9f, 9f, 0f, false, true, -9f, 9f)
            arcTo(9f, 9f, 0f, false, true, -9f, -9f)
            arcTo(9f, 9f, 0f, false, true, 9f, -9f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(InfoSapphire),
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 3f)
            lineTo(12f, 12f)
            lineTo(21f, 12f)
        }
    }.build()
}
