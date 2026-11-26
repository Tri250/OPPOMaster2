package com.silas.omaster.recognition

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.silas.omaster.R
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.theme.HasselbladOrange

/** POP 即拍入口三态（AnimatedContent 平滑过渡，避免加载→内容生硬跳变） */
private enum class PopHostState { Loading, Ready, Missing }

/** 按 presetId 异步加载预设后进入 POP 即拍页 */
@Composable
fun PopCaptureHost(presetId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var preset by remember { mutableStateOf<MasterPreset?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(presetId) {
        preset = PresetRepository.getInstance(context).getPresetById(presetId)
        loading = false
    }

    val hostState = when {
        loading -> PopHostState.Loading
        preset != null -> PopHostState.Ready
        else -> PopHostState.Missing
    }
    AnimatedContent(
        targetState = hostState,
        transitionSpec = {
            fadeIn(tween(220, easing = FastOutSlowInEasing))
                .togetherWith(fadeOut(tween(180, easing = FastOutSlowInEasing)))
        },
        label = "popHostState",
        modifier = Modifier.fillMaxSize()
    ) { state ->
        when (state) {
            PopHostState.Loading -> Surface(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = HasselbladOrange)
                }
            }
            PopHostState.Ready -> {
                val p = preset
                if (p != null) {
                    PopCaptureScreen(preset = p, onBack = onBack)
                }
            }
            PopHostState.Missing -> Surface(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.pop_preset_missing))
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onBack) { Text(stringResource(R.string.pop_back)) }
                    }
                }
            }
        }
    }
}
