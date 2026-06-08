package com.silas.omaster.data.model

/**
 * LUT资源数据模型
 */
data class LUTResource(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val downloadUrl: String,
    val downloadCount: Int,
    val rating: Double
)
