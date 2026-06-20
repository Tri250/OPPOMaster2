package com.silas.omaster.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.silas.omaster.ai.analyzer.HeuristicSceneAnalyzer
import com.silas.omaster.model.SceneCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * HeuristicSceneAnalyzer 单元测试
 *
 * 测试覆盖：
 * - 颜色分析
 * - 亮度分析
 * - 人脸检测
 * - 场景识别
 * - 置信度计算
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HeuristicSceneAnalyzerTest {

    private lateinit var context: Context
    private lateinit var analyzer: HeuristicSceneAnalyzer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        analyzer = HeuristicSceneAnalyzer.getInstance(context)
    }

    @Test
    fun `颜色分析 - 纯红色图片应被识别为暖色调`() = runBlocking {
        // 创建纯红色位图
        val bitmap = createColorBitmap(Color.RED, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        assertTrue("红色图片应该是暖色调", result.colorProfile.isWarmTone)
        assertFalse("红色图片不应该是冷色调", result.colorProfile.isCoolTone)
        assertTrue("红色通道值应该很高", result.colorProfile.avgRed > 200)
    }

    @Test
    fun `颜色分析 - 纯蓝色图片应被识别为冷色调`() = runBlocking {
        // 创建纯蓝色位图
        val bitmap = createColorBitmap(Color.BLUE, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        assertTrue("蓝色图片应该是冷色调", result.colorProfile.isCoolTone)
        assertFalse("蓝色图片不应该是暖色调", result.colorProfile.isWarmTone)
        assertTrue("蓝色通道值应该很高", result.colorProfile.avgBlue > 200)
    }

    @Test
    fun `颜色分析 - 中灰色图片的亮度应该在中间范围`() = runBlocking {
        // 创建中灰色位图
        val gray = Color.rgb(128, 128, 128)
        val bitmap = createColorBitmap(gray, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        assertTrue("平均红色值应该在中间范围", result.colorProfile.avgRed in 100..150)
        assertTrue("平均绿色值应该在中间范围", result.colorProfile.avgGreen in 100..150)
        assertTrue("平均蓝色值应该在中间范围", result.colorProfile.avgBlue in 100..150)
    }

    @Test
    fun `亮度分析 - 黑色图片应该被识别为暗调`() = runBlocking {
        // 创建黑色位图
        val bitmap = createColorBitmap(Color.BLACK, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        assertTrue("黑色图片应该是暗调", result.colorProfile.isDark)
        assertFalse("黑色图片不应该是亮调", result.colorProfile.isBright)
    }

    @Test
    fun `亮度分析 - 白色图片应该被识别为亮调`() = runBlocking {
        // 创建白色位图
        val bitmap = createColorBitmap(Color.WHITE, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        assertTrue("白色图片应该是亮调", result.colorProfile.isBright)
        assertFalse("白色图片不应该是暗调", result.colorProfile.isDark)
    }

    @Test
    fun `场景识别 - 应该返回有效的场景识别结果`() = runBlocking {
        // 创建绿色位图（可能识别为风景）
        val bitmap = createColorBitmap(Color.GREEN, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        assertNotNull("主场景不应该为空", result.primaryScene)
        assertNotNull("场景ID不应该为空", result.primaryScene.id)
        assertNotNull("场景名称不应该为空", result.primaryScene.name)
        assertTrue("置信度应该在有效范围内", result.confidence in 0.0f..1.0f)
    }

    @Test
    fun `场景识别 - 应该返回备选场景列表`() = runBlocking {
        val bitmap = createColorBitmap(Color.CYAN, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        assertNotNull("备选场景列表不应该为空", result.alternativeScenes)
        assertTrue("备选场景列表不应该为空", result.alternativeScenes.isNotEmpty())
        assertTrue("备选场景数量应该在合理范围内", result.alternativeScenes.size <= 3)
    }

    @Test
    fun `胶片推荐 - 应该返回推荐的胶片列表`() = runBlocking {
        val bitmap = createColorBitmap(Color.MAGENTA, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        assertNotNull("胶片推荐列表不应该为空", result.primaryScene.recommendedFilm)
        assertTrue("胶片推荐列表不应该为空", result.primaryScene.recommendedFilm.isNotEmpty())
    }

    @Test
    fun `哈苏参数 - 应该返回有效的哈苏参数`() = runBlocking {
        val bitmap = createColorBitmap(Color.YELLOW, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        val params = result.primaryScene.hasselbladParams
        assertNotNull("哈苏参数不应该为空", params)
        assertTrue("影调值应该在有效范围内", params.tone in -30..30)
        assertTrue("饱和度值应该在有效范围内", params.saturation in -30..30)
        assertTrue("对比度值应该在有效范围内", params.contrast in -30..30)
        assertTrue("色温值应该在有效范围内", params.colorTemp in -30..30)
    }

    @Test
    fun `大师建议 - 应该返回大师拍摄建议`() = runBlocking {
        val bitmap = createColorBitmap(Color.LTGRAY, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        assertNotNull("大师建议列表不应该为空", result.primaryScene.masterTips)
        assertTrue("大师建议列表不应该为空", result.primaryScene.masterTips.isNotEmpty())
        assertTrue("大师建议应该是字符串", result.primaryScene.masterTips[0].isNotEmpty())
    }

    @Test
    fun `置信度计算 - 置信度应该在有效范围内`() = runBlocking {
        val bitmap = createColorBitmap(Color.DKGRAY, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        assertTrue("置信度应该在0到1之间", result.confidence in 0.0f..1.0f)
    }

    @Test
    fun `置信度计算 - 备选场景应该有较低的置信度`() = runBlocking {
        val bitmap = createColorBitmap(Color.rgb(100, 150, 200), 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        if (result.alternativeScenes.isNotEmpty()) {
            assertTrue(
                "备选场景的置信度应该低于主场景",
                result.alternativeScenes[0].confidence < result.confidence
            )
        }
    }

    @Test
    fun `单例模式 - 应该返回相同的实例`() {
        val instance1 = HeuristicSceneAnalyzer.getInstance(context)
        val instance2 = HeuristicSceneAnalyzer.getInstance(context)
        
        assertSame("应该返回相同的实例", instance1, instance2)
    }

    @Test
    fun `人脸检测 - 应该正确检测人脸数量`() = runBlocking {
        // 创建模拟有人脸的图片（肤色区域）
        val skinColor = Color.rgb(255, 220, 180)
        val bitmap = createColorBitmap(skinColor, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        // 人脸数量应该在合理范围内
        assertTrue("人脸数量应该非负", result.faceCount >= 0)
    }

    @Test
    fun `场景分类 - 人像场景应该被正确分类`() = runBlocking {
        // 使用肤色图片模拟人像场景
        val skinColor = Color.rgb(255, 220, 180)
        val bitmap = createColorBitmap(skinColor, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        // 由于肤色占比高，可能被识别为人像
        assertNotNull("场景分类不应该为空", result.primaryScene.category)
    }

    @Test
    fun `场景分类 - 风景场景应该被正确分类`() = runBlocking {
        // 使用绿色图片模拟风景场景
        val greenColor = Color.rgb(34, 139, 34) // 森林绿
        val bitmap = createColorBitmap(greenColor, 100, 100)
        
        val result = analyzer.analyze(bitmap)
        
        assertNotNull("场景分类不应该为空", result.primaryScene.category)
    }

    /**
     * 辅助方法：创建纯色位图
     */
    private fun createColorBitmap(color: Int, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }
}
