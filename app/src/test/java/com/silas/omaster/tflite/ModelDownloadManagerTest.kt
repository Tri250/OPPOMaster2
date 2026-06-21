package com.silas.omaster.tflite

import org.junit.Assert.*
import org.junit.Test

/**
 * ModelDownloadManager 单元测试
 * 测试模型文件配置的正确性
 */
class ModelDownloadManagerTest {

    // ===== MODEL_FILES 条目数量 =====

    @Test
    fun `MODEL_FILES应该包含3个模型文件`() {
        assertEquals(3, ModelDownloadManager.MODEL_FILES.size)
    }

    // ===== 模型名称验证 =====

    @Test
    fun `模型文件名称应该正确`() {
        val modelNames = ModelDownloadManager.MODEL_FILES.map { it.name }
        assertEquals(
            listOf("scene_classifier.tflite", "quality_analyzer.tflite", "param_predictor.tflite"),
            modelNames
        )
    }

    @Test
    fun `所有模型文件名应该以tflite结尾`() {
        for (modelFile in ModelDownloadManager.MODEL_FILES) {
            assertTrue("模型文件名应以.tflite结尾: ${modelFile.name}", modelFile.name.endsWith(".tflite"))
        }
    }

    @Test
    fun `所有模型文件名不应为空`() {
        for (modelFile in ModelDownloadManager.MODEL_FILES) {
            assertTrue("模型文件名不应为空", modelFile.name.isNotBlank())
        }
    }

    // ===== 模型显示名称验证 =====

    @Test
    fun `所有模型显示名称不应为空`() {
        for (modelFile in ModelDownloadManager.MODEL_FILES) {
            assertTrue("模型显示名称不应为空: ${modelFile.name}", modelFile.displayName.isNotBlank())
        }
    }

    @Test
    fun `所有模型描述不应为空`() {
        for (modelFile in ModelDownloadManager.MODEL_FILES) {
            assertTrue("模型描述不应为空: ${modelFile.name}", modelFile.description.isNotBlank())
        }
    }

    // ===== checksum 验证 (开发模式) =====

    @Test
    fun `所有模型checksum应该为空字符串`() {
        for (modelFile in ModelDownloadManager.MODEL_FILES) {
            assertEquals(
                "模型 ${modelFile.name} 的checksum在开发模式下应为空字符串",
                "",
                modelFile.checksum
            )
        }
    }

    @Test
    fun `checksum为空字符串时不应以sha256开头`() {
        for (modelFile in ModelDownloadManager.MODEL_FILES) {
            val expectedHash = if (modelFile.checksum.startsWith("sha256:")) {
                modelFile.checksum.substring(7)
            } else {
                modelFile.checksum
            }
            assertTrue("checksum为空时解析后应为空", expectedHash.isEmpty())
        }
    }

    // ===== 期望大小验证 =====

    @Test
    fun `所有模型期望大小应该为正数`() {
        for (modelFile in ModelDownloadManager.MODEL_FILES) {
            assertTrue("模型 ${modelFile.name} 的expectedSize应为正数: ${modelFile.expectedSize}",
                modelFile.expectedSize > 0)
        }
    }

    @Test
    fun `模型期望大小应该合理`() {
        // 模型文件大小应在合理范围内（10KB - 100MB）
        for (modelFile in ModelDownloadManager.MODEL_FILES) {
            val sizeKB = modelFile.expectedSize / 1024
            assertTrue("模型 ${modelFile.name} 大小应至少10KB: ${sizeKB}KB", sizeKB >= 10)
            assertTrue("模型 ${modelFile.name} 大小应不超过100MB: ${sizeKB}KB", sizeKB <= 100 * 1024)
        }
    }

    @Test
    fun `场景分类模型期望大小应为700KB`() {
        val sceneClassifier = ModelDownloadManager.MODEL_FILES.find { it.name == "scene_classifier.tflite" }
        assertNotNull("场景分类模型应存在", sceneClassifier)
        assertEquals(700 * 1024L, sceneClassifier!!.expectedSize)
    }

    @Test
    fun `质量分析模型期望大小应为500KB`() {
        val qualityAnalyzer = ModelDownloadManager.MODEL_FILES.find { it.name == "quality_analyzer.tflite" }
        assertNotNull("质量分析模型应存在", qualityAnalyzer)
        assertEquals(500 * 1024L, qualityAnalyzer!!.expectedSize)
    }

    @Test
    fun `参数预测模型期望大小应为200KB`() {
        val paramPredictor = ModelDownloadManager.MODEL_FILES.find { it.name == "param_predictor.tflite" }
        assertNotNull("参数预测模型应存在", paramPredictor)
        assertEquals(200 * 1024L, paramPredictor!!.expectedSize)
    }

    // ===== 模型名称唯一性 =====

    @Test
    fun `所有模型名称应该唯一`() {
        val names = ModelDownloadManager.MODEL_FILES.map { it.name }
        assertEquals("模型名称应唯一", names.size, names.toSet().size)
    }

    // ===== DownloadSummary 验证 =====

    @Test
    fun `DownloadSummary progressPercent计算正确`() {
        val summary = ModelDownloadManager.DownloadSummary(
            totalModels = 3,
            downloadedCount = 2,
            missingCount = 1,
            downloadedModels = listOf("scene_classifier.tflite", "quality_analyzer.tflite"),
            missingModels = listOf("param_predictor.tflite"),
            totalSizeBytes = (700 + 500 + 200) * 1024L,
            downloadedSizeBytes = (700 + 500) * 1024L
        )
        assertEquals(66, summary.progressPercent) // 2/3 * 100 = 66
    }

    @Test
    fun `DownloadSummary progressPercent为0当没有下载任何模型`() {
        val summary = ModelDownloadManager.DownloadSummary(
            totalModels = 3,
            downloadedCount = 0,
            missingCount = 3,
            downloadedModels = emptyList(),
            missingModels = listOf("scene_classifier.tflite", "quality_analyzer.tflite", "param_predictor.tflite"),
            totalSizeBytes = (700 + 500 + 200) * 1024L,
            downloadedSizeBytes = 0
        )
        assertEquals(0, summary.progressPercent)
    }

    @Test
    fun `DownloadSummary progressPercent为100当全部下载`() {
        val summary = ModelDownloadManager.DownloadSummary(
            totalModels = 3,
            downloadedCount = 3,
            missingCount = 0,
            downloadedModels = listOf("scene_classifier.tflite", "quality_analyzer.tflite", "param_predictor.tflite"),
            missingModels = emptyList(),
            totalSizeBytes = (700 + 500 + 200) * 1024L,
            downloadedSizeBytes = (700 + 500 + 200) * 1024L
        )
        assertEquals(100, summary.progressPercent)
    }

    @Test
    fun `DownloadSummary remainingSizeBytes计算正确`() {
        val summary = ModelDownloadManager.DownloadSummary(
            totalModels = 3,
            downloadedCount = 2,
            missingCount = 1,
            downloadedModels = listOf("scene_classifier.tflite", "quality_analyzer.tflite"),
            missingModels = listOf("param_predictor.tflite"),
            totalSizeBytes = (700 + 500 + 200) * 1024L,
            downloadedSizeBytes = (700 + 500) * 1024L
        )
        assertEquals(200 * 1024L, summary.remainingSizeBytes)
    }

    @Test
    fun `DownloadSummary remainingSizeMB计算正确`() {
        val summary = ModelDownloadManager.DownloadSummary(
            totalModels = 3,
            downloadedCount = 2,
            missingCount = 1,
            downloadedModels = listOf("scene_classifier.tflite", "quality_analyzer.tflite"),
            missingModels = listOf("param_predictor.tflite"),
            totalSizeBytes = (700 + 500 + 200) * 1024L,
            downloadedSizeBytes = (700 + 500) * 1024L
        )
        val expectedMB = 200f * 1024f / (1024f * 1024f)
        assertEquals(expectedMB, summary.remainingSizeMB, 0.01f)
    }

    // ===== ModelFile 数据类验证 =====

    @Test
    fun `ModelFile数据类应该正确存储所有字段`() {
        val modelFile = ModelDownloadManager.ModelFile(
            name = "test_model.tflite",
            displayName = "测试模型",
            description = "用于测试的模型",
            expectedSize = 100 * 1024L,
            checksum = "sha256:abc123"
        )
        assertEquals("test_model.tflite", modelFile.name)
        assertEquals("测试模型", modelFile.displayName)
        assertEquals("用于测试的模型", modelFile.description)
        assertEquals(100 * 1024L, modelFile.expectedSize)
        assertEquals("sha256:abc123", modelFile.checksum)
    }

    // ===== DownloadState 密封类验证 =====

    @Test
    fun `DownloadState Idle状态应该正确`() {
        val state = ModelDownloadManager.DownloadState.Idle
        assertTrue(state is ModelDownloadManager.DownloadState.Idle)
    }

    @Test
    fun `DownloadState Checking状态应该包含模型名`() {
        val state = ModelDownloadManager.DownloadState.Checking("scene_classifier.tflite")
        assertTrue(state is ModelDownloadManager.DownloadState.Checking)
        assertEquals("scene_classifier.tflite", (state as ModelDownloadManager.DownloadState.Checking).model)
    }

    @Test
    fun `DownloadState Downloading状态应该包含进度信息`() {
        val state = ModelDownloadManager.DownloadState.Downloading("scene_classifier.tflite", 0.5f, 350000L, 700000L)
        assertTrue(state is ModelDownloadManager.DownloadState.Downloading)
        val downloading = state as ModelDownloadManager.DownloadState.Downloading
        assertEquals("scene_classifier.tflite", downloading.model)
        assertEquals(0.5f, downloading.progress)
        assertEquals(350000L, downloading.bytesDownloaded)
        assertEquals(700000L, downloading.totalBytes)
    }

    @Test
    fun `DownloadState Failed状态应该包含错误信息`() {
        val state = ModelDownloadManager.DownloadState.Failed("scene_classifier.tflite", "网络错误")
        assertTrue(state is ModelDownloadManager.DownloadState.Failed)
        val failed = state as ModelDownloadManager.DownloadState.Failed
        assertEquals("scene_classifier.tflite", failed.model)
        assertEquals("网络错误", failed.error)
    }

    @Test
    fun `DownloadState AllCompleted状态应该包含模型列表`() {
        val models = listOf("scene_classifier.tflite", "quality_analyzer.tflite")
        val state = ModelDownloadManager.DownloadState.AllCompleted(models)
        assertTrue(state is ModelDownloadManager.DownloadState.AllCompleted)
        val completed = state as ModelDownloadManager.DownloadState.AllCompleted
        assertEquals(2, completed.models.size)
        assertTrue(completed.models.contains("scene_classifier.tflite"))
    }

    // ===== 总大小验证 =====

    @Test
    fun `所有模型总大小应为正数`() {
        val totalSize = ModelDownloadManager.MODEL_FILES.sumOf { it.expectedSize }
        assertEquals((700 + 500 + 200) * 1024L, totalSize)
        assertTrue("总大小应为正数", totalSize > 0)
    }

    // ===== v1.8.5 模型就绪状态守卫 =====

    @Test
    fun `所有 AI 模型应标记为未就绪`() {
        for (modelFile in ModelDownloadManager.MODEL_FILES) {
            assertFalse(
                "模型 ${modelFile.name} 尚未提供真实二进制文件，应标记为 isReady=false",
                modelFile.isReady
            )
        }
    }

    @Test
    fun `未就绪模型不应出现在缺失列表中`() {
        // 未就绪模型视为已满足，避免 Release 构建触发无意义下载
        val missing = ModelDownloadManager.MODEL_FILES.filter { modelFile ->
            !modelFile.isReady
        }
        assertTrue("未就绪模型不应被视为缺失", missing.isEmpty())
    }

    @Test
    fun `checksum 为空时不应包含伪占位值`() {
        for (modelFile in ModelDownloadManager.MODEL_FILES) {
            assertTrue(
                "模型 ${modelFile.name} 的 checksum 必须是空字符串，禁止写入伪 SHA256 占位值",
                modelFile.checksum.isEmpty()
            )
        }
    }

    @Test
    fun `ModelFile 应支持 isReady 字段`() {
        val readyModel = ModelDownloadManager.ModelFile(
            name = "ready_model.tflite",
            displayName = "已就绪模型",
            description = "测试",
            expectedSize = 100 * 1024L,
            checksum = "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            isReady = true
        )
        assertTrue(readyModel.isReady)

        val notReadyModel = readyModel.copy(isReady = false)
        assertFalse(notReadyModel.isReady)
    }
}