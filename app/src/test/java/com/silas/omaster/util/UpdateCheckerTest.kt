package com.silas.omaster.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 提醒热更新判定测试（【v1.5.6】FORCE_UPDATE_MIN 基线 = v1.5.6）
 *
 * 版本码映射（VersionInfo.parseVersionCode = major*10000 + minor*100 + patch）：
 * v1.3.4  = 1*10000 + 3*100 +  4 = 10304
 * v1.5.1  = 1*10000 + 5*100 +  1 = 10501
 * v1.5.5  = 1*10000 + 5*100 +  5 = 10505
 * v1.5.6  = 1*10000 + 5*100 +  6 = 10506
 * v1.6.0  = 1*10000 + 6*100 +  0 = 10600
 */
class UpdateCheckerTest {

    @Test
    fun `低于基线的存量用户收到强更提醒`() {
        // 核心场景：v1.5.5 存量用户（10505）< 远端 v1.5.6（10506）→ 强制提醒
        assertTrue(UpdateChecker.shouldForceUpdate(10506, 10505))
        // 更旧版本同样命中：v1.4.5 → v1.5.6
        assertTrue(UpdateChecker.shouldForceUpdate(10506, 10405))
        assertTrue(UpdateChecker.shouldForceUpdate(10506, 10304))
    }

    @Test
    fun `基线以上版本走普通软提醒不锁定用户`() {
        // 远端 v1.6.0（10600）> 本地 v1.5.6（10506）：已达基线，普通提醒
        assertFalse(UpdateChecker.shouldForceUpdate(10600, 10506))
        // v1.5.7（10507）> v1.5.6（10506）：同为基线以上软提醒
        assertFalse(UpdateChecker.shouldForceUpdate(10507, 10506))
    }

    @Test
    fun `远端不高于本地时不强更`() {
        // 已是最新：v1.5.6 == v1.5.6
        assertFalse(UpdateChecker.shouldForceUpdate(10506, 10506))
        // 远端更旧（渠道回退拿到旧版本）：不提醒
        assertFalse(UpdateChecker.shouldForceUpdate(10505, 10506))
    }

    @Test
    fun `存量低版本用户远端有更新即强更_无论远端是否达到原基线`() {
        // 远端 v1.5.5（10505）> 本地 v1.5.1（10501）：本地低于基线（存量），
        // 只要有可用更新就提醒（v1.5.6 的管线修复对更旧版本同样关键）
        assertTrue(UpdateChecker.shouldForceUpdate(10505, 10501))
    }

    @Test
    fun `基线标记从Release正文下发解析`() {
        // 【v1.5.6 服务端下发】HTML 注释形式（GitHub 渲染不可见）
        assertEquals(
            "1.5.6",
            UpdateChecker.parseForceUpdateBaseline("<!-- force-update-baseline: 1.5.6 -->")
        )
        // 裸文本形式 + v 前缀
        assertEquals("1.6.0", UpdateChecker.parseForceUpdateBaseline("force-update-baseline: v1.6.0"))
        // 大小写不敏感
        assertEquals("1.5.6", UpdateChecker.parseForceUpdateBaseline("Force-Update-Baseline: 1.5.6"))
        // 无标记 / 空正文 → 回退客户端默认
        assertEquals("1.5.6", UpdateChecker.parseForceUpdateBaseline("## v1.5.6\n\n- 修复若干问题"))
        assertEquals("1.5.6", UpdateChecker.parseForceUpdateBaseline(null))
        // 服务端关闭提醒：下调基线至极低版本（本地 < 基线恒不成立 → 永不命中）
        assertEquals("0.0.0", UpdateChecker.parseForceUpdateBaseline("<!-- force-update-baseline: 0.0.0 -->"))
    }

    @Test
    fun `下发基线参与判定`() {
        // 服务端下调基线到 1.5.5：v1.5.5 > 本地 v1.5.1 → 命中（客户端默认时不命中）
        assertTrue(UpdateChecker.shouldForceUpdate(10505, 10501, "1.5.5"))
        // 服务端上调基线到 1.6.0：扩大覆盖面——v1.5.5 存量用户也纳入提醒
        assertTrue(UpdateChecker.shouldForceUpdate(10506, 10505, "1.6.0"))
    }

    @Test
    fun `update-sha256标记从Release正文下发解析`() {
        // 【P0-3 更新链路加固】HTML 注释形式（GitHub 渲染不可见，与 force-update-baseline 同款通道）
        val hex64 = "a1b2c3d4".repeat(8) // 64 位十六进制
        assertEquals(hex64, UpdateChecker.parseUpdateSha256("<!-- update-sha256: $hex64 -->"))
        // 裸文本形式 + 归一小写
        assertEquals(hex64, UpdateChecker.parseUpdateSha256("Update-SHA256: ${hex64.uppercase()}"))
        // 多行正文中的标记行
        assertEquals(hex64, UpdateChecker.parseUpdateSha256("## v1.6.0\n\n<!-- update-sha256: $hex64 -->\n\n- 修复"))
        // 无标记 / 空正文 → null（跳过哈希校验，签名一致性校验仍强制执行）
        assertEquals(null, UpdateChecker.parseUpdateSha256("## v1.6.0\n\n- 常规更新"))
        assertEquals(null, UpdateChecker.parseUpdateSha256(null))
        // 非 64 位十六进制（截断/非法字符）不匹配 → null
        assertEquals(null, UpdateChecker.parseUpdateSha256("update-sha256: deadbeef"))
        assertEquals(null, UpdateChecker.parseUpdateSha256("update-sha256: ${"zz".repeat(32)}"))
    }
}
