package com.silas.omaster.ui.components

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silas.omaster.MainActivity
import com.silas.omaster.OMasterApplication
import com.silas.omaster.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v2.3.6 崩溃恢复对话框
 *
 * 检测到上次崩溃时显示，提供以下选项：
 * 1. "清除缓存并重启" - 清理缓存目录并重启应用
 * 2. "忽略并继续" - 清除崩溃标记，继续正常使用
 */
@Composable
fun CrashRecoveryDialog(
    onIgnore: () -> Unit,
    onClearCacheAndRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isClearing by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 图标提示
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 标题
                Text(
                    text = stringResource(R.string.crash_recovery_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 描述
                Text(
                    text = stringResource(R.string.crash_recovery_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isClearing) {
                    // 清理进度指示
                    CircularProgressIndicator(
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.crash_recovery_clearing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // 按钮区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 忽略并继续
                        Button(
                            onClick = {
                                // 清除崩溃标记
                                try {
                                    OMasterApplication.safeGetInstance()?.clearCrashFlag()
                                } catch (e: Throwable) {
                                    Log.e("CrashRecoveryDialog", "清除崩溃标记失败", e)
                                    // 直接操作 SharedPreferences
                                    try {
                                        context.getSharedPreferences("omaster_prefs", Context.MODE_PRIVATE)
                                            .edit().putBoolean("app_crashed_last_run", false).apply()
                                    } catch (e2: Throwable) {
                                        Log.e("CrashRecoveryDialog", "直接清除标记失败", e2)
                                    }
                                }
                                onIgnore()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.crash_recovery_ignore),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 清除缓存并重启
                        Button(
                            onClick = {
                                isClearing = true
                                scope.launch {
                                    try {
                                        // 清理缓存
                                        withContext(Dispatchers.IO) {
                                            clearAppCache(context)
                                        }
                                        // 清除崩溃标记
                                        try {
                                            OMasterApplication.safeGetInstance()?.clearCrashFlag()
                                        } catch (e: Throwable) {
                                            Log.w("CrashRecoveryDialog", "清除崩溃标记失败", e)
                                        }
                                        // 短暂延迟让用户看到完成状态
                                        delay(500)
                                        // 重启应用
                                        restartApp(context)
                                    } catch (e: Throwable) {
                                        Log.e("CrashRecoveryDialog", "清理缓存失败", e)
                                        isClearing = false
                                        // 即使失败也尝试重启
                                        try {
                                            restartApp(context)
                                        } catch (e2: Throwable) {
                                            Log.e("CrashRecoveryDialog", "重启失败", e2)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.crash_recovery_clear_cache),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 清理应用缓存目录
 */
private fun clearAppCache(context: Context) {
    try {
        // 清理 cacheDir
        context.cacheDir.listFiles()?.forEach { file ->
            try {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            } catch (e: Throwable) {
                Log.w("CrashRecoveryDialog", "删除缓存文件失败: ${file.name}", e)
            }
        }

        // 清理外部缓存目录（如果有）
        context.externalCacheDir?.listFiles()?.forEach { file ->
            try {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            } catch (e: Throwable) {
                Log.w("CrashRecoveryDialog", "删除外部缓存文件失败: ${file.name}", e)
            }
        }

        // 清理代码缓存目录
        context.codeCacheDir.listFiles()?.forEach { file ->
            try {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            } catch (e: Throwable) {
                Log.w("CrashRecoveryDialog", "删除代码缓存文件失败: ${file.name}", e)
            }
        }

        Log.i("CrashRecoveryDialog", "缓存清理完成")
    } catch (e: Throwable) {
        Log.e("CrashRecoveryDialog", "清理缓存过程失败", e)
    }
}

/**
 * 重启应用
 */
private fun restartApp(context: Context) {
    try {
        // 清除崩溃标记后再重启
        context.getSharedPreferences("omaster_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("app_crashed_last_run", false).apply()

        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)

        // 杀掉当前进程
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(0)
    } catch (e: Throwable) {
        Log.e("CrashRecoveryDialog", "重启应用失败", e)
    }
}