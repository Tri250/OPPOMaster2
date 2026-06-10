package com.silas.omaster.data.repository

import com.silas.omaster.data.local.LUTLocalDataSource
import com.silas.omaster.data.model.*
import com.silas.omaster.data.remote.LUTRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

/**
 * LUT 数据仓库
 * 实现缓存优先 + 网络刷新策略
 */
class LUTRepository(
    private val remoteDataSource: LUTRemoteDataSource,
    private val localDataSource: LUTLocalDataSource
) {
    /**
     * 获取 LUT 列表（缓存优先 + 网络刷新）
     */
    fun getLUTs(
        category: LUTCategory = LUTCategory.ALL,
        query: String = "",
        sortBy: LUTSortBy = LUTSortBy.DOWNLOADS
    ): Flow<Resource<List<MasterLUT>>> = flow {
        // 1. 先发射缓存数据
        val cached = localDataSource.getLUTs(category, query, sortBy)
        emit(Resource.Loading(cached))

        // 2. 网络刷新
        try {
            val remote = remoteDataSource.fetchLUTList(category.key, query, sortBy.key)
            localDataSource.cacheLUTs(remote)
            val merged = localDataSource.getLUTs(category, query, sortBy)
            emit(Resource.Success(merged))
        } catch (e: Exception) {
            if (cached.isNotEmpty()) {
                emit(Resource.Success(cached)) // 离线可用缓存
            } else {
                emit(Resource.Error(e.message ?: "加载失败"))
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 根据 ID 获取单个 LUT
     */
    fun getLUTById(id: String): Flow<MasterLUT?> = localDataSource.getLUTById(id)

    /**
     * 获取热门 LUT
     */
    fun getHotLUTs(): Flow<List<MasterLUT>> = localDataSource.getHotLUTs()

    /**
     * 获取新品 LUT
     */
    fun getNewLUTs(): Flow<List<MasterLUT>> = localDataSource.getNewLUTs()

    /**
     * 下载 LUT 文件（带进度）
     */
    fun downloadLUT(lut: MasterLUT): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Starting(lut.id))
        try {
            // 检查本地是否已有
            if (localDataSource.hasLUTFile(lut.id)) {
                emit(DownloadProgress.Completed(lut.id, localDataSource.getLUTFilePath(lut.id)))
                return@flow
            }
            // 远程下载
            remoteDataSource.downloadLUTFile(lut.downloadUrl).collect { progress ->
                when (progress) {
                    is DownloadProgress.Downloading -> {
                        emit(progress)
                    }
                    is DownloadProgress.Completed -> {
                        // 保存到本地
                        localDataSource.saveLUTFile(lut.id, progress.filePath)
                        // 更新下载记录
                        localDataSource.recordDownload(lut.id)
                        emit(progress)
                    }
                    else -> emit(progress)
                }
            }
        } catch (e: Exception) {
            emit(DownloadProgress.Error(lut.id, e.message ?: "下载失败"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 获取已下载的 LUT 列表
     */
    fun getDownloadedLUTs(): Flow<List<MasterLUT>> = localDataSource.getDownloadedLUTs()

    /**
     * 删除已下载的 LUT
     */
    suspend fun deleteDownloadedLUT(id: String) = localDataSource.deleteDownloadedLUT(id)

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(lutId: String) = localDataSource.toggleFavorite(lutId)

    /**
     * 获取收藏列表
     */
    fun getFavorites(): Flow<List<MasterLUT>> = localDataSource.getFavorites()

    /**
     * 提交评分
     */
    suspend fun submitRating(lutId: String, rating: Float) {
        localDataSource.updateRating(lutId, rating)
        // 异步同步到远端
        try { remoteDataSource.submitRating(lutId, rating) } catch (_: Exception) {}
    }
}

/**
 * 资源状态封装
 */
sealed class Resource<out T> {
    data class Loading<T>(val data: T? = null) : Resource<T>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String, val data: T? = null) : Resource<T>()
}

/**
 * 下载进度状态
 */
sealed class DownloadProgress {
    data class Starting(val lutId: String) : DownloadProgress()
    data class Downloading(val lutId: String, val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : DownloadProgress()
    data class Completed(val lutId: String, val filePath: String) : DownloadProgress()
    data class Error(val lutId: String, val message: String) : DownloadProgress()
}

/**
 * LUT 排序方式
 */
enum class LUTSortBy(val key: String, val displayName: String) {
    DOWNLOADS("downloads", "最多下载"),
    RATING("rating", "最高评分"),
    NEWEST("newest", "最新发布"),
    NAME("name", "名称排序");
}
