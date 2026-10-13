package com.silas.omaster.ui.service

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.silas.omaster.util.IconFont

/**
 * MU-11② 悬浮窗视图构建器拆分 · 第一批：共享参数控件族。
 *
 * 从 FloatingWindowService 原样迁移（行为等价，交接单第 2 步）：
 * 图标按钮 / 区域标题 / 高亮参数项 / 网格参数项 / 参数行 / 渐变圆背景 / 外发光 / 间距。
 * 配色与 dp 换算由 Service 注入，交互回调仍由调用方组装。
 */
class FloatingWindowWidgetFactory(
    private val context: Context,
    private val dp: (Int) -> Int,
    private val primaryColor: Int,
    private val primaryDark: Int,
    private val cardBackground: Int,
    private val textPrimary: Int,
    private val textSecondary: Int,
    private val textMuted: Int
) {

    /** 创建图标按钮 */
    fun createIconButton(icon: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = icon
            textSize = 14f
            setTextColor(textSecondary)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(cardBackground)
            }
            setOnClickListener { onClick() }
        }
    }

    /** 创建区域标题 */
    fun createSectionTitle(title: String): TextView {
        return TextView(context).apply {
            text = title
            textSize = 11f
            setTextColor(textMuted)
            setPadding(0, dp(12), 0, dp(8))
        }
    }

    /** 创建高亮参数项（滤镜专用）- 带唯一 Tag */
    fun createHighlightedParam(icon: String, label: String, value: String, valueTag: String? = null): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(Color.parseColor("#20FF6B35"))
                setStroke(dp(1), Color.parseColor("#40FF6B35"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(8))
            }

            // 图标（使用 Iconfont）
            addView(TextView(this@FloatingWindowWidgetFactory.context).apply {
                text = icon
                textSize = 18f
                setTextColor(primaryColor)
                typeface = IconFont.getTypeface(context)
            })

            addView(createSpacing(dp(8)))

            // 标签
            addView(TextView(this@FloatingWindowWidgetFactory.context).apply {
                text = label
                textSize = 13f
                setTextColor(textSecondary)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            // 值 - 带唯一 Tag 用于数据刷新
            addView(TextView(this@FloatingWindowWidgetFactory.context).apply {
                text = value
                textSize = 14f
                setTextColor(primaryColor)
                setPadding(dp(8), dp(2), dp(8), dp(2))
                background = GradientDrawable().apply {
                    cornerRadius = dp(6).toFloat()
                    setColor(Color.parseColor("#30FF6B35"))
                }
                // 使用 label + span 作为唯一标识
                tag = "value_${label}_2"
            })
        }
    }

    /** 创建小型参数项（用于网格）- 带唯一 Tag */
    fun createSmallParamItem(icon: String, label: String, value: String, valueTag: String? = null): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(cardBackground)
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dp(4), 0)
            }

            // 图标（使用 Iconfont）
            addView(TextView(this@FloatingWindowWidgetFactory.context).apply {
                text = icon
                textSize = 14f
                setTextColor(primaryColor)
                typeface = IconFont.getTypeface(context)
            })

            addView(createSpacing(dp(4)))

            addView(TextView(this@FloatingWindowWidgetFactory.context).apply {
                text = "$label "
                textSize = 11f
                setTextColor(textMuted)
            })

            // 值 - 带唯一 Tag 用于数据刷新
            addView(TextView(this@FloatingWindowWidgetFactory.context).apply {
                text = value
                textSize = 12f
                setTextColor(textPrimary)
                // 使用 label + span 作为唯一标识
                tag = "value_${label}_1"
            })
        }
    }

    /** 创建参数行（两个参数并排） */
    fun createParamRow(left: LinearLayout, right: LinearLayout?): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, 0)

            addView(left)
            if (right != null) {
                right.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(4), 0, 0, 0)
                }
                addView(right)
            } else {
                // 占位
                addView(View(this@FloatingWindowWidgetFactory.context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                })
            }
        }
    }

    /** 创建渐变圆形背景（收起按钮） */
    fun createGradientCircleBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors = intArrayOf(primaryColor, primaryDark)
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = dp(24).toFloat()
        }
    }

    /** 创建外发光效果 */
    fun createGlowBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors = intArrayOf(
                Color.parseColor("#40FF6B35"),
                Color.parseColor("#20FF6B35"),
                Color.TRANSPARENT
            )
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = dp(28).toFloat()
        }
    }

    /** 创建间距 */
    fun createSpacing(size: Int): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(size, size)
        }
    }
}
