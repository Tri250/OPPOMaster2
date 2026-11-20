package com.silas.omaster.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ColorWalkManager 测试（Robolectric）：
 * 每日抽取上限、跨天重置、清除色卡不恢复抽取次数（修复回归）。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ColorWalkManagerTest {

    private lateinit var context: Context
    private lateinit var manager: ColorWalkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("color_walk_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        manager = ColorWalkManager(context)
    }

    @Test
    fun `每日最多抽取三次`() {
        assertEquals(0, manager.getDrawCount())
        assertEquals(3, manager.getRemainingDraws())
        assertTrue(manager.canDrawMore())

        repeat(3) { assertNotNull(manager.drawTodayCard()) }

        assertEquals(3, manager.getDrawCount())
        assertEquals(0, manager.getRemainingDraws())
        assertFalse(manager.canDrawMore())
        // 第四次抽卡被拒（不消耗次数、不改变状态）
        assertNull(manager.drawTodayCard())
        assertEquals(3, manager.getDrawCount())
    }

    @Test
    fun `清除今日色卡不恢复抽取次数`() {
        // 用满 3 次后清除色卡：不应重新获得抽取机会
        repeat(3) { manager.drawTodayCard() }
        assertFalse(manager.canDrawMore())
        assertTrue(manager.hasDrawnToday())

        manager.clearTodayCard()

        assertNull(manager.todayCardFlow.value)
        assertFalse(manager.hasDrawnToday())
        // 修复点：清除不能把当日次数清零（否则可"清除→抽取"无限刷次数）
        assertEquals(3, manager.getDrawCount())
        assertFalse(manager.canDrawMore())
        assertNull(manager.drawTodayCard())
    }

    @Test
    fun `抽卡结果写入历史记录`() {
        val card = manager.drawTodayCard()
        assertNotNull(card)
        assertTrue(manager.historyFlow.value.any { it.cardId == card!!.id })
    }
}
