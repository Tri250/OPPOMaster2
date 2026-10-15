package com.silas.omaster.ui.service

import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.silas.omaster.R
import com.silas.omaster.data.config.ConfigCenter
import com.silas.omaster.data.local.FloatingWindowMode
import com.silas.omaster.model.PresetItem
import com.silas.omaster.model.PresetSection
import com.silas.omaster.util.CameraFramingAvoidance
import com.silas.omaster.util.IconFont
import com.silas.omaster.util.Logger
import com.silas.omaster.util.PresetI18n
import com.silas.omaster.util.formatSigned

/**
 * 悬浮窗服务 - 高级美观版
 *
 * 优化内容：
 * 1. 毛玻璃效果背景
 * 2. 渐变标题栏
 * 3. 图标化参数展示
 * 4. 精致的收起/展开动画
 * 5. 悬浮球采用品牌色渐变
 * 6. 动态渲染内容（基于 sections）
 */
class FloatingWindowService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var isExpanded = true
    private var edgeAnimator: ValueAnimator? = null  // ✅ 新增：保存动画引用用于生命周期管理

    // 配色方案
    private val primaryColor = Color.parseColor("#FF6B35")      // 品牌橙色
    private val primaryDark = Color.parseColor("#E55A2B")       // 深橙色
    private val cardBackground = Color.parseColor("#26FFFFFF")  // 卡片背景
    private val textPrimary = Color.parseColor("#FFFFFF")       // 主文字
    private val textSecondary = Color.parseColor("#B3FFFFFF")   // 次要文字
    private val textMuted = Color.parseColor("#80FFFFFF")       // 弱化文字

    // MU-11②：共享参数控件族迁移至独立工厂（行为等价）
    private val widgets = FloatingWindowWidgetFactory(
        context = this,
        dp = { dpToPx(it) },
        primaryColor = primaryColor,
        primaryDark = primaryDark,
        cardBackground = cardBackground,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textMuted = textMuted
    )

    // MU-11②：紧凑视图（参数条）构建族迁移至独立工厂（行为等价）
    private val compactFactory = FloatingWindowCompactFactory(
        context = this,
        dp = { dpToPx(it) },
        primaryColor = primaryColor,
        cardBackground = cardBackground,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        getBackgroundColor = { getBackgroundColor(it) }
    )
    
    // 背景颜色根据设置动态计算
    private fun getBackgroundColor(context: Context): Int {
        val opacity = ConfigCenter.getInstance(context).floatingWindowOpacity
        val alpha = (opacity * 255 / 100).coerceIn(30, 255)
        return Color.argb(alpha, 26, 26, 26) // #1A1A1A with dynamic alpha
    }

    companion object {
        private const val EXTRA_NAME = "name"
        private const val EXTRA_SECTIONS = "sections"
        private const val EXTRA_PRESET_ID = "preset_id"
        private const val EXTRA_PRESET_INDEX = "preset_index"
        private const val EXTRA_PRESET_LIST = "preset_list"

        // 保存状态到 Intent 的键
        private const val EXTRA_IS_EXPANDED = "is_expanded"
        private const val EXTRA_POS_X = "pos_x"
        private const val EXTRA_POS_Y = "pos_y"
        private const val EXTRA_ACTION = "action"

        // Action 类型
        private const val ACTION_SHOW = "show"
        private const val ACTION_UPDATE = "update"

        // 广播 Action
        const val ACTION_SWITCH_PRESET = "com.silas.omaster.SWITCH_PRESET"
        const val EXTRA_SWITCH_DIRECTION = "switch_direction" // "prev" or "next"

        // 服务实例（用于更新内容）
        @Volatile
        private var instance: FloatingWindowService? = null

        fun show(context: Context, preset: com.silas.omaster.model.MasterPreset, presetIndex: Int = 0, presetIds: List<String> = emptyList()) {
            Logger.i("FloatingWindowService", "显示悬浮窗: ${preset.name}, 索引: $presetIndex, 总数: ${presetIds.size}")
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                putExtra(EXTRA_ACTION, ACTION_SHOW)
                putExtra(EXTRA_NAME, preset.name)
                // 获取动态生成的 sections
                val sections = preset.getDisplaySections(context)
                putParcelableArrayListExtra(EXTRA_SECTIONS, ArrayList(sections))

                putExtra(EXTRA_PRESET_ID, preset.id ?: "")
                putExtra(EXTRA_PRESET_INDEX, presetIndex)
                putStringArrayListExtra(EXTRA_PRESET_LIST, ArrayList(presetIds))
                putExtra(EXTRA_IS_EXPANDED, true)
            }
            context.startService(intent)
        }

        /**
         * 更新悬浮窗内容（不重启服务，避免闪动）
         */
        fun update(context: Context, preset: com.silas.omaster.model.MasterPreset, presetIndex: Int = 0, presetIds: List<String> = emptyList()) {
            Logger.d("FloatingWindowService", "更新悬浮窗: ${preset.name}, 索引: $presetIndex")
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                putExtra(EXTRA_ACTION, ACTION_UPDATE)
                putExtra(EXTRA_NAME, preset.name)
                val sections = preset.getDisplaySections(context)
                putParcelableArrayListExtra(EXTRA_SECTIONS, ArrayList(sections))

                putExtra(EXTRA_PRESET_ID, preset.id ?: "")
                putExtra(EXTRA_PRESET_INDEX, presetIndex)
                putStringArrayListExtra(EXTRA_PRESET_LIST, ArrayList(presetIds))
                putExtra(EXTRA_IS_EXPANDED, instance?.isExpanded ?: true)
            }
            context.startService(intent)
        }

        fun hide(context: Context) {
            Logger.i("FloatingWindowService", "隐藏悬浮窗")
            context.stopService(Intent(context, FloatingWindowService::class.java))
        }

        /**
         * 检查服务是否正在运行
         */
        fun isRunning(): Boolean = instance != null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        // ✅ 取消正在执行的贴边动画，防止内存泄漏和崩溃
        edgeAnimator?.cancel()
        edgeAnimator = null
        removeWindow()
        instance = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            if (floatingView == null) {
                stopSelf()
            }
            return START_NOT_STICKY
        }

        val action = intent.getStringExtra(EXTRA_ACTION) ?: ACTION_SHOW
        val rawName = intent.getStringExtra(EXTRA_NAME) ?: getString(R.string.floating_preset)
        val name = PresetI18n.getLocalizedPresetName(this, rawName)
        
        val sections = intent.getParcelableArrayListExtra<PresetSection>(EXTRA_SECTIONS) ?: arrayListOf()

        isExpanded = intent.getBooleanExtra(EXTRA_IS_EXPANDED, true)
        val savedX = intent.getIntExtra(EXTRA_POS_X, -1)
        val savedY = intent.getIntExtra(EXTRA_POS_Y, -1)
        val currentIndex = intent.getIntExtra(EXTRA_PRESET_INDEX, 0)
        val presetList = intent.getStringArrayListExtra(EXTRA_PRESET_LIST) ?: arrayListOf()
        val totalCount = presetList.size

        // 读取悬浮窗模式设置
        val configMode = ConfigCenter.getInstance(this).floatingWindowMode

        // ═══════════════════════════════════════════════════════════
        // MU-27 / v1.6：悬浮窗模式解析链（高 → 低）
        // ① Realme 预设 → STANDARD（Realme 相机 UI 与新版悬浮窗图标不兼容）
        // ② followCameraFraming=true && ColorOS ≥ 17 → FRAMING（取景框避位）
        // ③ 否则 → configMode（用户在设置里选的模式）
        // ═══════════════════════════════════════════════════════════
        val c2 = ConfigCenter.getInstance(this)
        val shouldFrame = c2.isFollowCameraFramingEnabled &&
                com.silas.omaster.util.DeviceDetector.isColorOsAtLeast(this, 17)
        val mode = when {
            PresetParamMapper.isRealmePreset(sections) -> FloatingWindowMode.STANDARD
            shouldFrame -> FloatingWindowMode.FRAMING
            else -> configMode
        }

        when (action) {
            ACTION_UPDATE -> {
                // 更新模式：只更新内容，不移除窗口（避免闪动）
                updateWindowContent(
                    name, sections, currentIndex, totalCount, mode
                )
            }
            else -> {
                // 显示模式：重新创建窗口
                removeWindow()
                if (isExpanded) {
                    when (mode) {
                        FloatingWindowMode.STANDARD -> showExpandedWindow(
                            name, sections, savedX, savedY,
                            currentIndex, totalCount
                        )
                        FloatingWindowMode.COMPACT -> showCompactWindow(
                            name, sections, savedX, savedY,
                            currentIndex, totalCount
                        )
                        // MU-27：FRAMING 取景框避位模式 — 在相机取景区域安全区放置悬浮窗
                        FloatingWindowMode.FRAMING -> showFramingWindow(
                            name, sections, savedX, savedY,
                            currentIndex, totalCount
                        )
                    }
                } else {
                    showCollapsedWindow(
                        name, sections, savedX, savedY
                    )
                }
            }
        }

        return START_STICKY
    }

    // 保存视图引用，用于更新内容
    private var mainContainer: LinearLayout? = null
    private var titleTextView: TextView? = null

    /**
     * 更新窗口内容（数据驱动刷新，避免 UI 重建）
     */
    private fun updateWindowContent(
        name: String,
        sections: ArrayList<PresetSection>,
        currentIndex: Int,
        totalCount: Int,
        mode: FloatingWindowMode = FloatingWindowMode.STANDARD
    ) {
        // 如果窗口不存在，直接创建新窗口
        if (floatingView == null || mainContainer == null) {
            when (mode) {
                FloatingWindowMode.STANDARD -> showExpandedWindow(
                    name, sections, 50, 300,
                    currentIndex, totalCount
                )
                FloatingWindowMode.COMPACT -> showCompactWindow(
                    name, sections, 50, 300,
                    currentIndex, totalCount
                )
            }
            return
        }

        try {
            // ✅ 1. 直接更新标题（无动画，避免闪烁）
            titleTextView?.text = name

            // ✅ 2. 更新内容区域（只更新数据，不重建视图）
            val contentContainer = mainContainer?.findViewWithTag<LinearLayout>("content_container")
            contentContainer?.let { container ->
                container.post {
                    when (mode) {
                        FloatingWindowMode.STANDARD -> updateStandardContent(sections)
                        FloatingWindowMode.COMPACT -> updateCompactContent(sections)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果更新失败（如 sections 结构变化），降级为重建窗口
            when (mode) {
                FloatingWindowMode.STANDARD -> showExpandedWindow(
                    name, sections, params?.x ?: 50, params?.y ?: 300,
                    currentIndex, totalCount
                )
                FloatingWindowMode.COMPACT -> showCompactWindow(
                    name, sections, params?.x ?: 50, params?.y ?: 300,
                    currentIndex, totalCount
                )
            }
        }
    }

    /**
     * 标准模式：只更新参数值（不重建视图）
     */
    private fun updateStandardContent(sections: List<PresetSection>) {
        // 遍历所有 section 和 item，根据 tag 查找并更新 TextView
        sections.forEach { section ->
            section.items.forEach { item ->
                // 为每个参数生成唯一的 tag（与创建时一致）
                val valueTag = "value_${item.label}_${item.span}"
                
                // 查找所有匹配的 TextView 并更新
                // 由于中文标签可能包含特殊字符，使用递归查找
                floatingView?.let { root ->
                    val valueView = findViewWithTagRecursive(root, valueTag)
                    valueView?.text = PresetI18n.resolveValue(this, item.value)
                }
            }
        }
    }

    /**
     * 递归查找带指定 Tag 的 TextView
     */
    private fun findViewWithTagRecursive(root: View, tag: String): TextView? {
        if (tag == root.tag) {
            return root as? TextView
        }
        
        if (root is android.view.ViewGroup) {
            for (i in 0 until root.childCount) {
                val child = root.getChildAt(i)
                val result = findViewWithTagRecursive(child, tag)
                if (result != null) {
                    return result
                }
            }
        }
        
        return null
    }

    /**
     * 紧凑模式：动态更新参数值（不重建视图）
     * 根据实际存在的参数数量动态更新
     */
    private fun updateCompactContent(sections: List<PresetSection>) {
        val paramData = PresetParamMapper.extractDynamicParams(this, sections)

        // 动态更新参数值 - 只更新实际存在的参数
        paramData.forEachIndexed { index, (value, _) ->
            val valueView = floatingView?.findViewWithTag<TextView>("compact_value_$index")
            valueView?.text = value
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showExpandedWindow(
        name: String,
        sections: ArrayList<PresetSection>,
        savedX: Int = -1,
        savedY: Int = -1,
        currentIndex: Int = 0,
        totalCount: Int = 1
    ) {
        try {
            val wm = windowManager ?: return

            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = if (savedX >= 0) savedX else 50
                y = if (savedY >= 0) savedY else 300
            }

            val rootLayout = createExpandedView(
                name, sections, currentIndex, totalCount
            ) { collapseToBubble(name, sections) }

            floatingView = rootLayout
            wm.addView(floatingView, params)
            setupDrag(wm)
            
            // 初始显示时自动贴边
            floatingView?.post { snapToEdge(wm) }

        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun showCollapsedWindow(
        name: String,
        sections: ArrayList<PresetSection>,
        savedX: Int = -1,
        savedY: Int = -1
    ) {
        try {
            val wm = windowManager ?: return

            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = if (savedX >= 0) savedX else 50
                y = if (savedY >= 0) savedY else 300
            }

            val miniButton = createCollapsedView(name) {
                val intent = Intent(this, FloatingWindowService::class.java).apply {
                    putExtra(EXTRA_NAME, name)
                    putParcelableArrayListExtra(EXTRA_SECTIONS, sections)
                    putExtra(EXTRA_IS_EXPANDED, true)
                    putExtra(EXTRA_POS_X, params?.x ?: 50)
                    putExtra(EXTRA_POS_Y, params?.y ?: 300)
                }
                startService(intent)
            }

            floatingView = miniButton
            wm.addView(floatingView, params)
            setupDrag(wm)
            
            // 初始显示时自动贴边
            floatingView?.post { snapToEdge(wm) }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * MU-27 FRAMING 取景框避位模式（v1.6 新增）
     *
     * 行为：复用 COMPACT 参数条样式，但通过 CameraFramingAvoidance 策略
     * 自动避开 OPPO/一加/Realme 相机取景区域（底部控件栏 + 顶部状态栏 + 取景框主体）。
     *
     * 用户拖动后，窗口会被 clamp 回安全区域，确保永不遮挡取景。
     * 不自动贴边（保留用户期望的位置但受限），用户手动收起后才悬浮球化。
     */
    private fun showFramingWindow(
        name: String,
        sections: ArrayList<PresetSection>,
        savedX: Int = -1,
        savedY: Int = -1,
        currentIndex: Int = 0,
        totalCount: Int = 1
    ) {
        try {
            val wm = windowManager ?: return

            val metrics = android.util.DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val windowWidth = screenWidth - dpToPx(32)  // 同 COMPACT：屏宽 - 32dp
            val windowHeight = dpToPx(120)

            // 1. 计算相机取景安全区域
            val safeArea = CameraFramingAvoidance.computeSafeArea(
                context = this,
                viewWidth = windowWidth, viewHeight = windowHeight,
                screenWidth = screenWidth, screenHeight = screenHeight
            )

            // 2. 坐标选择：优先用 savedX/savedY（用户上次拖动到的位置），没有则让 CameraFramingAvoidance 自动挑右下锚点
            val (anchorX, anchorY) = if (savedX >= 0 && savedY >= 0) {
                CameraFramingAvoidance.clampToSafeArea(
                    savedX, savedY, windowWidth, windowHeight, safeArea
                )
            } else {
                CameraFramingAvoidance.pickAnchor(safeArea, windowWidth, windowHeight)
            }

            params = WindowManager.LayoutParams(
                windowWidth, windowHeight,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = anchorX
                y = anchorY
            }

            // 3. 构建 COMPACT 样式的参数条视图（复用工厂）
            val rootLayout = compactFactory.createCompactView(
                name, sections, currentIndex, totalCount, windowWidth,
                onCollapse = { collapseToBubble(name, sections) },
                onSwitchPreset = { sendPresetSwitchBroadcast(it) },
                onClose = { stopSelf() }
            )
            mainContainer = compactFactory.mainContainerRef
            titleTextView = compactFactory.titleViewRef

            floatingView = rootLayout
            wm.addView(floatingView, params)

            // 4. 绑定拖动：拖动结束后 clampToSafeArea（不 snapToEdge），始终保持在安全区
            setupFramingDrag(wm, windowWidth, windowHeight, safeArea)

            Logger.i("FloatingWindow", "FRAMING 模式启动: 安全区(${safeArea.left},${safeArea.top}-${safeArea.right},${safeArea.bottom}), 初始位置(${params?.x},${params?.y})")

        } catch (e: Exception) {
            Logger.e("FloatingWindow", "showFramingWindow 启动失败", e)
            // MU-27 降级保护：FRAMING 任何异常降级为 COMPACT 模式
            showCompactWindow(name, sections, savedX, savedY, currentIndex, totalCount)
        }
    }

    /**
     * FRAMING 模式专用拖动逻辑 —— 拖动后 clampToSafeArea，避免用户拖到取景区。
     * 不调用 snapToEdge（保留用户位置选择但不越界）。
     */
    private fun setupFramingDrag(
        wm: WindowManager,
        viewWidth: Int,
        viewHeight: Int,
        safeArea: CameraFramingAvoidance.SafeArea
    ) {
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var isClick = false
            private val clickThreshold = 20f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params?.x ?: 0
                        initialY = params?.y ?: 0
                        touchX = event.rawX
                        touchY = event.rawY
                        isClick = true
                        isDragging = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - touchX
                        val dy = event.rawY - touchY
                        if (FloatingWindowGestureMath.isBeyondClickThreshold(dx, dy, clickThreshold)) {
                            isClick = false
                            isDragging = true
                        }
                        if (isDragging) {
                            val newX = initialX + dx.toInt()
                            val newY = FloatingWindowGestureMath.clampY(
                                initialY + dy.toInt(),
                                safeArea.bottom,
                                viewHeight
                            )
                            // 实时 clamp 到安全区域边界
                            val (clampedX, clampedY) = CameraFramingAvoidance.clampToSafeArea(
                                newX, newY, viewWidth, viewHeight, safeArea
                            )
                            params?.x = clampedX
                            params?.y = clampedY
                            floatingView?.let { view ->
                                params?.let { p ->
                                    runCatching { wm.updateViewLayout(view, p) }
                                }
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            // 再次 clamp（松手时最终安全确认）
                            val finalPos = CameraFramingAvoidance.clampToSafeArea(
                                params?.x ?: 0, params?.y ?: 0,
                                viewWidth, viewHeight, safeArea
                            )
                            params?.x = finalPos.first
                            params?.y = finalPos.second
                            floatingView?.let { view ->
                                runCatching { wm.updateViewLayout(view, params) }
                            }
                        }
                        isDragging = false
                        return !isClick
                    }
                }
                return isDragging && event.action == MotionEvent.ACTION_MOVE
            }
        })
    }

    /**
     * 显示新版紧凑悬浮窗（参数条样式）
     */
    private fun showCompactWindow(
        name: String,
        sections: ArrayList<PresetSection>,
        savedX: Int = -1,
        savedY: Int = -1,
        currentIndex: Int = 0,
        totalCount: Int = 1
    ) {
        try {
            val wm = windowManager ?: return

            // 获取屏幕宽度，计算悬浮窗宽度（屏幕宽度 - 32dp）
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getMetrics(metrics)
            val screenWidth = metrics.widthPixels
            val windowWidth = screenWidth - dpToPx(32)

            params = WindowManager.LayoutParams(
                windowWidth,
                dpToPx(120), // 高度增加到 120dp（标题栏40dp + 参数区80dp）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                // 默认位置：屏幕底部偏上，避开相机控制区
                x = if (savedX >= 0) savedX else dpToPx(16)
                y = if (savedY >= 0) savedY else metrics.heightPixels - dpToPx(300)
            }

            val rootLayout = compactFactory.createCompactView(
                name, sections, currentIndex, totalCount, windowWidth,
                onCollapse = { collapseToBubble(name, sections) },
                onSwitchPreset = { sendPresetSwitchBroadcast(it) },
                onClose = { stopSelf() }
            )
            // 构建产物引用回传（数据刷新逻辑依赖这两个字段）
            mainContainer = compactFactory.mainContainerRef
            titleTextView = compactFactory.titleViewRef

            floatingView = rootLayout
            wm.addView(floatingView, params)
            setupDrag(wm)

        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    // （紧凑视图构建族已迁移至 FloatingWindowCompactFactory，MU-11②）

    /**
     * 创建展开视图 - 高级美观设计
     */
    private fun createExpandedView(
        name: String,
        sections: ArrayList<PresetSection>,
        currentIndex: Int = 0,
        totalCount: Int = 1,
        onCollapse: () -> Unit
    ): FrameLayout {
        val windowWidth = getWindowWidth()

        return FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                windowWidth,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

            // 主容器 - 毛玻璃效果，固定宽度
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    windowWidth,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                background = createGlassmorphismBackground(context)
                setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(20))
            }
            mainContainer = container

            // 渐变标题栏（带切换按钮）
            val header = createGradientHeader(name, onCollapse, currentIndex, totalCount)
            container.addView(header)

            // 保存标题TextView引用
            titleTextView = (header as? LinearLayout)?.findViewWithTag<TextView>("title_text")

            // 内容容器（带tag，用于更新时查找）
            val contentContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                tag = "content_container"
            }

            // 添加内容
            contentContainer.addView(createContentArea(sections))

            container.addView(contentContainer)
            addView(container)
        }
    }

    /**
     * 创建内容区域（可复用） - 动态渲染
     */
    private fun createContentArea(sections: List<PresetSection>): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            sections.forEach { section ->
                // Section Title
                section.title?.let { title ->
                    addView(widgets.createSectionTitle(title))
                }

                // Section Items
                val items = section.items
                var i = 0
                while (i < items.size) {
                    val item = items[i]
                    if (item.span == 2) {
                        // Full width item (highlighted)
                        val icon = PresetParamMapper.getIconForLabel(item.label)
                        val localizedValue = PresetI18n.resolveValue(this@FloatingWindowService, item.value)
                        addView(widgets.createHighlightedParam(icon, item.label, localizedValue))
                        i++
                    } else {
                        // Half width item
                        val left = item
                        var right: PresetItem? = null
                        if (i + 1 < items.size && items[i+1].span == 1) {
                            right = items[i+1]
                            i++
                        }
                        
                        val leftIcon = PresetParamMapper.getIconForLabel(left.label)
                        val leftLocalizedValue = PresetI18n.resolveValue(this@FloatingWindowService, left.value)
                        val leftView = widgets.createSmallParamItem(leftIcon, left.label, leftLocalizedValue)
                        
                        val rightView = right?.let {
                            val rightIcon = PresetParamMapper.getIconForLabel(it.label)
                            val rightLocalizedValue = PresetI18n.resolveValue(this@FloatingWindowService, it.value)
                            widgets.createSmallParamItem(rightIcon, it.label, rightLocalizedValue)
                        }
                        
                        addView(widgets.createParamRow(leftView, rightView))
                        i++
                    }
                }
            }
        }
    }
    
    /**
     * 创建收起视图 - 圆形应用图标
     */
    private fun createCollapsedView(
        name: String,
        onExpand: () -> Unit
    ): FrameLayout {
        val size = dpToPx(56)

        return FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)

            // 外发光效果 - 品牌色外溢
            val glowView = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(size, size)
                background = widgets.createGlowBackground()
            }

            // 主按钮容器 - 圆形边框
            val button = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    dpToPx(48),
                    dpToPx(48)
                ).apply {
                    gravity = Gravity.CENTER
                }
                
                // 圆形黑色底色（防止图标透明部分看到背景）
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.BLACK)
                    setStroke(dpToPx(1), Color.parseColor("#1A000000")) // 极淡的描边增加立体感
                }

                // 应用图标
                val iconView = ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ).apply {
                        val margin = dpToPx(1) // 留一点边距，显示底色的圆边
                        setMargins(margin, margin, margin, margin)
                    }
                    setImageResource(R.mipmap.ic_launcher_round)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    // 禁用 ImageView 的触摸事件，让事件传递给父容器
                    isClickable = false
                    isFocusable = false
                }
                
                addView(iconView)
                // 禁用 button 容器的触摸事件拦截，让事件传递给最外层容器
                isClickable = false
                isFocusable = false
            }

            addView(glowView)
            addView(button)

            // 整个容器可点击
            setOnClickListener { onExpand() }
            // 确保外层容器可以接收触摸事件用于拖动
            isClickable = true
            isFocusable = true
        }
    }

    /**
     * 创建毛玻璃背景
     */
    private fun createGlassmorphismBackground(context: Context): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dpToPx(24).toFloat()
            setColor(getBackgroundColor(context))
            // 添加边框效果
            setStroke(dpToPx(1), Color.parseColor("#33FFFFFF"))
        }
    }

    /**
     * 创建渐变标题栏（带切换预设按钮）
     */
    private fun createGradientHeader(
        name: String,
        onCollapse: () -> Unit,
        currentIndex: Int = 0,
        totalCount: Int = 1
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(12))

            // 上一个预设按钮
            val prevBtn = widgets.createIconButton("◀") {
                sendPresetSwitchBroadcast("prev")
            }
            addView(prevBtn)
            addView(widgets.createSpacing(dpToPx(6)))

            // 预设名称 - 带渐变效果
            val titleView = TextView(context).apply {
                text = name
                textSize = if (name.length > 8) 15f else 18f
                paint.shader = LinearGradient(
                    0f, 0f, 200f, 0f,
                    primaryColor,
                    Color.parseColor("#FFB347"),
                    Shader.TileMode.CLAMP
                )
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
                tag = "title_text"
            }
            this@FloatingWindowService.titleTextView = titleView

            addView(titleView)

            // 下一个预设按钮
            addView(widgets.createSpacing(dpToPx(6)))
            val nextBtn = widgets.createIconButton("▶") {
                sendPresetSwitchBroadcast("next")
            }
            addView(nextBtn)

            addView(widgets.createSpacing(dpToPx(6)))

            // 收起按钮
            val collapseBtn = widgets.createIconButton("▼") { onCollapse() }
            addView(collapseBtn)

            addView(widgets.createSpacing(dpToPx(6)))

            // 关闭按钮
            val closeBtn = widgets.createIconButton("✕") { stopSelf() }
            addView(closeBtn)
        }
    }

    /**
     * 发送切换预设广播
     */
    private fun sendPresetSwitchBroadcast(direction: String) {
        val intent = Intent(ACTION_SWITCH_PRESET).apply {
            putExtra(EXTRA_SWITCH_DIRECTION, direction)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    // （共享参数控件族已迁移至 FloatingWindowWidgetFactory，MU-11②）

    private fun collapseToBubble(
        name: String,
        sections: ArrayList<PresetSection>
    ) {
        try {
            val currentX = params?.x ?: 50
            val currentY = params?.y ?: 300

            removeWindow()
            isExpanded = false

            val intent = Intent(this, FloatingWindowService::class.java).apply {
                putExtra(EXTRA_NAME, name)
                putParcelableArrayListExtra(EXTRA_SECTIONS, sections)
                putExtra(EXTRA_IS_EXPANDED, false)
                putExtra(EXTRA_POS_X, currentX)
                putExtra(EXTRA_POS_Y, currentY)
            }
            startService(intent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 悬浮窗宽度 - 固定 280dp
     * 无论横竖屏都使用相同的小宽度，确保不会铺满屏幕
     */
    private fun getWindowWidth(): Int {
        return dpToPx(280)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupDrag(wm: WindowManager) {
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var isClick = false
            private val clickThreshold = 20f
            private var isDragging = false  // ✅ 新增：跟踪是否正在拖动

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params?.x ?: 0
                        initialY = params?.y ?: 0
                        touchX = event.rawX
                        touchY = event.rawY
                        isClick = true
                        isDragging = false  // ✅ 重置拖动状态
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - touchX
                        val dy = event.rawY - touchY
                        if (FloatingWindowGestureMath.isBeyondClickThreshold(dx, dy, clickThreshold)) {
                            isClick = false
                            isDragging = true  // ✅ 标记为拖动中
                        }

                        // 只有在拖动时才更新位置
                        if (isDragging) {
                            val metrics = DisplayMetrics()
                            wm.defaultDisplay.getMetrics(metrics)

                            params?.x = initialX + dx.toInt()

                            // 垂直方向限制，防止超出屏幕
                            val newY = initialY + dy.toInt()
                            val viewHeight = floatingView?.height ?: dpToPx(56)
                            params?.y = FloatingWindowGestureMath.clampY(newY, metrics.heightPixels, viewHeight)

                            floatingView?.let { view ->
                                params?.let { p ->
                                    try {
                                        wm.updateViewLayout(view, p)
                                    } catch (e: Exception) {
                                        Logger.e("FloatingWindowService", "拖动更新布局失败", e)
                                    }
                                }
                            }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            // ✅ 只在拖动操作时才贴边，避免与点击事件冲突
                            snapToEdge(wm)
                        }
                        isDragging = false  // ✅ 重置拖动状态
                        // ✅ 如果是点击操作，返回 false 让 onClick 处理
                        // ✅ 如果是拖动操作，返回 true 消费事件，防止触发 onClick
                        return !isClick
                    }
                }
                // ✅ MOVE 事件：只在拖动时返回 true 消费事件
                return isDragging && event.action == MotionEvent.ACTION_MOVE
            }
        })
    }

    /**
     * 将悬浮窗平滑移动至屏幕边缘
     */
    private fun snapToEdge(wm: WindowManager) {
        val view = floatingView ?: return
        val p = params ?: return

        // ✅ 检查视图是否已经有效测量
        if (view.width <= 0 || view.height <= 0) {
            Logger.w("FloatingWindowService", "snapToEdge: 视图尚未完成测量，跳过贴边")
            return
        }

        // ✅ 取消之前的动画（防止重复执行）
        edgeAnimator?.cancel()
        edgeAnimator = null

        try {
            val metrics = DisplayMetrics()
            wm.defaultDisplay.getMetrics(metrics)
            val screenWidth = metrics.widthPixels
            val viewWidth = view.width

        // 计算目标位置：左边(0)或右边(screenWidth - viewWidth)
        // 如果是收起状态，可以进一步实现“半收纳”效果，即只露出一半图标
        val targetX = if (p.x + viewWidth / 2 < screenWidth / 2) {
            // 左侧收纳
            if (!isExpanded) {
                // 收起状态：半收纳，限制最小值防止崩溃
                (-viewWidth / 2).coerceAtLeast(-dpToPx(28))
            } else {
                0
            }
        } else {
            // 右侧收纳
            if (!isExpanded) {
                // 收起状态：半收纳，限制最大值防止崩溃
                (screenWidth - viewWidth / 2).coerceAtMost(screenWidth - dpToPx(28))
            } else {
                (screenWidth - viewWidth).coerceAtLeast(0)
            }
        }

        // 使用动画平滑移动
            // ✅ 增加完整的生命周期管理和异常保护
            val animator = ValueAnimator.ofInt(p.x, targetX)
            animator.duration = 300
            animator.interpolator = DecelerateInterpolator()

            animator.addUpdateListener { animation ->
                try {
                    // ✅ 多重检查，确保安全更新布局
                    if (floatingView !== view || params !== p) {
                        // View 或 Params 已被替换，取消动画
                        animator.cancel()
                        return@addUpdateListener
                    }

                    // ✅ 检查 View 的父容器是否还存在（即是否仍附加在 WindowManager 上）
                    if (view.parent == null) {
                        animator.cancel()
                        return@addUpdateListener
                    }

                    p.x = animation.animatedValue as Int
                    wm.updateViewLayout(view, p)
                } catch (e: IllegalArgumentException) {
                    // ✅ View 未附加到窗口管理器
                    Logger.e("FloatingWindowService", "snapToEdge 动画更新失败: View not attached to window manager", e)
                    animator.cancel()
                } catch (e: IllegalStateException) {
                    // ✅ View 已被移除或状态异常
                    Logger.e("FloatingWindowService", "snapToEdge 动画更新失败: View removed or invalid state", e)
                    animator.cancel()
                } catch (e: Exception) {
                    // ✅ 其他未知异常
                    Logger.e("FloatingWindowService", "snapToEdge 动画更新异常", e)
                    animator.cancel()
                }
            }

            // ✅ 动画结束时清理引用，防止内存泄漏
            animator.addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (edgeAnimator === animator) {
                        edgeAnimator = null
                    }
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    if (edgeAnimator === animator) {
                        edgeAnimator = null
                    }
                }
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })

            edgeAnimator = animator
            animator.start()
        } catch (e: Exception) {
            Logger.e("FloatingWindowService", "snapToEdge 执行失败", e)
        }
    }

    private fun removeWindow() {
        try {
            floatingView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        floatingView = null
    }
}
