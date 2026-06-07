package com.silas.omaster.util

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.silas.omaster.model.WatermarkTemplateList
import com.silas.omaster.model.WatermarkTemplateData
import com.silas.omaster.model.WatermarkTemplateUiData
import java.io.InputStreamReader

/**
 * 水印模板JSON加载工具类
 * 从assets目录加载watermarks.json文件
 */
object WatermarkJsonLoader {

    private val gson = Gson()
    private const val DEFAULT_FILE_NAME = "watermarks.json"
    private const val TAG = "WatermarkJsonLoader"

    /**
     * 内存缓存
     */
    private var cachedTemplates: List<WatermarkTemplateData>? = null

    /**
     * 从assets加载水印模板列表
     * @param context 应用上下文
     * @param fileName JSON文件名，默认为 "watermarks.json"
     * @return 解析后的水印模板列表，如果加载失败则返回空列表
     */
    fun loadTemplates(context: Context, fileName: String = DEFAULT_FILE_NAME): List<WatermarkTemplateData> {
        // 如果已有缓存，直接返回缓存
        cachedTemplates?.let {
            Log.d(TAG, "Returning cached templates, count: ${it.size}")
            return it
        }

        return try {
            context.assets.open(fileName).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val listType = object : TypeToken<WatermarkTemplateList>() {}.type
                    val templateList: WatermarkTemplateList? = gson.fromJson(reader, listType)
                    val templates = templateList?.templates ?: emptyList()
                    cachedTemplates = templates
                    Log.d(TAG, "Loaded ${templates.size} watermark templates from assets")
                    templates
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load watermark templates from assets", e)
            emptyList()
        }
    }

    /**
     * 加载并转换为UI数据
     * @param context 应用上下文
     * @return UI展示用的水印模板列表
     */
    fun loadTemplatesForUi(context: Context): List<WatermarkTemplateUiData> {
        return loadTemplates(context).map { WatermarkTemplateUiData.fromData(it) }
    }

    /**
     * 按分类获取模板
     * @param context 应用上下文
     * @param category 分类名称
     * @return 指定分类的模板列表
     */
    fun getTemplatesByCategory(context: Context, category: String): List<WatermarkTemplateUiData> {
        return loadTemplatesForUi(context).filter { 
            it.category.name.equals(category, ignoreCase = true) 
        }
    }

    /**
     * 根据ID获取模板
     * @param context 应用上下文
     * @param id 模板ID
     * @return 找到的模板，如果不存在返回null
     */
    fun getTemplateById(context: Context, id: String): WatermarkTemplateUiData? {
        return loadTemplatesForUi(context).find { it.id == id }
    }

    /**
     * 获取所有分类
     * @param context 应用上下文
     * @return 分类名称列表
     */
    fun getAllCategories(context: Context): List<String> {
        return loadTemplatesForUi(context)
            .map { it.category.displayName }
            .distinct()
    }

    /**
     * 清除缓存
     * 在需要重新加载数据时调用
     */
    fun invalidateCache() {
        cachedTemplates = null
        Log.d(TAG, "Cache invalidated")
    }

    /**
     * 获取模板总数
     * @param context 应用上下文
     * @return 模板数量
     */
    fun getTemplateCount(context: Context): Int {
        return loadTemplates(context).size
    }

    /**
     * 检查是否有模板数据
     * @param context 应用上下文
     * @return 是否有可用模板
     */
    fun hasTemplates(context: Context): Boolean {
        return loadTemplates(context).isNotEmpty()
    }
}
