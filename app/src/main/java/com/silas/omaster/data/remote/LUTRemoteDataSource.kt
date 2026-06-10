package com.silas.omaster.data.remote

import com.silas.omaster.data.model.*
import com.silas.omaster.data.repository.DownloadProgress
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

/**
 * LUT 远程数据源
 * 负责从 API/CDN 获取 LUT 数据和下载文件
 */
class LUTRemoteDataSource(
    private val client: HttpClient = HttpClient(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    // API 基础 URL
    companion object {
        const val API_BASE_URL = "https://api.omaster.app/v1"
        const val CDN_BASE_URL = "https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main"
    }

    /**
     * 获取 LUT 列表
     */
    suspend fun fetchLUTList(
        category: String,
        query: String,
        sortBy: String
    ): List<MasterLUT> = withContext(Dispatchers.IO) {
        try {
            val response: String = client.get("$API_BASE_URL/luts") {
                parameter("category", if (category == "all") null else category)
                parameter("q", if (query.isEmpty()) null else query)
                parameter("sort", sortBy)
            }.body()
            
            json.decodeFromString<List<MasterLUT>>(response)
        } catch (e: Exception) {
            // 降级到 CDN 静态数据
            fetchFromCDN()
        }
    }

    /**
     * 从 CDN 获取静态 LUT 数据
     */
    private suspend fun fetchFromCDN(): List<MasterLUT> = withContext(Dispatchers.IO) {
        try {
            val response: String = client.get("$CDN_BASE_URL/data/luts.json").body()
            json.decodeFromString<List<MasterLUT>>(response)
        } catch (_: Exception) {
            // 返回内置默认数据
            getDefaultLUTs()
        }
    }

    /**
     * 获取 LUT 元数据
     */
    suspend fun fetchLUTMeta(id: String): MasterLUT? = withContext(Dispatchers.IO) {
        try {
            val response: String = client.get("$API_BASE_URL/luts/$id").body()
            json.decodeFromString<MasterLUT>(response)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 下载 LUT 文件（带进度）
     */
    fun downloadLUTFile(url: String): Flow<DownloadProgress> = flow {
        val tempFile = File.createTempFile("lut_download_", ".tmp")
        
        try {
            val response = client.get(url)
            val contentLength = response.headers[HttpHeaders.ContentLength]?.toLong() ?: 0L
            
            var bytesDownloaded = 0L
            
            val channel: ByteReadChannel = response.bodyAsChannel()
            val buffer = ByteArray(8192)
            
            while (!channel.isClosedForRead) {
                val bytesRead = channel.readAvailable(buffer)
                if (bytesRead <= 0) break
                
                tempFile.appendBytes(buffer.copyOf(bytesRead))
                bytesDownloaded += bytesRead
                
                if (contentLength > 0) {
                    emit(DownloadProgress.Downloading(
                        lutId = "",
                        progress = bytesDownloaded.toFloat() / contentLength,
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = contentLength
                    ))
                }
            }
            
            emit(DownloadProgress.Completed("", tempFile.absolutePath))
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 提交评分
     */
    suspend fun submitRating(lutId: String, rating: Float): Boolean = withContext(Dispatchers.IO) {
        try {
            client.post("$API_BASE_URL/luts/$lutId/rating") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("rating" to rating))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 获取默认 LUT 数据（内置）
     */
    private fun getDefaultLUTs(): List<MasterLUT> = listOf(
        // 胶片经典
        MasterLUT(
            id = "kodak-portra-400",
            name = "柯达Portra 400",
            nameEn = "Kodak Portra 400",
            description = "经典人像胶片色彩，温暖肤色还原",
            category = LUTCategory.FILM,
            tags = listOf("人像", "温暖", "胶片"),
            suitableFor = listOf("人像", "婚礼", "户外"),
            format = LUTFormat.CUBE,
            size = LUTSize.SIZE_33,
            fileSize = 12000,
            coverImage = "https://images.unsplash.com/photo-1507003215708-59d24f5f3e0a?w=400",
            downloadUrl = "$CDN_BASE_URL/luts/film/kodak_portra_400_33.cube",
            author = "OMaster Team",
            source = LUTSource.OMASTER,
            isFree = true,
            isHot = true,
            downloads = 125600,
            likes = 8920,
            rating = 4.9f,
            createdAt = "2026-06-15"
        ),
        MasterLUT(
            id = "fuji-400h",
            name = "富士400H",
            nameEn = "Fuji 400H",
            description = "日系清新胶片风格",
            category = LUTCategory.FILM,
            tags = listOf("日系", "清新", "柔和"),
            suitableFor = listOf("Vlog", "人像", "旅行"),
            format = LUTFormat.CUBE,
            size = LUTSize.SIZE_33,
            fileSize = 11000,
            coverImage = "https://images.unsplash.com/photo-1493863641943-5b68c26e91bb?w=400",
            downloadUrl = "$CDN_BASE_URL/luts/film/fuji_400h_33.cube",
            author = "OMaster Team",
            source = LUTSource.OMASTER,
            isFree = true,
            isHot = true,
            downloads = 98500,
            likes = 7650,
            rating = 4.8f,
            createdAt = "2026-07-20"
        ),
        // 哈苏大师
        MasterLUT(
            id = "hasselblad-hncs-natural",
            name = "HNCS自然色彩",
            nameEn = "HNCS Natural Color",
            description = "哈苏自然色彩解决方案，HNCS认证",
            category = LUTCategory.HASSELBLAD,
            tags = listOf("哈苏", "HNCS", "专业"),
            suitableFor = listOf("专业", "风景", "人像"),
            format = LUTFormat.CUBE,
            size = LUTSize.SIZE_64,
            fileSize = 18000,
            coverImage = "https://images.unsplash.com/photo-150890-5a5a5a5a5a5a?w=400",
            downloadUrl = "$CDN_BASE_URL/luts/hasselblad/hncs_natural_33.cube",
            author = "Hasselblad",
            source = LUTSource.HASSELBLAD,
            isHncsCertified = true,
            hasselbladCollection = "大师赛2026",
            isFree = true,
            isHot = true,
            isNew = true,
            isFeatured = true,
            featuredReason = "哈苏官方HNCS认证",
            downloads = 67800,
            likes = 5900,
            rating = 4.9f,
            createdAt = "2026-03-01"
        )
    )
}
