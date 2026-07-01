package com.silas.omaster.background

import android.content.Context
import android.util.Log
import androidx.work.*
import com.silas.omaster.data.repository.PresetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager 后台同步 Worker
 *
 * 负责定期执行以下后台任务：
 * - 云端预设同步（拉取最新配方配置）
 * - 订阅源更新检查
 * - 崩溃日志上报（如有网络）
 * - 反馈数据后台上传
 *
 * 约束条件：
 * - 仅在有网络连接时执行（NetworkType.CONNECTED）
 * - 低电量时不执行（BatteryNotLow）
 * - 执行失败时指数退避重试
 *
 * 使用 WorkManager 确保：
 * - 即使应用被杀死，任务仍能按计划执行
 * - 系统根据设备状态智能调度（Doze 模式适配）
 * - 任务链式执行，保证顺序
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val UNIQUE_WORK_NAME = "omaster_periodic_sync"

        /**
         * 调度定期同步任务
         *
         * @param context 应用上下文
         * @param intervalHours 同步间隔（小时），默认 24 小时
         */
        fun schedule(context: Context, intervalHours: Long = 24) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                intervalHours, TimeUnit.HOURS,
                // flexInterval: 允许在 interval 的最后 4 小时内执行，避免所有设备同时请求
                4, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .addTag("omaster_sync")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // 如果已存在则保留现有任务
                syncRequest
            )

            Log.i(TAG, "定期同步已调度: 每 ${intervalHours}h 执行一次")
        }

        /**
         * 立即执行一次同步（用于手动触发）
         *
         * @return 可用于观察任务状态的 LiveData
         */
        fun syncNow(context: Context): androidx.lifecycle.LiveData<WorkInfo?> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .addTag("omaster_sync_manual")
                .build()

            WorkManager.getInstance(context).enqueue(request)
            Log.i(TAG, "手动同步已触发")
            return WorkManager.getInstance(context).getWorkInfoByIdLiveData(request.id)
        }

        /**
         * 取消定期同步任务
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            Log.i(TAG, "定期同步已取消")
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "后台同步开始执行...")

        return try {
            withContext(Dispatchers.IO) {
                var hasFailure = false

                // 1. 云端预设同步
                try {
                    syncPresets()
                    Log.i(TAG, "预设同步完成")
                } catch (e: Exception) {
                    Log.e(TAG, "预设同步失败", e)
                    hasFailure = true
                }

                // 2. 订阅源更新
                try {
                    syncSubscriptions()
                    Log.i(TAG, "订阅源更新完成")
                } catch (e: Exception) {
                    Log.e(TAG, "订阅源更新失败", e)
                    hasFailure = true
                }

                // 3. 反馈数据上传
                try {
                    uploadPendingFeedback()
                    Log.i(TAG, "反馈上传完成")
                } catch (e: Exception) {
                    Log.e(TAG, "反馈上传失败", e)
                    hasFailure = true
                }

                if (hasFailure) {
                    Result.retry()
                } else {
                    Log.i(TAG, "后台同步全部完成")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "后台同步异常", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * 同步云端预设
     */
    private suspend fun syncPresets() {
        try {
            val repository = PresetRepository.getInstance(applicationContext)
            // 触发云端同步
            repository.forceReloadFromFiles()
        } catch (e: Exception) {
            Log.w(TAG, "云端预设同步失败: ${e.message}")
            throw e
        }
    }

    /**
     * 同步订阅源
     */
    private suspend fun syncSubscriptions() {
        try {
            val subManager = com.silas.omaster.data.local.SubscriptionManager.getInstance(
                applicationContext
            )
            val enabledSubs = subManager.subscriptionsFlow.value.filter { it.isEnabled }
            for (sub in enabledSubs) {
                try {
                    com.silas.omaster.network.PresetRemoteManager.fetchAndSave(
                        applicationContext, sub.url, forceUpdate = true
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "订阅源同步失败: ${sub.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "订阅源同步失败: ${e.message}")
            throw e
        }
    }

    /**
     * 上传待发送的反馈数据
     */
    private suspend fun uploadPendingFeedback() {
        try {
            // FeedbackManager 的 attemptUpload 会处理离线队列
            // 仅在网络可用时触发
            val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? android.net.ConnectivityManager
            val isConnected = cm?.activeNetworkInfo?.isConnected == true
            if (isConnected) {
                com.silas.omaster.feedback.FeedbackManager(applicationContext).retryAll()
            }
        } catch (e: Exception) {
            Log.w(TAG, "反馈上传失败: ${e.message}")
            throw e
        }
    }
}