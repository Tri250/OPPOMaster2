package com.silas.omaster.util

import org.junit.Assert.assertEquals
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

    @Test
    fun `基线标记从Release正文下发解析`() {
        // 【v1.8.10 服务端下发】HTML 注释形式（GitHub 渲染不可见）
        assertEquals(
            "1.8.10",
            UpdateChecker.parseForceUpdateBaseline("<!-- force-update-baseline: 1.8.10 -->")
        )
        // 裸文本形式 + v 前缀
        assertEquals("1.9.0", UpdateChecker.parseForceUpdateBaseline("force-update-baseline: v1.9.0"))
        // 大小写不敏感
        assertEquals("1.8.10", UpdateChecker.parseForceUpdateBaseline("Force-Update-Baseline: 1.8.10"))
        // 无标记 / 空正文 → 回退客户端默认
        assertEquals("1.8.10", UpdateChecker.parseForceUpdateBaseline("## v1.8.10\n\n- 修复若干问题"))
        assertEquals("1.8.10", UpdateChecker.parseForceUpdateBaseline(null))
        // 服务端关闭提醒：写超高基线
        assertEquals("99.0.0", UpdateChecker.parseForceUpdateBaseline("<!-- force-update-baseline: 99.0.0 -->"))
    }

    @Test
    fun `下发基线参与判定`() {
        // 服务端下调基线到 1.8.9：v1.8.9 > 本地 v1.8.5 → 命中（客户端默认时不命中）
        assertTrue(UpdateChecker.shouldForceUpdate(1809, 1805, "1.8.9"))
        // 服务端上调基线到 1.9.0：远端 v1.8.10 不再命中（等效关闭提醒）
        assertFalse(UpdateChecker.shouldForceUpdate(1810, 1809, "1.9.0"))
    }
}
