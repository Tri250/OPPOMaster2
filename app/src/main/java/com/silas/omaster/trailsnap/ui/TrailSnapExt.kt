package com.silas.omaster.trailsnap.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.silas.omaster.trailsnap.model.TrailPhoto
import com.silas.omaster.trailsnap.model.TrailLocation
import com.silas.omaster.trailsnap.model.MediaType

/**
 * 行影集通用 UI 扩展
 * 集中处理跨页面的外部 Intent 跳转与错误日志，减少重复代码。
 */

private const val TAG = "TrailSnapExt"

/**
 * 使用系统应用打开照片或视频查看器。
 */
fun openPhotoViewer(context: android.content.Context, photo: TrailPhoto) {
    val uri = photo.uri ?: return
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, if (photo.mediaType == MediaType.VIDEO) "video/*" else "image/*")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w(TAG, "无法打开照片查看器: $uri", e)
    }
}

/**
 * 使用系统地图应用打开指定地点。
 */
fun openLocationInMap(context: android.content.Context, location: TrailLocation) {
    val uri = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${location.name})")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w(TAG, "无法打开地图应用: ${location.name}", e)
    }
}
