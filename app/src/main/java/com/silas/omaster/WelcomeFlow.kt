package com.silas.omaster

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.silas.omaster.ui.components.WelcomeDialog
import com.silas.omaster.ui.detail.PrivacyPolicyScreen

/**
 * 首次启动欢迎流程
 *
 * 展示欢迎对话框，用户必须同意隐私政策才能进入主界面。
 * "查看隐私政策" 按钮在欢迎对话框和隐私政策详情页之间切换。
 */
@Composable
fun WelcomeFlow(
    navController: NavHostController,
    onAgree: () -> Unit,
    onDisagree: () -> Unit
) {
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    // 处理系统返回键：在隐私政策页时返回欢迎页
    BackHandler(enabled = showPrivacyPolicy) {
        showPrivacyPolicy = false
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
                onDisagree = onDisagree,
                onViewPrivacyPolicy = {
                    showPrivacyPolicy = true
                }
            )
        }
    }
}
