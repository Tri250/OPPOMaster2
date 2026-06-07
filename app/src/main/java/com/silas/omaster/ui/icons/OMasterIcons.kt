package com.silas.omaster.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.theme.*

/**
 * =====================================================
 * OMaster 设计系统 - Lucide风格图标
 * =====================================================
 * 图标风格：线性图标，24x24，strokeWidth=2
 * 参考：lucide-react图标库
 */

object OMasterIcons {

    // ==================== 导航图标 ====================

    /**
     * 首页 - 相机图标
     * lucide: Camera
     */
    val Camera: ImageVector = ImageVector.Builder(
        name = "Camera",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(TextWhite),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // 相机轮廓
            moveTo(23f, 19f)
            curveTo(23f, 19.5f, 22.8f, 20f, 22.4f, 20.4f)
            curveTo(22f, 20.8f, 21.5f, 21f, 21f, 21f)
            lineTo(3f, 21f)
            curveTo(2.5f, 21f, 2f, 20.8f, 1.6f, 20.4f)
            curveTo(1.2f, 20f, 1f, 19.5f, 1f, 19f)
            lineTo(1f, 8f)
            curveTo(1f, 7.5f, 1.2f, 7f, 1.6f, 6.6f)
            curveTo(2f, 6.2f, 2.5f, 6f, 3f, 6f)
            lineTo(7f, 6f)
            lineTo(9f, 3f)
            lineTo(15f, 3f)
            lineTo(17f, 6f)
            lineTo(21f, 6f)
            curveTo(21.5f, 6f, 22f, 6.2f, 22.4f, 6.6f)
            curveTo(22.8f, 7f, 23f, 7.5f, 23f, 8f)
            close()
            // 镜头圆
            moveTo(12f, 17f)
            arcTo(4f, 4f, 0f, false, true, 4f, 4f)
            arcTo(4f, 4f, 0f, false, true, -4f, 4f)
            arcTo(4f, 4f, 0f, false, true, -4f, -4f)
            arcTo(4f, 4f, 0f, false, true, 4f, -4f)
            close()
        }
    }.build()

    /**
     * AI - 星光图标
     * lucide: Sparkles
     */
    val Sparkles: ImageVector = ImageVector.Builder(
        name = "Sparkles",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(TextWhite),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // 中心星
            moveTo(12f, 3f)
            lineTo(13.5f, 8.5f)
            lineTo(18f, 12f)
            lineTo(13.5f, 15.5f)
            lineTo(12f, 21f)
            lineTo(10.5f, 15.5f)
            lineTo(6f, 12f)
            lineTo(10.5f, 8.5f)
            close()
            // 左上小星
            moveTo(5f, 3f)
            lineTo(5.5f, 5f)
            lineTo(7f, 5.5f)
            lineTo(5.5f, 6f)
            lineTo(5f, 8f)
            lineTo(4.5f, 6f)
            lineTo(3f, 5.5f)
            lineTo(4.5f, 5f)
            close()
            // 右下小星
            moveTo(19f, 17f)
            lineTo(19.5f, 19f)
            lineTo(21f, 19.5f)
            lineTo(19.5f, 20f)
            lineTo(19f, 22f)
            lineTo(18.5f, 20f)
            lineTo(17f, 19.5f)
            lineTo(18.5f, 19f)
            close()
        }
    }.build()

    /**
     * 水印 - 标签图标
     * lucide: Tag
     */
    val Tag: ImageVector = ImageVector.Builder(
        name = "Tag",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(TextWhite),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(20f, 13.5f)
            lineTo(20f, 5.5f)
            curveTo(20f, 5f, 19.5f, 4.5f, 19f, 4.5f)
            lineTo(12f, 4.5f)
            curveTo(11.7f, 4.5f, 11.4f, 4.6f, 11.2f, 4.8f)
            lineTo(3.8f, 12.2f)
            curveTo(3.4f, 12.6f, 3.4f, 13.4f, 3.8f, 13.8f)
            lineTo(10.2f, 20.2f)
            curveTo(10.6f, 20.6f, 11.4f, 20.6f, 11.8f, 20.2f)
            lineTo(19.2f, 12.8f)
            curveTo(19.4f, 12.6f, 19.5f, 12.3f, 19.5f, 12f)
            lineTo(19.5f, 11f)
            // 标签孔
            moveTo(7.5f, 7.5f)
            arcTo(0.5f, 0.5f, 0f, false, true, 0.5f, 0.5f)
            arcTo(0.5f, 0.5f, 0f, false, true, -0.5f, 0.5f)
            arcTo(0.5f, 0.5f, 0f, false, true, -0.5f, -0.5f)
            arcTo(0.5f, 0.5f, 0f, false, true, 0.5f, -0.5f)
            close()
        }
    }.build()

    /**
     * 设置 - 齿轮图标
     * lucide: Settings
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
            stroke = SolidColor(TextWhite),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // 外圈齿轮
            moveTo(12.2f, 1f)
            lineTo(14.8f, 3.6f)
            lineTo(18f, 3f)
            lineTo(19f, 6f)
            lineTo(22f, 7.2f)
            lineTo(21f, 10.4f)
            lineTo(23f, 12.8f)
            lineTo(21f, 15.2f)
            lineTo(22f, 18.4f)
            lineTo(19f, 19.6f)
            lineTo(18f, 22.6f)
            lineTo(14.8f, 22f)
            lineTo(12.2f, 24f)
            lineTo(9.8f, 22f)
            lineTo(6.6f, 22.6f)
            lineTo(5.6f, 19.6f)
            lineTo(2.6f, 18.4f)
            lineTo(3.6f, 15.2f)
            lineTo(1f, 12.8f)
            lineTo(3.6f, 10.4f)
            lineTo(2.6f, 7.2f)
            lineTo(5.6f, 6f)
            lineTo(6.6f, 3f)
            lineTo(9.8f, 3.6f)
            close()
            // 内圈
            moveTo(12f, 12f)
            arcTo(3f, 3f, 0f, false, true, 3f, 3f)
            arcTo(3f, 3f, 0f, false, true, -3f, 3f)
            arcTo(3f, 3f, 0f, false, true, -3f, -3f)
            arcTo(3f, 3f, 0f, false, true, 3f, -3f)
            close()
        }
    }.build()

    // ==================== 功能图标 ====================

    /**
     * 下载 - 向下箭头
     * lucide: Download
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
            stroke = SolidColor(TextWhite),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // 向下箭头
            moveTo(12f, 3f)
            lineTo(12f, 15f)
            moveTo(8f, 11f)
            lineTo(12f, 15f)
            lineTo(16f, 11f)
            // 底部横线
            moveTo(4f, 17f)
            lineTo(4f, 19f)
            curveTo(4f, 20f, 5f, 21f, 6f, 21f)
            lineTo(18f, 21f)
            curveTo(19f, 21f, 20f, 20f, 20f, 19f)
            lineTo(20f, 17f)
        }
    }.build()

    /**
     * 星星评分
     * lucide: Star
     */
    val Star: ImageVector = ImageVector.Builder(
        name = "Star",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Yellow400),
            stroke = null
        ) {
            moveTo(12f, 2f)
            lineTo(15.1f, 8.3f)
            lineTo(22f, 9.3f)
            lineTo(17f, 14.3f)
            lineTo(18.2f, 21.2f)
            lineTo(12f, 18f)
            lineTo(5.8f, 21.2f)
            lineTo(7f, 14.3f)
            lineTo(2f, 9.3f)
            lineTo(8.9f, 8.3f)
            close()
        }
    }.build()

    /**
     * 星星空心
     * lucide: Star (outline)
     */
    val StarOutline: ImageVector = ImageVector.Builder(
        name = "StarOutline",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Yellow400),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 2f)
            lineTo(15.1f, 8.3f)
            lineTo(22f, 9.3f)
            lineTo(17f, 14.3f)
            lineTo(18.2f, 21.2f)
            lineTo(12f, 18f)
            lineTo(5.8f, 21.2f)
            lineTo(7f, 14.3f)
            lineTo(2f, 9.3f)
            lineTo(8.9f, 8.3f)
            close()
        }
    }.build()

    /**
     * 认证盾牌
     * lucide: Shield
     */
    val Shield: ImageVector = ImageVector.Builder(
        name = "Shield",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Orange500),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // 盾牌轮廓
            moveTo(12f, 22f)
            curveTo(12f, 22f, 20f, 18f, 20f, 12f)
            curveTo(20f, 12f, 20f, 6f, 20f, 5f)
            curveTo(20f, 4f, 19f, 3f, 18f, 3f)
            lineTo(6f, 3f)
            curveTo(5f, 3f, 4f, 4f, 4f, 5f)
            curveTo(4f, 6f, 4f, 12f, 4f, 12f)
            curveTo(4f, 18f, 12f, 22f, 12f, 22f)
            close()
            // 对勾
            moveTo(9f, 12f)
            lineTo(11f, 14f)
            lineTo(15f, 10f)
        }
    }.build()

    /**
     * GitHub
     * lucide: Github
     */
    val Github: ImageVector = ImageVector.Builder(
        name = "Github",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(TextWhite),
            stroke = null
        ) {
            moveTo(12f, 0f)
            curveTo(5.4f, 0f, 0f, 5.4f, 0f, 12f)
            curveTo(0f, 17.4f, 3.5f, 22f, 8.3f, 23.6f)
            curveTo(8.9f, 23.7f, 9.1f, 23.3f, 9.1f, 23f)
            curveTo(9.1f, 22.7f, 9.1f, 21.8f, 9.1f, 20.8f)
            curveTo(5.7f, 21.5f, 5f, 19.2f, 5f, 19.2f)
            curveTo(4.5f, 18f, 3.7f, 17.6f, 3.7f, 17.6f)
            curveTo(2.5f, 16.9f, 3.8f, 16.9f, 3.8f, 16.9f)
            curveTo(5.1f, 17f, 5.8f, 18.2f, 5.8f, 18.2f)
            curveTo(7f, 20.1f, 8.8f, 19.5f, 9.2f, 19.2f)
            curveTo(9.3f, 18.3f, 9.7f, 17.7f, 10.1f, 17.4f)
            curveTo(7.3f, 17.1f, 4.4f, 16f, 4.4f, 11.4f)
            curveTo(4.4f, 10f, 4.9f, 8.9f, 5.8f, 8f)
            curveTo(5.7f, 7.7f, 5.2f, 6.4f, 5.9f, 4.8f)
            curveTo(5.9f, 4.8f, 7f, 4.4f, 9.1f, 5.9f)
            curveTo(10.1f, 5.6f, 11.1f, 5.5f, 12.1f, 5.5f)
            curveTo(13.1f, 5.5f, 14.1f, 5.6f, 15.1f, 5.9f)
            curveTo(17.2f, 4.4f, 18.3f, 4.8f, 18.3f, 4.8f)
            curveTo(19f, 6.4f, 18.5f, 7.7f, 18.4f, 8f)
            curveTo(19.3f, 8.9f, 19.8f, 10f, 19.8f, 11.4f)
            curveTo(19.8f, 16f, 16.9f, 17.1f, 14.1f, 17.4f)
            curveTo(14.6f, 17.8f, 15f, 18.5f, 15f, 19.6f)
            curveTo(15f, 21.2f, 15f, 22.5f, 15f, 23f)
            curveTo(15f, 23.3f, 15.2f, 23.7f, 15.8f, 23.6f)
            curveTo(20.6f, 22f, 24f, 17.4f, 24f, 12f)
            curveTo(24f, 5.4f, 18.6f, 0f, 12f, 0f)
            close()
        }
    }.build()

    /**
     * 箭头右
     * lucide: ChevronRight
     */
    val ChevronRight: ImageVector = ImageVector.Builder(
        name = "ChevronRight",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(TextZinc500),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(9f, 18f)
            lineTo(15f, 12f)
            lineTo(9f, 6f)
        }
    }.build()

    /**
     * 箭头左
     * lucide: ChevronLeft
     */
    val ChevronLeft: ImageVector = ImageVector.Builder(
        name = "ChevronLeft",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(TextWhite),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(15f, 18f)
            lineTo(9f, 12f)
            lineTo(15f, 6f)
        }
    }.build()

    /**
     * 箭头下
     * lucide: ChevronDown
     */
    val ChevronDown: ImageVector = ImageVector.Builder(
        name = "ChevronDown",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(TextZinc500),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 9f)
            lineTo(12f, 15f)
            lineTo(18f, 9f)
        }
    }.build()

    /**
     * 检查/对勾
     * lucide: Check
     */
    val Check: ImageVector = ImageVector.Builder(
        name = "Check",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Green500),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(20f, 6f)
            lineTo(9f, 17f)
            lineTo(4f, 12f)
        }
    }.build()

    /**
     * X关闭
     * lucide: X
     */
    val X: ImageVector = ImageVector.Builder(
        name = "X",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(TextZinc400),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(18f, 6f)
            lineTo(6f, 18f)
            moveTo(6f, 6f)
            lineTo(18f, 18f)
        }
    }.build()

    /**
     * 圆点/指示器
     * 用于列表项
     */
    val Dot: ImageVector = ImageVector.Builder(
        name = "Dot",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Orange500),
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
     * 更多/三点
     * lucide: MoreHorizontal
     */
    val MoreHorizontal: ImageVector = ImageVector.Builder(
        name = "MoreHorizontal",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(TextZinc400),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(12f, 12f)
            arcTo(1f, 1f, 0f, false, true, 1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, -1f)
            arcTo(1f, 1f, 0f, false, true, 1f, -1f)
            close()
            moveTo(19f, 12f)
            arcTo(1f, 1f, 0f, false, true, 1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, -1f)
            arcTo(1f, 1f, 0f, false, true, 1f, -1f)
            close()
            moveTo(5f, 12f)
            arcTo(1f, 1f, 0f, false, true, 1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, 1f)
            arcTo(1f, 1f, 0f, false, true, -1f, -1f)
            arcTo(1f, 1f, 0f, false, true, 1f, -1f)
            close()
        }
    }.build()
}