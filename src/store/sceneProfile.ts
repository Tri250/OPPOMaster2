// Web端统一数据模型 - 对齐 Android SceneProfile
// Layer 1: 大师数据 (Master Data)

/**
 * 场景大类（一级分类）
 */
export enum SceneCategory {
  PORTRAIT = 'PORTRAIT',      // 人像
  LANDSCAPE = 'LANDSCAPE',    // 风景
  NIGHT = 'NIGHT',            // 夜景
  FOOD = 'FOOD',              // 美食
  URBAN = 'URBAN',            // 城市
  STILL_LIFE = 'STILL_LIFE',  // 静物
  MACRO = 'MACRO',            // 微距
  EVENT = 'EVENT'             // 活动
}

export const SceneCategoryMeta: Record<SceneCategory, { displayName: string; icon: string; color: string }> = {
  [SceneCategory.PORTRAIT]: { displayName: '人像', icon: '👤', color: '#FF6B35' },
  [SceneCategory.LANDSCAPE]: { displayName: '风景', icon: '🏔️', color: '#4CAF50' },
  [SceneCategory.NIGHT]: { displayName: '夜景', icon: '🌃', color: '#2196F3' },
  [SceneCategory.FOOD]: { displayName: '美食', icon: '🍜', color: '#FF9800' },
  [SceneCategory.URBAN]: { displayName: '城市', icon: '🏢', color: '#9C27B0' },
  [SceneCategory.STILL_LIFE]: { displayName: '静物', icon: '🍃', color: '#00BCD4' },
  [SceneCategory.MACRO]: { displayName: '微距', icon: '🔍', color: '#E91E63' },
  [SceneCategory.EVENT]: { displayName: '活动', icon: '🎉', color: '#FF5722' },
};

/**
 * 柔光模式
 */
export enum SoftLightMode {
  NONE = 'NONE',      // 无
  SOFT = 'SOFT',      // 柔
  DREAMY = 'DREAMY'   // 梦幻
}

export const SoftLightModeMeta: Record<SoftLightMode, { displayName: string; description: string }> = {
  [SoftLightMode.NONE]: { displayName: '无', description: '标准效果' },
  [SoftLightMode.SOFT]: { displayName: '柔', description: '柔和光线效果' },
  [SoftLightMode.DREAMY]: { displayName: '梦幻', description: '梦幻柔光效果' },
};

/**
 * 胶片系列分类
 */
export enum FilmSeries {
  CLASSIC = 'CLASSIC',        // 原生经典: CC, NC, NH
  EMOTION = 'EMOTION',        // 情绪与表达: Portra, RDP3
  STRUCTURE = 'STRUCTURE',    // 结构与时间: 800T, TX400
  DIGITAL = 'DIGITAL'         // 数字记忆: 冷CCD, 暖CCD
}

export const FilmSeriesMeta: Record<FilmSeries, { displayName: string; films: string[] }> = {
  [FilmSeries.CLASSIC]: { displayName: '原生经典', films: ['cc', 'nc', 'nh'] },
  [FilmSeries.EMOTION]: { displayName: '情绪与表达', films: ['portra', 'rdp3'] },
  [FilmSeries.STRUCTURE]: { displayName: '结构与时间', films: ['800t', 'tx400'] },
  [FilmSeries.DIGITAL]: { displayName: '数字记忆', films: ['ccd_cool', 'ccd_warm'] },
};

/**
 * 哈苏大师参数（对齐 OPPO 大师模式真实参数范围）
 * 所有参数范围：-30 ~ +30
 */
export interface HasselbladParams {
  tone: number;           // 影调 -30 ~ +30
  saturation: number;     // 饱和度 -30 ~ +30
  contrast: number;       // 对比度 -30 ~ +30
  colorTemp: number;      // 色温 -30 ~ +30
  sharpness: number;      // 锐度 -30 ~ +30
  vignette: number;       // 暗角 -30 ~ +30
  cyanMagenta: number;    // 青品调 -30 ~ +30
  softLight: SoftLightMode;
  highlights?: number;    // 高光 -30 ~ +30
  shadows?: number;       // 阴影 -30 ~ +30
  clarity?: number;       // 清晰度 0 ~ +30
}

/**
 * 胶片预设（对齐 OPPO 9 款原生胶片）
 */
export interface FilmPreset {
  id: string;              // "portra", "cc", "nc", "nh", "rdp3", "800t", "tx400", "ccd_cool", "ccd_warm"
  name: string;            // "Portra 400", "CC 经典负片"
  series: FilmSeries;
  matchScore: number;      // 场景匹配度 0-1
  description: string;
}

/**
 * 相机参数
 */
export interface CameraParams {
  iso?: number;
  shutterSpeed?: string;
  aperture?: number;
  focalLength?: number;
  whiteBalance?: string;
  focusMode?: string;
}

/**
 * 统一场景数据模型
 */
export interface SceneProfile {
  id: string;                        // "portrait-backlit"
  name: string;                      // "逆光人像"
  category: SceneCategory;           // PORTRAIT
  description: string;               // "侧逆光环境下的柔美人像..."
  color: string;                     // 主题色 "#FF6B35" (哈苏橙)
  confidence: number;                // 识别置信度 0-1
  hasselbladParams: HasselbladParams;
  recommendedFilm: FilmPreset[];
  masterTips: string[];
  cameraParams?: CameraParams;
  timestamp?: number;
}

/**
 * 9款原生胶片完整定义
 */
export const ALL_FILM_PRESETS: FilmPreset[] = [
  // 原生经典系列
  { id: 'cc', name: 'CC 经典负片', series: FilmSeries.CLASSIC, matchScore: 0.85, description: '经典胶片质感，复古风格' },
  { id: 'nc', name: 'NC 自然', series: FilmSeries.CLASSIC, matchScore: 0.85, description: '自然柔和，日常记录' },
  { id: 'nh', name: 'NH 浓郁', series: FilmSeries.CLASSIC, matchScore: 0.85, description: '浓郁色彩，戏剧性' },
  
  // 情绪与表达系列
  { id: 'portra', name: 'Portra 400', series: FilmSeries.EMOTION, matchScore: 0.9, description: '柔和肤色，人像首选' },
  { id: 'rdp3', name: 'RDP3', series: FilmSeries.EMOTION, matchScore: 0.9, description: '反转片质感，高饱和' },
  
  // 结构与时间系列
  { id: '800t', name: '800T', series: FilmSeries.STRUCTURE, matchScore: 0.85, description: '夜景电影感，霓虹风格' },
  { id: 'tx400', name: 'TX400', series: FilmSeries.STRUCTURE, matchScore: 0.9, description: '经典黑白，颗粒粗犷' },
  
  // 数字记忆系列
  { id: 'ccd_cool', name: '冷 CCD', series: FilmSeries.DIGITAL, matchScore: 0.75, description: '冷色调，数字质感' },
  { id: 'ccd_warm', name: '暖 CCD', series: FilmSeries.DIGITAL, matchScore: 0.8, description: '暖色调，温馨氛围' },
];

/**
 * 50+ 场景哈苏参数映射表
 */
export const SCENE_PRESETS: SceneProfile[] = [
  // ========== PORTRAIT 人像 ==========
  {
    id: 'portrait-standard',
    name: '标准人像',
    category: SceneCategory.PORTRAIT,
    description: '柔和自然的人像拍摄',
    color: '#FF6B35',
    confidence: 0.85,
    hasselbladParams: { tone: -3, saturation: 10, contrast: -15, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[3], ALL_FILM_PRESETS[0]], // Portra, CC
    masterTips: ['使用大光圈获得柔和背景', '对焦眼睛确保清晰度', '肤色曝光向右曝光原则'],
  },
  {
    id: 'portrait-backlit',
    name: '逆光人像',
    category: SceneCategory.PORTRAIT,
    description: '侧逆光环境下的柔美人像',
    color: '#FF6B35',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 0, contrast: 0, colorTemp: -10, sharpness: -15, vignette: 20, cyanMagenta: 0, softLight: SoftLightMode.SOFT },
    recommendedFilm: [ALL_FILM_PRESETS[3], ALL_FILM_PRESETS[0]], // Portra, 和光
    masterTips: ['保留高光细节', '使用HDR或提升阴影', '逆光产生轮廓光'],
  },
  {
    id: 'portrait-studio',
    name: '棚拍人像',
    category: SceneCategory.PORTRAIT,
    description: '专业影棚控制光线',
    color: '#FF6B35',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 0, contrast: 15, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[2], ALL_FILM_PRESETS[0]], // NH, 浓郁
    masterTips: ['精确控制曝光', '使用专业灯光', '注意肤色还原'],
  },
  {
    id: 'portrait-bw',
    name: '黑白人像',
    category: SceneCategory.PORTRAIT,
    description: '经典黑白人像风格',
    color: '#FF6B35',
    confidence: 0.85,
    hasselbladParams: { tone: -10, saturation: -30, contrast: 25, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[6]], // TX400
    masterTips: ['关注光影对比', '突出人物轮廓', '经典黑白质感'],
  },
  {
    id: 'portrait-group',
    name: '合影',
    category: SceneCategory.PORTRAIT,
    description: '多人合影场景',
    color: '#FF6B35',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 8, contrast: -10, colorTemp: 0, sharpness: 12, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[0], ALL_FILM_PRESETS[1]], // CC, NC
    masterTips: ['注意站位协调', '整体光线均匀', '关注所有人表情'],
  },
  {
    id: 'portrait-child',
    name: '儿童',
    category: SceneCategory.PORTRAIT,
    description: '活泼可爱的儿童人像',
    color: '#FF6B35',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 12, contrast: -8, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.DREAMY },
    recommendedFilm: [ALL_FILM_PRESETS[8], ALL_FILM_PRESETS[3]], // 暖CCD, Portra
    masterTips: ['使用柔和光线', '捕捉自然表情', '梦幻柔光效果'],
  },
  {
    id: 'portrait-couple',
    name: '情侣',
    category: SceneCategory.PORTRAIT,
    description: '温馨浪漫的情侣人像',
    color: '#FF6B35',
    confidence: 0.85,
    hasselbladParams: { tone: 5, saturation: 10, contrast: -5, colorTemp: 10, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.SOFT },
    recommendedFilm: [ALL_FILM_PRESETS[3], ALL_FILM_PRESETS[8]], // Portra, 暖CCD
    masterTips: ['营造温馨氛围', '使用暖色调', '捕捉互动瞬间'],
  },
  {
    id: 'portrait-senior',
    name: '老人',
    category: SceneCategory.PORTRAIT,
    description: '有质感的老人人像',
    color: '#FF6B35',
    confidence: 0.85,
    hasselbladParams: { tone: -5, saturation: 5, contrast: 20, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[6], ALL_FILM_PRESETS[0]], // TX400, CC
    masterTips: ['突出人物质感', '关注光影对比', '经典黑白风格'],
  },

  // ========== LANDSCAPE 风景 ==========
  {
    id: 'landscape-standard',
    name: '标准风景',
    category: SceneCategory.LANDSCAPE,
    description: '自然风光拍摄',
    color: '#4CAF50',
    confidence: 0.85,
    hasselbladParams: { tone: 5, saturation: 15, contrast: 12, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[4], ALL_FILM_PRESETS[2]], // RDP3, 浓郁
    masterTips: ['使用小光圈获得全景深', '使用三脚架确保稳定', '注意构图层次'],
  },
  {
    id: 'landscape-sunset',
    name: '日落',
    category: SceneCategory.LANDSCAPE,
    description: '壮观的日落场景',
    color: '#4CAF50',
    confidence: 0.85,
    hasselbladParams: { tone: -5, saturation: 25, contrast: 0, colorTemp: 20, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[4], ALL_FILM_PRESETS[3]], // RDP3, Portra
    masterTips: ['等待最佳时刻', '保留高光细节', '使用渐变滤镜'],
  },
  {
    id: 'landscape-sky',
    name: '蓝天白云',
    category: SceneCategory.LANDSCAPE,
    description: '明亮的蓝天白云',
    color: '#4CAF50',
    confidence: 0.85,
    hasselbladParams: { tone: 10, saturation: 10, contrast: 0, colorTemp: -15, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[2], ALL_FILM_PRESETS[1]], // 浓郁, NC
    masterTips: ['注意天空比例', '避免过曝', '使用偏振镜'],
  },
  {
    id: 'landscape-forest',
    name: '森林',
    category: SceneCategory.LANDSCAPE,
    description: '郁郁葱葱的森林',
    color: '#4CAF50',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 20, contrast: 0, colorTemp: 5, sharpness: 15, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[2], ALL_FILM_PRESETS[0]], // 浓郁, CC
    masterTips: ['利用光线穿透', '注意层次感', '广角镜头'],
  },
  {
    id: 'landscape-autumn',
    name: '秋景',
    category: SceneCategory.LANDSCAPE,
    description: '绚丽多彩的秋景',
    color: '#4CAF50',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 25, contrast: 10, colorTemp: 15, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[4], ALL_FILM_PRESETS[2]], // RDP3, 浓郁
    masterTips: ['捕捉色彩变化', '注意光线角度', '突出秋叶质感'],
  },
  {
    id: 'landscape-snow',
    name: '雪景',
    category: SceneCategory.LANDSCAPE,
    description: '洁白的雪景',
    color: '#4CAF50',
    confidence: 0.85,
    hasselbladParams: { tone: 15, saturation: -5, contrast: -10, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[7], ALL_FILM_PRESETS[2]], // 冷CCD, NH
    masterTips: ['注意曝光补偿', '避免过曝', '突出纯净感'],
  },
  {
    id: 'landscape-beach',
    name: '海滩',
    category: SceneCategory.LANDSCAPE,
    description: '阳光明媚的海滩',
    color: '#4CAF50',
    confidence: 0.85,
    hasselbladParams: { tone: 10, saturation: 8, contrast: 0, colorTemp: -5, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[1], ALL_FILM_PRESETS[2]], // NC, 浓郁
    masterTips: ['注意水面反光', '使用偏振镜', '捕捉海浪动态'],
  },
  {
    id: 'landscape-waterfall',
    name: '瀑布',
    category: SceneCategory.LANDSCAPE,
    description: '柔美的瀑布场景',
    color: '#4CAF50',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 10, contrast: 0, colorTemp: 0, sharpness: 20, vignette: -10, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[2], ALL_FILM_PRESETS[4]], // 浓郁, RDP3
    masterTips: ['使用长曝光', '注意水流质感', '三脚架必备'],
  },
  {
    id: 'landscape-desert',
    name: '沙漠',
    category: SceneCategory.LANDSCAPE,
    description: '广袤神秘的沙漠',
    color: '#4CAF50',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 15, contrast: 0, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[4], ALL_FILM_PRESETS[0]], // RDP3, CC
    masterTips: ['注意光线角度', '利用沙丘纹理', '避免正午拍摄'],
  },

  // ========== NIGHT 夜景 ==========
  {
    id: 'night-city',
    name: '城市夜景',
    category: SceneCategory.NIGHT,
    description: '繁华璀璨的城市夜景',
    color: '#2196F3',
    confidence: 0.85,
    hasselbladParams: { tone: -15, saturation: 0, contrast: 25, colorTemp: -5, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[5], ALL_FILM_PRESETS[6]], // 800T, TX400
    masterTips: ['使用三脚架', '注意曝光平衡', '捕捉城市灯光'],
  },
  {
    id: 'night-neon',
    name: '霓虹灯',
    category: SceneCategory.NIGHT,
    description: '绚丽多彩的霓虹灯',
    color: '#2196F3',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 20, contrast: 15, colorTemp: -10, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[5], ALL_FILM_PRESETS[7]], // 800T, 冷CCD
    masterTips: ['寻找最佳角度', '注意色彩对比', '使用高ISO'],
  },
  {
    id: 'night-starry',
    name: '星空',
    category: SceneCategory.NIGHT,
    description: '浩瀚的星空',
    color: '#2196F3',
    confidence: 0.85,
    hasselbladParams: { tone: -20, saturation: 0, contrast: 30, colorTemp: 0, sharpness: 25, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[6], ALL_FILM_PRESETS[5]], // TX400, 800T
    masterTips: ['使用高ISO', '长曝光拍摄', '远离城市光污染'],
  },
  {
    id: 'night-candle',
    name: '烛光',
    category: SceneCategory.NIGHT,
    description: '柔和的烛光场景',
    color: '#2196F3',
    confidence: 0.85,
    hasselbladParams: { tone: -10, saturation: 5, contrast: 0, colorTemp: 15, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.SOFT },
    recommendedFilm: [ALL_FILM_PRESETS[8], ALL_FILM_PRESETS[3]], // 暖CCD, Portra
    masterTips: ['使用暖色调', '注意光线柔和', '避免过曝'],
  },
  {
    id: 'night-fireworks',
    name: '烟花',
    category: SceneCategory.NIGHT,
    description: '绚烂绽放的烟花',
    color: '#2196F3',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 15, contrast: 20, colorTemp: 0, sharpness: 15, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[4], ALL_FILM_PRESETS[5]], // RDP3, 800T
    masterTips: ['预判烟花位置', '使用长曝光', '注意构图'],
  },

  // ========== FOOD 美食 ==========
  {
    id: 'food-restaurant',
    name: '餐厅美食',
    category: SceneCategory.FOOD,
    description: '精致的餐厅美食',
    color: '#FF9800',
    confidence: 0.85,
    hasselbladParams: { tone: -5, saturation: 15, contrast: 0, colorTemp: 10, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[8], ALL_FILM_PRESETS[0]], // 暖CCD, CC
    masterTips: ['注意光线细节', '突出质感', '使用自然光'],
  },
  {
    id: 'food-dessert',
    name: '甜点',
    category: SceneCategory.FOOD,
    description: '精致诱人的甜点',
    color: '#FF9800',
    confidence: 0.85,
    hasselbladParams: { tone: 5, saturation: 20, contrast: 0, colorTemp: 5, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.SOFT },
    recommendedFilm: [ALL_FILM_PRESETS[8], ALL_FILM_PRESETS[0]], // 暖CCD, 和光
    masterTips: ['使用柔和光线', '突出色彩', '注意细节质感'],
  },
  {
    id: 'food-drink',
    name: '饮品',
    category: SceneCategory.FOOD,
    description: '清爽诱人的饮品',
    color: '#FF9800',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 10, contrast: 12, colorTemp: 5, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[1], ALL_FILM_PRESETS[0]], // NC, CC
    masterTips: ['注意透明质感', '突出色彩', '使用侧光'],
  },
  {
    id: 'food-bbq',
    name: '烧烤',
    category: SceneCategory.FOOD,
    description: '热气腾腾的烧烤',
    color: '#FF9800',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 10, contrast: 8, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[0], ALL_FILM_PRESETS[8]], // CC, 暖CCD
    masterTips: ['突出热气效果', '注意质感', '使用暖色调'],
  },

  // ========== URBAN 城市 ==========
  {
    id: 'urban-street',
    name: '街拍',
    category: SceneCategory.URBAN,
    description: '充满活力的街头',
    color: '#9C27B0',
    confidence: 0.85,
    hasselbladParams: { tone: -5, saturation: 5, contrast: 18, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[6], ALL_FILM_PRESETS[0]], // TX400, CC
    masterTips: ['捕捉瞬间', '注意构图', '关注人物动态'],
  },
  {
    id: 'urban-architecture',
    name: '建筑',
    category: SceneCategory.URBAN,
    description: '现代建筑设计',
    color: '#9C27B0',
    confidence: 0.85,
    hasselbladParams: { tone: 5, saturation: 5, contrast: 25, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[6], ALL_FILM_PRESETS[2]], // TX400, NH
    masterTips: ['注意线条构图', '选择合适角度', '突出结构'],
  },
  {
    id: 'urban-cafe',
    name: '咖啡馆',
    category: SceneCategory.URBAN,
    description: '温馨舒适的咖啡馆',
    color: '#9C27B0',
    confidence: 0.85,
    hasselbladParams: { tone: -10, saturation: 8, contrast: 0, colorTemp: 15, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.SOFT },
    recommendedFilm: [ALL_FILM_PRESETS[8], ALL_FILM_PRESETS[0]], // 暖CCD, 和光
    masterTips: ['营造氛围感', '使用柔和光线', '捕捉生活气息'],
  },
  {
    id: 'urban-museum',
    name: '博物馆',
    category: SceneCategory.URBAN,
    description: '庄严肃穆的博物馆',
    color: '#9C27B0',
    confidence: 0.85,
    hasselbladParams: { tone: -5, saturation: 0, contrast: 15, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[2], ALL_FILM_PRESETS[6]], // NH, TX400
    masterTips: ['尊重环境', '注意光线', '关注展品细节'],
  },
  {
    id: 'urban-station',
    name: '车站',
    category: SceneCategory.URBAN,
    description: '繁忙的交通枢纽',
    color: '#9C27B0',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 0, contrast: 15, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[5], ALL_FILM_PRESETS[6]], // 800T, TX400
    masterTips: ['注意动态捕捉', '利用环境光线', '关注人流'],
  },
  {
    id: 'urban-park',
    name: '公园',
    category: SceneCategory.URBAN,
    description: '城市中的绿洲',
    color: '#9C27B0',
    confidence: 0.85,
    hasselbladParams: { tone: 5, saturation: 0, contrast: 0, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[1], ALL_FILM_PRESETS[3]], // NC, Portra
    masterTips: ['利用自然光线', '注意季节变化', '捕捉生活气息'],
  },

  // ========== STILL_LIFE 静物 ==========
  {
    id: 'still-flower',
    name: '花卉',
    category: SceneCategory.STILL_LIFE,
    description: '精致的花卉摄影',
    color: '#00BCD4',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 5, contrast: 0, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[1], ALL_FILM_PRESETS[4]], // NC, RDP3
    masterTips: ['注意光线柔和', '突出质感', '关注细节'],
  },
  {
    id: 'still-product',
    name: '产品',
    category: SceneCategory.STILL_LIFE,
    description: '产品展示摄影',
    color: '#00BCD4',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 0, contrast: 8, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[2], ALL_FILM_PRESETS[4]], // NH, RDP3
    masterTips: ['控制光线均匀', '突出产品特点', '注意细节质感'],
  },
  {
    id: 'still-book',
    name: '书籍',
    category: SceneCategory.STILL_LIFE,
    description: '静谧的书籍场景',
    color: '#00BCD4',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 0, contrast: 5, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[8], ALL_FILM_PRESETS[1]], // 暖CCD, NC
    masterTips: ['营造阅读氛围', '注意光线质感', '突出细节'],
  },
  {
    id: 'still-art',
    name: '艺术品',
    category: SceneCategory.STILL_LIFE,
    description: '艺术品展示',
    color: '#00BCD4',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 0, contrast: 8, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[2], ALL_FILM_PRESETS[6]], // NH, TX400
    masterTips: ['注意光线控制', '突出艺术质感', '关注细节'],
  },

  // ========== MACRO 微距 ==========
  {
    id: 'macro-insect',
    name: '昆虫',
    category: SceneCategory.MACRO,
    description: '细致的昆虫摄影',
    color: '#E91E63',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 0, contrast: 0, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[4], ALL_FILM_PRESETS[2]], // RDP3, NH
    masterTips: ['使用微距镜头', '注意景深控制', '捕捉细节质感'],
  },
  {
    id: 'macro-water',
    name: '水滴',
    category: SceneCategory.MACRO,
    description: '晶莹剔透的水滴',
    color: '#E91E63',
    confidence: 0.85,
    hasselbladParams: { tone: 10, saturation: 0, contrast: 0, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[1], ALL_FILM_PRESETS[2]], // NC, NH
    masterTips: ['注意光线折射', '捕捉瞬间', '突出晶莹感'],
  },
  {
    id: 'macro-texture',
    name: '纹理',
    category: SceneCategory.MACRO,
    description: '细腻的纹理细节',
    color: '#E91E63',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 0, contrast: 25, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[6], ALL_FILM_PRESETS[4]], // TX400, RDP3
    masterTips: ['突出纹理质感', '注意光线角度', '关注细节'],
  },

  // ========== EVENT 活动 ==========
  {
    id: 'event-wedding',
    name: '婚礼',
    category: SceneCategory.EVENT,
    description: '温馨浪漫的婚礼',
    color: '#FF5722',
    confidence: 0.85,
    hasselbladParams: { tone: 5, saturation: 10, contrast: -5, colorTemp: 10, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.SOFT },
    recommendedFilm: [ALL_FILM_PRESETS[3], ALL_FILM_PRESETS[8]], // Portra, 暖CCD
    masterTips: ['捕捉重要瞬间', '注意光线柔和', '营造温馨氛围'],
  },
  {
    id: 'event-party',
    name: '派对',
    category: SceneCategory.EVENT,
    description: '热闹欢乐的派对',
    color: '#FF5722',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 12, contrast: 0, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[0], ALL_FILM_PRESETS[7]], // CC, ccd_cool
    masterTips: ['捕捉欢乐瞬间', '注意光线变化', '关注人物表情'],
  },
  {
    id: 'event-concert',
    name: '演唱会',
    category: SceneCategory.EVENT,
    description: '激情澎湃的演唱会',
    color: '#FF5722',
    confidence: 0.85,
    hasselbladParams: { tone: 0, saturation: 0, contrast: 20, colorTemp: 15, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [ALL_FILM_PRESETS[6], ALL_FILM_PRESETS[5]], // TX400, 800T
    masterTips: ['使用快速镜头', '注意舞台光线', '捕捉动态'],
  },
];

/**
 * 根据场景ID获取预设
 */
export function getScenePresetById(id: string): SceneProfile | undefined {
  return SCENE_PRESETS.find(preset => preset.id === id);
}

/**
 * 根据场景类别获取预设列表
 */
export function getScenePresetsByCategory(category: SceneCategory): SceneProfile[] {
  return SCENE_PRESETS.filter(preset => preset.category === category);
}

/**
 * 格式化哈苏参数显示值
 */
export function formatHasselbladParamValue(value: number): string {
  return value >= 0 ? `+${value}` : `${value}`;
}

/**
 * 创建默认哈苏参数
 */
export function createDefaultHasselbladParams(): HasselbladParams {
  return {
    tone: 0,
    saturation: 0,
    contrast: 0,
    colorTemp: 0,
    sharpness: 0,
    vignette: 0,
    cyanMagenta: 0,
    softLight: SoftLightMode.NONE,
    highlights: 0,
    shadows: 0,
    clarity: 0,
  };
}