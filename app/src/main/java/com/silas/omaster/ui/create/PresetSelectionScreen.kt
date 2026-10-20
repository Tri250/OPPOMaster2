package com.silas.omaster.ui.create

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.R
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.components.PresetCard
import com.silas.omaster.ui.theme.AppDesign
import com.silas.omaster.ui.theme.themedBackground
import com.silas.omaster.ui.theme.themedTextSecondary

@Composable
fun PresetSelectionScreen(
    onPresetSelected: (String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PresetRepository.getInstance(context) }
    val presets by repository.getAllPresets().collectAsState(initial = emptyList())
    // FD-01b 粘贴导入入口；FD-01a 文件导入入口
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    // FD-01a：SAF 选择预设 JSON 文件，读取文本后复用导入对话框
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val content = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (content != null) {
                importText = content
                showImportDialog = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themedBackground())
    ) {
        OMasterTopAppBar(
            title = stringResource(R.string.select_template),
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDesign.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(AppDesign.ItemSpacing))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPresetSelected(null) },
                shape = RoundedCornerShape(AppDesign.MediumRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Add, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.start_from_scratch),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(AppDesign.ItemSpacing))

            // FD-01b：粘贴预设 JSON 导入（经 R-3 校验管道入库）
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showImportDialog = true },
                shape = RoundedCornerShape(AppDesign.MediumRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ContentPaste, null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.paste_import),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(AppDesign.ItemSpacing))

            // FD-01a：从文件导入预设 JSON
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        filePicker.launch(arrayOf("application/json", "text/plain"))
                    },
                shape = RoundedCornerShape(AppDesign.MediumRadius),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Description, null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.file_import),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(AppDesign.SectionSpacing))

            Text(
                stringResource(R.string.or_choose_template),
                style = MaterialTheme.typography.labelLarge,
                color = themedTextSecondary(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // v1.6.15 性能修复：补 key，预设列表变化时保持 item 状态与复用率
                items(presets, key = { it.id ?: it.name }) { preset ->
                    PresetCard(
                        preset = preset,
                        onClick = { onPresetSelected(preset.id) }
                    )
                }
            }
        }

        if (showImportDialog) {
            ClipboardImportDialog(
                initialText = importText,
                onDismiss = {
                    showImportDialog = false
                    importText = ""
                }
            )
        }
    }
}
