package com.silas.omaster.colorcard

import com.silas.omaster.model.ColorCard
import com.silas.omaster.model.ColorInfo
import com.silas.omaster.model.ColorRole

/**
 * 色卡图书馆
 * 参考 iCurrer/OMaster ColorCardLibrary
 * 
 * 整合 12 个摄影色彩主题：
 * - 城市暖调、海边清新、森林绿、工业冷调
 * - 夕阳余晖、霓虹之夜、复古胶片
 * - 极简黑白、春日樱花、咖啡时光、深邃海洋、秋日落叶
 * 
 * 每个色卡含：色板 + 主题 + 描述 + 拍摄技巧 + 挑战 + 场景标签
 */
object ColorCardLibrary {

    private object C {
        // 颜色定义
        val brickRed       = ColorInfo("D4A574", "赤陶红", ColorRole.PRIMARY)
        val darkBrown      = ColorInfo("8B4513", "深棕", ColorRole.PRIMARY)
        val bisque         = ColorInfo("FFE4C4", "米黄", ColorRole.ACCENT)
        val darkGray       = ColorInfo("4A4A4A", "深灰", ColorRole.ACCENT)
        val skyBlue        = ColorInfo("87CEEB", "天蓝", ColorRole.PRIMARY)
        val powderBlue     = ColorInfo("B0E0E6", "粉蓝", ColorRole.PRIMARY)
        val whitePrimary   = ColorInfo("FFFFFF", "白", ColorRole.PRIMARY)
        val whiteAccent    = ColorInfo("FFFFFF", "白", ColorRole.ACCENT)
        val wheat          = ColorInfo("F5DEB3", "麦色", ColorRole.ACCENT)
        val forestGreen    = ColorInfo("228B22", "森林绿", ColorRole.PRIMARY)
        val lightGreen     = ColorInfo("90EE90", "浅绿", ColorRole.PRIMARY)
        val saddleBrown    = ColorInfo("8B4513", "鞍棕", ColorRole.ACCENT)
        val beige          = ColorInfo("F5F5DC", "米色", ColorRole.ACCENT)
        val slateGray      = ColorInfo("708090", "石板灰", ColorRole.PRIMARY)
        val steelBlue      = ColorInfo("4682B4", "钢蓝", ColorRole.PRIMARY)
        val darkSlateGray  = ColorInfo("2F4F4F", "暗石板灰", ColorRole.ACCENT)
        val silver         = ColorInfo("C0C0C0", "银", ColorRole.ACCENT)
        val coral          = ColorInfo("FF7F50", "珊瑚", ColorRole.PRIMARY)
        val lightPink      = ColorInfo("FFB6C1", "粉红", ColorRole.PRIMARY)
        val mediumPurple   = ColorInfo("9370DB", "中紫", ColorRole.ACCENT)
        val midnightBlue   = ColorInfo("191970", "午夜蓝", ColorRole.ACCENT)
        val deepPink       = ColorInfo("FF1493", "深粉", ColorRole.PRIMARY)
        val cyan           = ColorInfo("00FFFF", "青", ColorRole.PRIMARY)
        val gold           = ColorInfo("FFD700", "金", ColorRole.ACCENT)
        val navy           = ColorInfo("000080", "海军蓝", ColorRole.ACCENT)
        val tan            = ColorInfo("D2B48C", "棕褐", ColorRole.PRIMARY)
        val siennaPrimary  = ColorInfo("A0522D", "赭石", ColorRole.PRIMARY)
        val khaki          = ColorInfo("F0E68C", "卡其", ColorRole.ACCENT)
        val darkOliveGreen = ColorInfo("556B2F", "深橄榄绿", ColorRole.ACCENT)
        val black          = ColorInfo("000000", "黑", ColorRole.PRIMARY)
        val gray           = ColorInfo("808080", "灰", ColorRole.ACCENT)
        val lightGray      = ColorInfo("D3D3D3", "浅灰", ColorRole.ACCENT)
        val cherryBlossom  = ColorInfo("FFB7C5", "樱花粉", ColorRole.PRIMARY)
        val hotPink        = ColorInfo("FF69B4", "桃红", ColorRole.PRIMARY)
        val paleGreen      = ColorInfo("98FB98", "淡绿", ColorRole.ACCENT)
        val lemonChiffon   = ColorInfo("FFFACD", "柠檬绸", ColorRole.ACCENT)
        val coffee         = ColorInfo("6F4E37", "咖啡", ColorRole.PRIMARY)
        val chocolate      = ColorInfo("D2691E", "巧克力", ColorRole.PRIMARY)
        val cornsilk       = ColorInfo("FFF8DC", "玉米丝", ColorRole.ACCENT)
        val siennaAccent   = ColorInfo("A0522D", "赭石", ColorRole.ACCENT)
        val oceanBlue      = ColorInfo("006994", "海洋蓝", ColorRole.PRIMARY)
        val darkBlue       = ColorInfo("003366", "深蓝", ColorRole.PRIMARY)
        val turquoise      = ColorInfo("40E0D0", "松石绿", ColorRole.ACCENT)
        val honeydew       = ColorInfo("F0FFF0", "蜜瓜绿", ColorRole.ACCENT)
        val orangeRed      = ColorInfo("FF4500", "橙红", ColorRole.PRIMARY)
        val goldenrod      = ColorInfo("DAA520", "金菊黄", ColorRole.PRIMARY)
        val darkRed        = ColorInfo("8B0000", "暗红", ColorRole.ACCENT)
    }

    private fun card(
        id: String,
        theme: String,
        desc: String,
        tips: String,
        challenge: String,
        colors: List<ColorInfo>,
        tags: List<String>
    ) = ColorCard(id, colors, theme, desc, tips, challenge, tags)

    val allCards = with(C) {
        listOf(
            card(
                "urban_warm",
                "城市暖调",
                "在钢筋水泥的城市里寻找温暖的光线，捕捉日落前后的暖意。",
                "利用黄金时刻的低角度光线，注意玻璃幕墙的反光，多角度尝试同一建筑。",
                "挑战:在同一个十字路口拍摄 10 张不同色调的照片。",
                listOf(brickRed, darkBrown, bisque, darkGray),
                listOf("建筑", "街拍", "光线")
            ),
            card(
                "seaside_fresh",
                "海边清新",
                "海洋与天空的蓝，沙滩的米白，呈现最纯净的自然色调。",
                "关注水平线的位置，逆光拍摄波光粼粼的海面，捕捉白色帆船的剪影。",
                "挑战:只使用蓝白两色完成一组海景作品。",
                listOf(skyBlue, powderBlue, whiteAccent, wheat),
                listOf("海边", "天空", "极简")
            ),
            card(
                "forest_green",
                "森林绿",
                "深入森林，记录植物的层次与光影。",
                "使用长焦压缩空间感，注意叶片上的高光，逆光拍摄呈现半透明感。",
                "挑战:在一片树林里找到 5 种不同层次的绿色。",
                listOf(forestGreen, lightGreen, darkBrown, beige),
                listOf("公园", "植物", "自然")
            ),
            card(
                "industrial_cool",
                "工业冷调",
                "工厂、桥梁、机械的冷峻金属质感。",
                "利用蓝色时刻拍摄工业建筑的几何线条，注意重复结构的对称美。",
                "挑战:在工业场景中寻找 3 个完美的对称构图。",
                listOf(slateGray, steelBlue, darkSlateGray, silver),
                listOf("工业", "几何", "桥梁")
            ),
            card(
                "sunset_glow",
                "夕阳余晖",
                "黄昏时分的浪漫色彩，粉紫蓝的多彩天空。",
                "提前 30 分钟到达机位，使用包围曝光，云层厚度的判断是关键。",
                "挑战:在同一天拍摄从黄金时刻到蓝调时刻的 5 张照片。",
                listOf(coral, lightPink, mediumPurple, midnightBlue),
                listOf("夕阳", "城市天际线", "倒影")
            ),
            card(
                "neon_night",
                "霓虹之夜",
                "赛博朋克风的夜晚，霓虹灯的色彩狂欢。",
                "雨后的反光地面是最佳时机，使用高 ISO 拍摄，注意色温的冷暖对比。",
                "挑战:在雨夜拍摄一组赛博朋克风格的城市夜景。",
                listOf(deepPink, cyan, gold, navy),
                listOf("夜景", "霓虹", "城市")
            ),
            card(
                "vintage_film",
                "复古胶片",
                "怀旧色调，讲述时光的故事。",
                "寻找老街、老建筑、老物件，模拟 Kodak Portra 400 胶片的色彩。",
                "挑战:用手机拍摄一组'80年代'风格的城市记忆。",
                listOf(tan, siennaPrimary, khaki, darkOliveGreen),
                listOf("老街", "咖啡馆", "故事感")
            ),
            card(
                "minimalist_bw",
                "极简黑白",
                "去除色彩的干扰，专注于光影与构图。",
                "寻找有强烈光影对比的场景，注意黑白的灰度过渡。",
                "挑战:在一座建筑里找到 10 个值得拍摄的黑白画面。",
                listOf(black, whitePrimary, gray, lightGray),
                listOf("黑白", "建筑", "光影")
            ),
            card(
                "spring_blossom",
                "春日樱花",
                "春天的粉色浪漫，樱花的柔美时刻。",
                "选择蓝天为背景，仰拍樱花枝条，注意光圈虚化效果。",
                "挑战:在同一棵樱花树下拍摄 10 种不同构图的樱花照片。",
                listOf(cherryBlossom, hotPink, paleGreen, lemonChiffon),
                listOf("花卉", "公园", "柔光")
            ),
            card(
                "coffee_time",
                "咖啡时光",
                "咖啡馆的温馨时刻，咖啡的醇厚色调。",
                "利用窗边自然光，注意杯子的摆放构图，背景虚化突出主体。",
                "挑战:用 5 个不同角度拍摄同一杯咖啡。",
                listOf(coffee, chocolate, cornsilk, siennaAccent),
                listOf("咖啡馆", "静物", "室内")
            ),
            card(
                "ocean_deep",
                "深邃海洋",
                "大海的深邃蓝调，蓝色时刻的极致表达。",
                "蓝调时刻拍摄海边，使用长曝光拍摄海面雾化效果。",
                "挑战:在海边连续拍摄 24 小时，记录光影变化。",
                listOf(oceanBlue, darkBlue, turquoise, honeydew),
                listOf("海洋", "蓝调时刻", "倒影")
            ),
            card(
                "autumn_leaves",
                "秋日落叶",
                "金色的秋天，落叶的温暖色调。",
                "逆光拍摄树叶的透光感，注意地面落叶的层次。",
                "挑战:用红色、黄色、棕色三种主色完成一组秋景。",
                listOf(orangeRed, goldenrod, darkRed, wheat),
                listOf("秋景", "公园", "逆光")
            )
        )
    }

    /**
     * 获取随机色卡（每日推荐）
     */
    fun getRandomCard(): ColorCard = allCards.random()

    /**
     * 根据 ID 获取色卡
     */
    fun getCardById(id: String): ColorCard? = allCards.find { it.id == id }

    /**
     * 根据标签筛选
     */
    fun getCardsByTag(tag: String): List<ColorCard> {
        return allCards.filter { card -> card.tags.any { it.contains(tag) } }
    }
}
