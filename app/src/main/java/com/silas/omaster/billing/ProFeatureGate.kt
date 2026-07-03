package com.silas.omaster.billing

/**
 * Pro 功能枚举
 *
 * 定义所有需要付费解锁的功能模块。
 * 免费用户可使用：基础相机、基础预设、基础编辑。
 * Pro 用户可使用全部功能。
 */
enum class ProFeature(
    val displayName: String,
    val description: String
) {
    /** AI 高级调色（多维度参数微调） */
    AI_FINE_TUNE_ADVANCED(
        displayName = "AI 高级调色",
        description = "多维度 AI 参数微调，智能优化每一帧"
    ),

    /** 批量处理 */
    BATCH_PROCESSING(
        displayName = "批量处理",
        description = "一键批量应用预设，高效出片"
    ),

    /** 云同步 */
    CLOUD_SYNC(
        displayName = "云同步",
        description = "预设与参数跨设备云同步"
    ),

    /** 夜景模式 */
    NIGHT_MODE(
        displayName = "夜景模式",
        description = "多帧降噪夜景合成，手持长曝光"
    ),

    /** 光绘模式 */
    LIGHT_PAINTING(
        displayName = "光绘模式",
        description = "创意光绘叠加，长曝光光轨艺术"
    ),

    /** 人像散景 */
    PORTRAIT_BOKEH(
        displayName = "人像散景",
        description = "AI 人像虚化，专业散景效果"
    ),

    /** 风格 LUT 生成器 */
    STYLE_LUT_GENERATOR(
        displayName = "风格 LUT 生成器",
        description = "AI 色彩迁移，自动生成 .cube LUT 文件"
    ),

    /** 预设社区 */
    PRESET_COMMUNITY(
        displayName = "预设社区",
        description = "分享与下载全球摄影师的调色预设"
    ),

    /** 哈苏高级模式 */
    HASSELBLAD_ADVANCED(
        displayName = "哈苏高级模式",
        description = "完整哈苏色彩科学引擎与大师参数"
    );

    companion object {
        /** 所有 Pro 功能列表 */
        val allFeatures = entries
    }
}

/**
 * Pro 功能门控
 *
 * 检查指定功能是否对当前用户可用。
 * 依赖 [BillingManager] 提供的订阅状态。
 */
object ProFeatureGate {

    /**
     * 检查指定功能是否可用
     *
     * @param feature 要检查的 Pro 功能
     * @return 如果用户是 Pro 或 Lifetime，返回 true；否则返回 false
     */
    fun isFeatureAvailable(feature: ProFeature): Boolean {
        val state = BillingManager.instance?.subscriptionState?.value ?: SubscriptionState.FREE
        return state.isActivePro
    }

    /**
     * 获取功能不可用时的提示消息
     *
     * @param feature Pro 功能
     * @return 升级提示文案
     */
    fun getFeatureMessage(feature: ProFeature): String {
        return "${feature.displayName}为 Pro 专属功能，${feature.description}\n升级 Pro 即可解锁全部功能。"
    }

    /**
     * 获取所有被锁定的功能列表（对免费用户）
     */
    fun getLockedFeatures(): List<ProFeature> {
        return if (isFeatureAvailable(ProFeature.AI_FINE_TUNE_ADVANCED)) {
            emptyList()
        } else {
            ProFeature.allFeatures
        }
    }
}
