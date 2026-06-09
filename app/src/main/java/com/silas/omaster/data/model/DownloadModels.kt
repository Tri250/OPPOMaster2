package com.silas.omaster.data.model

/**
 * 资源加载状态封装
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Loading<T>(data: T? = null) : Resource<T>(data)
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
}

/**
 * 下载进度状态
 */
sealed class DownloadProgress {
    data class Starting(val lutId: String) : DownloadProgress()
    data class Downloading(
        val lutId: String,
        val progress: Int,      // 0-100
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadProgress()
    data class Completed(
        val lutId: String,
        val filePath: String
    ) : DownloadProgress()
    data class Error(
        val lutId: String,
        val message: String
    ) : DownloadProgress()
}

/**
 * LUT排序方式
 */
enum class LUTSortBy(val key: String, val displayName: String) {
    DOWNLOADS("downloads", "下载量"),
    RATING("rating", "评分"),
    NEWEST("newest", "最新"),
    NAME("name", "名称");

    companion object {
        fun fromKey(key: String): LUTSortBy =
            entries.find { it.key == key } ?: DOWNLOADS
    }
}

/**
 * 下载状态记录
 */
data class DownloadState(
    val lutId: String,
    val isDownloading: Boolean = false,
    val progress: Int = 0,
    val isCompleted: Boolean = false,
    val filePath: String? = null,
    val error: String? = null,
    val downloadedAt: Long? = null
)