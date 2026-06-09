package com.silas.omaster.data.remote

import com.silas.omaster.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * LUT远程数据源
 * 从API/CDN获取数据
 */
class LUTRemoteDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val baseUrl: String = "https://cdn.hasselblad.com/api/v1",
    private val downloadDir: File
) {
    /**
     * 获取LUT列表
     */
    suspend fun fetchLUTList(
        category: String,
        query: String,
        sortBy: String
    ): List<MasterLUT> {
        // 实际项目中应从API获取
        // 这里使用本地数据作为示例
        return MasterLUTRepository.ALL_LUTS.filter { lut ->
            (category == "all" || lut.category.key == category) &&
            (query.isEmpty() || lut.name.contains(query, ignoreCase = true) ||
             lut.nameEn.contains(query, ignoreCase = true) ||
             lut.tags.any { it.contains(query, ignoreCase = true) })
        }.sortedWith(getComparator(sortBy))
    }

    /**
     * 获取单个LUT详情
     */
    suspend fun fetchLUTMeta(id: String): MasterLUT? {
        return MasterLUTRepository.ALL_LUTS.find { it.id == id }
    }

    /**
     * 获取热门LUT
     */
    suspend fun fetchHotLUTs(): List<MasterLUT> {
        return MasterLUTRepository.getHotLUTs()
    }

    /**
     * 获取新品LUT
     */
    suspend fun fetchNewLUTs(): List<MasterLUT> {
        return MasterLUTRepository.getNewLUTs()
    }

    /**
     * 获取精选LUT
     */
    suspend fun fetchFeaturedLUTs(): List<MasterLUT> {
        return MasterLUTRepository.getFeaturedLUTs()
    }

    /**
     * 搜索LUT
     */
    suspend fun searchLUTs(query: String): List<MasterLUT> {
        return MasterLUTRepository.search(query)
    }

    /**
     * 下载LUT文件（带进度）
     */
    fun downloadLUTFile(
        downloadUrl: String,
        totalBytes: Long
    ): Flow<DownloadProgress> = flow {
        val fileName = downloadUrl.substringAfterLast("/")
        val targetFile = File(downloadDir, fileName)

        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(DownloadProgress.Error("", "HTTP ${response.code}"))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(DownloadProgress.Error("", "响应体为空"))
                return@flow
            }

            val contentLength = body.contentLength()
            val actualTotal = if (contentLength > 0) contentLength else totalBytes

            FileOutputStream(targetFile).use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesDownloaded = 0L
                    var lastProgress = 0

                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break

                        output.write(buffer, 0, read)
                        bytesDownloaded += read

                        val progress = if (actualTotal > 0) {
                            ((bytesDownloaded * 100) / actualTotal).toInt()
                        } else {
                            0
                        }

                        // 每5%更新一次进度
                        if (progress - lastProgress >= 5 || progress == 100) {
                            lastProgress = progress
                            emit(DownloadProgress.Downloading(
                                "",
                                progress,
                                bytesDownloaded,
                                actualTotal
                            ))
                        }
                    }
                }
            }

            emit(DownloadProgress.Completed("", targetFile.absolutePath))

        } catch (e: Exception) {
            // 清理部分下载的文件
            if (targetFile.exists()) {
                targetFile.delete()
            }
            emit(DownloadProgress.Error("", e.message ?: "下载失败"))
        }
    }

    /**
     * 提交评分
     */
    suspend fun submitRating(lutId: String, rating: Float) {
        // 实际项目中应POST到API
        // 这里仅做示例
    }

    /**
     * 获取排序比较器
     */
    private fun getComparator(sortBy: String): Comparator<MasterLUT> {
        return when (sortBy) {
            "downloads" -> compareByDescending { it.downloads }
            "rating" -> compareByDescending { it.rating }
            "newest" -> compareByDescending { it.createdAt }
            "name" -> compareBy { it.name }
            else -> compareByDescending { it.downloads }
        }
    }
}