package com.silas.omaster.recognition

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.silas.omaster.R
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset

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

    if (loading) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFEC7223))
            }
        }
    } else if (preset != null) {
        PopCaptureScreen(preset = preset!!, onBack = onBack)
    } else {
        Surface(modifier = Modifier.fillMaxSize()) {
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
