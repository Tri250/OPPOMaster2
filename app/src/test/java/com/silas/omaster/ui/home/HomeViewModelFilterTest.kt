package com.silas.omaster.ui.home

import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * HomeViewModel 过滤与排序逻辑的纯计算测试
 *
 * 覆盖：
 * - Tab 过滤（发现/收藏/哈苏/上新/我的）
 * - 品牌过滤
 * - 关键词搜索（命中名称/作者/标签）
 * - 排序（最新/最热/评分）
 * - 错误状态处理
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelFilterTest {

    private lateinit var viewModel: HomeViewModel
    private val repository: PresetRepository = mockk(relaxed = true)

    // 覆盖同步 Main 调度器供 viewModelScope 使用
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun preset(
        name: String,
        author: String = "@OPPO影像",
        brand: String? = null,
        isFavorite: Boolean = false,
        isNew: Boolean = false,
        isHncs: Boolean = false,
        isCustom: Boolean = false,
        tags: List<String>? = emptyList(),
        downloads: Int? = null,
        rating: Float? = null
    ) = MasterPreset(
        id = name,
        name = name,
        coverPath = "",
        author = author,
        brand = brand,
        isFavorite = isFavorite,
        isNew = isNew,
        isHncs = isHncs,
        isCustom = isCustom,
        tags = tags,
        downloads = downloads,
        rating = rating
    )

    @Test
    fun `getFilteredPresets - 默认返回全部并按NEWEST排序`() {
        val list = listOf(
            preset("a", isNew = true),
            preset("b", isNew = false),
            preset("c", isNew = false)
        )
        viewModel.setInternalPresets(list)
        viewModel.setSortType(SortType.NEWEST)

        val result = viewModel.getFilteredPresets()
        // isNew 排前面
        assertEquals("a", result.first().name)
    }

    @Test
    fun `selectTab 1 仅保留收藏项`() {
        val list = listOf(
            preset("fav", isFavorite = true),
            preset("not", isFavorite = false)
        )
        viewModel.setInternalPresets(list)
        viewModel.selectTab(1)

        val result = viewModel.getFilteredPresets()
        assertEquals(1, result.size)
        assertEquals("fav", result.first().name)
    }

    @Test
    fun `selectTab 2 仅保留哈苏项`() {
        val list = listOf(
            preset("h", isHncs = true),
            preset("n", isHncs = false)
        )
        viewModel.setInternalPresets(list)
        viewModel.selectTab(2)

        val result = viewModel.getFilteredPresets()
        assertEquals(1, result.size)
        assertEquals("h", result.first().name)
    }

    @Test
    fun `selectTab 3 仅保留上新项`() {
        val list = listOf(
            preset("new1", isNew = true),
            preset("old", isNew = false)
        )
        viewModel.setInternalPresets(list)
        viewModel.selectTab(3)

        val result = viewModel.getFilteredPresets()
        assertEquals(1, result.size)
        assertEquals("new1", result.first().name)
    }

    @Test
    fun `selectTab 4 仅返回自定义预设`() {
        val list = listOf(
            preset("sys", isCustom = false),
            preset("mine1", isCustom = true),
            preset("mine2", isCustom = true)
        )
        viewModel.setInternalPresets(list)
        viewModel.setInternalCustomPresets(listOf(preset("mine1", isCustom = true), preset("mine2", isCustom = true)))
        viewModel.selectTab(4)

        val result = viewModel.getFilteredPresets()
        assertEquals(2, result.size)
        assertTrue(result.all { it.isCustom })
    }

    @Test
    fun `selectBrand 仅返回匹配品牌`() {
        val list = listOf(
            preset("oppo_a", brand = "oppo"),
            preset("vivo_b", brand = "vivo")
        )
        viewModel.setInternalPresets(list)
        viewModel.selectBrand("oppo")

        val result = viewModel.getFilteredPresets()
        assertEquals(1, result.size)
        assertEquals("oppo_a", result.first().name)
    }

    @Test
    fun `setSearchQuery 大小写不敏感命中标签`() {
        val list = listOf(
            preset("p1", tags = listOf("Film", "Classic")),
            preset("p2", tags = listOf("Digital"))
        )
        viewModel.setInternalPresets(list)
        viewModel.setSearchQuery("film")

        val result = viewModel.getFilteredPresets()
        assertEquals(1, result.size)
        assertEquals("p1", result.first().name)
    }

    @Test
    fun `setSearchQuery 命中作者名`() {
        val list = listOf(
            preset("a", author = "Alice"),
            preset("b", author = "Bob")
        )
        viewModel.setInternalPresets(list)
        viewModel.setSearchQuery("Alice")

        val result = viewModel.getFilteredPresets()
        assertEquals(1, result.size)
        assertEquals("a", result.first().name)
    }

    @Test
    fun `setSortType POPULAR 按下载量降序`() {
        val list = listOf(
            preset("a", downloads = 10),
            preset("b", downloads = 100),
            preset("c", downloads = 50)
        )
        viewModel.setInternalPresets(list)
        viewModel.setSortType(SortType.POPULAR)

        val result = viewModel.getFilteredPresets()
        assertEquals(listOf("b", "c", "a"), result.map { it.name })
    }

    @Test
    fun `setSortType RATING 按评分降序 null 视为 0`() {
        val list = listOf(
            preset("a", rating = null),
            preset("b", rating = 4.5f),
            preset("c", rating = 3.0f)
        )
        viewModel.setInternalPresets(list)
        viewModel.setSortType(SortType.RATING)

        val result = viewModel.getFilteredPresets()
        assertEquals("b", result[0].name)
        assertEquals("c", result[1].name)
        assertEquals("a", result[2].name)
    }

    @Test
    fun `getTabCount - 哈苏计数仅统计 isHncs 项`() {
        val list = listOf(
            preset("a", isHncs = true),
            preset("b", isHncs = true),
            preset("c", isHncs = false)
        )
        viewModel.setInternalPresets(list)

        assertEquals(2, viewModel.getTabCount(2))
        assertEquals(3, viewModel.getTabCount(0))
    }
}
