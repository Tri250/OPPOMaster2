package com.silas.omaster.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 滑动式前后对比视图
 *
 * 左侧显示"处理前"原图，右侧显示"处理后"效果图
 * 可拖拽中间分隔线对比效果
 */
@Composable
fun CompareSliderView(
    beforeImage: Bitmap?,  // Original image
    afterImage: Bitmap?,   // Edited image
    modifier: Modifier = Modifier,
    dividerPosition: Float = 0.5f,         // 0.0~1.0, where the divider is
    onDividerPositionChanged: (Float) -> Unit = {},
    dividerColor: Color = Color.White,
    handleRadius: Float = 20f
) {
    val beforeImageBitmap = remember(beforeImage) { beforeImage?.asImageBitmap() }
    val afterImageBitmap = remember(afterImage) { afterImage?.asImageBitmap() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val newX = (change.position.x / size.width).coerceIn(0f, 1f)
                        onDividerPositionChanged(newX)
                        change.consume()
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val dividerX = canvasWidth * dividerPosition

            // 1. Draw the AFTER image full size (as the base layer)
            if (afterImageBitmap != null) {
                val srcSize = computeFitSize(
                    srcWidth = afterImageBitmap.width,
                    srcHeight = afterImageBitmap.height,
                    dstWidth = canvasWidth,
                    dstHeight = canvasHeight
                )
                val offsetX = (canvasWidth - srcSize.width) / 2f
                val offsetY = (canvasHeight - srcSize.height) / 2f

                drawImage(
                    image = afterImageBitmap,
                    dstOffset = Offset(offsetX, offsetY),
                    dstSize = srcSize
                )

                // 2. Clip and draw the BEFORE image on the left side
                clipRect(0f, 0f, dividerX, canvasHeight) {
                    if (beforeImageBitmap != null) {
                        drawImage(
                            image = beforeImageBitmap,
                            dstOffset = Offset(offsetX, offsetY),
                            dstSize = srcSize
                        )
                    }
                }
            } else if (beforeImageBitmap != null) {
                // Only before image available, show it on left
                val srcSize = computeFitSize(
                    srcWidth = beforeImageBitmap.width,
                    srcHeight = beforeImageBitmap.height,
                    dstWidth = canvasWidth,
                    dstHeight = canvasHeight
                )
                val offsetX = (canvasWidth - srcSize.width) / 2f
                val offsetY = (canvasHeight - srcSize.height) / 2f

                clipRect(0f, 0f, dividerX, canvasHeight) {
                    drawImage(
                        image = beforeImageBitmap,
                        dstOffset = Offset(offsetX, offsetY),
                        dstSize = srcSize
                    )
                }
            }

            // 3. Draw the divider line
            drawLine(
                color = dividerColor,
                start = Offset(dividerX, 0f),
                end = Offset(dividerX, canvasHeight),
                strokeWidth = 2.5f
            )

            // 4. Draw the handle circle with arrows
            val handleCenterY = canvasHeight / 2f

            // Semi-transparent background circle for handle
            drawCircle(
                color = dividerColor.copy(alpha = 0.9f),
                radius = handleRadius,
                center = Offset(dividerX, handleCenterY)
            )

            // Left arrow (◄)
            val arrowSize = handleRadius * 0.4f
            val arrowLeftPath = Path().apply {
                val cx = dividerX - arrowSize * 1.2f
                val cy = handleCenterY
                moveTo(cx, cy - arrowSize)
                lineTo(cx - arrowSize, cy)
                lineTo(cx, cy + arrowSize)
                close()
            }
            drawPath(
                path = arrowLeftPath,
                color = Color.Black.copy(alpha = 0.7f)
            )

            // Right arrow (►)
            val arrowRightPath = Path().apply {
                val cx = dividerX + arrowSize * 1.2f
                val cy = handleCenterY
                moveTo(cx, cy - arrowSize)
                lineTo(cx + arrowSize, cy)
                lineTo(cx, cy + arrowSize)
                close()
            }
            drawPath(
                path = arrowRightPath,
                color = Color.Black.copy(alpha = 0.7f)
            )

            // 5. Draw shadow on divider line for visibility
            drawLine(
                color = Color.Black.copy(alpha = 0.3f),
                start = Offset(dividerX - 1f, 0f),
                end = Offset(dividerX - 1f, canvasHeight),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.3f),
                start = Offset(dividerX + 1f, 0f),
                end = Offset(dividerX + 1f, canvasHeight),
                strokeWidth = 1f
            )
        }

        // 6. "BEFORE" / "AFTER" labels at top corners
        Text(
            text = "原图",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Text(
            text = "效果",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Compute the destination size for ContentScale.Fit behavior
 */
private fun computeFitSize(
    srcWidth: Int,
    srcHeight: Int,
    dstWidth: Float,
    dstHeight: Float
): Size {
    if (srcWidth <= 0 || srcHeight <= 0) return Size(dstWidth, dstHeight)
    val srcAspect = srcWidth.toFloat() / srcHeight.toFloat()
    val dstAspect = dstWidth / dstHeight
    return if (srcAspect > dstAspect) {
        // Source is wider, fit by width
        Size(dstWidth, dstWidth / srcAspect)
    } else {
        // Source is taller, fit by height
        Size(dstHeight * srcAspect, dstHeight)
    }
}
