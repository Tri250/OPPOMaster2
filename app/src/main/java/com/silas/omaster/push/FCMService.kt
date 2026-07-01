package com.silas.omaster.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.silas.omaster.MainActivity
import com.silas.omaster.data.local.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

/**
 * Firebase Cloud Messaging 服务
 *
 * 处理推送通知：
 * - onNewToken: 保存 FCM token 到 SettingsManager 并上报服务器
 * - onMessageReceived: 处理不同类型的推送消息
 *
 * 支持的消息类型：
 * - "new_preset": 新预设上线，点击通知跳转到预设详情
 * - "update_available": 新版本可用
 * - "subscription_expiring": 订阅即将到期
 * - "general": 通用通知
 */
class FCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "omaster_news"
        private const val CHANNEL_NAME = "OMaster 资讯"
        private const val CHANNEL_DESC = "接收预设更新、版本更新和订阅提醒"
        private const val NOTIFICATION_ID_BASE = 1000

        // 消息类型
        private const val TYPE_NEW_PRESET = "new_preset"
        private const val TYPE_UPDATE_AVAILABLE = "update_available"
        private const val TYPE_SUBSCRIPTION_EXPIRING = "subscription_expiring"
        private const val TYPE_GENERAL = "general"

        private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM Token 已更新: ${token.take(10)}...")

        // 保存 token 到 SettingsManager
        try {
            val settingsManager = SettingsManager.getInstance(this)
            settingsManager.fcmToken = token
        } catch (e: Exception) {
            Log.e(TAG, "保存 FCM Token 失败", e)
        }

        // 发送 token 到服务器
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "收到推送消息: from=${message.from}, type=${message.data["type"]}")

        val data = message.data
        val type = data["type"] ?: TYPE_GENERAL
        val title = data["title"] ?: message.notification?.title ?: "OMaster"
        val body = data["body"] ?: message.notification?.body ?: ""

        when (type) {
            TYPE_NEW_PRESET -> handleNewPreset(data, title, body)
            TYPE_UPDATE_AVAILABLE -> handleUpdateAvailable(title, body)
            TYPE_SUBSCRIPTION_EXPIRING -> handleSubscriptionExpiring(title, body)
            else -> handleGeneralNotification(title, body)
        }
    }

    // ==================== 消息处理 ====================

    private fun handleNewPreset(data: Map<String, String>, title: String, body: String) {
        val presetId = data["preset_id"] ?: ""
        val presetName = data["preset_name"] ?: ""

        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("omaster://preset/$presetId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        showNotification(
            id = NOTIFICATION_ID_BASE + presetId.hashCode().coerceIn(0, Int.MAX_VALUE),
            title = title,
            body = if (presetName.isNotEmpty()) "新预设「$presetName」已上线，点击查看详情" else body,
            pendingIntent = pendingIntent
        )
    }

    private fun handleUpdateAvailable(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID_BASE + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        showNotification(
            id = NOTIFICATION_ID_BASE + 1,
            title = title,
            body = body,
            pendingIntent = pendingIntent
        )
    }

    private fun handleSubscriptionExpiring(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID_BASE + 2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        showNotification(
            id = NOTIFICATION_ID_BASE + 2,
            title = title,
            body = body,
            pendingIntent = pendingIntent
        )
    }

    private fun handleGeneralNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID_BASE + 3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        showNotification(
            id = NOTIFICATION_ID_BASE + 3,
            title = title,
            body = body,
            pendingIntent = pendingIntent
        )
    }

    // ==================== 通知显示 ====================

    private fun showNotification(id: Int, title: String, body: String, pendingIntent: PendingIntent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "未授予通知权限，跳过通知显示")
                return
            }
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(id, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "显示通知失败", e)
        }
    }

    // ==================== 通知渠道 ====================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ==================== Token 上报 ====================

    private fun sendTokenToServer(token: String) {
        serviceScope.launch {
            try {
                val settingsManager = SettingsManager.getInstance(this@FCMService)
                val apiEndpoint = settingsManager.presetApiEndpoint
                if (apiEndpoint.isEmpty()) {
                    Log.w(TAG, "API 端点未配置，跳过 token 上报")
                    return@launch
                }

                val url = URL("${apiEndpoint.trimEnd('/')}/v1/device/register")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("User-Agent", "OMaster/${com.silas.omaster.BuildConfig.VERSION_NAME}")
                conn.doOutput = true
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000

                val body = """{"token":"$token","platform":"android"}"""
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                conn.disconnect()

                if (code in 200..299) {
                    Log.i(TAG, "Token 上报成功")
                } else {
                    Log.w(TAG, "Token 上报失败: HTTP $code")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Token 上报异常", e)
            }
        }
    }
}