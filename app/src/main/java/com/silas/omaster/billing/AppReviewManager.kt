package com.silas.omaster.billing

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 应用内评分管理器
 *
 * 使用 Google Play In-App Review API 引导用户评分：
 * - 跟踪使用次数，触发评分时机：5+ 次成功操作后
 * - 每 30 天内最多展示一次
 * - 处理 ReviewException 优雅降级
 * - 在 HomeScreen 用户应用预设后触发
 */
class AppReviewManager private constructor(private val activity: Activity) {

    companion object {
        private const val TAG = "AppReviewManager"
        private const val PREFS_NAME = "app_review_prefs"
        private const val KEY_SUCCESS_COUNT = "success_action_count"
        private const val KEY_LAST_REVIEW_TIME = "last_review_time"
        private const val KEY_REVIEW_SHOWN_COUNT = "review_shown_count"
        private const val MIN_SUCCESS_ACTIONS = 5
        private const val MIN_DAYS_BETWEEN_REVIEWS = 30L
        private const val MIN_DAYS_MS = MIN_DAYS_BETWEEN_REVIEWS * 24 * 60 * 60 * 1000L

        @Volatile
        private var instance: AppReviewManager? = null

        fun getInstance(activity: Activity): AppReviewManager {
            return instance ?: synchronized(this) {
                instance ?: AppReviewManager(activity.applicationContext as? Activity ?: activity)
                    .also { instance = it }
            }
        }

        /**
         * 在 Activity 创建时更新实例引用
         */
        fun updateActivity(activity: Activity) {
            synchronized(this) {
                instance = AppReviewManager(activity)
            }
        }
    }

    private val prefs: SharedPreferences =
        activity.applicationContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * 记录一次成功的操作（如：应用预设、创建预设、收藏预设等）
     * 当成功次数达到阈值时，尝试触发评分弹窗
     */
    fun recordSuccessAction() {
        val currentCount = prefs.getInt(KEY_SUCCESS_COUNT, 0) + 1
        prefs.edit().putInt(KEY_SUCCESS_COUNT, currentCount).apply()

        Log.d(TAG, "成功操作计数: $currentCount/$MIN_SUCCESS_ACTIONS")

        if (currentCount >= MIN_SUCCESS_ACTIONS && shouldShowReview()) {
            requestReview()
        }
    }

    /**
     * 直接尝试触发评分（在应用预设等重要操作后调用）
     * HomeScreen 中用户应用预设后调用
     */
    fun tryShowReview() {
        recordSuccessAction()
    }

    /**
     * 请求 Google Play 应用内评分
     */
    private fun requestReview() {
        scope.launch {
            try {
                val reviewManager = ReviewManagerFactory.create(activity.applicationContext)
                val request = reviewManager.requestReviewFlow()
                val reviewInfo = request.await()

                val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                flow.await()

                // 记录已展示
                markReviewShown()
                Log.i(TAG, "应用内评分已展示")
            } catch (e: ReviewException) {
                when (e.errorCode) {
                    ReviewErrorCode.NO_ERROR -> {
                        // 实际上没有错误，但 flow 可能未完成
                        Log.d(TAG, "评分流程完成（无错误）")
                    }
                    ReviewErrorCode.INVALID_REQUEST -> {
                        Log.w(TAG, "评分请求无效，可能已超过频率限制")
                    }
                    ReviewErrorCode.PLAY_STORE_NOT_FOUND -> {
                        Log.w(TAG, "未找到 Google Play 商店")
                    }
                    else -> {
                        Log.e(TAG, "评分请求失败: errorCode=${e.errorCode}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "应用内评分异常", e)
            }
        }
    }

    /**
     * 判断是否应该展示评分弹窗
     */
    private fun shouldShowReview(): Boolean {
        val lastReviewTime = prefs.getLong(KEY_LAST_REVIEW_TIME, 0L)
        val now = System.currentTimeMillis()

        if (lastReviewTime == 0L) return true

        val daysSinceLastReview = (now - lastReviewTime) / MIN_DAYS_MS
        return daysSinceLastReview >= MIN_DAYS_BETWEEN_REVIEWS
    }

    /**
     * 标记评分已展示
     */
    private fun markReviewShown() {
        prefs.edit().apply {
            putLong(KEY_LAST_REVIEW_TIME, System.currentTimeMillis())
            putInt(KEY_REVIEW_SHOWN_COUNT, prefs.getInt(KEY_REVIEW_SHOWN_COUNT, 0) + 1)
            // 重置计数器，避免短时间内重复触发
            putInt(KEY_SUCCESS_COUNT, 0)
            apply()
        }
    }

    /**
     * 获取已展示次数（用于调试）
     */
    fun getReviewShownCount(): Int {
        return prefs.getInt(KEY_REVIEW_SHOWN_COUNT, 0)
    }

    /**
     * 重置所有评分状态（用于测试）
     */
    fun reset() {
        prefs.edit().clear().apply()
    }
}