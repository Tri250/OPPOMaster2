package com.silas.omaster.billing

import kotlinx.serialization.Serializable

/**
 * 订阅等级
 */
@Serializable
enum class Tier {
    /** 免费用户 */
    FREE,
    /** Pro 月度订阅 */
    PRO_MONTHLY,
    /** Pro 年度订阅 */
    PRO_YEARLY,
    /** 终身买断 */
    LIFETIME;

    /** 是否为付费等级 */
    val isPaid: Boolean get() = this != FREE

    /** 是否为订阅类型（非买断） */
    val isSubscription: Boolean get() = this == PRO_MONTHLY || this == PRO_YEARLY
}

/**
 * 订阅状态数据类
 *
 * @param tier 当前等级
 * @param expiryDate 过期时间戳（毫秒），Lifetime 和 Free 时为 null
 * @param isTrial 是否为试用期
 * @param purchaseToken Google Play 购买令牌，用于服务端验证
 */
@Serializable
data class SubscriptionState(
    val tier: Tier = Tier.FREE,
    val expiryDate: Long? = null,
    val isTrial: Boolean = false,
    val purchaseToken: String? = null
) {
    /** 是否为付费用户（Pro 或 Lifetime） */
    val isPro: Boolean get() = tier.isPaid

    /** 订阅是否已过期（仅对订阅类型有效） */
    val isExpired: Boolean
        get() = if (tier.isSubscription && expiryDate != null) {
            System.currentTimeMillis() > expiryDate
        } else {
            false
        }

    /** 有效的付费状态（付费且未过期） */
    val isActivePro: Boolean get() = isPro && !isExpired

    /** 序列化为 JSON 用于本地持久化 */
    fun toJson(): String {
        return kotlinx.serialization.json.Json.encodeToString(serializer(), this)
    }

    companion object {
        /** 默认免费状态 */
        val FREE = SubscriptionState(tier = Tier.FREE)

        /** 从本地持久化数据反序列化 */
        fun fromJson(json: String): SubscriptionState {
            return try {
                kotlinx.serialization.json.Json.decodeFromString(serializer(), json)
            } catch (_: Exception) {
                FREE
            }
        }
    }
}
