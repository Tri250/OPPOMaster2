package com.silas.omaster.ui.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.silas.omaster.R
import com.silas.omaster.billing.BillingManager
import com.silas.omaster.billing.ProFeature
import com.silas.omaster.billing.ProFeatureGate
import com.silas.omaster.billing.SubscriptionState
import com.silas.omaster.billing.Tier
import com.silas.omaster.ui.theme.*

/**
 * 付费墙 / 升级页面
 *
 * 三档定价卡片：月度、年度（含折扣标签）、终身
 * 功能对比列表
 * 购买按钮集成 BillingManager
 * 恢复购买按钮
 * 遵循 PureBlack #0A0A0A 深色主题 + HasselbladOrange #E8650A 强调色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    onBack: () -> Unit,
    onPurchaseComplete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val billingManager = remember { BillingManager.instance }
    val subscriptionState by billingManager?.subscriptionState?.collectAsState()
        ?: mutableStateOf(SubscriptionState.FREE)
    val productDetails by billingManager?.productDetails?.collectAsState()
        ?: mutableStateOf(emptyList<ProductDetails>())
    val isBillingAvailable by billingManager?.isBillingAvailable?.collectAsState()
        ?: mutableStateOf(false)

    var selectedPlan by remember { mutableIntStateOf(1) } // 0=月度, 1=年度, 2=终身
    var isPurchasing by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var showRestoreResult by remember { mutableStateOf<String?>(null) }

    // 如果已经是 Pro，显示已解锁状态
    val isPro = subscriptionState.isActivePro

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.paywall_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ========== 品牌标识 ==========
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "解锁 OMaster Pro",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "全部专业功能，尽情创作",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ========== 三档定价卡片 ==========
            if (isPro) {
                // 已是 Pro 用户
                ProActiveCard(subscriptionState = subscriptionState)
            } else if (!isBillingAvailable) {
                // BillingClient 不可用（如华为设备）
                BillingUnavailableCard()
            } else {
                // 定价选择卡片
                PricingCards(
                    productDetails = productDetails,
                    selectedPlan = selectedPlan,
                    onPlanSelected = { selectedPlan = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ========== 购买/恢复按钮 ==========
            if (!isPro && isBillingAvailable) {
                // 立即升级按钮
                Button(
                    onClick = {
                        val activity = context as? android.app.Activity ?: return@Button
                        val manager = billingManager ?: return@Button
                        isPurchasing = true

                        val productId = when (selectedPlan) {
                            0 -> BillingManager.PRODUCT_MONTHLY
                            1 -> BillingManager.PRODUCT_YEARLY
                            else -> BillingManager.PRODUCT_LIFETIME
                        }
                        val details = manager.getProductDetails(productId)
                        if (details != null) {
                            val result = manager.launchBillingFlow(activity, details)
                            if (result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                                isPurchasing = false
                            }
                            // 购买结果通过 PurchasesUpdatedListener 回调处理
                        } else {
                            isPurchasing = false
                        }
                    },
                    enabled = !isPurchasing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("处理中...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.upgrade_now),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 恢复购买按钮
                OutlinedButton(
                    onClick = {
                        isRestoring = true
                        billingManager?.restorePurchases { success ->
                            isRestoring = false
                            showRestoreResult = if (success) "恢复成功" else "未找到购买记录"
                        }
                    },
                    enabled = !isRestoring,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            listOf(HasselbladOrange, HasselbladOrangeLight)
                        )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = HasselbladOrange
                    )
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = HasselbladOrange,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("恢复中...")
                    } else {
                        Icon(Icons.Default.Restore, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.paywall_restore))
                    }
                }

                // 恢复结果提示
                showRestoreResult?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        msg,
                        fontSize = 12.sp,
                        color = if (msg.contains("成功")) SuccessGreen else Color.White.copy(alpha = 0.5f)
                    )
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(3000L)
                        showRestoreResult = null
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ========== 功能对比列表 ==========
            Text(
                "功能对比",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            FeatureComparisonList()

            Spacer(modifier = Modifier.height(24.dp))

            // ========== 底部说明 ==========
            Text(
                "订阅将自动续期，可随时在 Google Play 中取消\n终身买断一次付费，永久使用",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.35f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ==================== 定价卡片 ====================

@Composable
private fun PricingCards(
    productDetails: List<ProductDetails>,
    selectedPlan: Int,
    onPlanSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 月度
        PricingCard(
            title = stringResource(R.string.paywall_monthly),
            price = formatPrice(productDetails, BillingManager.PRODUCT_MONTHLY),
            badge = null,
            isSelected = selectedPlan == 0,
            onClick = { onPlanSelected(0) },
            modifier = Modifier.weight(1f)
        )

        // 年度（推荐）
        PricingCard(
            title = stringResource(R.string.paywall_yearly),
            price = formatPrice(productDetails, BillingManager.PRODUCT_YEARLY),
            badge = "省 40%",
            isSelected = selectedPlan == 1,
            onClick = { onPlanSelected(1) },
            modifier = Modifier.weight(1f)
        )

        // 终身
        PricingCard(
            title = stringResource(R.string.paywall_lifetime),
            price = formatPrice(productDetails, BillingManager.PRODUCT_LIFETIME),
            badge = "最划算",
            isSelected = selectedPlan == 2,
            onClick = { onPlanSelected(2) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PricingCard(
    title: String,
    price: String,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) HasselbladOrange else Color.White.copy(alpha = 0.1f)
    val bgColor = if (isSelected) HasselbladOrange.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f)

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = bgColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) HasselbladOrange else Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    price,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    when (title) {
                        "月度" -> "/月"
                        "年度" -> "/年"
                        else -> "一次付费"
                    },
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }

        // 折扣标签
        if (badge != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-8).dp),
                color = HasselbladOrange,
                shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
            ) {
                Text(
                    badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

// ==================== 功能对比列表 ====================

@Composable
private fun FeatureComparisonList() {
    val features = listOf(
        Triple(stringResource(R.string.paywall_feature_1), true, true),  // 基础相机
        Triple(stringResource(R.string.paywall_feature_2), true, true),  // 基础预设
        Triple(stringResource(R.string.paywall_feature_3), true, true),  // 基础编辑
        Triple(stringResource(R.string.paywall_feature_4), false, true), // AI 高级调色
        Triple(stringResource(R.string.paywall_feature_5), false, true), // 夜景模式
        Triple(stringResource(R.string.paywall_feature_6), false, true), // 光绘模式
        Triple(stringResource(R.string.paywall_feature_7), false, true), // 人像散景
        Triple(stringResource(R.string.paywall_feature_8), false, true), // 风格 LUT 生成器
        Triple(stringResource(R.string.paywall_feature_9), false, true), // 云同步
        Triple(stringResource(R.string.paywall_feature_10), false, true) // 哈苏高级模式
    )

    // 表头
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "功能",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        Text(
            "免费",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.Center
        )
        Text(
            "Pro",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = HasselbladOrange,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.Center
        )
    }

    features.forEach { (feature, freeAvailable, proAvailable) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                feature,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (freeAvailable) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (freeAvailable) SuccessGreen else Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(16.dp)
                    .width(48.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                if (proAvailable) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (proAvailable) HasselbladOrange else Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(16.dp)
                    .width(48.dp)
            )
        }
    }
}

// ==================== Pro 已激活卡片 ====================

@Composable
private fun ProActiveCard(subscriptionState: SubscriptionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder(enabled = false).copy(
            brush = Brush.horizontalGradient(listOf(HasselbladOrange, HasselbladOrangeLight))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Verified,
                contentDescription = null,
                tint = HasselbladOrange,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Pro 已激活",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = HasselbladOrange
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                when (subscriptionState.tier) {
                    Tier.PRO_MONTHLY -> "月度订阅"
                    Tier.PRO_YEARLY -> "年度订阅"
                    Tier.LIFETIME -> "终身会员"
                    else -> ""
                },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            subscriptionState.expiryDate?.let { expiry ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "有效期至: ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(java.util.Date(expiry))}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ==================== BillingClient 不可用卡片 ====================

@Composable
private fun BillingUnavailableCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Storefront,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "应用商店不可用",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "当前设备不支持 Google Play 支付\n请联系开发者获取其他购买方式",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==================== 工具方法 ====================

private fun formatPrice(productDetails: List<ProductDetails>, productId: String): String {
    val details = productDetails.find { it.productId == productId }
    return details?.oneTimePurchaseOfferDetails?.formattedPrice
        ?: details?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases
            ?.pricingPhaseList?.firstOrNull()?.formattedPrice
        ?: when (productId) {
            BillingManager.PRODUCT_MONTHLY -> "¥18"
            BillingManager.PRODUCT_YEARLY -> "¥128"
            BillingManager.PRODUCT_LIFETIME -> "¥298"
            else -> "—"
        }
}
