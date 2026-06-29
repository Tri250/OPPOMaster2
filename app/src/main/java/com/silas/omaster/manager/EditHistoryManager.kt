package com.silas.omaster.manager

import android.graphics.Bitmap
import com.silas.omaster.renderer.RenderParameters
import java.util.UUID

/**
 * 编辑步骤快照
 * 记录某一时刻的全部参数状态，支持非破坏性回退
 */
data class EditSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val params: RenderParameters,
    val thumbnail: Bitmap? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 编辑历史管理器
 * 替代简单 undo/redo Stack，支持：
 * - 命名步骤快照
 * - 回退到任意历史步骤
 * - 步骤分支（从任意步骤创建新分支）
 * - 步骤列表浏览
 */
class EditHistoryManager {

    private val snapshots = mutableListOf<EditSnapshot>()
    private var currentIndex: Int = -1

    /** 最大保存步数 */
    var maxSteps: Int = 50
        set(value) {
            field = value.coerceIn(10, 200)
            trimExcess()
        }

    /** 当前是否可以撤销 */
    val canUndo: Boolean
        get() = currentIndex > 0

    /** 当前是否可以重做 */
    val canRedo: Boolean
        get() = currentIndex < snapshots.size - 1

    /** 当前步骤索引 */
    val index: Int
        get() = currentIndex

    /** 已保存的步骤数量 */
    val size: Int
        get() = snapshots.size

    /** 获取所有快照列表（只读副本） */
    fun getSnapshots(): List<EditSnapshot> = snapshots.toList()

    /** 获取当前快照 */
    fun getCurrentSnapshot(): EditSnapshot? = snapshots.getOrNull(currentIndex)

    /**
     * 记录一个新步骤
     * @param name 步骤名称（如"调整曝光"、"应用曲线"）
     * @param params 当前参数快照
     * @param thumbnail 可选缩略图
     */
    fun record(name: String, params: RenderParameters, thumbnail: Bitmap? = null) {
        // 如果在历史中间做了新操作，丢弃当前索引之后的所有步骤
        if (currentIndex < snapshots.size - 1) {
            val removeFrom = currentIndex + 1
            for (i in removeFrom until snapshots.size) {
                snapshots[i].thumbnail?.recycle()
            }
            snapshots.subList(removeFrom, snapshots.size).clear()
        }

        // 创建深拷贝避免后续修改影响快照
        val snapshot = EditSnapshot(
            name = name,
            params = params.copy(),
            thumbnail = thumbnail
        )

        snapshots.add(snapshot)
        currentIndex = snapshots.size - 1

        trimExcess()
    }

    /**
     * 撤销一步
     * @return 回退后的参数，如果无法撤销返回 null
     */
    fun undo(): RenderParameters? {
        if (!canUndo) return null
        currentIndex--
        return snapshots[currentIndex].params.copy()
    }

    /**
     * 重走一步
     * @return 前进后的参数，如果无法重做返回 null
     */
    fun redo(): RenderParameters? {
        if (!canRedo) return null
        currentIndex++
        return snapshots[currentIndex].params.copy()
    }

    /**
     * 回退到指定步骤
     * @param snapshotId 目标步骤ID
     * @return 目标步骤的参数，未找到返回 null
     */
    fun jumpTo(snapshotId: String): RenderParameters? {
        val targetIndex = snapshots.indexOfFirst { it.id == snapshotId }
        if (targetIndex == -1) return null
        currentIndex = targetIndex
        return snapshots[currentIndex].params.copy()
    }

    /**
     * 回退到指定索引
     * @param targetIndex 目标索引
     * @return 目标步骤的参数，索引无效返回 null
     */
    fun jumpToIndex(targetIndex: Int): RenderParameters? {
        if (targetIndex !in 0 until snapshots.size) return null
        currentIndex = targetIndex
        return snapshots[currentIndex].params.copy()
    }

    /**
     * 从当前步骤创建分支（保留当前步骤之前的所有历史，清空之后）
     * 用于尝试不同调整方向时保存当前状态
     * @param branchName 分支名称
     */
    fun createBranch(branchName: String): String {
        val branchSnapshot = EditSnapshot(
            name = "[分支] $branchName",
            params = snapshots.getOrNull(currentIndex)?.params?.copy() ?: RenderParameters(),
            thumbnail = snapshots.getOrNull(currentIndex)?.thumbnail
        )
        // 在当前位置插入分支标记，并截断后续历史
        if (currentIndex < snapshots.size - 1) {
            snapshots.subList(currentIndex + 1, snapshots.size).clear()
        }
        snapshots.add(branchSnapshot)
        currentIndex = snapshots.size - 1
        return branchSnapshot.id
    }

    /**
     * 重置全部历史
     */
    fun clear() {
        snapshots.forEach { it.thumbnail?.recycle() }
        snapshots.clear()
        currentIndex = -1
    }

    /**
     * 生成自动步骤名称，基于参数变化
     */
    fun autoNameForParamsChange(key: String): String {
        return when (key) {
            "exposure" -> "调整曝光"
            "brightness" -> "调整亮度"
            "contrast" -> "调整对比度"
            "saturation" -> "调整饱和度"
            "vibrance" -> "调整鲜艳度"
            "warmth" -> "调整色温"
            "tint" -> "调整色调"
            "highlights" -> "调整高光"
            "shadows" -> "调整阴影"
            "whites" -> "调整白色色阶"
            "blacks" -> "调整黑色色阶"
            "texture" -> "调整纹理"
            "clarity" -> "调整清晰度"
            "sharpness" -> "调整锐度"
            "dehaze" -> "调整去霾"
            "denoise" -> "调整降噪"
            "grain" -> "调整颗粒"
            "fade" -> "调整褪色"
            "skinSmooth" -> "调整肤色平滑"
            "vignette" -> "调整暗角"
            "style" -> "应用风格"
            "hsl" -> "调整HSL"
            "curve" -> "调整曲线"
            "local" -> "局部调整"
            "crop" -> "裁剪旋转"
            "lut" -> "应用3D LUT"
            else -> "调整参数"
        }
    }

    private fun trimExcess() {
        while (snapshots.size > maxSteps) {
            snapshots.removeAt(0).thumbnail?.recycle()
            currentIndex = (currentIndex - 1).coerceAtLeast(-1)
        }
    }
}
