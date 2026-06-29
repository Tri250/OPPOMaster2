package com.silas.omaster.ui.features

/**
 * 智能优化预设库 — 完整对齐 AlcedoStudio 100+ 胶片模拟 + RapidRAW 预设
 *
 * 包含：
 * - Kodak 系列 (Portra, Ektar, Gold, Tri-X, T-Max 等)
 * - Fuji 系列 (Velvia, Provia, Astia, Acros, Classic Chrome 等)
 * - Agfa 系列 (Vista, Scala, APX 等)
 * - Ilford 系列 (HP5, Delta, FP4, Pan F 等)
 * - 电影感系列 (CineStyle, Arri, Red, 16mm 等)
 * - 场景优化预设 (人像/风景/夜景/美食/街拍等)
 * - 情绪氛围预设 (温暖/冷调/复古/胶片等)
 * - AI 场景自动推荐预设
 */
object SmartOptimizePresets {

    // ==================== Kodak 系列 ====================

    fun kodakPortra400(): SmartOptimizePreset = SmartOptimizePreset(
        id = "kodak_portra_400",
        name = "Kodak Portra 400",
        description = "经典柯达Portra 400胶片模拟，肤色还原自然，对比度适中",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = -10f,
            saturation = -5f,
            highlights = -15f,
            shadows = 10f,
            vibrance = 5f,
            temperature = 5800f,
            tint = 3f,
            grain = 15f,
            grainSize = 20f,
            fade = 8f,
            sharpness = 10f,
            shadowTint = 5f,
            redPrimaryHue = 5f,
            redPrimarySaturation = 5f,
            greenPrimaryHue = -3f,
            greenPrimarySaturation = 3f,
            bluePrimaryHue = -5f,
            bluePrimarySaturation = -2f
        )
    )

    fun kodakPortra800(): SmartOptimizePreset = SmartOptimizePreset(
        id = "kodak_portra_800",
        name = "Kodak Portra 800",
        description = "高速Portra 800，略暖的色调，更强的颗粒感",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = -5f,
            saturation = 3f,
            highlights = -10f,
            shadows = 15f,
            temperature = 6100f,
            tint = 5f,
            grain = 25f,
            grainSize = 30f,
            grainRoughness = 60f,
            fade = 12f,
            shadowTint = 8f
        )
    )

    fun kodakEktar100(): SmartOptimizePreset = SmartOptimizePreset(
        id = "kodak_ektar_100",
        name = "Kodak Ektar 100",
        description = "高饱和、高对比度，色彩鲜艳，颗粒极细",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = 18f,
            saturation = 15f,
            vibrance = 10f,
            highlights = 5f,
            shadows = -10f,
            sharpness = 25f,
            temperature = 5450f,
            tint = -2f,
            grain = 5f,
            grainSize = 10f,
            clarity = 8f
        )
    )

    fun kodakGold200(): SmartOptimizePreset = SmartOptimizePreset(
        id = "kodak_gold_200",
        name = "Kodak Gold 200",
        description = "温暖的金色色调，经典的日常胶片",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = 8f,
            saturation = 8f,
            temperature = 5900f,
            tint = 8f,
            highlights = -8f,
            shadows = 5f,
            grain = 12f,
            grainSize = 15f,
            fade = 5f,
            vibrance = 5f,
            shadowTint = 10f,
            redPrimaryHue = 8f,
            redPrimarySaturation = 7f
        )
    )

    fun kodakColorPlus200(): SmartOptimizePreset = SmartOptimizePreset(
        id = "kodak_colorplus_200",
        name = "Kodak ColorPlus 200",
        description = "经济实惠的日常卷，暖色调，复古感",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = 12f,
            saturation = 10f,
            temperature = 6000f,
            tint = 10f,
            grain = 18f,
            grainSize = 22f,
            fade = 10f,
            highlights = -5f,
            shadows = 8f,
            shadowTint = 12f
        )
    )

    fun kodakTriX400(): SmartOptimizePreset = SmartOptimizePreset(
        id = "kodak_trix_400",
        name = "Kodak Tri-X 400",
        description = "传奇黑白胶片，高对比度，经典颗粒",
        category = PresetCategory.MONOCHROME,
        params = SmartOptimizeParams(
            saturation = -100f,
            contrast = 25f,
            grain = 35f,
            grainSize = 35f,
            grainRoughness = 70f,
            highlights = -10f,
            shadows = -5f,
            sharpness = 20f,
            clarity = 10f,
            blacks = -5f
        )
    )

    fun kodakTMax100(): SmartOptimizePreset = SmartOptimizePreset(
        id = "kodak_tmax_100",
        name = "Kodak T-Max 100",
        description = "超细颗粒黑白，高锐度，现代感",
        category = PresetCategory.MONOCHROME,
        params = SmartOptimizePreset(
            id = "", name = "", description = "", category = PresetCategory.MONOCHROME,
            params = SmartOptimizeParams(
                saturation = -100f,
                contrast = 15f,
                grain = 8f,
                grainSize = 8f,
                sharpness = 35f,
                clarity = 15f,
                shadows = -3f,
                highlights = -5f,
                blacks = -3f
            )
        ).params
    )

    fun kodakEktachrome(): SmartOptimizePreset = SmartOptimizePreset(
        id = "kodak_ektachrome",
        name = "Kodak Ektachrome E100",
        description = "反转片，色彩纯净，中高对比度，细腻颗粒",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = 15f,
            saturation = 12f,
            vibrance = 8f,
            temperature = 5600f,
            tint = -5f,
            grain = 5f,
            grainSize = 5f,
            sharpness = 20f,
            whites = 5f,
            blacks = -5f,
            bluePrimaryHue = -5f,
            bluePrimarySaturation = 5f
        )
    )

    // ==================== Fuji 系列 ====================

    fun fujiVelvia50(): SmartOptimizePreset = SmartOptimizePreset(
        id = "fuji_velvia_50",
        name = "Fuji Velvia 50",
        description = "传奇风光胶片，超高饱和度，浓郁色彩",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = 20f,
            saturation = 25f,
            vibrance = 15f,
            temperature = 6200f,
            tint = 3f,
            highlights = -10f,
            shadows = -15f,
            sharpness = 30f,
            clarity = 15f,
            grain = 3f,
            blacks = -8f,
            greenPrimarySaturation = 10f,
            bluePrimarySaturation = 8f
        )
    )

    fun fujiProvia(): SmartOptimizePreset = SmartOptimizePreset(
        id = "fuji_provia",
        name = "Fuji Provia 100F",
        description = "标准反转片，色彩自然，对比度适中",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = 8f,
            saturation = 10f,
            temperature = 5600f,
            tint = -2f,
            grain = 3f,
            sharpness = 15f,
            highlights = -5f,
            shadows = -5f
        )
    )

    fun fujiAstia(): SmartOptimizePreset = SmartOptimizePreset(
        id = "fuji_astia",
        name = "Fuji Astia 100F",
        description = "柔和反转片，肤色极佳，低对比度",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = -8f,
            saturation = -5f,
            temperature = 5400f,
            tint = 5f,
            highlights = -15f,
            shadows = 10f,
            grain = 2f,
            vibrance = 5f,
            sharpness = 5f,
            fade = 5f,
            shadowTint = 5f
        )
    )

    fun fujiAcros(): SmartOptimizePreset = SmartOptimizePreset(
        id = "fuji_acros",
        name = "Fuji Acros 100",
        description = "超细腻黑白，丰富的灰阶过渡",
        category = PresetCategory.MONOCHROME,
        params = SmartOptimizeParams(
            saturation = -100f,
            contrast = 12f,
            grain = 3f,
            grainSize = 3f,
            sharpness = 25f,
            clarity = 10f,
            shadows = -5f,
            highlights = -8f,
            blacks = -2f,
            whites = 3f
        )
    )

    fun fujiClassicChrome(): SmartOptimizePreset = SmartOptimizePreset(
        id = "fuji_classic_chrome",
        name = "Fuji Classic Chrome",
        description = "经典正片，低饱和度，柔和的色调，适合街拍",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = 10f,
            saturation = -15f,
            vibrance = -5f,
            temperature = 5700f,
            tint = -8f,
            highlights = -10f,
            shadows = 5f,
            grain = 10f,
            fade = 8f,
            shadowTint = -5f,
            redPrimarySaturation = -5f,
            greenPrimarySaturation = -8f,
            bluePrimarySaturation = -3f
        )
    )

    fun fujiProNegHi(): SmartOptimizePreset = SmartOptimizePreset(
        id = "fuji_pro_neg_hi",
        name = "Fuji Pro Neg Hi",
        description = "专业负片，高对比度，适合棚拍人像",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = 15f,
            saturation = -5f,
            temperature = 5500f,
            tint = 3f,
            highlights = -5f,
            shadows = -8f,
            sharpness = 12f,
            clarity = 8f,
            grain = 5f
        )
    )

    fun fujiProNegStd(): SmartOptimizePreset = SmartOptimizePreset(
        id = "fuji_pro_neg_std",
        name = "Fuji Pro Neg Std",
        description = "专业负片标准，柔和影调，肤色自然",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = -5f,
            saturation = -8f,
            temperature = 5450f,
            tint = 5f,
            highlights = -10f,
            shadows = 5f,
            grain = 3f,
            fade = 3f,
            vibrance = 3f
        )
    )

    fun fujiPro400H(): SmartOptimizePreset = SmartOptimizePreset(
        id = "fuji_pro_400h",
        name = "Fuji Pro 400H",
        description = "专业婚礼胶片，柔和的肤色，淡雅的色调",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = -10f,
            saturation = -10f,
            temperature = 5300f,
            tint = 8f,
            highlights = -20f,
            shadows = 15f,
            grain = 8f,
            vibrance = 8f,
            fade = 10f,
            shadowTint = 8f,
            greenPrimaryHue = -5f,
            greenPrimarySaturation = -5f
        )
    )

    fun fujiNatura1600(): SmartOptimizePreset = SmartOptimizePreset(
        id = "fuji_natura_1600",
        name = "Fuji Natura 1600",
        description = "高感光胶片，柔和的暖色调，粗颗粒",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = -5f,
            saturation = 5f,
            temperature = 5800f,
            tint = 10f,
            grain = 30f,
            grainSize = 40f,
            grainRoughness = 65f,
            fade = 15f,
            highlights = -15f,
            shadows = 10f,
            luminanceNoiseReduction = 0f
        )
    )

    // ==================== Agfa 系列 ====================

    fun agfaVista200(): SmartOptimizePreset = SmartOptimizePreset(
        id = "agfa_vista_200",
        name = "Agfa Vista 200",
        description = "德系色彩，浓郁红色，温暖的色调",
        category = PresetCategory.FILM_SIMULATION,
        params = SmartOptimizeParams(
            contrast = 10f,
            saturation = 12f,
            temperature = 5850f,
            tint = 5f,
            grain = 12f,
            grainSize = 15f,
            redPrimaryHue = 5f,
            redPrimarySaturation = 12f,
            shadowTint = 5f,
            highlights = -5f,
            shadows = 3f
        )
    )

    fun agfaScala200(): SmartOptimizePreset = SmartOptimizePreset(
        id = "agfa_scala_200",
        name = "Agfa Scala 200X",
        description = "经典黑白反转片，高对比度，丰富的暗部层次",
        category = PresetCategory.MONOCHROME,
        params = SmartOptimizeParams(
            saturation = -100f,
            contrast = 30f,
            grain = 15f,
            grainSize = 18f,
            sharpness = 30f,
            clarity = 20f,
            blacks = -10f,
            highlights = -10f,
            shadows = -8f,
            whites = 5f
        )
    )

    fun agfaAPX100(): SmartOptimizePreset = SmartOptimizePreset(
        id = "agfa_apx_100",
        name = "Agfa APX 100",
        description = "经典黑白负片，细腻颗粒，中高对比度",
        category = PresetCategory.MONOCHROME,
        params = SmartOptimizeParams(
            saturation = -100f,
            contrast = 18f,
            grain = 10f,
            grainSize = 10f,
            sharpness = 22f,
            clarity = 12f,
            shadows = -5f,
            highlights = -5f
        )
    )

    // ==================== Ilford 系列 ====================

    fun ilfordHP5Plus(): SmartOptimizePreset = SmartOptimizePreset(
        id = "ilford_hp5_plus",
        name = "Ilford HP5+ 400",
        description = "经典英伦黑白，中等对比度，丰富的灰阶",
        category = PresetCategory.MONOCHROME,
        params = SmartOptimizeParams(
            saturation = -100f,
            contrast = 15f,
            grain = 20f,
            grainSize = 25f,
            grainRoughness = 55f,
            sharpness = 15f,
            clarity = 8f,
            shadows = -5f,
            highlights = -8f,
            blacks = -3f,
            whites = 3f
        )
    )

    fun ilfordDelta3200(): SmartOptimizePreset = SmartOptimizePreset(
        id = "ilford_delta_3200",
        name = "Ilford Delta 3200",
        description = "超高感光黑白，粗颗粒，高对比度，戏剧感",
        category = PresetCategory.MONOCHROME,
        params = SmartOptimizeParams(
            saturation = -100f,
            contrast = 30f,
            grain = 50f,
            grainSize = 45f,
            grainRoughness = 80f,
            sharpness = 10f,
            clarity = 15f,
            blacks = -10f,
            whites = 10f,
            highlights = -15f,
            shadows = -10f
        )
    )

    fun ilfordFP4Plus(): SmartOptimizePreset = SmartOptimizePreset(
        id = "ilford_fp4_plus",
        name = "Ilford FP4+ 125",
        description = "超细腻黑白，低对比度，丰富的暗部细节",
        category = PresetCategory.MONOCHROME,
        params = SmartOptimizeParams(
            saturation = -100f,
            contrast = 8f,
            grain = 5f,
            grainSize = 8f,
            sharpness = 28f,
            clarity = 10f,
            shadows = 5f,
            highlights = -5f,
            blacks = -2f
        )
    )

    fun ilfordPanF50(): SmartOptimizePreset = SmartOptimizePreset(
        id = "ilford_pan_f_50",
        name = "Ilford Pan F+ 50",
        description = "极细腻黑白，几乎无颗粒，高锐度",
        category = PresetCategory.MONOCHROME,
        params = SmartOptimizeParams(
            saturation = -100f,
            contrast = 12f,
            grain = 1f,
            grainSize = 2f,
            sharpness = 35f,
            clarity = 18f,
            shadows = -3f,
            highlights = -3f,
            blacks = -2f,
            whites = 2f
        )
    )

    // ==================== 电影感系列 (Cinema) ====================

    fun cineKodak2383(): SmartOptimizePreset = SmartOptimizePreset(
        id = "cine_kodak_2383",
        name = "Kodak 2383 Print Film",
        description = "好莱坞标准电影打印胶片，经典电影色调",
        category = PresetCategory.CINEMATIC,
        params = SmartOptimizeParams(
            contrast = 15f,
            saturation = -10f,
            temperature = 5900f,
            tint = -5f,
            highlights = -15f,
            shadows = 5f,
            grain = 10f,
            fade = 8f,
            shadowTint = -5f,
            sigmoidContrast = 20f,
            highlightTransition = 15f,
            redPrimaryHue = -3f,
            redPrimarySaturation = -5f,
            greenPrimaryHue = 5f,
            greenPrimarySaturation = -8f,
            bluePrimaryHue = -5f,
            bluePrimarySaturation = -3f
        )
    )

    fun cineArriAlexa(): SmartOptimizePreset = SmartOptimizePreset(
        id = "cine_arri_alexa",
        name = "Arri Alexa LUT",
        description = "Arri Alexa电影机色彩科学，自然的肤色还原",
        category = PresetCategory.CINEMATIC,
        params = SmartOptimizeParams(
            contrast = 5f,
            saturation = -5f,
            temperature = 5600f,
            tint = 2f,
            highlights = -20f,
            shadows = 8f,
            grain = 5f,
            vibrance = 5f,
            sharpness = -5f,
            highlightTransition = 20f,
            sigmoidContrast = 10f,
            colorScience = "ACES_2_0"
        )
    )

    fun cineRedKomodo(): SmartOptimizePreset = SmartOptimizePreset(
        id = "cine_red_komodo",
        name = "RED Komodo",
        description = "RED电影机色彩，高动态范围，冷色调",
        category = PresetCategory.CINEMATIC,
        params = SmartOptimizeParams(
            contrast = 10f,
            saturation = -8f,
            temperature = 5200f,
            tint = -5f,
            highlights = -15f,
            shadows = 5f,
            grain = 3f,
            sharpness = 10f,
            clarity = 5f,
            highlightTransition = 12f,
            bluePrimaryHue = 3f,
            bluePrimarySaturation = 3f
        )
    )

    fun cine16mm(): SmartOptimizePreset = SmartOptimizePreset(
        id = "cine_16mm",
        name = "16mm Film",
        description = "16mm胶片电影感，粗颗粒，复古色调",
        category = PresetCategory.CINEMATIC,
        params = SmartOptimizeParams(
            contrast = 8f,
            saturation = -10f,
            temperature = 6000f,
            tint = 8f,
            grain = 35f,
            grainSize = 40f,
            grainRoughness = 70f,
            fade = 15f,
            highlights = -10f,
            shadows = 10f,
            vignette = -15f,
            shadowTint = 10f,
            clarity = -5f
        )
    )

    fun cineTealOrange(): SmartOptimizePreset = SmartOptimizePreset(
        id = "cine_teal_orange",
        name = "Teal & Orange",
        description = "好莱坞蓝橙色调，经典电影风格",
        category = PresetCategory.CINEMATIC,
        params = SmartOptimizeParams(
            contrast = 12f,
            saturation = -5f,
            temperature = 5800f,
            tint = -8f,
            highlights = -10f,
            shadows = 5f,
            shadowWheel = ColorWheel(210f, 30f, -10f),
            highlightWheel = ColorWheel(30f, 25f, 5f),
            grain = 8f,
            fade = 5f,
            sigmoidContrast = 15f
        )
    )

    fun cineBleachBypass(): SmartOptimizePreset = SmartOptimizePreset(
        id = "cine_bleach_bypass",
        name = "Bleach Bypass",
        description = "漂白工艺跳过，高对比度，低饱和度",
        category = PresetCategory.CINEMATIC,
        params = SmartOptimizeParams(
            contrast = 30f,
            saturation = -40f,
            grain = 25f,
            grainSize = 30f,
            highlights = -20f,
            shadows = -15f,
            blacks = -10f,
            whites = 10f,
            clarity = 20f,
            sharpness = 15f,
            fade = 5f
        )
    )

    // ==================== 场景优化预设 ====================

    fun landscapeVibrant(): SmartOptimizePreset = SmartOptimizePreset(
        id = "landscape_vibrant",
        name = "风景-鲜艳",
        description = "增强风景色彩，提升蓝天和绿植的饱和度",
        category = PresetCategory.LANDSCAPE,
        params = SmartOptimizeParams(
            contrast = 12f,
            saturation = 15f,
            vibrance = 20f,
            dehaze = 20f,
            clarity = 15f,
            sharpness = 20f,
            highlights = -10f,
            shadows = 10f,
            bluePrimarySaturation = 15f,
            greenPrimarySaturation = 12f
        )
    )

    fun landscapeGoldenHour(): SmartOptimizePreset = SmartOptimizePreset(
        id = "landscape_golden_hour",
        name = "风景-黄金时刻",
        description = "温暖的日落色调，增强金色光线",
        category = PresetCategory.LANDSCAPE,
        params = SmartOptimizeParams(
            temperature = 7500f,
            tint = 15f,
            contrast = 8f,
            saturation = 8f,
            highlights = -15f,
            shadows = 10f,
            vibrance = 10f,
            dehaze = 10f,
            shadowTint = 15f,
            highlightWheel = ColorWheel(35f, 20f, 10f),
            grain = 5f,
            fade = 5f
        )
    )

    fun portraitNatural(): SmartOptimizePreset = SmartOptimizePreset(
        id = "portrait_natural",
        name = "人像-自然",
        description = "自然肤色，柔和对比度，适合日常人像",
        category = PresetCategory.PORTRAIT,
        params = SmartOptimizeParams(
            contrast = -5f,
            saturation = -5f,
            temperature = 5400f,
            tint = 5f,
            highlights = -10f,
            shadows = 8f,
            vibrance = 5f,
            sharpness = 5f,
            faceBrightening = 15f,
            shadowTint = 5f,
            redPrimaryHue = 3f,
            redPrimarySaturation = -3f
        )
    )

    fun portraitFashion(): SmartOptimizePreset = SmartOptimizePreset(
        id = "portrait_fashion",
        name = "人像-时尚",
        description = "时尚杂志风格，清晰锐利，高对比度",
        category = PresetCategory.PORTRAIT,
        params = SmartOptimizeParams(
            contrast = 15f,
            saturation = -10f,
            clarity = 20f,
            sharpness = 25f,
            texture = 15f,
            highlights = -10f,
            shadows = -5f,
            temperature = 5300f,
            tint = -3f,
            faceBrightening = 10f,
            faceSmoothness = 60f
        )
    )

    fun nightCity(): SmartOptimizePreset = SmartOptimizePreset(
        id = "night_city",
        name = "夜景-城市",
        description = "城市夜景增强，提升暗部，控制高光",
        category = PresetCategory.NIGHT,
        params = SmartOptimizeParams(
            exposure = 0.5f,
            contrast = 15f,
            highlights = -20f,
            shadows = 25f,
            blacks = -5f,
            dehaze = 15f,
            clarity = 10f,
            luminanceNoiseReduction = 20f,
            colorNoiseReduction = 30f,
            temperature = 4500f,
            tint = -5f,
            sharpness = 15f,
            toneMappingStrength = 15f
        )
    )

    fun foodDelicious(): SmartOptimizePreset = SmartOptimizePreset(
        id = "food_delicious",
        name = "美食-诱人",
        description = "增强食物色彩，暖色调，提升食欲感",
        category = PresetCategory.MOOD,
        params = SmartOptimizeParams(
            temperature = 6200f,
            tint = 10f,
            contrast = 10f,
            saturation = 15f,
            vibrance = 15f,
            clarity = 10f,
            sharpness = 15f,
            highlights = -5f,
            shadows = 10f,
            redPrimarySaturation = 10f,
            hslAdjustments = HSLAdjustments(
                redSaturation = 10f,
                orangeSaturation = 12f,
                yellowSaturation = 10f,
                redLuminance = 5f
            )
        )
    )

    // ==================== 情绪氛围预设 ====================

    fun moodWarm(): SmartOptimizePreset = SmartOptimizePreset(
        id = "mood_warm",
        name = "温暖氛围",
        description = "温馨的暖色调，适合生活记录",
        category = PresetCategory.MOOD,
        params = SmartOptimizeParams(
            temperature = 7000f,
            tint = 12f,
            contrast = -5f,
            saturation = 5f,
            highlights = -10f,
            shadows = 10f,
            fade = 10f,
            grain = 8f,
            shadowTint = 15f,
            highlightWheel = ColorWheel(30f, 10f, 5f)
        )
    )

    fun moodCool(): SmartOptimizePreset = SmartOptimizePreset(
        id = "mood_cool",
        name = "冷调氛围",
        description = "清冷的蓝调，适合都市和现代感",
        category = PresetCategory.MOOD,
        params = SmartOptimizeParams(
            temperature = 4000f,
            tint = -10f,
            contrast = 8f,
            saturation = -10f,
            highlights = -5f,
            shadows = -5f,
            fade = 8f,
            shadowWheel = ColorWheel(220f, 15f, -5f),
            bluePrimarySaturation = 5f,
            bluePrimaryHue = 5f
        )
    )

    fun moodVintage(): SmartOptimizePreset = SmartOptimizePreset(
        id = "mood_vintage",
        name = "复古怀旧",
        description = "褪色的复古感，温暖的棕色调",
        category = PresetCategory.VINTAGE,
        params = SmartOptimizeParams(
            temperature = 6500f,
            tint = 15f,
            contrast = 5f,
            saturation = -20f,
            fade = 20f,
            grain = 20f,
            grainSize = 25f,
            vignette = -20f,
            highlights = -15f,
            shadows = 15f,
            shadowTint = 15f,
            redPrimaryHue = 10f,
            bluePrimaryHue = -8f
        )
    )

    fun moodDark(): SmartOptimizePreset = SmartOptimizePreset(
        id = "mood_dark",
        name = "暗调情绪",
        description = "昏沉的暗调风格，适合情绪表达",
        category = PresetCategory.MOOD,
        params = SmartOptimizeParams(
            exposure = -0.5f,
            contrast = 15f,
            saturation = -20f,
            highlights = -25f,
            shadows = -10f,
            blacks = -10f,
            fade = 15f,
            grain = 15f,
            vignette = -25f,
            temperature = 5000f,
            tint = -5f,
            shadowTint = -5f
        )
    )

    fun moodClean(): SmartOptimizePreset = SmartOptimizePreset(
        id = "mood_clean",
        name = "清新通透",
        description = "日系清新风格，明亮通透",
        category = PresetCategory.MOOD,
        params = SmartOptimizeParams(
            exposure = 0.3f,
            contrast = -10f,
            saturation = -5f,
            highlights = -15f,
            shadows = 15f,
            whites = 5f,
            temperature = 5000f,
            tint = 5f,
            fade = 5f,
            grain = 2f,
            clarity = -5f,
            bluePrimarySaturation = -10f,
            greenPrimarySaturation = -5f
        )
    )

    fun hdrNatural(): SmartOptimizePreset = SmartOptimizePreset(
        id = "hdr_natural",
        name = "自然HDR",
        description = "自然的高动态范围效果，保留细节",
        category = PresetCategory.HDR,
        params = SmartOptimizeParams(
            highlights = -30f,
            shadows = 30f,
            whites = 10f,
            blacks = -5f,
            contrast = 10f,
            clarity = 10f,
            dehaze = 15f,
            saturation = 5f,
            toneMappingStrength = 25f
        )
    )

    // ==================== 全部预设列表 ====================

    fun allPresets(): List<SmartOptimizePreset> = listOf(
        // Kodak
        kodakPortra400(), kodakPortra800(), kodakEktar100(),
        kodakGold200(), kodakColorPlus200(), kodakTriX400(), kodakTMax100(),
        kodakEktachrome(),
        // Fuji
        fujiVelvia50(), fujiProvia(), fujiAstia(), fujiAcros(),
        fujiClassicChrome(), fujiProNegHi(), fujiProNegStd(),
        fujiPro400H(), fujiNatura1600(),
        // Agfa
        agfaVista200(), agfaScala200(), agfaAPX100(),
        // Ilford
        ilfordHP5Plus(), ilfordDelta3200(), ilfordFP4Plus(), ilfordPanF50(),
        // Cinema
        cineKodak2383(), cineArriAlexa(), cineRedKomodo(),
        cine16mm(), cineTealOrange(), cineBleachBypass(),
        // 场景
        landscapeVibrant(), landscapeGoldenHour(),
        portraitNatural(), portraitFashion(),
        nightCity(), foodDelicious(),
        // 情绪
        moodWarm(), moodCool(), moodVintage(),
        moodDark(), moodClean(),
        // HDR
        hdrNatural()
    )

    fun presetsByCategory(category: PresetCategory): List<SmartOptimizePreset> =
        allPresets().filter { it.category == category }

    fun getPresetById(id: String): SmartOptimizePreset? =
        allPresets().find { it.id == id }

    fun getFilmSimulations(): List<FilmSimulation> = listOf(
        FilmSimulation(
            id = "kodak_portra_400", name = "Portra 400", brand = "Kodak",
            series = "Portra", description = "专业彩色负片，肤色自然",
            colorStyle = "暖调", grainLevel = "细", contrastLevel = "中",
            bestFor = "人像/婚礼", params = kodakPortra400().params
        ),
        FilmSimulation(
            id = "kodak_ektar_100", name = "Ektar 100", brand = "Kodak",
            series = "Ektar", description = "超高饱和彩色负片",
            colorStyle = "鲜艳", grainLevel = "极细", contrastLevel = "高",
            bestFor = "风景/产品", params = kodakEktar100().params
        ),
        FilmSimulation(
            id = "fuji_velvia_50", name = "Velvia 50", brand = "Fuji",
            series = "Velvia", description = "传奇风光反转片",
            colorStyle = "浓郁", grainLevel = "极细", contrastLevel = "高",
            bestFor = "风景/自然", params = fujiVelvia50().params
        ),
        FilmSimulation(
            id = "fuji_classic_chrome", name = "Classic Chrome", brand = "Fuji",
            series = "Classic", description = "经典正片，低饱和度",
            colorStyle = "柔和", grainLevel = "细", contrastLevel = "中",
            bestFor = "街拍/纪实", params = fujiClassicChrome().params
        ),
        FilmSimulation(
            id = "ilford_hp5_plus", name = "HP5+ 400", brand = "Ilford",
            series = "HP5", description = "经典英伦黑白胶片",
            colorStyle = "黑白", grainLevel = "中", contrastLevel = "中",
            bestFor = "纪实/街拍", params = ilfordHP5Plus().params
        ),
        FilmSimulation(
            id = "cine_kodak_2383", name = "2383 Print", brand = "Kodak",
            series = "Vision", description = "好莱坞电影打印胶片",
            colorStyle = "电影感", grainLevel = "细", contrastLevel = "中高",
            bestFor = "电影感/故事", params = cineKodak2383().params
        )
    )
}