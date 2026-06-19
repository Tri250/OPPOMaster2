package com.silas.omaster.data.model

import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.PresetComment
import com.silas.omaster.model.PresetDescription
import com.silas.omaster.model.PresetItem
import com.silas.omaster.model.PresetList
import com.silas.omaster.model.PresetSection
import org.junit.Assert.*
import org.junit.Test

/**
 * MasterPreset 及相关数据模型单元测试
 * 测试数据模型创建、验证、空值处理
 */
class MasterPresetTest {

    // ===== MasterPreset 创建验证 =====

    @Test
    fun `创建最小MasterPreset应该成功`() {
        val preset = MasterPreset(
            name = "Test Preset",
            coverPath = "https://example.com/cover.jpg"
        )
        assertEquals("Test Preset", preset.name)
        assertEquals("https://example.com/cover.jpg", preset.coverPath)
    }

    @Test
    fun `创建完整MasterPreset应该成功`() {
        val preset = MasterPreset(
            id = "test_preset_1",
            name = "Complete Preset",
            coverPath = "https://example.com/cover.jpg",
            galleryImages = listOf("img1.jpg", "img2.jpg"),
            author = "Test Author",
            mode = "pro",
            filter = "胶片",
            whiteBalance = "日光",
            colorTone = "暖调",
            exposureCompensation = "+0.7",
            colorTemperature = 5500,
            colorHue = 10,
            iso = "200",
            shutterSpeed = "1/125",
            softLight = "柔",
            tone = 5,
            saturation = 10,
            warmCool = -5,
            cyanMagenta = 3,
            sharpness = 80,
            vignette = "开",
            isFavorite = true,
            isCustom = false,
            isNew = true,
            description = PresetDescription("Title", "Content"),
            shootingTips = "建议在室外拍摄",
            sections = listOf(
                PresetSection("参数", listOf(PresetItem("ISO", "200")))
            ),
            tags = listOf("portrait", "warm"),
            brand = "oppo",
            version = 1,
            build = 2,
            params = mapOf("ISO" to "200"),
            colorGradingParams = mapOf("saturation" to "10"),
            createdAt = 1234567890L,
            downloads = 100,
            rating = 4.5f,
            ratingCount = 50,
            comments = listOf(
                PresetComment("1", "User", content = "Great!", rating = 5f)
            ),
            isHncs = true
        )

        assertEquals("test_preset_1", preset.id)
        assertEquals("Complete Preset", preset.name)
        assertEquals("https://example.com/cover.jpg", preset.coverPath)
        assertEquals(2, preset.galleryImages?.size)
        assertEquals("Test Author", preset.author)
        assertEquals("pro", preset.mode)
        assertEquals("胶片", preset.filter)
        assertEquals("日光", preset.whiteBalance)
        assertEquals("暖调", preset.colorTone)
        assertEquals("+0.7", preset.exposureCompensation)
        assertEquals(5500, preset.colorTemperature)
        assertEquals(10, preset.colorHue)
        assertEquals("200", preset.iso)
        assertEquals("1/125", preset.shutterSpeed)
        assertEquals("柔", preset.softLight)
        assertEquals(5, preset.tone)
        assertEquals(10, preset.saturation)
        assertEquals(-5, preset.warmCool)
        assertEquals(3, preset.cyanMagenta)
        assertEquals(80, preset.sharpness)
        assertEquals("开", preset.vignette)
        assertTrue(preset.isFavorite)
        assertFalse(preset.isCustom)
        assertTrue(preset.isNew)
        assertEquals("Title", preset.description?.title)
        assertEquals("Content", preset.description?.content)
        assertEquals("建议在室外拍摄", preset.shootingTips)
        assertEquals(1, preset.sections?.size)
        assertEquals(listOf("portrait", "warm"), preset.tags)
        assertEquals("oppo", preset.brand)
        assertEquals(1, preset.version)
        assertEquals(2, preset.build)
        assertEquals(mapOf("ISO" to "200"), preset.params)
        assertEquals(mapOf("saturation" to "10"), preset.colorGradingParams)
        assertEquals(1234567890L, preset.createdAt)
        assertEquals(100, preset.downloads)
        assertEquals(4.5f, preset.rating)
        assertEquals(50, preset.ratingCount)
        assertEquals(1, preset.comments?.size)
        assertTrue(preset.isHncs)
    }

    // ===== 默认值验证 =====

    @Test
    fun `默认author应为OPPO影像`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg"
        )
        assertEquals("@OPPO影像", preset.author)
    }

    @Test
    fun `默认isFavorite应为false`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg"
        )
        assertFalse(preset.isFavorite)
    }

    @Test
    fun `默认isCustom应为false`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg"
        )
        assertFalse(preset.isCustom)
    }

    @Test
    fun `默认isNew应为false`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg"
        )
        assertFalse(preset.isNew)
    }

    @Test
    fun `默认build应为1`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg"
        )
        assertEquals(1, preset.build)
    }

    @Test
    fun `默认createdAt应为0`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg"
        )
        assertEquals(0L, preset.createdAt)
    }

    @Test
    fun `默认tags应为空列表`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg"
        )
        assertEquals(emptyList<String>(), preset.tags)
    }

    @Test
    fun `默认isHncs应为false`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg"
        )
        assertFalse(preset.isHncs)
    }

    // ===== 可空字段验证 =====

    @Test
    fun `id可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            id = null
        )
        assertNull(preset.id)
    }

    @Test
    fun `galleryImages可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            galleryImages = null
        )
        assertNull(preset.galleryImages)
    }

    @Test
    fun `mode可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            mode = null
        )
        assertNull(preset.mode)
    }

    @Test
    fun `filter可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            filter = null
        )
        assertNull(preset.filter)
    }

    @Test
    fun `colorTemperature可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            colorTemperature = null
        )
        assertNull(preset.colorTemperature)
    }

    @Test
    fun `colorHue可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            colorHue = null
        )
        assertNull(preset.colorHue)
    }

    @Test
    fun `tone可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            tone = null
        )
        assertNull(preset.tone)
    }

    @Test
    fun `saturation可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            saturation = null
        )
        assertNull(preset.saturation)
    }

    @Test
    fun `warmCool可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            warmCool = null
        )
        assertNull(preset.warmCool)
    }

    @Test
    fun `cyanMagenta可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            cyanMagenta = null
        )
        assertNull(preset.cyanMagenta)
    }

    @Test
    fun `sharpness可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            sharpness = null
        )
        assertNull(preset.sharpness)
    }

    @Test
    fun `description可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            description = null
        )
        assertNull(preset.description)
    }

    @Test
    fun `sections可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            sections = null
        )
        assertNull(preset.sections)
    }

    @Test
    fun `params可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            params = null
        )
        assertNull(preset.params)
    }

    @Test
    fun `colorGradingParams可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            colorGradingParams = null
        )
        assertNull(preset.colorGradingParams)
    }

    @Test
    fun `downloads可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            downloads = null
        )
        assertNull(preset.downloads)
    }

    @Test
    fun `rating可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            rating = null
        )
        assertNull(preset.rating)
    }

    @Test
    fun `comments可为null`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            comments = null
        )
        assertNull(preset.comments)
    }

    // ===== allImages 属性验证 =====

    @Test
    fun `allImages在没有galleryImages时应只包含封面`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            galleryImages = null
        )
        assertEquals(1, preset.allImages.size)
        assertEquals("https://example.com/cover.jpg", preset.allImages[0])
    }

    @Test
    fun `allImages在galleryImages为空时应只包含封面`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            galleryImages = emptyList()
        )
        assertEquals(1, preset.allImages.size)
        assertEquals("https://example.com/cover.jpg", preset.allImages[0])
    }

    @Test
    fun `allImages在galleryImages非空时应包含封面和图库`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            galleryImages = listOf("img1.jpg", "img2.jpg")
        )
        assertEquals(3, preset.allImages.size)
        assertEquals("https://example.com/cover.jpg", preset.allImages[0])
        assertEquals("img1.jpg", preset.allImages[1])
        assertEquals("img2.jpg", preset.allImages[2])
    }

    // ===== PresetList 验证 =====

    @Test
    fun `创建空PresetList应该成功`() {
        val presetList = PresetList()
        assertNull(presetList.name)
        assertNull(presetList.author)
        assertEquals(1, presetList.build)
        assertEquals(1, presetList.version)
        assertTrue(presetList.presets.isEmpty())
    }

    @Test
    fun `创建带数据的PresetList应该成功`() {
        val presets = listOf(
            MasterPreset(name = "P1", coverPath = "c1.jpg"),
            MasterPreset(name = "P2", coverPath = "c2.jpg")
        )
        val presetList = PresetList(
            name = "My Presets",
            author = "Author",
            build = 5,
            version = 2,
            presets = presets
        )
        assertEquals("My Presets", presetList.name)
        assertEquals("Author", presetList.author)
        assertEquals(5, presetList.build)
        assertEquals(2, presetList.version)
        assertEquals(2, presetList.presets.size)
    }

    // ===== PresetItem 验证 =====

    @Test
    fun `创建PresetItem应该成功`() {
        val item = PresetItem("ISO", "200")
        assertEquals("ISO", item.label)
        assertEquals("200", item.value)
        assertEquals(1, item.span)
    }

    @Test
    fun `PresetItem默认span应为1`() {
        val item = PresetItem("ISO", "200")
        assertEquals(1, item.span)
    }

    @Test
    fun `PresetItem span=2应为全宽`() {
        val item = PresetItem("滤镜", "胶片", 2)
        assertEquals(2, item.span)
    }

    // ===== PresetSection 验证 =====

    @Test
    fun `创建PresetSection应该成功`() {
        val items = listOf(PresetItem("ISO", "200"), PresetItem("快门", "1/125"))
        val section = PresetSection("专业参数", items)
        assertEquals("专业参数", section.title)
        assertEquals(2, section.items.size)
    }

    @Test
    fun `PresetSection title可为null`() {
        val items = listOf(PresetItem("ISO", "200"))
        val section = PresetSection(null, items)
        assertNull(section.title)
        assertEquals(1, section.items.size)
    }

    // ===== PresetDescription 验证 =====

    @Test
    fun `创建PresetDescription应该成功`() {
        val desc = PresetDescription("标题", "详细内容")
        assertEquals("标题", desc.title)
        assertEquals("详细内容", desc.content)
    }

    // ===== PresetComment 验证 =====

    @Test
    fun `创建PresetComment应该成功`() {
        val comment = PresetComment(
            id = "1",
            user = "User",
            content = "Great preset!",
            rating = 5f
        )
        assertEquals("1", comment.id)
        assertEquals("User", comment.user)
        assertEquals("Great preset!", comment.content)
        assertEquals(5f, comment.rating)
    }

    @Test
    fun `PresetComment默认值应该正确`() {
        val comment = PresetComment(
            id = "1",
            user = "User",
            content = "Nice"
        )
        assertNull(comment.avatar)
        assertEquals(0f, comment.rating)
        assertEquals(0L, comment.timestamp)
        assertEquals(0, comment.likes)
    }

    @Test
    fun `PresetComment avatar可为null`() {
        val comment = PresetComment(
            id = "1",
            user = "User",
            content = "Nice",
            avatar = null
        )
        assertNull(comment.avatar)
    }

    // ===== 空参数处理 =====

    @Test
    fun `空section列表应正确处理`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            sections = emptyList()
        )
        assertNotNull(preset.sections)
        assertTrue(preset.sections!!.isEmpty())
    }

    @Test
    fun `空params映射应正确处理`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            params = emptyMap()
        )
        assertNotNull(preset.params)
        assertTrue(preset.params!!.isEmpty())
    }

    @Test
    fun `空colorGradingParams映射应正确处理`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            colorGradingParams = emptyMap()
        )
        assertNotNull(preset.colorGradingParams)
        assertTrue(preset.colorGradingParams!!.isEmpty())
    }

    // ===== 数据类相等性验证 =====

    @Test
    fun `相同字段的MasterPreset应该相等`() {
        val preset1 = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            saturation = 10
        )
        val preset2 = MasterPreset(
            name = "Test",
            coverPath = "https://example.com/cover.jpg",
            saturation = 10
        )
        assertEquals(preset1, preset2)
        assertEquals(preset1.hashCode(), preset2.hashCode())
    }

    @Test
    fun `不同字段的MasterPreset应该不相等`() {
        val preset1 = MasterPreset(
            name = "Test1",
            coverPath = "https://example.com/cover.jpg"
        )
        val preset2 = MasterPreset(
            name = "Test2",
            coverPath = "https://example.com/cover.jpg"
        )
        assertNotEquals(preset1, preset2)
    }

    @Test
    fun `相同字段的PresetItem应该相等`() {
        val item1 = PresetItem("ISO", "200")
        val item2 = PresetItem("ISO", "200")
        assertEquals(item1, item2)
    }

    @Test
    fun `相同字段的PresetSection应该相等`() {
        val section1 = PresetSection("参数", listOf(PresetItem("ISO", "200")))
        val section2 = PresetSection("参数", listOf(PresetItem("ISO", "200")))
        assertEquals(section1, section2)
    }

    @Test
    fun `相同字段的PresetDescription应该相等`() {
        val desc1 = PresetDescription("标题", "内容")
        val desc2 = PresetDescription("标题", "内容")
        assertEquals(desc1, desc2)
    }

    @Test
    fun `相同字段的PresetComment应该相等`() {
        val comment1 = PresetComment("1", "User", content = "Nice", rating = 5f)
        val comment2 = PresetComment("1", "User", content = "Nice", rating = 5f)
        assertEquals(comment1, comment2)
    }
}