package com.silas.omaster.ui.service

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.silas.omaster.model.PresetSection
import com.silas.omaster.util.IconFont

/**
 * MU-11② 悬浮窗拆分 · 第二批：紧凑视图（参数条）构建族。
 *
 * 从 FloatingWindowService 原样迁移（行为等价，交接单第 2 步）：
 * createCompactView / Header / IconButton / ContentArea / ValueCell / IconCell / Background。
 * 服务耦合点通过构造器与回调注入：切换/关闭/收起回调、背景色策略。
 * 构建产生的 mainContainer / titleView 引用通过属性回传，Service 数据刷新逻辑不变。
 */
class FloatingWindowCompactFactory(
    private val context: Context,
    private val dp: (Int) -> Int,
    private val primaryColor: Int,
    private val cardBackground: Int,
    private val textPrimary: Int,
    private val textSecondary: Int,
    private val getBackgroundColor: (Context) -> Int
) {

    /** 构建产物引用：Service 用于后续数据刷新（mainContainer / titleTextView） */
    var mainContainerRef: LinearLayout? = null
    var titleViewRef: TextView? = null

    /** 创建紧凑视图 - 新版参数条样式 */
    fun createCompactView(
        name: String,
        sections: ArrayList<PresetSection>,
        currentIndex: Int = 0,
        totalCount: Int = 1,
        windowWidth: Int,
        onCollapse: () -> Unit,
        onSwitchPreset: (String) -> Unit,
        onClose: () -> Unit
    ): FrameLayout {
        return FrameLayout(this.context).apply {
            layoutParams = FrameLayout.LayoutParams(
                windowWidth,
                dp(120)
            )

            // 主容器
            val container = LinearLayout(this@FloatingWindowCompactFactory.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    windowWidth,
                    dp(120)
                )
                background = createCompactBackground(this@FloatingWindowCompactFactory.context)
            }
            mainContainerRef = container

            // 添加标题栏（预设名称 + 切换按钮 + 收起按钮）
            container.addView(createCompactHeader(name, onCollapse, onSwitchPreset, onClose, currentIndex, totalCount))

            // 内容容器（带tag，用于更新时查找）
            val contentContainer = LinearLayout(this@FloatingWindowCompactFactory.context).apply {
                orientation = LinearLayout.VERTICAL
                tag = "content_container"
            }
            contentContainer.addView(createCompactContentArea(sections))
            container.addView(contentContainer)

            addView(container)
        }
    }

    /** 创建紧凑标题栏 */
    private fun createCompactHeader(
        name: String,
        onCollapse: () -> Unit,
        onSwitchPreset: (String) -> Unit,
        onClose: () -> Unit,
        currentIndex: Int = 0,
        totalCount: Int = 1
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(40)
            )
            setPadding(dp(12), dp(4), dp(12), dp(4))

            // 上一个预设按钮
            addView(createCompactIconButton("◀") { onSwitchPreset("prev") })

            addView(spacing(dp(8)))

            // 预设名称
            val titleView = TextView(this@FloatingWindowCompactFactory.context).apply {
                text = name
                textSize = 14f
                setTextColor(primaryColor)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
            titleViewRef = titleView
            addView(titleView)

            addView(spacing(dp(8)))

            // 下一个预设按钮
            addView(createCompactIconButton("▶") { onSwitchPreset("next") })

            addView(spacing(dp(8)))

            // 收起按钮
            addView(createCompactIconButton("▼") { onCollapse() })

            addView(spacing(dp(4)))

            // 关闭按钮
            addView(createCompactIconButton("✕") { onClose() })
        }
    }

    /** 创建紧凑图标按钮 */
    private fun createCompactIconButton(icon: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = icon
            textSize = 12f
            setTextColor(textSecondary)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setColor(cardBackground)
            }
            // 禁用父视图拦截按钮的触摸事件
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 请求父视图不要拦截此事件
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false // 继续传递给 onClickListener
            }
            setOnClickListener { onClick() }
        }
    }

    /** 创建紧凑内容区域 - 动态适配参数数量（支持8-11参数），只显示实际存在的参数 */
    private fun createCompactContentArea(sections: List<PresetSection>): LinearLayout {
        // 提取实际存在的参数（过滤掉无效的"-"）
        val paramData = PresetParamMapper.extractDynamicParams(context, sections)

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(80) // 参数区域高度保持 80dp
            )
            setPadding(dp(4), dp(4), dp(4), dp(4))

            // 第一行：参数值（带索引 Tag）
            val valuesRow = LinearLayout(this@FloatingWindowCompactFactory.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                gravity = Gravity.CENTER_VERTICAL

                paramData.forEachIndexed { index, (value, _) ->
                    addView(createCompactValueCell(value, index))
                }
            }
            addView(valuesRow)

            // 分割线
            addView(View(this@FloatingWindowCompactFactory.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                )
                setBackgroundColor(Color.parseColor("#20FFFFFF"))
            })

            // 第二行：图标（使用 Iconfont 或文字标签）
            val iconsRow = LinearLayout(this@FloatingWindowCompactFactory.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                gravity = Gravity.CENTER_VERTICAL

                paramData.forEach { (_, iconCode) ->
                    addView(createCompactIconCell(iconCode))
                }
            }
            addView(iconsRow)
        }
    }

    /** 创建紧凑参数值单元格（带 Tag 用于更新） */
    private fun createCompactValueCell(value: String, index: Int = 0): TextView {
        return TextView(context).apply {
            text = value
            textSize = 10f  // 11列时减小字体，避免溢出
            setTextColor(textPrimary)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, dp(2), 0, dp(2))
            // 带唯一 Tag 用于数据刷新
            tag = "compact_value_$index"
        }
    }

    /** 创建紧凑图标单元格（支持 Iconfont 和文字标签） */
    private fun createCompactIconCell(iconCode: String): TextView {
        return TextView(context).apply {
            text = iconCode
            textSize = 14f  // 11列时减小图标，保持美观
            setTextColor(primaryColor)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(0, dp(2), 0, dp(2))
            // Iconfont 图标用品牌字体，文字标签（如"清晰"、"颗粒"）用默认字体
            if (iconCode.startsWith("\\u") || iconCode.length == 1 && iconCode[0].code >= 0xE600) {
                typeface = IconFont.getTypeface(this@FloatingWindowCompactFactory.context)
            } else {
                typeface = Typeface.DEFAULT
            }
        }
    }

    /** 创建紧凑背景 */
    private fun createCompactBackground(bgContext: Context): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(getBackgroundColor(bgContext))
            setStroke(dp(1), Color.parseColor("#33FFFFFF"))
        }
    }

    private fun spacing(size: Int): View {
        return android.view.View(context).apply {
            layoutParams = LinearLayout.LayoutParams(size, size)
        }
    }
}
