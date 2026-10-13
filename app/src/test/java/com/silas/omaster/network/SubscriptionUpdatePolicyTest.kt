package com.silas.omaster.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ED-03 测试基线：订阅更新判定（SubscriptionUpdatePolicy）。
 */
class SubscriptionUpdatePolicyTest {

    @Test
    fun `本地无该订阅时不跳过`() {
        assertFalse(
            SubscriptionUpdatePolicy.shouldSkipUpdate(
                remoteBuild = 3, localBuild = null, forceUpdate = false
            )
        )
    }

    @Test
    fun `远端 build 更新时不跳过`() {
        assertFalse(
            SubscriptionUpdatePolicy.shouldSkipUpdate(
                remoteBuild = 4, localBuild = 3, forceUpdate = false
            )
        )
    }

    @Test
    fun `同 build 时跳过避免无效覆盖`() {
        assertTrue(
            SubscriptionUpdatePolicy.shouldSkipUpdate(
                remoteBuild = 3, localBuild = 3, forceUpdate = false
            )
        )
    }

    @Test
    fun `强制更新时永不跳过`() {
        assertFalse(
            SubscriptionUpdatePolicy.shouldSkipUpdate(
                remoteBuild = 3, localBuild = 3, forceUpdate = true
            )
        )
    }

    @Test
    fun `远端 build 回退时不跳过 本地数据保护由写入层负责`() {
        assertFalse(
            SubscriptionUpdatePolicy.shouldSkipUpdate(
                remoteBuild = 2, localBuild = 3, forceUpdate = false
            )
        )
    }
}
