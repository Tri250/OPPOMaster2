package com.silas.omaster.data.model

/**
 * LUT资源数据模型
 * 对齐 Web 端 LUTResource 接口
 */
data class LUTResource(
    val id: String,
    val name: String,
    val nameEn: String,
    val description: String,
    val category: String,
    val tags: List<String> = emptyList(),
    val downloadUrl: String,
    val previewImage: String = "",
    val format: String = "cube",
    val size: String = "33",
    val fileSize: Int = 0,
    val author: String = "OMaster Team",
    val authorUrl: String = "",
    val downloads: Int = 0,
    val likes: Int = 0,
    val rating: Float = 4.5f,
    val isFree: Boolean = true,
    val isHot: Boolean = false,
    val isNew: Boolean = false,
    val suitableFor: List<String> = emptyList(),
    val createdAt: String = "2026-01-01"
)
