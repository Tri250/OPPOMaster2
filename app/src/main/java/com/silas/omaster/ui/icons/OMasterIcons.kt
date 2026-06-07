package com.silas.omaster.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.OPPOGreen
import com.silas.omaster.ui.theme.DeepSpaceBlack
import androidx.compose.ui.graphics.Color

/**
 * =====================================================
 * OMaster 专业摄影图标系统
 * =====================================================
 * 设计标准：ColorOS 16 Aquatic Design
 * 图标风格：线性图标，2dp描边，圆角端点
 * 目标用户：OPPO Find 系列高端摄影用户
 * 视觉定位：专业、精致、高端、有质感
 */

object OMasterIcons {

    // ==================== 导航图标 ====================
    /**
     * 主页图标 - 专业相机
     * 设计理念：模拟专业相机镜头，体现摄影专业性
     */
    val Home: ImageVector = ImageVector.Builder(
        name = "Home",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 相机机身轮廓
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(4f, 8f)
            lineTo(4f, 16f)
            cubicTo(4f, 17.1f, 4.9f, 18f, 6f, 18f)
            lineTo(18f, 18f)
            cubicTo(19.1f, 18f, 20f, 17.1f, 20f, 16f)
            lineTo(20f, 8f)
            cubicTo(20f, 6.9f, 19.1f, 6f, 18f, 6f)
            lineTo(15f, 6f)
            lineTo(14f, 4f)
            lineTo(10f, 4f)
            lineTo(9f, 6f)
            lineTo(6f, 6f)
            cubicTo(4.9f, 6f, 4f, 6.9f, 4f, 8f)
            close()
        }
        // 镜头圆环
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 12f)
            cubicTo(12f, 14.21f, 10.21f, 16f, 8f, 16f)
            cubicTo(5.79f, 16f, 4f, 14.21f, 4f, 12f)
            cubicTo(4f, 9.79f, 5.79f, 8f, 8f, 8f)
            cubicTo(10.21f, 8f, 12f, 9.79f, 12f, 12f)
        }
        // 镜头内核
        path(
            fill = SolidColor(Color.White.copy(alpha = 0.3f)),
            stroke = null
        ) {
            moveTo(8f, 12f)
            relativeMoveTo(-2f, 0f)
            arcTo(2f, 2f, 0f, false, true, 2f, -2f)
            arcTo(2f, 2f, 0f, false, true, 2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, -2f)
            close()
        }
    }.build()

    /**
     * AI图标 - 智能大脑
     * 设计理念：神经网络节点，体现AI智能识别
     */
    val AI: ImageVector = ImageVector.Builder(
        name = "AI",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 大脑轮廓
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 2f)
            cubicTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
            cubicTo(2f, 17.5f, 6.5f, 22f, 12f, 22f)
            cubicTo(17.5f, 22f, 22f, 17.5f, 22f, 12f)
            cubicTo(22f, 6.5f, 17.5f, 2f, 12f, 2f)
        }
        // 神经网络节点
        path(
            fill = SolidColor(Color.White),
            stroke = null
        ) {
            moveTo(12f, 8f)
            arcTo(2f, 2f, 0f, false, true, 2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, -2f)
            arcTo(2f, 2f, 0f, false, true, 2f, -2f)
            close()
        }
        // 连接线
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 10f)
            lineTo(12f, 14f)
            moveTo(8f, 12f)
            lineTo(16f, 12f)
            moveTo(10f, 10f)
            lineTo(14f, 14f)
            moveTo(14f, 10f)
            lineTo(10f, 14f)
        }
        // 外围节点
        path(
            fill = SolidColor(Color.White.copy(alpha = 0.5f)),
            stroke = null
        ) {
            moveTo(7f, 7f)
            arcTo(1f, 1f, 0f, false, true, 1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, -1f)
            arcTo(1f, 1f, 0f, false, true, 1f, -1f)
            close()
            moveTo(17f, 7f)
            arcTo(1f, 1f, 0f, false, true, 1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, -1f)
            arcTo(1f, 1f, 0f, false, true, 1f, -1f)
            close()
            moveTo(7f, 17f)
            arcTo(1f, 1f, 0f, false, true, 1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, -1f)
            arcTo(1f, 1f, 0f, false, true, 1f, -1f)
            close()
            moveTo(17f, 17f)
            arcTo(1f, 1f, 0f, false, true, 1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, -1f)
            arcTo(1f, 1f, 0f, false, true, 1f, -1f)
            close()
        }
    }.build()

    /**
     * 水印图标 - 专业标签
     * 设计理念：品牌水印徽章，体现专业认证
     */
    val Watermark: ImageVector = ImageVector.Builder(
        name = "Watermark",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 徽章轮廓
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 2f)
            lineTo(15f, 5f)
            lineTo(20f, 5f)
            lineTo(20f, 10f)
            lineTo(23f, 13f)
            lineTo(20f, 16f)
            lineTo(20f, 21f)
            lineTo(15f, 21f)
            lineTo(12f, 24f)
            lineTo(9f, 21f)
            lineTo(4f, 21f)
            lineTo(4f, 16f)
            lineTo(1f, 13f)
            lineTo(4f, 10f)
            lineTo(4f, 5f)
            lineTo(9f, 5f)
            close()
        }
        // 内部印章
        path(
            fill = SolidColor(Color.White.copy(alpha = 0.2f)),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f
        ) {
            moveTo(12f, 8f)
            arcTo(5f, 5f, 0f, false, true, 5f, 5f)
            arcTo(5f, 5f, 0f, false, true, -5f, 5f)
            arcTo(5f, 5f, 0f, false, true, -5f, -5f)
            arcTo(5f, 5f, 0f, false, true, 5f, -5f)
            close()
        }
        // 认证标记
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(9f, 13f)
            lineTo(11f, 15f)
            lineTo(15f, 11f)
        }
    }.build()

    /**
     * 设置图标 - 精密调节
     * 设计理念：相机参数调节旋钮，体现专业控制
     */
    val Settings: ImageVector = ImageVector.Builder(
        name = "Settings",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 外圈齿轮
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
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
        // 齿轮齿
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 2f)
            lineTo(12f, 5f)
            moveTo(12f, 19f)
            lineTo(12f, 22f)
            moveTo(2f, 12f)
            lineTo(5f, 12f)
            moveTo(19f, 12f)
            lineTo(22f, 12f)
            moveTo(4.22f, 4.22f)
            lineTo(6.34f, 6.34f)
            moveTo(17.66f, 17.66f)
            lineTo(19.78f, 19.78f)
            moveTo(4.22f, 19.78f)
            lineTo(6.34f, 17.66f)
            moveTo(17.66f, 6.34f)
            lineTo(19.78f, 4.22f)
        }
        // 内圈调节点
        path(
            fill = SolidColor(Color.White),
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

    // ==================== 功能图标 ====================
    /**
     * 哈苏认证徽章图标
     */
    val HNCSCertified: ImageVector = ImageVector.Builder(
        name = "HNCSCertified",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 徽章盾形
        path(
            fill = SolidColor(HasselbladOrange),
            stroke = null
        ) {
            moveTo(12f, 2f)
            lineTo(20f, 6f)
            lineTo(20f, 12f)
            cubicTo(20f, 17f, 16f, 21f, 12f, 22f)
            cubicTo(8f, 21f, 4f, 17f, 4f, 12f)
            lineTo(4f, 6f)
            close()
        }
        // 认证勾选
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
     * 评分星星图标
     */
    val RatingStar: ImageVector = ImageVector.Builder(
        name = "RatingStar",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color(0xFFFFB800)),
            stroke = null
        ) {
            moveTo(12f, 2f)
            lineTo(15f, 8.5f)
            lineTo(22f, 9.5f)
            lineTo(17f, 14.5f)
            lineTo(18f, 21.5f)
            lineTo(12f, 18f)
            lineTo(6f, 21.5f)
            lineTo(7f, 14.5f)
            lineTo(2f, 9.5f)
            lineTo(9f, 8.5f)
            close()
        }
    }.build()

    /**
     * 下载图标
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
            stroke = SolidColor(OPPOGreen),
            strokeLineWidth = 2f,
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
            cubicTo(4f, 20.1f, 4.9f, 21f, 6f, 21f)
            lineTo(18f, 21f)
            cubicTo(19.1f, 21f, 20f, 20.1f, 20f, 19f)
            lineTo(20f, 17f)
        }
    }.build()

    // ==================== 相机参数图标 ====================
    /**
     * ISO图标 - 感光度
     */
    val ISO: ImageVector = ImageVector.Builder(
        name = "ISO",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 光线感光符号
        path(
            fill = null,
            stroke = SolidColor(HasselbladOrange),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 12f)
            lineTo(12f, 4f)
            moveTo(12f, 12f)
            lineTo(12f, 20f)
            moveTo(12f, 12f)
            lineTo(4f, 12f)
            moveTo(12f, 12f)
            lineTo(20f, 12f)
            moveTo(12f, 12f)
            lineTo(7f, 7f)
            moveTo(12f, 12f)
            lineTo(17f, 17f)
            moveTo(12f, 12f)
            lineTo(17f, 7f)
            moveTo(12f, 12f)
            lineTo(7f, 17f)
        }
        // 中心点
        path(
            fill = SolidColor(HasselbladOrange),
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
     * 快门图标 - 时间控制
     */
    val Shutter: ImageVector = ImageVector.Builder(
        name = "Shutter",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 快门叶片
        path(
            fill = null,
            stroke = SolidColor(OPPOGreen),
            strokeLineWidth = 2f,
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
        // 叶片分割线
        path(
            fill = null,
            stroke = SolidColor(OPPOGreen),
            strokeLineWidth = 1.5f
        ) {
            moveTo(12f, 4f)
            lineTo(12f, 12f)
            lineTo(20f, 12f)
            moveTo(12f, 12f)
            lineTo(18f, 6f)
            moveTo(12f, 12f)
            lineTo(18f, 18f)
            moveTo(12f, 12f)
            lineTo(6f, 18f)
            moveTo(12f, 12f)
            lineTo(6f, 6f)
        }
        // 中心圆
        path(
            fill = SolidColor(OPPOGreen),
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
     * 光圈图标 - 景深控制
     */
    val Aperture: ImageVector = ImageVector.Builder(
        name = "Aperture",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 光圈叶片
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF1890FF)),
            strokeLineWidth = 2f,
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
        // f数值符号
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF1890FF)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(10f, 8f)
            lineTo(10f, 16f)
            moveTo(10f, 8f)
            lineTo(14f, 8f)
            moveTo(10f, 12f)
            lineTo(13f, 12f)
        }
    }.build()

    /**
     * 曝光补偿图标 - EV
     */
    val EV: ImageVector = ImageVector.Builder(
        name = "EV",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 亮度条
        path(
            fill = null,
            stroke = SolidColor(Color(0xFFFFB800)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 12f)
            lineTo(20f, 12f)
        }
        // 加减符号
        path(
            fill = null,
            stroke = SolidColor(Color(0xFFFFB800)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(8f, 8f)
            lineTo(8f, 16f)
            moveTo(5f, 12f)
            lineTo(11f, 12f)
            moveTo(16f, 12f)
            lineTo(19f, 12f)
        }
    }.build()

    /**
     * 白平衡图标 - WB
     */
    val WB: ImageVector = ImageVector.Builder(
        name = "WB",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 温度条
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF8B5CF6)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(4f, 12f)
            cubicTo(4f, 8f, 8f, 8f, 12f, 8f)
            cubicTo(16f, 8f, 20f, 8f, 20f, 12f)
            cubicTo(20f, 16f, 16f, 16f, 12f, 16f)
            cubicTo(8f, 16f, 4f, 16f, 4f, 12f)
        }
        // 调节点
        path(
            fill = SolidColor(Color(0xFF8B5CF6)),
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

    // ==================== 场景识别图标 ====================
    /**
     * 人像场景图标
     */
    val Portrait: ImageVector = ImageVector.Builder(
        name = "Portrait",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 人物轮廓
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 4f)
            arcTo(4f, 4f, 0f, false, true, 4f, 4f)
            arcTo(4f, 4f, 0f, false, true, -4f, 4f)
            arcTo(4f, 4f, 0f, false, true, -4f, -4f)
            arcTo(4f, 4f, 0f, false, true, 4f, -4f)
            close()
        }
        // 身体
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 8f)
            cubicTo(8f, 8f, 5f, 11f, 5f, 15f)
            lineTo(5f, 22f)
            lineTo(19f, 22f)
            lineTo(19f, 15f)
            cubicTo(19f, 11f, 16f, 8f, 12f, 8f)
        }
    }.build()

    /**
     * 夜景场景图标
     */
    val Night: ImageVector = ImageVector.Builder(
        name = "Night",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 月亮
        path(
            fill = SolidColor(Color.White.copy(alpha = 0.8f)),
            stroke = null
        ) {
            moveTo(18f, 6f)
            arcTo(6f, 6f, 0f, false, true, 6f, 6f)
            arcTo(6f, 6f, 0f, false, true, -6f, 6f)
            arcTo(4f, 4f, 0f, false, true, 4f, -4f)
            arcTo(6f, 6f, 0f, false, true, 6f, 6f)
            close()
        }
        // 星星
        path(
            fill = SolidColor(Color.White),
            stroke = null
        ) {
            moveTo(6f, 10f)
            lineTo(6.5f, 11f)
            lineTo(7.5f, 11f)
            lineTo(6.8f, 11.8f)
            lineTo(7f, 12.5f)
            lineTo(6f, 12f)
            lineTo(5f, 12.5f)
            lineTo(5.2f, 11.8f)
            lineTo(4.5f, 11f)
            lineTo(5.5f, 11f)
            close()
        }
    }.build()

    /**
     * 美食场景图标
     */
    val Food: ImageVector = ImageVector.Builder(
        name = "Food",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 盘子
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
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
        // 餐具
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(8f, 6f)
            lineTo(8f, 18f)
            moveTo(16f, 6f)
            lineTo(16f, 10f)
            moveTo(16f, 10f)
            lineTo(16f, 18f)
        }
    }.build()

    /**
     * 风景场景图标
     */
    val Landscape: ImageVector = ImageVector.Builder(
        name = "Landscape",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 山脉
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(2f, 20f)
            lineTo(8f, 10f)
            lineTo(12f, 14f)
            lineTo(16f, 8f)
            lineTo(22f, 20f)
        }
        // 太阳
        path(
            fill = SolidColor(Color.White.copy(alpha = 0.5f)),
            stroke = null
        ) {
            moveTo(18f, 6f)
            arcTo(3f, 3f, 0f, false, true, 3f, 3f)
            arcTo(3f, 3f, 0f, false, true, -3f, 3f)
            arcTo(3f, 3f, 0f, false, true, -3f, -3f)
            arcTo(3f, 3f, 0f, false, true, 3f, -3f)
            close()
        }
    }.build()

    /**
     * 街拍场景图标
     */
    val Street: ImageVector = ImageVector.Builder(
        name = "Street",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 建筑
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 22f)
            lineTo(4f, 8f)
            lineTo(10f, 8f)
            lineTo(10f, 22f)
            moveTo(14f, 22f)
            lineTo(14f, 4f)
            lineTo(20f, 4f)
            lineTo(20f, 22f)
        }
        // 窗户
        path(
            fill = SolidColor(Color.White.copy(alpha = 0.3f)),
            stroke = null
        ) {
            moveTo(6f, 10f)
            lineTo(6f, 12f)
            lineTo(8f, 12f)
            lineTo(8f, 10f)
            close()
            moveTo(16f, 6f)
            lineTo(16f, 8f)
            lineTo(18f, 8f)
            lineTo(18f, 6f)
            close()
        }
    }.build()

    /**
     * 微距场景图标
     */
    val Macro: ImageVector = ImageVector.Builder(
        name = "Macro",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // 花朵
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 12f)
            arcTo(3f, 3f, 0f, false, true, 3f, 3f)
            arcTo(3f, 3f, 0f, false, true, -3f, 3f)
            arcTo(3f, 3f, 0f, false, true, -3f, -3f)
            arcTo(3f, 3f, 0f, false, true, 3f, -3f)
            close()
        }
        // 花瓣
        path(
            fill = SolidColor(Color.White.copy(alpha = 0.2f)),
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f
        ) {
            moveTo(12f, 6f)
            arcTo(2f, 2f, 0f, false, true, 2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, -2f)
            arcTo(2f, 2f, 0f, false, true, 2f, -2f)
            close()
            moveTo(12f, 18f)
            arcTo(2f, 2f, 0f, false, true, 2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, -2f)
            arcTo(2f, 2f, 0f, false, true, 2f, -2f)
            close()
            moveTo(6f, 12f)
            arcTo(2f, 2f, 0f, false, true, 2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, -2f)
            arcTo(2f, 2f, 0f, false, true, 2f, -2f)
            close()
            moveTo(18f, 12f)
            arcTo(2f, 2f, 0f, false, true, 2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, 2f)
            arcTo(2f, 2f, 0f, false, true, -2f, -2f)
            arcTo(2f, 2f, 0f, false, true, 2f, -2f)
            close()
        }
        // 放大镜框
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(2f, 2f)
            lineTo(22f, 2f)
            lineTo(22f, 22f)
            lineTo(2f, 22f)
            close()
        }
    }.build()
}