package com.silas.omaster

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.silas.omaster.ui.components.WelcomeDialog
import com.silas.omaster.ui.detail.PrivacyPolicyScreen

/**
 * 首次启动欢迎流程
 *
 * 展示欢迎对话框，用户必须同意隐私政策才能进入主界面。
 * "查看隐私政策" 按钮在欢迎对话框和隐私政策详情页之间切换。
 *
 * 2.3.5 修复：
 *  - Android 16 预测性返回手势兼容：PredictiveBackHandler 仅在 API 34+ 使用，
 *    旧版本降级使用 BackHandler
 *  - onDisagree 改用 safeGetInstance()，避免 Application 未完全初始化时崩溃
 *  - LocalContext 用于在 OMasterApplication 未初始化时通过 SharedPreferences 直接操作
 */
@Composable
fun WelcomeFlow(
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 2.3.5 修复：Android 16 预测性返回手势兼容
    // PredictiveBackHandler 仅在 API 34+ (Android 14+) 可用
    // 旧版本降级使用 BackHandler
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        PredictiveBackHandler(enabled = showPrivacyPolicy) { _ ->
            showPrivacyPolicy = false
        }
    } else {
        BackHandler(enabled = showPrivacyPolicy) {
            showPrivacyPolicy = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showPrivacyPolicy) {
            PrivacyPolicyScreen(
                onBack = {
                    showPrivacyPolicy = false
                }
            )
        } else {
            WelcomeDialog(
                onAgree = onAgree,
                onDisagree = {
                    // 2.2.0 闪退修复：使用 safeGetInstance 替代 getInstance，
                    // 并在 Application 未初始化时通过 SharedPreferences 直接写入
                    try {
                        OMasterApplication.safeGetInstance()?.setUserAgreed(false)
                    } catch (e: Throwable) {
                        try {
                            val prefs = context.getSharedPreferences(
                                "omaster_prefs",
                                android.content.Context.MODE_PRIVATE
                            )
                            prefs.edit().putBoolean("user_agreed_to_policy", false).apply()
                        } catch (e2: Throwable) {
                            android.util.Log.e("WelcomeFlow", "保存用户协议状态失败", e2)
                        }
                    }
                    onDisagree()
                },
                onViewPrivacyPolicy = {
                    showPrivacyPolicy = true
                }
            )
        }
    }
}
