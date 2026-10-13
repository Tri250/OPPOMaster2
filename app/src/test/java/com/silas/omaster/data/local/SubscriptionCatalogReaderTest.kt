package com.silas.omaster.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silas.omaster.data.config.ConfigCenter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/**
 * FD-02① 订阅目录预览单测（Robolectric）
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SubscriptionCatalogReaderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun fileFor(url: String): File =
        File(context.filesDir, ConfigCenter.getInstance(context).getSubscriptionFileName(url))

    @Test
    fun `解析本地订阅文件返回预设名称列表并过滤空白名`() {
        val url = "https://example.com/catalog-test.json"
        val file = fileFor(url)
        file.parentFile?.mkdirs()
        file.writeText(
            """{"build":1,"presets":[
                {"id":"p1","name":"胶片","coverPath":""},
                {"id":"p2","name":"黑白","coverPath":""},
                {"id":"p3","name":"  ","coverPath":""}
            ]}"""
        )
        try {
            val names = SubscriptionCatalogReader.readPresetNames(context, url)
            assertEquals(listOf("胶片", "黑白"), names)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `limit 参数截断返回数量`() {
        val url = "https://example.com/catalog-limit.json"
        val file = fileFor(url)
        file.parentFile?.mkdirs()
        file.writeText(
            """{"build":1,"presets":[
                {"id":"p1","name":"甲","coverPath":""},
                {"id":"p2","name":"乙","coverPath":""},
                {"id":"p3","name":"丙","coverPath":""}
            ]}"""
        )
        try {
            assertEquals(listOf("甲", "乙"), SubscriptionCatalogReader.readPresetNames(context, url, limit = 2))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `文件不存在或内容损坏返回空列表`() {
        val missing = SubscriptionCatalogReader.readPresetNames(context, "https://example.com/not-exist.json")
        assertTrue(missing.isEmpty())

        val url = "https://example.com/catalog-broken.json"
        val file = fileFor(url)
        file.parentFile?.mkdirs()
        file.writeText("{ 不是 JSON")
        try {
            assertTrue(SubscriptionCatalogReader.readPresetNames(context, url).isEmpty())
        } finally {
            file.delete()
        }
    }
}
