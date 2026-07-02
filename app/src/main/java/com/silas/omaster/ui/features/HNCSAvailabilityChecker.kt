package com.silas.omaster.ui.features

import android.content.Context
import android.widget.Toast
import com.silas.omaster.model.MasterPreset

/**
 * HNCS 可用性检查器
 * 用于判断当前预设/场景是否支持 HNCS 3.0 渲染管线
 */
object HNCSAvailabilityChecker {

    // 支持 HNCS 的品牌列表
    private val HNCS_SUPPORTED_BRANDS = setOf("hasselblad", "哈苏")

    /**
     * 检查预设是否支持 HNCS
     */
    fun isHncsAvailable(preset: MasterPreset?): Boolean {
        if (preset == null) return false
        return preset.isHncs || HNCS_SUPPORTED_BRANDS.contains(preset.brand?.lowercase())
    }

    /**
     * 检查品牌是否支持 HNCS
     */
    fun isHncsAvailableForBrand(brand: String?): Boolean {
        return HNCS_SUPPORTED_BRANDS.contains(brand?.lowercase())
    }

    /**
     * 检查并提示：如果不支持 HNCS，弹出 Toast 提示
     * @return true 表示支持 HNCS，false 表示不支持并已提示
     */
    fun checkAndWarn(context: Context, preset: MasterPreset?): Boolean {
        return if (isHncsAvailable(preset)) {
            true
        } else {
            Toast.makeText(
                context,
                "仅哈苏预设可用 HNCS 3.0",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    /**
     * 检查并提示：如果不支持 HNCS，弹出 Toast 提示
     * @return true 表示支持 HNCS，false 表示不支持并已提示
     */
    fun checkAndWarn(context: Context, brand: String?): Boolean {
        return if (isHncsAvailableForBrand(brand)) {
            true
        } else {
            Toast.makeText(
                context,
                "仅哈苏预设可用 HNCS 3.0",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }
}
