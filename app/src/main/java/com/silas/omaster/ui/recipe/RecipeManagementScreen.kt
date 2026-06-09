package com.silas.omaster.ui.recipe

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silas.omaster.ai.recipe.*
import com.silas.omaster.ui.hasselblad.HasselbladParamsDisplaySimple
import com.silas.omaster.ui.theme.HasselbladTheme
import kotlinx.coroutines.launch

/**
 * 配方管理页面
 * 支持配方保存、分享、导入、收藏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeManagementScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager()
    val recipeManager = remember { RecipeManager.getInstance(context) }

    val recipes by recipeManager.recipesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val stats = remember(recipes) { recipeManager.getStats() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var selectedRecipe by remember { mutableStateOf<RecipeProfile?>(null) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }

    val filteredRecipes = remember(recipes, searchQuery, selectedTab) {
        val baseList = if (searchQuery.text.isNotEmpty()) {
            recipeManager.searchRecipes(searchQuery.text)
        } else {
            when (selectedTab) {
                0 -> recipes // 全部
                1 -> recipes.filter { it.isFavorite } // 收藏
                2 -> recipes.sortedByDescending { it.usageCount } // 常用
                else -> recipes
            }
        }
        baseList
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "哈苏配方库",
                        fontWeight = FontWeight.Bold,
                        color = HasselbladTheme.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                        onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "返回",
                            tint = HasselbladTheme.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Select)
                        showImportDialog = true
                    }) {
                        Icon(
                            Icons.Default.Download,
                            "导入配方",
                            tint = HasselbladTheme.HasselbladOrange
                        )
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Select)
                        showCreateDialog = true
                    }) {
                        Icon(
                            Icons.Default.Add,
                            "创建配方",
                            tint = HasselbladTheme.HasselbladOrange
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HasselbladTheme.PureBlack,
                    titleContentColor = HasselbladTheme.TextPrimary
                )
            )
        },
        containerColor = HasselbladTheme.PureBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 统计卡片
            RecipeStatsCard(
                stats = stats,
                modifier = Modifier.padding(16.dp)
            )

            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("搜索配方...", color = HasselbladTheme.TextTertiary) },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = HasselbladTheme.TextTertiary)
                },
                trailingIcon = {
                    if (searchQuery.text.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = TextFieldValue("") }) {
                            Icon(Icons.Default.Clear, null, tint = HasselbladTheme.TextTertiary)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HasselbladTheme.HasselbladOrange,
                    unfocusedBorderColor = HasselbladTheme.DividerColor,
                    focusedContainerColor = HasselbladTheme.CardBackground,
                    unfocusedContainerColor = HasselbladTheme.CardBackground,
                    cursorColor = HasselbladTheme.HasselbladOrange
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 分类标签
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecipeTabChip(
                    label = "全部 (${recipes.size})",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                RecipeTabChip(
                    label = "收藏 (${recipes.filter { it.isFavorite }.size})",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                RecipeTabChip(
                    label = "常用",
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 配方列表
            if (filteredRecipes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = HasselbladTheme.TextTertiary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "暂无配方",
                            color = HasselbladTheme.TextTertiary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "点击右上角 + 创建你的第一个哈苏配方",
                            color = HasselbladTheme.TextDisabled,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRecipes) { recipe ->
                        RecipeCard(
                            recipe = recipe,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Select)
                                selectedRecipe = recipe
                            },
                            onFavorite = {
                                scope.launch {
                                    recipeManager.toggleFavorite(recipe.id)
                                }
                            },
                            onShare = {
                                haptic.performHapticFeedback(HapticFeedbackType.Select)
                                selectedRecipe = recipe
                                showShareDialog = true
                            },
                            onDelete = {
                                scope.launch {
                                    recipeManager.deleteRecipe(recipe.id)
                                }
                            }
                        )
                    }

                    // 底部间距
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // 分享弹窗
    if (showShareDialog && selectedRecipe != null) {
        RecipeShareDialog(
            recipe = selectedRecipe!!,
            onDismiss = {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                showShareDialog = false
            },
            onCopyCode = {
                clipboardManager.setText(AnnotatedString(selectedRecipe!!.toShareCode()))
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        )
    }

    // 导入弹窗
    if (showImportDialog) {
        RecipeImportDialog(
            onDismiss = {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                showImportDialog = false
            },
            onImport = { code ->
                scope.launch {
                    val result = recipeManager.importRecipe(code)
                    when (result) {
                        is ImportResult.Success -> {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            showImportDialog = false
                        }
                        is ImportResult.Duplicate -> {
                            // 提示已存在
                        }
                        is ImportResult.Error -> {
                            // 提示错误
                        }
                    }
                }
            }
        )
    }

    // 配方详情弹窗
    if (selectedRecipe != null && !showShareDialog) {
        RecipeDetailDialog(
            recipe = selectedRecipe!!,
            onApply = {
                scope.launch {
                    recipeManager.useRecipe(selectedRecipe!!.id)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                selectedRecipe = null
            },
            onDismiss = {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                selectedRecipe = null
            }
        )
    }
}

/**
 * 配方统计卡片
 */
@Composable
fun RecipeStatsCard(
    stats: RecipeStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(HasselbladTheme.CardCornerRadius.dp),
        colors = CardDefaults.cardColors(
            containerColor = HasselbladTheme.CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "配方总数",
                value = stats.totalRecipes.toString(),
                icon = "📚"
            )
            StatItem(
                label = "收藏数",
                value = stats.favoriteCount.toString(),
                icon = "⭐"
            )
            StatItem(
                label = "使用次数",
                value = stats.totalUsageCount.toString(),
                icon = "🎯"
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = HasselbladTheme.HasselbladOrange,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = HasselbladTheme.TextTertiary,
            fontSize = 12.sp
        )
    }
}

/**
 * 分类标签芯片
 */
@Composable
fun RecipeTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) HasselbladTheme.HasselbladOrange.copy(alpha = 0.2f) else HasselbladTheme.CardBackground,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, HasselbladTheme.HasselbladOrange) else null,
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (selected) HasselbladTheme.HasselbladOrange else HasselbladTheme.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

/**
 * 配方卡片
 */
@Composable
fun RecipeCard(
    recipe: RecipeProfile,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = HasselbladTheme.CardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.scene.icon,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = recipe.name,
                    color = HasselbladTheme.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                // 收藏按钮
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (recipe.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (recipe.isFavorite) HasselbladTheme.HasselbladOrange else HasselbladTheme.TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 场景和胶片信息
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HasselbladTheme.CardBackgroundHighlight
                ) {
                    Text(
                        text = recipe.scene.displayName,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        color = HasselbladTheme.TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HasselbladTheme.HasselbladOrange.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "🎞️ ${recipe.film.displayName}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        color = HasselbladTheme.HasselbladOrange,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 参数摘要
            HasselbladParamsDisplaySimple(params = recipe.hasselbladParams)

            Spacer(modifier = Modifier.height(12.dp))

            // 底部信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "使用 ${recipe.usageCount} 次",
                    color = HasselbladTheme.TextTertiary,
                    fontSize = 11.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            null,
                            tint = HasselbladTheme.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            tint = HasselbladTheme.TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 配方分享弹窗
 */
@Composable
fun RecipeShareDialog(
    recipe: RecipeProfile,
    onDismiss: () -> Unit,
    onCopyCode: () -> Unit
) {
    val shareCode = remember { recipe.toShareCode() }
    val qrCodeBitmap = remember { RecipeShareHelper.generateBrandedQRCode(shareCode) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HasselbladTheme.CardCornerRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = HasselbladTheme.PureBlack
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "分享配方",
                    color = HasselbladTheme.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 二维码
                if (qrCodeBitmap != null) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = "配方二维码",
                        modifier = Modifier.size(200.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "扫描二维码或复制配方代码",
                    color = HasselbladTheme.TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 配方代码（截取显示）
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HasselbladTheme.CardBackground
                ) {
                    Text(
                        text = shareCode.take(40) + "...",
                        modifier = Modifier.padding(12.dp),
                        color = HasselbladTheme.TextTertiary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("关闭")
                    }

                    Button(
                        onClick = onCopyCode,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HasselbladTheme.HasselbladOrange
                        )
                    ) {
                        Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("复制代码")
                    }
                }
            }
        }
    }
}

/**
 * 配方导入弹窗
 */
@Composable
fun RecipeImportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var codeInput by remember { mutableStateOf(TextFieldValue("")) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HasselbladTheme.CardCornerRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = HasselbladTheme.PureBlack
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "导入配方",
                    color = HasselbladTheme.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { 
                        codeInput = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("粘贴配方代码...", color = HasselbladTheme.TextTertiary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HasselbladTheme.HasselbladOrange,
                        unfocusedBorderColor = HasselbladTheme.DividerColor
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = HasselbladTheme.Error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = {
                            if (codeInput.text.isEmpty()) {
                                errorMessage = "请输入配方代码"
                            } else if (!RecipeShareHelper.validateShareCode(codeInput.text)) {
                                errorMessage = "无效的配方代码"
                            } else {
                                onImport(codeInput.text)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HasselbladTheme.HasselbladOrange
                        )
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("导入")
                    }
                }
            }
        }
    }
}

/**
 * 配方详情弹窗
 */
@Composable
fun RecipeDetailDialog(
    recipe: RecipeProfile,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HasselbladTheme.CardCornerRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = HasselbladTheme.PureBlack
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // 标题
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = recipe.scene.icon, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = recipe.name,
                        color = HasselbladTheme.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = recipe.description,
                    color = HasselbladTheme.TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 场景和胶片
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = HasselbladTheme.CardBackgroundHighlight
                    ) {
                        Text(
                            text = "场景: ${recipe.scene.displayName}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = HasselbladTheme.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = HasselbladTheme.HasselbladOrange.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "胶片: ${recipe.film.displayName} (${recipe.film.matchPercent}%)",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = HasselbladTheme.HasselbladOrange,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 哈苏参数
                Text(
                    text = "哈苏大师参数",
                    color = HasselbladTheme.HasselbladOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                HasselbladParamsDisplaySimple(params = recipe.hasselbladParams)

                Spacer(modifier = Modifier.height(16.dp))

                // 大师建议
                if (recipe.masterTips.isNotEmpty()) {
                    Text(
                        text = "大师建议",
                        color = HasselbladTheme.HasselbladOrange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    recipe.masterTips.forEach { tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "💡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tip,
                                color = HasselbladTheme.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("关闭")
                    }

                    Button(
                        onClick = onApply,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HasselbladTheme.HasselbladOrange
                        )
                    ) {
                        Text("应用配方", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}