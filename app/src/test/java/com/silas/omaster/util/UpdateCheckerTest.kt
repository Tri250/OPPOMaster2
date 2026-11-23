package com.silas.omaster.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 提醒热更新判定测试（【v1.8.10】FORCE_UPDATE_MIN 基线 = v1.8.10）
 *
 * 版本码映射（VersionInfo.parseVersionCode）：
 * v1.8.9  = 1*10000 +  8*100 +  9 = 1809
 * v1.8.10 = 1*10000 +  8*100 + 10 = 1810
 * v1.9.0  = 1*10000 +  9*100 +  0 = 1900
 */
class UpdateCheckerTest {

    @Test
    fun `低于基线的存量用户收到强更提醒`() {
        // 核心场景：v1.8.9 存量用户（1809）< 远端 v1.8.10（1810）→ 强制提醒
        assertTrue(UpdateChecker.shouldForceUpdate(1810, 1809))
        // 更旧版本同样命中：v1.7.x → v1.8.10
        assertTrue(UpdateChecker.shouldForceUpdate(1810, 1700))
        assertTrue(UpdateChecker.shouldForceUpdate(1810, 604))
    }

    @Test
    fun `基线以上版本走普通软提醒不锁定用户`() {
        // 远端 v1.9.0（1900）> 本地 v1.8.10（1810）：已达基线，普通提醒
        assertFalse(UpdateChecker.shouldForceUpdate(1900, 1810))
        // v1.8.11（1811）> v1.8.10（1810）：同为基线以上软提醒
        assertFalse(UpdateChecker.shouldForceUpdate(1811, 1810))
    }

    @Test
    fun `远端不高于本地时不强更`() {
        // 已是最新：v1.8.10 == v1.8.10
        assertFalse(UpdateChecker.shouldForceUpdate(1810, 1810))
        // 远端更旧（渠道回退拿到旧版本）：不提醒
        assertFalse(UpdateChecker.shouldForceUpdate(1809, 1810))
    }

    @Test
    fun `远端低于基线时即使更高也不强更`() {
        // 远端 v1.8.9（1809）> 本地 v1.8.5（1805）：有更新但低于基线，软提醒
        assertFalse(UpdateChecker.shouldForceUpdate(1809, 1805))
    }
}
