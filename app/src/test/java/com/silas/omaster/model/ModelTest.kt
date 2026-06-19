package com.silas.omaster.model

import org.junit.Test
import org.junit.Assert.*

/**
 * MasterPreset 单元测试
 * 测试预设数据模型
 */
class MasterPresetTest {

    @Test
    fun `预设创建 - 应该正确创建预设对象`() {
        val preset = MasterPreset(
            id = "test_001",
            name = "Test Preset",
            coverPath = "cover.jpg",
            author = "Test Author"
        )
        
        assertEquals("test_001", preset.id)
        assertEquals("Test Preset", preset.name)
        assertEquals("cover.jpg", preset.coverPath)
        assertEquals("Test Author", preset.author)
    }

    @Test
    fun `预设默认值 - 应该有正确的默认值`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "cover.jpg"
        )
        
        assertNull(preset.id)
        assertEquals("@OPPO影像", preset.author)
        assertFalse(preset.isFavorite)
        assertFalse(preset.isCustom)
        assertFalse(preset.isNew)
        assertFalse(preset.isHncs)
    }

    @Test
    fun `预设复制 - 应该正确复制预设`() {
        val original = MasterPreset(
            id = "001",
            name = "Original",
            coverPath = "cover.jpg",
            saturation = 10
        )
        
        val copy = original.copy(name = "Copied")
        
        assertEquals("001", copy.id)
        assertEquals("Copied", copy.name)
        assertEquals(10, copy.saturation)
    }

    @Test
    fun `预设参数范围 - 饱和度应该在有效范围内`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "cover.jpg",
            saturation = 15
        )
        
        assertTrue("饱和度应该在-100到100之间", preset.saturation!! in -100..100)
    }

    @Test
    fun `预设参数范围 - 影调应该在有效范围内`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "cover.jpg",
            tone = -10
        )
        
        assertTrue("影调应该在-100到100之间", preset.tone!! in -100..100)
    }

    @Test
    fun `预设参数范围 - 色温应该在有效范围内`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "cover.jpg",
            colorTemperature = 5500
        )
        
        assertTrue("色温应该在2000到8000之间", preset.colorTemperature!! in 2000..8000)
    }

    @Test
    fun `图库图片 - allImages应该包含封面和图库`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "cover.jpg",
            galleryImages = listOf("img1.jpg", "img2.jpg")
        )
        
        val allImages = preset.allImages
        
        assertEquals(3, allImages.size)
        assertEquals("cover.jpg", allImages[0])
        assertEquals("img1.jpg", allImages[1])
        assertEquals("img2.jpg", allImages[2])
    }

    @Test
    fun `图库图片 - 无图库时应该只返回封面`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "cover.jpg"
        )
        
        val allImages = preset.allImages
        
        assertEquals(1, allImages.size)
        assertEquals("cover.jpg", allImages[0])
    }

    @Test
    fun `HNCS认证 - 应该正确标记HNCS预设`() {
        val hncsPreset = MasterPreset(
            name = "HNCS Preset",
            coverPath = "cover.jpg",
            isHncs = true
        )
        
        assertTrue(hncsPreset.isHncs)
        
        val normalPreset = MasterPreset(
            name = "Normal Preset",
            coverPath = "cover.jpg"
        )
        
        assertFalse(normalPreset.isHncs)
    }

    @Test
    fun `社区数据 - 应该正确设置社区数据`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "cover.jpg",
            downloads = 1000,
            rating = 4.5f,
            ratingCount = 100
        )
        
        assertEquals(1000, preset.downloads)
        assertEquals(4.5f, preset.rating!!)
        assertEquals(100, preset.ratingCount)
    }

    @Test
    fun `标签 - 应该正确设置标签`() {
        val preset = MasterPreset(
            name = "Test",
            coverPath = "cover.jpg",
            tags = listOf("portrait", "skin", "natural")
        )
        
        assertEquals(3, preset.tags!!.size)
        assertTrue(preset.tags!!.contains("portrait"))
    }
}

/**
 * SceneProfile 单元测试
 * 测试场景画像数据模型
 */
class SceneProfileTest {

    @Test
    fun `场景创建 - 应该正确创建场景对象`() {
        val profile = SceneProfile(
            id = "portrait-indoor",
            name = "室内人像",
            category = SceneCategory.PORTRAIT,
            description = "室内环境人像拍摄",
            color = 0xFFFF6B35,
            confidence = 0.85f,
            hasselbladParams = HasselbladParams(),
            recommendedFilm = emptyList(),
            masterTips = listOf("使用柔和光线")
        )
        
        assertEquals("portrait-indoor", profile.id)
        assertEquals("室内人像", profile.name)
        assertEquals(SceneCategory.PORTRAIT, profile.category)
        assertEquals(0.85f, profile.confidence)
    }

    @Test
    fun `场景分类 - 应该有正确的显示名称`() {
        assertEquals("人像", SceneCategory.PORTRAIT.displayName)
        assertEquals("风景", SceneCategory.LANDSCAPE.displayName)
        assertEquals("夜景", SceneCategory.NIGHT.displayName)
        assertEquals("美食", SceneCategory.FOOD.displayName)
        assertEquals("城市", SceneCategory.URBAN.displayName)
        assertEquals("静物", SceneCategory.STILL_LIFE.displayName)
        assertEquals("微距", SceneCategory.MACRO.displayName)
        assertEquals("活动", SceneCategory.EVENT.displayName)
    }

    @Test
    fun `场景分类 - 应该有正确的图标`() {
        assertEquals("👤", SceneCategory.PORTRAIT.icon)
        assertEquals("🏔️", SceneCategory.LANDSCAPE.icon)
        assertEquals("🌃", SceneCategory.NIGHT.icon)
    }

    @Test
    fun `场景分类 - 应该有正确的颜色`() {
        assertEquals(0xFFFF6B35.toLong(), SceneCategory.PORTRAIT.color) // 哈苏橙
        assertEquals(0xFF4CAF50.toLong(), SceneCategory.LANDSCAPE.color) // 自然绿
        assertEquals(0xFF2196F3.toLong(), SceneCategory.NIGHT.color) // 夜空蓝
    }
}

/**
 * HasselbladParams 单元测试
 * 测试哈苏大师参数
 */
class HasselbladParamsTest {

    @Test
    fun `参数默认值 - 应该全部为0`() {
        val params = HasselbladParams()
        
        assertEquals(0, params.tone)
        assertEquals(0, params.saturation)
        assertEquals(0, params.contrast)
        assertEquals(0, params.colorTemp)
        assertEquals(0, params.sharpness)
        assertEquals(0, params.vignette)
        assertEquals(0, params.cyanMagenta)
        assertEquals(SoftLightMode.NONE, params.softLight)
    }

    @Test
    fun `参数范围 - 所有参数应该在有效范围内`() {
        val params = HasselbladParams(
            tone = -15,
            saturation = 10,
            contrast = 5,
            colorTemp = -10,
            sharpness = 20,
            vignette = 15,
            cyanMagenta = -5
        )
        
        assertTrue(params.tone in -30..30)
        assertTrue(params.saturation in -30..30)
        assertTrue(params.contrast in -30..30)
        assertTrue(params.colorTemp in -30..30)
        assertTrue(params.sharpness in -30..30)
        assertTrue(params.vignette in -30..30)
        assertTrue(params.cyanMagenta in -30..30)
    }

    @Test
    fun `参数格式化 - 正值应该带加号`() {
        val params = HasselbladParams(saturation = 10)
        assertEquals("+10", params.formatParamValue(10))
    }

    @Test
    fun `参数格式化 - 负值应该保持负号`() {
        val params = HasselbladParams()
        assertEquals("-10", params.formatParamValue(-10))
    }

    @Test
    fun `参数格式化 - 零应该不带符号`() {
        val params = HasselbladParams()
        assertEquals("0", params.formatParamValue(0))
    }

    @Test
    fun `柔光模式 - 应该有正确的显示名称`() {
        assertEquals("无", SoftLightMode.NONE.displayName)
        assertEquals("柔", SoftLightMode.SOFT.displayName)
        assertEquals("梦幻", SoftLightMode.DREAMY.displayName)
    }
}

/**
 * FilmPreset 单元测试
 * 测试胶片预设
 */
class FilmPresetTest {

    @Test
    fun `胶片创建 - 应该正确创建胶片对象`() {
        val film = FilmPreset(
            id = "portra",
            name = "Portra 400",
            series = FilmSeries.EMOTION,
            matchScore = 0.9f,
            description = "柔和肤色，自然色彩"
        )
        
        assertEquals("portra", film.id)
        assertEquals("Portra 400", film.name)
        assertEquals(FilmSeries.EMOTION, film.series)
        assertEquals(0.9f, film.matchScore)
    }

    @Test
    fun `胶片系列 - 应该有正确的显示名称`() {
        assertEquals("原生经典", FilmSeries.CLASSIC.displayName)
        assertEquals("情绪与表达", FilmSeries.EMOTION.displayName)
        assertEquals("结构与时间", FilmSeries.STRUCTURE.displayName)
        assertEquals("数字记忆", FilmSeries.DIGITAL.displayName)
    }

    @Test
    fun `胶片系列 - 应该包含正确的胶片`() {
        assertTrue(FilmSeries.CLASSIC.films.contains("cc"))
        assertTrue(FilmSeries.CLASSIC.films.contains("nc"))
        assertTrue(FilmSeries.CLASSIC.films.contains("nh"))
        
        assertTrue(FilmSeries.EMOTION.films.contains("portra"))
        assertTrue(FilmSeries.EMOTION.films.contains("rdp3"))
        
        assertTrue(FilmSeries.STRUCTURE.films.contains("800t"))
        assertTrue(FilmSeries.STRUCTURE.films.contains("tx400"))
        
        assertTrue(FilmSeries.DIGITAL.films.contains("ccd_cool"))
        assertTrue(FilmSeries.DIGITAL.films.contains("ccd_warm"))
    }

    @Test
    fun `匹配分数 - 应该在有效范围内`() {
        val films = listOf(
            FilmPreset("cc", "CC", FilmSeries.CLASSIC, 0.85f),
            FilmPreset("nc", "NC", FilmSeries.CLASSIC, 0.8f),
            FilmPreset("portra", "Portra", FilmSeries.EMOTION, 0.9f)
        )
        
        for (film in films) {
            assertTrue("匹配分数应该在0到1之间", film.matchScore in 0.0f..1.0f)
        }
    }
}

/**
 * Subscription 单元测试
 * 测试订阅数据模型
 */
class SubscriptionTest {

    @Test
    fun `订阅创建 - 应该正确创建订阅对象`() {
        val subscription = Subscription(
            url = "https://example.com/presets.json",
            name = "Official Presets",
            author = "OMaster",
            isEnabled = true,
            presetCount = 50
        )
        
        assertEquals("https://example.com/presets.json", subscription.url)
        assertEquals("Official Presets", subscription.name)
        assertEquals("OMaster", subscription.author)
        assertTrue(subscription.isEnabled)
        assertEquals(50, subscription.presetCount)
    }

    @Test
    fun `订阅默认值 - 应该有正确的默认值`() {
        val subscription = Subscription(
            url = "https://example.com/presets.json"
        )
        
        assertEquals("", subscription.name)
        assertEquals("", subscription.author)
        assertTrue(subscription.isEnabled)
        assertEquals(0, subscription.presetCount)
        assertEquals(0L, subscription.lastUpdateTime)
    }

    @Test
    fun `订阅状态 - 应该正确切换启用状态`() {
        val subscription = Subscription(
            url = "https://example.com/presets.json",
            isEnabled = true
        )
        
        assertTrue(subscription.isEnabled)
        
        val disabled = subscription.copy(isEnabled = false)
        assertFalse(disabled.isEnabled)
    }

    @Test
    fun `订阅列表 - 应该正确创建订阅列表`() {
        val list = SubscriptionList(
            subscriptions = listOf(
                Subscription(url = "https://example1.com/presets.json"),
                Subscription(url = "https://example2.com/presets.json")
            )
        )
        
        assertEquals(2, list.subscriptions.size)
    }
}

/**
 * PresetComment 单元测试
 * 测试预设评论
 */
class PresetCommentTest {

    @Test
    fun `评论创建 - 应该正确创建评论对象`() {
        val comment = PresetComment(
            id = "comment_001",
            user = "User1",
            content = "Great preset!",
            rating = 5.0f,
            timestamp = System.currentTimeMillis()
        )
        
        assertEquals("comment_001", comment.id)
        assertEquals("User1", comment.user)
        assertEquals("Great preset!", comment.content)
        assertEquals(5.0f, comment.rating)
    }

    @Test
    fun `评论评分 - 应该在有效范围内`() {
        val comment = PresetComment(
            id = "comment_001",
            user = "User1",
            content = "Good",
            rating = 4.5f
        )
        
        assertTrue(comment.rating in 0.0f..5.0f)
    }
}

/**
 * PresetDescription 单元测试
 */
class PresetDescriptionTest {

    @Test
    fun `描述创建 - 应该正确创建描述对象`() {
        val description = PresetDescription(
            title = "拍摄场景",
            content = "适合室内人像拍摄"
        )
        
        assertEquals("拍摄场景", description.title)
        assertEquals("适合室内人像拍摄", description.content)
    }
}

/**
 * CameraParams 单元测试
 */
class CameraParamsTest {

    @Test
    fun `相机参数创建 - 应该正确创建参数对象`() {
        val params = CameraParams(
            iso = 400,
            shutterSpeed = "1/125",
            aperture = 2.8f,
            focalLength = 50f,
            whiteBalance = "Auto"
        )
        
        assertEquals(400, params.iso)
        assertEquals("1/125", params.shutterSpeed)
        assertEquals(2.8f, params.aperture!!)
        assertEquals(50f, params.focalLength!!)
        assertEquals("Auto", params.whiteBalance)
    }
}

/**
 * ExifData 单元测试
 */
class ExifDataTest {

    @Test
    fun `EXIF数据创建 - 应该正确创建EXIF对象`() {
        val exif = ExifData(
            cameraModel = "Hasselblad X2D 100C",
            lensModel = "XCD 90V",
            focalLength = 90f,
            fNumber = 3.2f,
            exposureTime = "1/125",
            iso = 400,
            dateTime = "2024:01:15 14:30:00",
            gpsLatitude = null,
            gpsLongitude = null
        )
        
        assertEquals("Hasselblad X2D 100C", exif.cameraModel)
        assertEquals("XCD 90V", exif.lensModel)
        assertEquals(90f, exif.focalLength!!)
        assertEquals(3.2f, exif.fNumber!!)
    }
}

/**
 * ScenePresets 单元测试
 */
class ScenePresetsTest {

    @Test
    fun `场景列表 - 应该包含所有场景`() {
        val scenes = ScenePresets.allScenes
        
        assertTrue("场景列表不应该为空", scenes.isNotEmpty())
        assertTrue("场景数量应该大于50", scenes.size >= 50)
    }

    @Test
    fun `场景分类 - 应该正确按类别获取场景`() {
        val portraitScenes = ScenePresets.getScenesByCategory(SceneCategory.PORTRAIT)
        val landscapeScenes = ScenePresets.getScenesByCategory(SceneCategory.LANDSCAPE)
        
        assertTrue("人像场景不应该为空", portraitScenes.isNotEmpty())
        assertTrue("风景场景不应该为空", landscapeScenes.isNotEmpty())
        
        for (scene in portraitScenes) {
            assertEquals(SceneCategory.PORTRAIT, scene.category)
        }
    }

    @Test
    fun `场景查找 - 应该能通过ID找到场景`() {
        val scene = ScenePresets.getSceneById("portrait-standard")
        
        assertNotNull("应该能找到场景", scene)
        assertEquals("portrait-standard", scene!!.id)
    }

    @Test
    fun `场景参数 - 所有场景应该有有效的参数`() {
        for (scene in ScenePresets.allScenes) {
            val params = scene.hasselbladParams
            
            assertTrue("${scene.id} 的影调值应该在有效范围内", params.tone in -30..30)
            assertTrue("${scene.id} 的饱和度值应该在有效范围内", params.saturation in -30..30)
            assertTrue("${scene.id} 的对比度值应该在有效范围内", params.contrast in -30..30)
        }
    }

    @Test
    fun `场景胶片推荐 - 所有场景应该有胶片推荐`() {
        for (scene in ScenePresets.allScenes) {
            assertTrue("${scene.id} 应该有胶片推荐", scene.recommendedFilm.isNotEmpty())
        }
    }

    @Test
    fun `场景大师建议 - 所有场景应该有大师建议`() {
        for (scene in ScenePresets.allScenes) {
            assertTrue("${scene.id} 应该有大师建议", scene.masterTips.isNotEmpty())
        }
    }
}

/**
 * FilmPresets 单元测试
 */
class FilmPresetsTest {

    @Test
    fun `胶片列表 - 应该包含所有胶片`() {
        val films = FilmPresets.allFilms
        
        assertTrue("胶片列表不应该为空", films.isNotEmpty())
        assertEquals("应该有9款胶片", 9, films.size)
    }

    @Test
    fun `胶片查找 - 应该能通过ID找到胶片`() {
        val film = FilmPresets.getFilmById("portra")
        
        assertNotNull("应该能找到胶片", film)
        assertEquals("Portra 400", film!!.name)
    }

    @Test
    fun `胶片系列过滤 - 应该正确按系列过滤胶片`() {
        val classicFilms = FilmPresets.getFilmsBySeries(FilmSeries.CLASSIC)
        
        assertEquals("原生经典系列应该有3款胶片", 3, classicFilms.size)
        
        for (film in classicFilms) {
            assertEquals(FilmSeries.CLASSIC, film.series)
        }
    }
}
