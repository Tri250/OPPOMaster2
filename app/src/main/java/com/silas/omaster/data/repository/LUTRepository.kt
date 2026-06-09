package com.silas.omaster.data.repository

import com.silas.omaster.data.local.LUTLocalDataSource
import com.silas.omaster.data.model.*
import com.silas.omaster.data.remote.LUTRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/**
 * LUT数据仓库
 * 缓存优先策略 + 网络刷新
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
     * 获取单个 LUT 详情
     */
    fun getLUTById(id: String): Flow<MasterLUT?> = flow {
        // 先查本地
        val local = localDataSource.getLUTById(id)
        if (local != null) {
            emit(local)
        }
        // 再查网络更新
        try {
            val remote = remoteDataSource.fetchLUTMeta(id)
            if (remote != null) {
                localDataSource.cacheLUT(remote)
                emit(remote)
            }
        } catch (e: Exception) {
            // 保持本地数据
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 获取热门 LUT
     */
    fun getHotLUTs(): Flow<List<MasterLUT>> = flow {
        // 先发射本地缓存
        val cached = localDataSource.getHotLUTs()
        emit(cached)
        // 网络刷新
        try {
            val remote = remoteDataSource.fetchHotLUTs()
            localDataSource.cacheLUTs(remote)
            emit(remote)
        } catch (e: Exception) {
            // 保持缓存
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 获取新品 LUT
     */
    fun getNewLUTs(): Flow<List<MasterLUT>> = flow {
        val cached = localDataSource.getNewLUTs()
        emit(cached)
        try {
            val remote = remoteDataSource.fetchNewLUTs()
            localDataSource.cacheLUTs(remote)
            emit(remote)
        } catch (e: Exception) {
            // 保持缓存
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 获取精选 LUT
     */
    fun getFeaturedLUTs(): Flow<List<MasterLUT>> = flow {
        val cached = localDataSource.getFeaturedLUTs()
        emit(cached)
        try {
            val remote = remoteDataSource.fetchFeaturedLUTs()
            localDataSource.cacheLUTs(remote)
            emit(remote)
        } catch (e: Exception) {
            // 保持缓存
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 下载 LUT 文件（带进度）
     */
    fun downloadLUT(lut: MasterLUT): Flow<DownloadProgress> = flow {
        emit(DownloadProgress.Starting(lut.id))
        try {
            // 检查本地是否已有
            if (localDataSource.hasLUTFile(lut.id)) {
                val filePath = localDataSource.getLUTFilePath(lut.id)
                emit(DownloadProgress.Completed(lut.id, filePath))
                return@flow
            }
            // 远程下载
            remoteDataSource.downloadLUTFile(lut.downloadUrl, lut.fileSize).collect { progress ->
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
                    is DownloadProgress.Error -> {
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
    fun getDownloadedLUTs(): Flow<List<MasterLUT>> = 
        localDataSource.getDownloadedLUTs()
            .flowOn(Dispatchers.IO)

    /**
     * 删除已下载的 LUT 文件
     */
    suspend fun deleteDownloadedLUT(id: String) = withContext(Dispatchers.IO) {
        localDataSource.deleteLUTFile(id)
        localDataSource.removeDownloadRecord(id)
    }

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(lutId: String) = withContext(Dispatchers.IO) {
        localDataSource.toggleFavorite(lutId)
    }

    /**
     * 获取收藏列表
     */
    fun getFavorites(): Flow<List<MasterLUT>> = 
        localDataSource.getFavorites()
            .flowOn(Dispatchers.IO)

    /**
     * 检查是否已收藏
     */
    fun isFavorite(lutId: String): Flow<Boolean> = 
        localDataSource.isFavorite(lutId)
            .flowOn(Dispatchers.IO)

    /**
     * 提交评分
     */
    suspend fun submitRating(lutId: String, rating: Float) = withContext(Dispatchers.IO) {
        localDataSource.saveRating(lutId, rating)
        // 同步到服务器
        try {
            remoteDataSource.submitRating(lutId, rating)
        } catch (e: Exception) {
            // 离线时仅保存本地
        }
    }

    /**
     * 获取用户评分
     */
    fun getUserRating(lutId: String): Flow<Float?> = 
        localDataSource.getUserRating(lutId)
            .flowOn(Dispatchers.IO)

    /**
     * 搜索 LUT
     */
    fun searchLUTs(query: String): Flow<List<MasterLUT>> = flow {
        val cached = localDataSource.searchLUTs(query)
        emit(cached)
        try {
            val remote = remoteDataSource.searchLUTs(query)
            localDataSource.cacheLUTs(remote)
            emit(remote)
        } catch (e: Exception) {
            // 保持缓存
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 按系列获取 LUT
     */
    fun getLUTsByCollection(collection: String): Flow<List<MasterLUT>> = 
        localDataSource.getLUTsByCollection(collection)
            .flowOn(Dispatchers.IO)

    /**
     * 获取 HNCS 认证的 LUT
     */
    fun getHncsCertifiedLUTs(): Flow<List<MasterLUT>> = 
        localDataSource.getHncsCertifiedLUTs()
            .flowOn(Dispatchers.IO)

    /**
     * 刷新所有数据
     */
    suspend fun refreshAll() = withContext(Dispatchers.IO) {
        try {
            val allLUTs = remoteDataSource.fetchLUTList("all", "", "downloads")
            localDataSource.cacheLUTs(allLUTs)
        } catch (e: Exception) {
            // 保持缓存
        }
    }

    /**
     * 获取下载状态
     */
    fun getDownloadState(lutId: String): Flow<DownloadState?> = 
        localDataSource.getDownloadState(lutId)
            .flowOn(Dispatchers.IO)

    /**
     * 获取所有下载状态
     */
    fun getAllDownloadStates(): Flow<Map<String, DownloadState>> = 
        localDataSource.getAllDownloadStates()
            .flowOn(Dispatchers.IO)
}