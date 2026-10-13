package com.silas.omaster.network

/**
 * 订阅更新判定（纯函数，供 ED-03 单测覆盖）。
 *
 * 规则：
 * - 强制更新时永不跳过；
 * - 本地无该订阅（localBuild == null）时需要拉取；
 * - 本地 build 与远端一致时跳过，避免无效覆盖；
 * - 远端 build 高于或低于本地时均视为需要同步（远端回退场景的本地数据保护由
 *   「最后已知良好」策略在写入层保障，属 FD-02③ 范围）。
 */
object SubscriptionUpdatePolicy {

    /**
     * @param remoteBuild 远端订阅 JSON 的 build 号
     * @param localBuild  本地记录的订阅 build 号；null 表示本地无该订阅
     * @param forceUpdate 用户手动强制刷新
     * @return true 表示跳过本次同步
     */
    fun shouldSkipUpdate(remoteBuild: Int, localBuild: Int?, forceUpdate: Boolean): Boolean {
        if (forceUpdate) return false
        return localBuild != null && localBuild == remoteBuild
    }
}
