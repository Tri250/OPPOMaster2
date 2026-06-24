package com.silas.omaster

import com.silas.omaster.data.repository.LUTResourceRepository
import com.silas.omaster.data.model.LUTResource
import com.silas.omaster.util.UrlConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * V1.93 回归测试
 *
 * 验证 5 大问题域的真实代码逻辑（非自证自明）：
 * 1. 订阅刷新使用 forceReloadFromFiles 强制从文件重新加载
 * 2. 哈苏之眼拍照回调线程切换（IO 加载 bitmap / Main 更新 UI）
 * 3. AI 构图辅助模块（CompositionGuide / ARGuideType / 排序）
 * 4. 按钮字体变形修复（13sp + maxLines=1 + Ellipsis + contentPadding）
 * 5. LUT 资源库品牌更新（restore 分类 + 17 品牌 54 个 LUT，采集自 cubelut.cn/restore.php）
 */
class V193RegressionTest {

    // ==================== 问题1: 订阅刷新 - 真实代码引用 ====================

    @Test
    fun `问题1-PresetRepository必须提供forceReloadFromFiles方法`() {
        // 反射验证方法存在
        val method = PresetRepository::class.java.declaredMethods
            .firstOrNull { it.name == "forceReloadFromFiles" }
        assertNotNull("PresetRepository 必须有 forceReloadFromFiles 方法", method)
        assertTrue(
            "forceReloadFromFiles 必须是 suspend 函数",
            method!!.parameterTypes.size == 1 &&
                method.parameterTypes[0].name.contains("Continuation")
        )
    }

    @Test
    fun `问题1-reloadDefaultPresets方法必须保留供向后兼容`() {
        val method = PresetRepository::class.java.declaredMethods
            .firstOrNull { it.name == "reloadDefaultPresets" }
        assertNotNull("reloadDefaultPresets 仍需保留", method)
    }

    @Test
    fun `问题1-所有reloadDefaultPresets调用点都应改为forceReloadFromFiles`() {
        // 扫描所有 Kotlin 源文件，验证业务调用点已切换
        val projectRoot = System.getProperty("user.dir") ?: ""
        val ktFiles = java.io.File("$projectRoot/app/src/main")
            .walkTopDown()
            .filter { it.extension == "kt" }
            .filter { !it.absolutePath.contains("/build/") }
            .toList()

        // 仅允许 PresetRepository 内部 + 1 处明确的迁移路径保留
        val allowedFiles = setOf(
            "PresetRepository.kt"  // 函数定义本身
        )

        val stillUsingReload = mutableListOf<String>()
        for (file in ktFiles) {
            val text = file.readText()
            // 跳过定义文件
            if (allowedFiles.any { file.name == it }) continue
            // 跳过注释
            val codeLines = text.lines().filter { line ->
                val trimmed = line.trim()
                !trimmed.startsWith("//") && !trimmed.startsWith("*") && !trimmed.startsWith("/*")
            }
            for (line in codeLines) {
                if (line.contains(".reloadDefaultPresets()")) {
                    stillUsingReload.add("${file.name}: ${line.trim()}")
                }
            }
        }

        assertEquals(
            "应无业务代码再调用 reloadDefaultPresets()，发现遗漏：$stillUsingReload",
            0, stillUsingReload.size
        )
    }

    // ==================== 问题2: 哈苏之眼拍照卡顿 - 真实代码引用 ====================

    @Test
    fun `问题2-HasselbladScreen所有loadBitmapFromUri调用必须在IO调度器`() {
        // 扫描 HasselbladScreen.kt，验证每次 loadBitmapFromUri 调用都包裹在 withContext(Dispatchers.IO)
        val screenFile = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/features/HasselbladScreen.kt"
        )
        assertTrue("HasselbladScreen.kt 必须存在", screenFile.exists())

        val text = screenFile.readText()
        // 提取所有 loadBitmapFromUri( 调用所在的代码行
        val lines = text.lines()
        val violations = mutableListOf<String>()
        for (i in lines.indices) {
            if (lines[i].contains("loadBitmapFromUri(") && !lines[i].trim().startsWith("fun ") && !lines[i].trim().startsWith("private fun ")) {
                // 向上查找 5 行内是否包含 withContext(Dispatchers.IO)
                val lookback = (i - 1 downTo maxOf(0, i - 5)).map { lines[it] }
                val hasIOContext = lookback.any { it.contains("withContext(Dispatchers.IO)") }
                if (!hasIOContext) {
                    violations.add("第${i + 1}行: ${lines[i].trim()}")
                }
            }
        }
        assertEquals(
            "loadBitmapFromUri 调用点必须在 IO 调度器中，违规：$violations",
            0, violations.size
        )
    }

    @Test
    fun `问题2-HasselbladScreen所有Toast调用必须在Main调度器`() {
        val screenFile = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/features/HasselbladScreen.kt"
        )
        val text = screenFile.readText()
        val lines = text.lines()
        // 统计 Toast.makeText 调用
        val toastLines = mutableListOf<Int>()
        for (i in lines.indices) {
            if (lines[i].contains("Toast.makeText(") && !lines[i].trim().startsWith("//")) {
                toastLines.add(i)
            }
        }
        // 拍照回调路径下的 Toast 必须在 Main；其他位置（如 UI 事件）允许直接在 Main 调用
        // 这里验证：至少所有拍照回调内的 Toast 都使用 withContext(Dispatchers.Main)
        val violations = mutableListOf<String>()
        for (lineIdx in toastLines) {
            // 向上查 8 行
            val lookback = (lineIdx - 1 downTo maxOf(0, lineIdx - 8)).map { lines[it] }
            val context = lookback.joinToString("\n")
            // 拍照回调识别：包含 loadBitmapFromUri 或 scope.launch
            if (context.contains("loadBitmapFromUri")) {
                val hasMainContext = lookback.any { it.contains("withContext(Dispatchers.Main)") }
                if (!hasMainContext) {
                    violations.add("第${lineIdx + 1}行附近的 Toast 未在 Main 调度器")
                }
            }
        }
        assertEquals("拍照回调路径下的 Toast 必须在 Main 调度器，违规：$violations", 0, violations.size)
    }

    // ==================== 问题3: AI 构图辅助 - 真实代码引用 ====================

    @Test
    fun `问题3-ARGuideType枚举必须恰好8种类型`() {
        val klass = Class.forName("com.silas.omaster.ui.features.ARGuideType")
        @Suppress("UNCHECKED_CAST")
        val enumClass = klass as Class<out Enum<*>>
        val constants = enumClass.enumConstants
        assertEquals("ARGuideType 必须恰好 8 种类型", 8, constants.size)
    }

    @Test
    fun `问题3-ARGuideType必须包含所有8种引导线`() {
        val klass = Class.forName("com.silas.omaster.ui.features.ARGuideType")
        val names = klass.enumConstants.map { (it as Enum<*>).name }.toSet()
        val required = setOf(
            "THIRDS", "GOLDEN_RATIO", "DIAGONAL", "CENTER_CROSS",
            "SPIRAL", "FRAME", "HORIZON", "TRIANGLE"
        )
        for (r in required) {
            assertTrue("ARGuideType 必须包含 $r，实际：$names", r in names)
        }
    }

    @Test
    fun `问题3-CompositionSceneMode必须7种场景`() {
        val klass = Class.forName("com.silas.omaster.ui.features.CompositionSceneMode")
        val names = klass.enumConstants.map { (it as Enum<*>).name }.toSet()
        assertEquals("CompositionSceneMode 应有 7 种场景（对标 OPPO Find X9 哈苏大师）", 7, names.size)
        for (r in listOf("TRAVEL", "PORTRAIT", "FOOD", "PET", "NIGHT", "MACRO", "STREET")) {
            assertTrue("CompositionSceneMode 必须包含 $r，实际：$names", r in names)
        }
    }

    @Test
    fun `问题3-CompositionGuide数据类必须包含所有必需字段`() {
        val klass = Class.forName("com.silas.omaster.ui.features.CompositionGuide")
        val declaredFields = klass.declaredFields.map { it.name }.toSet()
        val required = setOf(
            "id", "name", "description", "icon", "tips",
            "applicableCategories", "difficulty", "arGuideType",
            "sceneMode", "arOverlayDescription"
        )
        for (r in required) {
            assertTrue("CompositionGuide 必须有字段 $r，实际：$declaredFields", r in declaredFields)
        }
    }

    @Test
    fun `问题3-HasselbladEyeViewModel必须实现applyCompositionGuide方法`() {
        val klass = Class.forName("com.silas.omaster.ui.features.HasselbladEyeViewModel")
        val method = klass.declaredMethods.firstOrNull { it.name == "applyCompositionGuide" }
        assertNotNull("HasselbladEyeViewModel 必须有 applyCompositionGuide 方法", method)
    }

    @Test
    fun `问题3-HasselbladEyeViewModel必须实现clearAppliedComposition方法`() {
        val klass = Class.forName("com.silas.omaster.ui.features.HasselbladEyeViewModel")
        val method = klass.declaredMethods.firstOrNull { it.name == "clearAppliedComposition" }
        assertNotNull("必须有 clearAppliedComposition 方法", method)
    }

    @Test
    fun `问题3-HasselbladEyeViewModel必须暴露appliedCompositionGuideId状态`() {
        val klass = Class.forName("com.silas.omaster.ui.features.HasselbladEyeViewModel")
        val methods = klass.declaredMethods.map { it.name }
        // 通过 Kotlin 编译会生成 getAppliedCompositionGuideId$delegate 等
        // 验证至少有 getter 方法
        val hasGetter = methods.any { it.startsWith("getAppliedCompositionGuideId") }
        assertTrue("HasselbladEyeViewModel 必须暴露 appliedCompositionGuideId 状态", hasGetter)
    }

    @Test
    fun `问题3-CompositionGuideCard的onGuideClick回调必须真正调用applyCompositionGuide`() {
        val screenFile = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/features/HasselbladScreen.kt"
        )
        val text = screenFile.readText()
        // 验证 ResultsContent 内 onGuideClick 回调调用了 viewModel.applyCompositionGuide
        val hasRealCall = text.contains("onGuideClick = { guide ->") &&
            text.contains("viewModel.applyCompositionGuide(guide)")
        assertTrue(
            "CompositionGuideCard 的 onGuideClick 必须真正调用 viewModel.applyCompositionGuide",
            hasRealCall
        )
    }

    @Test
    fun `问题3-保存图片文件名必须包含构图ID`() {
        val viewModelFile = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/features/HasselbladEyeViewModel.kt"
        )
        val text = viewModelFile.readText()
        val hasCompositionInFilename = text.contains("compositionTag") &&
            text.contains("_appliedCompositionGuideId.value")
        assertTrue(
            "saveImage 中文件名必须包含构图 ID（compositionTag）",
            hasCompositionInFilename
        )
    }

    // ==================== 哈苏之眼全面优化验证 ====================

    @Test
    fun `优化-哈苏原厂胶片预设必须包含4种`() {
        // 验证新增的哈苏原厂胶片预设：X1D、HCD、Portra 160、T-MAX
        val modesFile = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/features/HasselbladModes.kt"
        )
        val text = modesFile.readText()
        val requiredPresets = listOf("hasselblad-x1d", "hasselblad-hcd", "hasselblad-portra160", "hasselblad-tmax")
        for (preset in requiredPresets) {
            assertTrue("必须包含哈苏原厂胶片预设：$preset", text.contains(preset))
        }
    }

    @Test
    fun `优化-ViewModel必须暴露operationError状态`() {
        val klass = Class.forName("com.silas.omaster.ui.features.HasselbladEyeViewModel")
        val methods = klass.declaredMethods.map { it.name }
        val hasGetter = methods.any { it.startsWith("getOperationError") }
        val hasClear = methods.any { it == "clearOperationError" }
        assertTrue("ViewModel 必须暴露 operationError 状态（保存/分享失败反馈）", hasGetter)
        assertTrue("ViewModel 必须有 clearOperationError 方法", hasClear)
    }

    @Test
    fun `优化-HEIF编码必须使用WEBP_LOSSY而非JPEG`() {
        val viewModelFile = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/features/HasselbladEyeViewModel.kt"
        )
        val text = viewModelFile.readText()
        // 验证 HEIF 不再使用 JPEG 编码（修复格式不一致问题）
        assertFalse("HEIF 不应使用 JPEG 编码", text.contains("ExportFormat.HEIF -> Bitmap.CompressFormat.JPEG"))
        assertTrue("HEIF 应使用 WEBP_LOSSY 编码", text.contains("Bitmap.CompressFormat.WEBP_LOSSY"))
    }

    @Test
    fun `优化-ViewModel必须导入Intent类`() {
        val viewModelFile = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/features/HasselbladEyeViewModel.kt"
        )
        val text = viewModelFile.readText()
        assertTrue("ViewModel 必须导入 Intent 类", text.contains("import android.content.Intent"))
    }

    @Test
    fun `优化-权限二次引导对话框必须实现`() {
        val screenFile = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/features/HasselbladScreen.kt"
        )
        val text = screenFile.readText()
        // 验证权限二次引导对话框实现
        assertTrue("必须实现 shouldShowRequestPermissionRationale 判断", text.contains("shouldShowRequestPermissionRationale"))
        assertTrue("必须实现跳转设置页", text.contains("ACTION_APPLICATION_DETAILS_SETTINGS"))
        assertTrue("必须有权限引导对话框状态", text.contains("showPermissionRationale"))
    }

    @Test
    fun `优化-新增构图指南必须包含长曝光景深消失点`() {
        val screenFile = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/features/HasselbladScreen.kt"
        )
        val text = screenFile.readText()
        // 验证新增的 3 种构图指南
        assertTrue("必须包含长曝光构图指南", text.contains("id = \"long-exposure\""))
        assertTrue("必须包含景深引导构图指南", text.contains("id = \"depth-of-field\""))
        assertTrue("必须包含消失点构图指南", text.contains("id = \"vanishing-point\""))
    }

    @Test
    fun `优化-AR取景器模拟卡片必须实现`() {
        val screenFile = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/features/HasselbladScreen.kt"
        )
        val text = screenFile.readText()
        // 验证 AR 取景器模拟卡片实现
        assertTrue("必须实现 ViewfinderSimulatorCard 组件", text.contains("fun ViewfinderSimulatorCard"))
        assertTrue("SetupContent 必须调用 ViewfinderSimulatorCard", text.contains("ViewfinderSimulatorCard()"))
    }

    @Test
    fun `优化-ResultsContent必须接收appliedGuideId参数`() {
        val screenFile = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/features/HasselbladScreen.kt"
        )
        val text = screenFile.readText()
        // 验证 ResultsContent 通过参数接收 appliedGuideId（修复编译错误）
        assertTrue("ResultsContent 必须接收 appliedGuideId 参数", text.contains("appliedGuideId: String?"))
    }

    // ==================== 问题4: 字体变形 - 真实代码引用 ====================

    @Test
    fun `问题4-ApplyPresetButton必须使用13sp字号`() {
        val file = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/components/PresetDetailComponents.kt"
        )
        val text = file.readText()
        val lines = text.lines()
        // 找到 ApplyPresetButton 函数
        val startIdx = lines.indexOfFirst { it.contains("fun ApplyPresetButton(") }
        assertTrue("ApplyPresetButton 必须存在", startIdx >= 0)
        // 在函数体内查找 fontSize = 13.sp
        var endIdx = lines.size
        var depth = 0
        for (i in startIdx until lines.size) {
            depth += lines[i].count { it == '{' }
            depth -= lines[i].count { it == '}' }
            if (depth == 0 && i > startIdx) {
                endIdx = i
                break
            }
        }
        val funcBody = lines.subList(startIdx, endIdx).joinToString("\n")
        assertTrue("ApplyPresetButton 内必须有 fontSize = 13.sp", funcBody.contains("fontSize = 13.sp"))
        assertTrue("ApplyPresetButton 内必须有 maxLines = 1", funcBody.contains("maxLines = 1"))
        assertTrue("ApplyPresetButton 内必须有 TextOverflow.Ellipsis", funcBody.contains("TextOverflow.Ellipsis"))
        assertTrue("ApplyPresetButton 必须有 contentPadding", funcBody.contains("contentPadding"))
    }

    @Test
    fun `问题4-FavoriteButton必须使用13sp字号`() {
        val file = java.io.File(
            "${System.getProperty("user.dir")}/app/src/main/java/com/silas/omaster/ui/components/PresetDetailComponents.kt"
        )
        val text = file.readText()
        val lines = text.lines()
        val startIdx = lines.indexOfFirst { it.contains("fun FavoriteButton(") }
        assertTrue("FavoriteButton 必须存在", startIdx >= 0)
        var endIdx = lines.size
        var depth = 0
        for (i in startIdx until lines.size) {
            depth += lines[i].count { it == '{' }
            depth -= lines[i].count { it == '}' }
            if (depth == 0 && i > startIdx) {
                endIdx = i
                break
            }
        }
        val funcBody = lines.subList(startIdx, endIdx).joinToString("\n")
        assertTrue("FavoriteButton 内必须有 fontSize = 13.sp", funcBody.contains("fontSize = 13.sp"))
        assertTrue("FavoriteButton 内必须有 maxLines = 1", funcBody.contains("maxLines = 1"))
        assertTrue("FavoriteButton 内必须有 TextOverflow.Ellipsis", funcBody.contains("TextOverflow.Ellipsis"))
        assertTrue("FavoriteButton 必须有 contentPadding", funcBody.contains("contentPadding"))
    }

    // ==================== 问题5: LUT 资源库 - 真实代码引用 ====================

    @Test
    fun `问题5-CATEGORIES必须包含restore分类`() {
        val categories = LUTResourceRepository.CATEGORIES
        val restoreCategory = categories.firstOrNull { it.id == "restore" }
        assertNotNull("CATEGORIES 必须包含 restore 分类", restoreCategory)
        assertEquals("restore 分类名称必须为 LOG还原", "LOG还原", restoreCategory!!.name)
    }

    @Test
    fun `问题5-RESOURCES必须包含54个品牌LOG还原LUT`() {
        val restoreLuts = LUTResourceRepository.getResources("restore")
        assertEquals("restore 分类下应恰好 54 个品牌 LUT（采集自 cubelut.cn/restore.php 17 品牌），实际：${restoreLuts.size}", 54, restoreLuts.size)
    }

    @Test
    fun `问题5-17品牌LUT ID必须完整`() {
        val restoreLuts = LUTResourceRepository.getResources("restore")
        val ids = restoreLuts.map { it.id }.toSet()
        // 原有 8 个品牌 LUT（向后兼容）
        val legacyRequired = setOf(
            "oppo-rec709", "oppo-rec2020", "oppo-olog2-rec709",
            "vivo-vlog", "xiaomi-milog", "oneplus-olog",
            "fujifilm-flog", "fujifilm-flog2"
        )
        // 新增 9 个品牌代表 LUT（cubelut.cn 采集）
        val newBrandRequired = setOf(
            "apple-log",           // Apple
            "arri-logc3",          // ARRI
            "canon-c-log",         // Canon
            "dji-d-log",           // DJI
            "gopro-fw200",         // GoPro
            "huawei-h-log",        // 华为
            "insta360-acepro2",    // Insta360
            "nikon-n-log",         // Nikon
            "panasonic-v-log",     // Panasonic
            "red-log3g10",         // RED
            "sony-s-log3",         // Sony
            "samsung-galaxy-log"   // 三星
        )
        for (r in legacyRequired + newBrandRequired) {
            assertTrue("必须包含品牌 LUT: $r，实际：$ids", r in ids)
        }
    }

    @Test
    fun `问题5-所有品牌LUT的previewImage不能为空字符串`() {
        val restoreLuts = LUTResourceRepository.getResources("restore")
        for (lut in restoreLuts) {
            assertFalse("LUT ${lut.id} 的 previewImage 不能为空", lut.previewImage.isBlank())
            assertTrue(
                "LUT ${lut.id} 的 previewImage 必须使用 UrlConstants 拼接",
                lut.previewImage.contains(UrlConstants.SAMPLES_BASE_PATH) ||
                    lut.previewImage.contains("/samples/")
            )
        }
    }

    @Test
    fun `问题5-所有品牌LUT的downloadUrl必须HTTPS且有效`() {
        val restoreLuts = LUTResourceRepository.getResources("restore")
        for (lut in restoreLuts) {
            assertTrue(
                "LUT ${lut.id} 的 downloadUrl 必须以 https:// 开头",
                lut.downloadUrl.startsWith("https://")
            )
            assertTrue(
                "LUT ${lut.id} 的 downloadUrl 必须包含 camera= 参数",
                lut.downloadUrl.contains("camera=")
            )
            assertTrue(
                "LUT ${lut.id} 的 downloadUrl 必须包含 variant= 参数",
                lut.downloadUrl.contains("variant=")
            )
        }
    }

    @Test
    fun `问题5-所有品牌LUT必须分类为restore且免费`() {
        val restoreLuts = LUTResourceRepository.getResources("restore")
        for (lut in restoreLuts) {
            assertEquals(
                "LUT ${lut.id} 必须分类为 restore",
                "restore", lut.category
            )
            assertTrue("LUT ${lut.id} 必须免费", lut.isFree)
        }
    }

    @Test
    fun `问题5-searchResources必须能搜到品牌名`() {
        val result = LUTResourceRepository.searchResources("OPPO")
        assertTrue("搜索 OPPO 必须返回至少 1 个结果", result.isNotEmpty())
        // 至少有一个 restore 分类
        assertTrue(
            "搜索结果应包含 restore 分类 LUT",
            result.any { it.category == "restore" }
        )
    }

    @Test
    fun `问题5-新LUT为新品`() {
        val restoreLuts = LUTResourceRepository.getResources("restore")
        for (lut in restoreLuts) {
            assertTrue("LUT ${lut.id} 必须是 isNew", lut.isNew)
        }
    }

    // ==================== 兼容性验证 ====================

    @Test
    fun `兼容性-getResources空参数返回所有LUT`() {
        val all = LUTResourceRepository.getResources(null)
        assertTrue("getResources(null) 必须返回所有 LUT", all.isNotEmpty())
        assertEquals("getResources(\"all\") 应与 null 等价", all.size, LUTResourceRepository.getResources("all").size)
    }

    @Test
    fun `兼容性-getResources的restore子集一定含17个品牌`() {
        val restore = LUTResourceRepository.getResources("restore")
        // 17 个品牌（采集自 cubelut.cn/restore.php）
        val brands = setOf(
            "apple", "arri", "canon", "dji", "fujifilm", "gopro",
            "huawei", "insta360", "nikon", "oppo", "panasonic",
            "red", "sony", "vivo", "xiaomi", "samsung", "oneplus"
        )
        for (lut in restore) {
            val hasBrand = brands.any { lut.id.startsWith(it) || lut.tags.any { t -> t.lowercase().contains(it) } }
            assertTrue("LUT ${lut.id} 必须属于已知 17 品牌之一", hasBrand)
        }
    }

    @Test
    fun `稳定性-空字符串过滤不掉searchResources`() {
        val all = LUTResourceRepository.RESOURCES
        val withBlankQuery = LUTResourceRepository.searchResources("")
        assertEquals("空查询应返回所有", all.size, withBlankQuery.size)
    }

    @Test
    fun `稳定性-formatFileSize正确处理KB和MB`() {
        assertEquals("10 KB", LUTResourceRepository.formatFileSize(10))
        assertEquals("1024 KB", LUTResourceRepository.formatFileSize(1024))
        assertTrue(
            "2048 KB 应格式化为 MB",
            LUTResourceRepository.formatFileSize(2048).contains("MB")
        )
    }

    // ==================== URL 安全性验证 ====================

    @Test
    fun `安全-所有LUT下载URL必须HTTPS`() {
        for (lut in LUTResourceRepository.RESOURCES) {
            assertTrue(
                "LUT ${lut.id} 的 downloadUrl 必须 HTTPS: ${lut.downloadUrl}",
                lut.downloadUrl.startsWith("https://")
            )
        }
    }
}
