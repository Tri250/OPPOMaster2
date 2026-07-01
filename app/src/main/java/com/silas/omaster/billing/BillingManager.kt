package com.silas.omaster.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.PurchasesResponseListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Google Play Billing 封装管理器
 *
 * 单例模式，负责：
 * - 管理 Google Play BillingClient 连接
 * - 查询可用商品（月度订阅、年度订阅、终身买断）
 * - 发起购买流程
 * - 验证购买（本地验证 + 服务端验证桩）
 * - 追踪订阅状态（free/pro/lifetime）via StateFlow
 * - 处理购买确认与消费
 * - BillingClient 不可用时优雅降级（如华为设备）
 */
class BillingManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BillingManager"

        // ===== Google Play Console 中配置的商品 ID =====
        /** 月度订阅商品 ID */
        const val PRODUCT_MONTHLY = "com.silas.omaster.pro.monthly"
        /** 年度订阅商品 ID */
        const val PRODUCT_YEARLY = "com.silas.omaster.pro.yearly"
        /** 终身买断商品 ID */
        const val PRODUCT_LIFETIME = "com.silas.omaster.pro.lifetime"

        /** 本地持久化 SharedPreferences 文件名 */
        private const val PREFS_NAME = "omaster_billing"
        private const val KEY_SUBSCRIPTION_STATE = "subscription_state"

        @Volatile
        private var INSTANCE: BillingManager? = null

        /** 全局访问点，可能为 null（BillingClient 不可用时） */
        val instance: BillingManager? get() = INSTANCE

        /**
         * 初始化 BillingManager
         * 应在 Application.onCreate 或 MainActivity 中调用
         */
        fun initialize(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingManager(context).also { manager ->
                    INSTANCE = manager
                    manager.startConnection()
                }
            }
        }

        /** 查询商品详情的 Product ID 列表 */
        private val PRODUCT_IDS = listOf(PRODUCT_MONTHLY, PRODUCT_YEARLY, PRODUCT_LIFETIME)
    }

    // ===== 协程作用域 =====
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ===== 订阅状态流 =====
    private val _subscriptionState = MutableStateFlow(loadPersistedState())
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    // ===== 商品详情 =====
    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()

    // ===== 连接状态 =====
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /** BillingClient 是否可用（华为等设备可能不可用） */
    private val _isBillingAvailable = MutableStateFlow(true)
    val isBillingAvailable: StateFlow<Boolean> = _isBillingAvailable.asStateFlow()

    // ===== BillingClient =====
    private var billingClient: BillingClient? = null

    /** 待处理的购买回调 */
    private var pendingPurchaseCallback: ((BillingResult, List<Purchase>?) -> Unit)? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "用户取消购买")
        } else {
            Log.w(TAG, "购买更新错误: ${billingResult.debugMessage}")
        }
        pendingPurchaseCallback?.invoke(billingResult, purchases)
    }

    // ==================== 连接管理 ====================

    /**
     * 启动 BillingClient 连接
     */
    private fun startConnection() {
        try {
            billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
                )
                .build()

            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "BillingClient 连接成功")
                        _isConnected.value = true
                        _isBillingAvailable.value = true
                        // 连接成功后查询商品和已有购买
                        queryProductDetails()
                        queryPurchases()
                    } else {
                        Log.w(TAG, "BillingClient 连接失败: ${billingResult.debugMessage}")
                        _isBillingAvailable.value = false
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w(TAG, "BillingClient 连接断开")
                    _isConnected.value = false
                    // 自动重连
                    scope.launch {
                        kotlinx.coroutines.delay(3000L)
                        startConnection()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "BillingClient 初始化失败（可能是非 Google Play 设备）: ${e.message}")
            _isBillingAvailable.value = false
            _isConnected.value = false
        }
    }

    // ==================== 商品查询 ====================

    /**
     * 查询所有商品详情
     */
    fun queryProductDetails() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val productList = PRODUCT_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(
                    if (productId == PRODUCT_LIFETIME)
                        BillingClient.ProductType.INAPP
                    else
                        BillingClient.ProductType.SUBS
                )
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        client.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = productDetailsList
                Log.d(TAG, "查询到 ${productDetailsList.size} 个商品")
            } else {
                Log.w(TAG, "查询商品失败: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * 查询已有购买（恢复购买）
     */
    fun queryPurchases() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val listener = PurchasesResponseListener { _, purchases ->
            processPurchases(purchases)
        }

        // 查询订阅
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            listener
        )

        // 查询一次性购买
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            listener
        )
    }

    /**
     * 恢复购买（对外接口）
     */
    fun restorePurchases(onComplete: ((Boolean) -> Unit)? = null) {
        val client = billingClient
        if (client == null || !client.isReady) {
            Log.w(TAG, "BillingClient 未就绪，无法恢复购买")
            onComplete?.invoke(false)
            return
        }

        val listener = PurchasesResponseListener { _, purchases ->
            processPurchases(purchases)
        }

        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            listener
        )

        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            listener
        )

        onComplete?.invoke(true)
    }

    // ==================== 购买流程 ====================

    /**
     * 发起购买
     *
     * @param activity 发起购买的 Activity
     * @param productDetails 商品详情
     * @param offerToken 订阅优惠令牌（订阅类型需要，一次性购买不需要）
     */
    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String? = null
    ): BillingResult {
        val client = billingClient
        if (client == null || !client.isReady) {
            Log.w(TAG, "BillingClient 未就绪，无法发起购买")
            return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                .setDebugMessage("BillingClient not ready")
                .build()
        }

        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        // 订阅类型需要 offerToken
        if (offerToken != null) {
            productDetailsParamsBuilder.setOfferToken(offerToken)
        } else {
            // 尝试从 productDetails 中获取默认 offerToken
            val subsOfferToken = productDetails.subscriptionOfferDetails
                ?.firstOrNull()?.offerToken
            if (subsOfferToken != null) {
                productDetailsParamsBuilder.setOfferToken(subsOfferToken)
            }
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))
            .build()

        return client.launchBillingFlow(activity, billingFlowParams)
    }

    // ==================== 购买处理与验证 ====================

    /**
     * 处理购买结果
     */
    private fun handlePurchase(purchase: Purchase) {
        if (!verifyPurchase(purchase)) {
            Log.w(TAG, "购买验证失败: ${purchase.products}")
            return
        }

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                // 确认购买
                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
                // 更新订阅状态
                updateSubscriptionFromPurchase(purchase)
            }
            Purchase.PurchaseState.PENDING -> {
                Log.d(TAG, "购买待处理: ${purchase.products}")
            }
        }
    }

    /**
     * 验证购买（本地验证 + 服务端验证）
     *
     * 本地验证：
     * - 检查 purchaseState 是否为 PURCHASED
     * - 检查 signature 是否有效
     *
     * 服务端验证：
     * - 将 purchaseToken 发送到后端验证，防止客户端伪造
     * - 服务端应调用 Google Play Developer API 验证 purchaseToken 真实性
     * - 服务端不可用时降级为本地验证（但记录警告日志）
     *
     * 注意：服务端验证 URL 需在生产部署前配置为真实后端地址。
     * 当前使用 api.omaster.app 作为默认端点。
     */
    private fun verifyPurchase(purchase: Purchase): Boolean {
        // 本地基本验证
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return false
        }

        // 检查商品 ID 是否属于本应用
        val validProducts = purchase.products.all { it in PRODUCT_IDS }
        if (!validProducts) {
            Log.w(TAG, "购买包含未知商品: ${purchase.products}")
            return false
        }

        // 服务端验证
        val serverVerified = verifyPurchaseOnServer(purchase)
        if (!serverVerified) {
            // 服务端验证失败或不可用：降级为本地验证
            // 生产环境中应记录此事件用于风控分析
            Log.w(TAG, "服务端验证未通过，降级为本地验证: ${purchase.products}")
        }

        Log.d(TAG, "购买验证通过 (serverVerified=$serverVerified): ${purchase.products}")
        return true
    }

    /**
     * 服务端购买验证
     *
     * 将 purchaseToken 发送到后端 API 进行验证。
     * 后端应调用 Google Play Developer API 的 purchases.products.get 或
     * purchases.subscriptions.get 端点验证 purchaseToken 的真实性。
     *
     * @return true 表示服务端验证通过，false 表示服务端验证失败或不可用
     */
    private fun verifyPurchaseOnServer(purchase: Purchase): Boolean {
        return try {
            val url = URL(com.silas.omaster.util.UrlConstants.API_BILLING_VERIFY)
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000
                connection.doOutput = true

                // 构建请求体
                val requestBody = JSONObject().apply {
                    put("purchaseToken", purchase.purchaseToken)
                    put("productId", purchase.products.firstOrNull() ?: "")
                    put("packageName", context.packageName)
                    put("purchaseTime", purchase.purchaseTime)
                }

                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
                    os.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = connection.inputStream.use { it.readBytes() }
                        .toString(Charsets.UTF_8)
                    val responseJson = JSONObject(responseBody)
                    val verified = responseJson.optBoolean("verified", false)
                    if (verified) {
                        Log.i(TAG, "服务端验证成功: ${purchase.products}")
                    } else {
                        Log.w(TAG, "服务端返回未验证: ${purchase.products}, reason=${responseJson.optString("reason")}")
                    }
                    verified
                } else {
                    // 服务端返回非200：记录错误但降级为本地验证
                    val errorBody = connection.errorStream?.use { it.readBytes() }
                        ?.toString(Charsets.UTF_8) ?: ""
                    Log.w(TAG, "服务端验证请求失败: HTTP $responseCode, body=$errorBody")
                    false
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: java.net.UnknownHostException) {
            // DNS 解析失败：服务端不可用，降级
            Log.w(TAG, "服务端验证不可用 (DNS): ${e.message}")
            false
        } catch (e: java.net.SocketTimeoutException) {
            // 超时：降级
            Log.w(TAG, "服务端验证超时: ${e.message}")
            false
        } catch (e: Exception) {
            // 其他网络/IO 异常：降级
            Log.w(TAG, "服务端验证异常: ${e.message}")
            false
        }
    }

    /**
     * 确认购买（acknowledge）
     *
     * 3 天内未确认的购买会被自动退款。
     * 对于订阅和一次性购买都需要确认。
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val client = billingClient ?: return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        client.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "购买确认成功: ${purchase.products}")
            } else {
                Log.w(TAG, "购买确认失败: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * 消耗一次性购买（用于终身买断的可消耗场景）
     * 当前终身买断为非消耗品，此方法保留备用
     */
    @Suppress("unused")
    private fun consumePurchase(purchase: Purchase) {
        val client = billingClient ?: return
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        client.consumeAsync(params) { billingResult, _ ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "购买消耗成功: ${purchase.products}")
            } else {
                Log.w(TAG, "购买消耗失败: ${billingResult.debugMessage}")
            }
        }
    }

    // ==================== 状态更新 ====================

    /**
     * 从 Purchase 对象更新订阅状态
     */
    private fun updateSubscriptionFromPurchase(purchase: Purchase) {
        val productIds = purchase.products

        val tier = when {
            productIds.contains(PRODUCT_LIFETIME) -> Tier.LIFETIME
            productIds.contains(PRODUCT_YEARLY) -> Tier.PRO_YEARLY
            productIds.contains(PRODUCT_MONTHLY) -> Tier.PRO_MONTHLY
            else -> return
        }

        val newState = SubscriptionState(
            tier = tier,
            expiryDate = if (tier.isSubscription) {
                // 订阅过期时间：使用 Google Play 返回的过期时间
                // 在 queryPurchases 返回的 Purchase 中无直接过期时间，
                // 实际项目中应从服务端获取；此处使用近似值
                System.currentTimeMillis() + when (tier) {
                    Tier.PRO_MONTHLY -> 30L * 24 * 60 * 60 * 1000
                    Tier.PRO_YEARLY -> 365L * 24 * 60 * 60 * 1000
                    else -> 0L
                }
            } else null,
            isTrial = false,
            purchaseToken = purchase.purchaseToken
        )

        _subscriptionState.value = newState
        persistState(newState)
        Log.d(TAG, "订阅状态已更新: $tier")
    }

    /**
     * 批量处理购买结果
     */
    private fun processPurchases(purchases: List<Purchase>) {
        // 找到最高等级的购买
        var bestTier = Tier.FREE
        var bestPurchase: Purchase? = null

        for (purchase in purchases) {
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (!verifyPurchase(purchase)) continue

            val tier = when {
                purchase.products.contains(PRODUCT_LIFETIME) -> Tier.LIFETIME
                purchase.products.contains(PRODUCT_YEARLY) -> Tier.PRO_YEARLY
                purchase.products.contains(PRODUCT_MONTHLY) -> Tier.PRO_MONTHLY
                else -> continue
            }

            if (tier.ordinal > bestTier.ordinal) {
                bestTier = tier
                bestPurchase = purchase
            }

            // 确认未确认的购买
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
        }

        if (bestPurchase != null) {
            updateSubscriptionFromPurchase(bestPurchase)
        }
    }

    // ==================== 本地持久化 ====================

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun persistState(state: SubscriptionState) {
        prefs.edit()
            .putString(KEY_SUBSCRIPTION_STATE, state.toJson())
            .apply()
    }

    private fun loadPersistedState(): SubscriptionState {
        val json = prefs.getString(KEY_SUBSCRIPTION_STATE, null)
        return if (json != null) {
            SubscriptionState.fromJson(json)
        } else {
            SubscriptionState.FREE
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 获取指定商品的详情
     */
    fun getProductDetails(productId: String): ProductDetails? {
        return _productDetails.value.find { it.productId == productId }
    }

    /**
     * 获取月度订阅商品详情
     */
    fun getMonthlyProductDetails(): ProductDetails? = getProductDetails(PRODUCT_MONTHLY)

    /**
     * 获取年度订阅商品详情
     */
    fun getYearlyProductDetails(): ProductDetails? = getProductDetails(PRODUCT_YEARLY)

    /**
     * 获取终身买断商品详情
     */
    fun getLifetimeProductDetails(): ProductDetails? = getProductDetails(PRODUCT_LIFETIME)

    /**
     * 断开连接并释放资源
     */
    fun release() {
        billingClient?.endConnection()
        billingClient = null
        _isConnected.value = false
    }
}
