package com.silas.omaster.data.model

/**
 * LUT资源数据模型
 * 包含元数据和二进制下载/校验信息
 */
data class LUTResource(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val downloadUrl: String,
    val downloadCount: Int,
    val rating: Double,
    // ========== 新增：二进制下载/校验字段 ==========
    val fileSize: Long? = null,              // 文件大小（字节）
    val checksum: String? = null,            // SHA-256 校验和
    val cubeSize: Int? = 33,                 // LUT 立方体尺寸（默认 33x33x33）
    val format: String = "cube",             // 格式：cube / 3dl / csp
    val isFree: Boolean = false,             // 是否免费
    val version: Int = 1,                    // 版本号
    val author: String? = null,              // 作者
    val previewUrl: String? = null,          // 效果预览图 URL
    val createdAt: Long = 0,                 // 创建时间戳
    val updatedAt: Long = 0                  // 更新时间戳
) {
    /**
     * 获取本地缓存路径
     */
    fun getLocalPath(context: android.content.Context): java.io.File {
        return java.io.File(context.filesDir, "luts/${id}_${version}.cube")
    }
    
    /**
     * 检查是否已下载到本地
     */
    fun isDownloaded(context: android.content.Context): Boolean {
        val localFile = getLocalPath(context)
        return localFile.exists() && localFile.length() > 0
    }
    
    /**
     * 验证本地文件完整性
     */
    fun verifyIntegrity(context: android.content.Context): Boolean {
        val localFile = getLocalPath(context)
        if (!localFile.exists()) return false
        
        // 校验文件大小
        if (fileSize != null && localFile.length() != fileSize) {
            return false
        }
        
        // 校验 SHA-256（如果提供）
        if (checksum != null) {
            val actualChecksum = calculateSHA256(localFile)
            return actualChecksum == checksum
        }
        
        return true
    }
    
    /**
     * 计算 SHA-256 校验和
     */
    private fun calculateSHA256(file: java.io.File): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            java.io.FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
