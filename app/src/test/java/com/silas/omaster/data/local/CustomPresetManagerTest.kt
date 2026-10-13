package com.silas.omaster.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silas.omaster.model.MasterPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * ED-03 持久层测试（Robolectric，无需真机）：
 * 自定义预设库的增 / 改 / 删 / 查与 SharedPreferences 持久化回读。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CustomPresetManagerTest {

    private lateinit var context: Context
    private lateinit var manager: CustomPresetManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("omaster_custom_presets", Context.MODE_PRIVATE)
            .edit().clear().commit()
        manager = CustomPresetManager(context)
    }

    private fun preset(id: String? = null, name: String = "测试预设") =
        MasterPreset(id = id, name = name, coverPath = "", isCustom = true)

    @Test
    fun `新增自定义预设自动生成 id 并置顶`() {
        manager.addCustomPreset(preset(name = "A"))
        manager.addCustomPreset(preset(name = "B"))

        val presets = manager.getCustomPresets()
        assertEquals(2, presets.size)
        // 最新的在前面
        assertEquals("B", presets[0].name)
        assertNotNull(presets[0].id)
        assertTrue(presets.all { it.isCustom })
    }

    @Test
    fun `重新实例化后数据仍可读回 持久化生效`() {
        manager.addCustomPreset(preset(name = "持久化"))
        val reloaded = CustomPresetManager(context)

        val presets = reloaded.getCustomPresets()
        assertEquals(1, presets.size)
        assertEquals("持久化", presets[0].name)
    }

    @Test
    fun `更新预设保留原 id`() {
        manager.addCustomPreset(preset(id = "t1", name = "原始"))
        val original = manager.getPresetById("t1")!!
        manager.updateCustomPreset(original.copy(name = "修改后"))

        val updated = manager.getPresetById("t1")!!
        assertEquals("修改后", updated.name)
        assertEquals("t1", updated.id)
    }

    @Test
    fun `删除预设后不再可查`() {
        manager.addCustomPreset(preset(id = "t1"))
        manager.deleteCustomPreset(context, "t1")

        assertEquals(0, manager.getCustomPresets().size)
        assertNull(manager.getPresetById("t1"))
    }

    @Test
    fun `查询不存在的 id 返回 null`() {
        assertNull(manager.getPresetById("不存在"))
    }

    @Test
    fun `兼容 Gson 时期写入的旧格式数据`() {
        // ED-04：升级用户的数据由 Gson 写入（可能缺字段 / 带显式 null），必须可读回
        context.getSharedPreferences("omaster_custom_presets", Context.MODE_PRIVATE)
            .edit()
            .putString(
                "custom_presets",
                """[{"id":"g1","name":"旧数据","coverPath":"","isCustom":true,"galleryImages":null}]"""
            )
            .commit()
        val reloaded = CustomPresetManager(context)

        val preset = reloaded.getPresetById("g1")
        assertNotNull(preset)
        assertEquals("旧数据", preset!!.name)
        assertTrue(preset.isCustom)
    }
}
