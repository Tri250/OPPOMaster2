// 哈苏大师数据模型 - 与Android端同步

// 场景大类
export enum SceneCategory {
  PORTRAIT = 'portrait',
  LANDSCAPE = 'landscape',
  NIGHT = 'night',
  FOOD = 'food',
  URBAN = 'urban',
  PET = 'pet',
  MACRO = 'macro',
  SPECIAL = 'special'
}

export const SceneCategoryInfo: Record<SceneCategory, { displayName: string; icon: string }> = {
  [SceneCategory.PORTRAIT]: { displayName: '人像', icon: '👤' },
  [SceneCategory.LANDSCAPE]: { displayName: '风景', icon: '🏔️' },
  [SceneCategory.NIGHT]: { displayName: '夜景', icon: '🌃' },
  [SceneCategory.FOOD]: { displayName: '美食', icon: '🍜' },
  [SceneCategory.URBAN]: { displayName: '城市', icon: '🏙️' },
  [SceneCategory.PET]: { displayName: '宠物', icon: '🐕' },
  [SceneCategory.MACRO]: { displayName: '微距', icon: '🔍' },
  [SceneCategory.SPECIAL]: { displayName: '特殊', icon: '✨' }
}

// 柔光模式
export enum SoftLightMode {
  NONE = 'none',
  SOFT = 'soft',
  DREAMY = 'dreamy'
}

export const SoftLightModeInfo: Record<SoftLightMode, { displayName: string }> = {
  [SoftLightMode.NONE]: { displayName: '无' },
  [SoftLightMode.SOFT]: { displayName: '柔美' },
  [SoftLightMode.DREAMY]: { displayName: '梦幻' }
}

// 哈苏大师参数（对齐OPPO大师模式）
export interface HasselbladParams {
  tone: number;           // 影调 -30 ~ +30
  saturation: number;     // 饱和度 -30 ~ +30
  contrast: number;       // 对比度 -30 ~ +30
  colorTemp: number;      // 色温 -30 ~ +30
  sharpness: number;      // 锐度 -30 ~ +30
  vignette: number;       // 暗角 -30 ~ +30
  cyanMagenta: number;    // 青品调 -30 ~ +30
  softLight: SoftLightMode;
}

export const DEFAULT_HASSELBLAD_PARAMS: HasselbladParams = {
  tone: 0,
  saturation: 0,
  contrast: 0,
  colorTemp: 0,
  sharpness: 0,
  vignette: 0,
  cyanMagenta: 0,
  softLight: SoftLightMode.NONE
}

// 胶片系列
export enum FilmSeries {
  CLASSIC = 'classic',
  EMOTION = 'emotion',
  STRUCTURE = 'structure',
  DIGITAL = 'digital'
}

export const FilmSeriesInfo: Record<FilmSeries, { displayName: string; color: string }> = {
  [FilmSeries.CLASSIC]: { displayName: '原生经典', color: '#FFB800' },
  [FilmSeries.EMOTION]: { displayName: '情绪与表达', color: '#FF6B9B' },
  [FilmSeries.STRUCTURE]: { displayName: '结构与时间', color: '#6B7FFF' },
  [FilmSeries.DIGITAL]: { displayName: '数字记忆', color: '#00D4AA' }
}

// 胶片预设（9款原生胶片）
export interface FilmPreset {
  id: string;
  name: string;
  displayName: string;
  series: FilmSeries;
  matchScore: number;
  description: string;
  colorCharacteristics: string;
  bestFor: string[];
}

export const FILM_PRESETS: FilmPreset[] = [
  // 原生经典系列
  { id: 'cc', name: 'CC', displayName: 'CC 经典负片', series: FilmSeries.CLASSIC, matchScore: 0, description: '经典负片风格，色彩浓郁复古', colorCharacteristics: '浓郁暖调，高对比度', bestFor: ['街拍', '人像', '风景', '建筑'] },
  { id: 'nc', name: 'NC', displayName: '富士 NC', series: FilmSeries.CLASSIC, matchScore: 0, description: '富士经典负片，柔和自然', colorCharacteristics: '柔和自然，日系风格', bestFor: ['人像', '日常', '旅行', '日系'] },
  { id: 'nh', name: 'NH', displayName: 'NH 浓郁', series: FilmSeries.CLASSIC, matchScore: 0, description: '浓郁色彩，强烈对比', colorCharacteristics: '浓郁饱和，高对比', bestFor: ['风景', '建筑', '棚拍', '艺术'] },

  // 情绪与表达系列
  { id: 'portra', name: 'Portra', displayName: 'Portra 400', series: FilmSeries.EMOTION, matchScore: 0, description: '柯达 Portra 400，柔美人像胶片', colorCharacteristics: '柔美肤色，低对比度', bestFor: ['人像', '逆光', '婚礼', '柔美'] },
  { id: 'rdp3', name: 'RDP3', displayName: 'RDP3', series: FilmSeries.EMOTION, matchScore: 0, description: '富士 Velvia 风格，风景专用', colorCharacteristics: '高饱和，风景专用', bestFor: ['风景', '日落', '秋景', '自然'] },

  // 结构与时间系列
  { id: '800t', name: '800T', displayName: '800T', series: FilmSeries.STRUCTURE, matchScore: 0, description: '夜景胶片，霓虹灯专用', colorCharacteristics: '夜景专用，霓虹感', bestFor: ['夜景', '霓虹', '城市', '星空'] },
  { id: 'tx400', name: 'TX400', displayName: 'TX400', series: FilmSeries.STRUCTURE, matchScore: 0, description: '黑白胶片，经典质感', colorCharacteristics: '黑白经典，高对比', bestFor: ['黑白', '街拍', '建筑', '纪实'] },

  // 数字记忆系列
  { id: 'ccd_cool', name: 'CCD-Cool', displayName: '冷调 CCD', series: FilmSeries.DIGITAL, matchScore: 0, description: '数码 CCD 冷调风格', colorCharacteristics: '冷色调，清透感', bestFor: ['雪景', '天空', '海滩', '冷调'] },
  { id: 'ccd_warm', name: 'CCD-Warm', displayName: '暖调 CCD', series: FilmSeries.DIGITAL, matchScore: 0, description: '数码 CCD 暖调风格', colorCharacteristics: '暖色调，温馨感', bestFor: ['美食', '咖啡馆', '烛光', '儿童'] }
]

// 场景配置
export interface SceneProfile {
  id: string;
  name: string;
  category: SceneCategory;
  subCategory: string;
  description: string;
  color: number;
  confidence: number;
  hasselbladParams: HasselbladParams;
  recommendedFilm: FilmPreset[];
  masterTips: string[];
  tags: string[];
  bestTime?: string;
  environmentTips?: string;
}

// 哈苏橙主题色
export const HASSELBLAD_ORANGE = 0xFFFF6B35

// 50+ 场景配置库（与Android端同步）
export const SCENE_PROFILES: SceneProfile[] = [
  // ─── 人像系列 ───
  {
    id: 'portrait-standard',
    name: '标准人像',
    category: SceneCategory.PORTRAIT,
    subCategory: '标准人像',
    description: '自然光环境下的标准人像拍摄，追求真实自然的肤色表现',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: -3, saturation: 10, contrast: -15, colorTemp: -5, sharpness: -15, vignette: 20, cyanMagenta: -5, softLight: SoftLightMode.SOFT },
    recommendedFilm: [
      { ...FILM_PRESETS[3], matchScore: 0.85 },
      { ...FILM_PRESETS[0], matchScore: 0.70 }
    ],
    masterTips: ['使用柔光模式营造自然肤色', '降低对比度保持皮肤质感', '注意眼神光和面部轮廓'],
    tags: ['人像', '自然光', '柔美'],
    bestTime: '上午10点-下午4点',
    environmentTips: '自然光或柔和人工光源，避免直射阳光'
  },
  {
    id: 'portrait-backlit',
    name: '逆光人像',
    category: SceneCategory.PORTRAIT,
    subCategory: '逆光人像',
    description: '侧逆光环境下的柔美人像，营造梦幻光晕效果',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: -5, saturation: 12, contrast: -10, colorTemp: -10, sharpness: -10, vignette: 25, cyanMagenta: -8, softLight: SoftLightMode.DREAMY },
    recommendedFilm: [
      { ...FILM_PRESETS[3], matchScore: 0.90 },
      { ...FILM_PRESETS[8], matchScore: 0.75 }
    ],
    masterTips: ['利用逆光创造光晕效果', '梦幻柔光增强氛围感', '注意面部曝光补偿'],
    tags: ['人像', '逆光', '梦幻', '光晕'],
    bestTime: '日落前1-2小时',
    environmentTips: '侧逆光或全逆光环境，寻找有遮挡的背景'
  },
  {
    id: 'portrait-studio',
    name: '棚拍人像',
    category: SceneCategory.PORTRAIT,
    subCategory: '棚拍人像',
    description: '专业摄影棚环境，追求高对比度和浓郁色彩',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 0, saturation: 0, contrast: 15, colorTemp: 0, sharpness: 10, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [
      { ...FILM_PRESETS[2], matchScore: 0.85 },
      { ...FILM_PRESETS[0], matchScore: 0.70 }
    ],
    masterTips: ['高对比度突出轮廓', '注意光影造型', '控制背景简洁'],
    tags: ['人像', '棚拍', '专业', '高对比'],
    environmentTips: '专业摄影棚，可控光源环境'
  },
  {
    id: 'portrait-bw',
    name: '黑白人像',
    category: SceneCategory.PORTRAIT,
    subCategory: '黑白人像',
    description: '经典黑白人像，追求光影质感和艺术表达',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: -10, saturation: -30, contrast: 25, colorTemp: 0, sharpness: 20, vignette: 15, cyanMagenta: 0, softLight: SoftLightMode.SOFT },
    recommendedFilm: [
      { ...FILM_PRESETS[6], matchScore: 0.95 }
    ],
    masterTips: ['黑白摄影注重光影对比', '寻找有纹理和质感的场景', '注意构图简洁有力'],
    tags: ['人像', '黑白', '艺术', '光影'],
    bestTime: '强烈光影对比时段',
    environmentTips: '强烈光影对比场景，如阳光直射或聚光灯'
  },
  {
    id: 'portrait-group',
    name: '合影',
    category: SceneCategory.PORTRAIT,
    subCategory: '合影',
    description: '多人合影拍摄，追求整体协调和清晰度',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 0, saturation: 8, contrast: -10, colorTemp: 0, sharpness: 12, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [
      { ...FILM_PRESETS[0], matchScore: 0.75 },
      { ...FILM_PRESETS[1], matchScore: 0.70 }
    ],
    masterTips: ['确保所有人清晰可见', '注意站位和表情', '使用小光圈保证景深'],
    tags: ['人像', '合影', '多人'],
    environmentTips: '光线均匀的环境，避免强光阴影'
  },
  {
    id: 'portrait-children',
    name: '儿童',
    category: SceneCategory.PORTRAIT,
    subCategory: '儿童',
    description: '儿童摄影，追求温馨柔和的色彩表现',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 5, saturation: 12, contrast: -8, colorTemp: 10, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.DREAMY },
    recommendedFilm: [
      { ...FILM_PRESETS[8], matchScore: 0.85 },
      { ...FILM_PRESETS[3], matchScore: 0.80 }
    ],
    masterTips: ['梦幻柔光营造童话氛围', '捕捉自然表情和动作', '注意安全距离'],
    tags: ['人像', '儿童', '温馨', '梦幻'],
    bestTime: '清晨或阴天散射光',
    environmentTips: '柔和光线环境，色彩丰富的场景'
  },

  // ─── 风景系列 ───
  {
    id: 'landscape-standard',
    name: '标准风景',
    category: SceneCategory.LANDSCAPE,
    subCategory: '标准风景',
    description: '自然风景拍摄，追求色彩饱满和清晰度',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 5, saturation: 15, contrast: 12, colorTemp: 0, sharpness: 15, vignette: -5, cyanMagenta: -3, softLight: SoftLightMode.NONE },
    recommendedFilm: [
      { ...FILM_PRESETS[4], matchScore: 0.85 },
      { ...FILM_PRESETS[2], matchScore: 0.75 }
    ],
    masterTips: ['使用HDR增强动态范围', '注意构图层次', '寻找前景增加纵深感'],
    tags: ['风景', '自然', 'HDR'],
    bestTime: '日出后或日落前',
    environmentTips: '光线充足的户外环境'
  },
  {
    id: 'landscape-sunset',
    name: '日落',
    category: SceneCategory.LANDSCAPE,
    subCategory: '日落',
    description: '日落时分，追求暖色调和浓郁色彩',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: -5, saturation: 25, contrast: 10, colorTemp: 20, sharpness: 12, vignette: 0, cyanMagenta: 5, softLight: SoftLightMode.NONE },
    recommendedFilm: [
      { ...FILM_PRESETS[4], matchScore: 0.90 },
      { ...FILM_PRESETS[3], matchScore: 0.80 }
    ],
    masterTips: ['增强暖色调表现', '寻找有层次的天空', '注意曝光控制'],
    tags: ['风景', '日落', '暖调', '天空'],
    bestTime: '日落前30分钟',
    environmentTips: '开阔视野，有云层的天空更佳'
  },
  {
    id: 'landscape-blue-sky',
    name: '蓝天白云',
    category: SceneCategory.LANDSCAPE,
    subCategory: '蓝天白云',
    description: '蓝天白云场景，追求通透清新',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 10, saturation: 10, contrast: 8, colorTemp: -15, sharpness: 12, vignette: 0, cyanMagenta: -5, softLight: SoftLightMode.NONE },
    recommendedFilm: [
      { ...FILM_PRESETS[2], matchScore: 0.80 },
      { ...FILM_PRESETS[1], matchScore: 0.75 }
    ],
    masterTips: ['冷色调增强蓝色表现', '注意天空与地面比例', '寻找有趣的云层形态'],
    tags: ['风景', '天空', '蓝天', '清新'],
    bestTime: '晴朗天气',
    environmentTips: '晴朗天气，视野开阔'
  },
  {
    id: 'landscape-forest',
    name: '森林',
    category: SceneCategory.LANDSCAPE,
    subCategory: '森林',
    description: '森林场景，追求绿色自然通透',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 3, saturation: 20, contrast: 10, colorTemp: 5, sharpness: 15, vignette: -8, cyanMagenta: -5, softLight: SoftLightMode.NONE },
    recommendedFilm: [
      { ...FILM_PRESETS[2], matchScore: 0.85 },
      { ...FILM_PRESETS[0], matchScore: 0.70 }
    ],
    masterTips: ['增强绿色表现力', '利用光线穿透树叶', '寻找有趣的树木形态'],
    tags: ['风景', '森林', '绿色', '自然'],
    bestTime: '上午或阴天',
    environmentTips: '户外自然光，森林、草地、植物丰富的场景'
  },
  {
    id: 'landscape-autumn',
    name: '秋景',
    category: SceneCategory.LANDSCAPE,
    subCategory: '秋景',
    description: '秋景拍摄，追求金黄暖色调',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 0, saturation: 25, contrast: 10, colorTemp: 15, sharpness: 12, vignette: 5, cyanMagenta: 3, softLight: SoftLightMode.SOFT },
    recommendedFilm: [
      { ...FILM_PRESETS[4], matchScore: 0.90 },
      { ...FILM_PRESETS[2], matchScore: 0.80 }
    ],
    masterTips: ['增强暖色调表现', '寻找色彩丰富的秋叶', '注意光影层次'],
    tags: ['风景', '秋景', '金黄', '暖调'],
    bestTime: '秋季晴天',
    environmentTips: '秋季户外，色彩丰富的落叶场景'
  },
  {
    id: 'landscape-snow',
    name: '雪景',
    category: SceneCategory.LANDSCAPE,
    subCategory: '雪景',
    description: '雪景拍摄，追求纯净冷色调',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 15, saturation: -5, contrast: -10, colorTemp: -10, sharpness: 8, vignette: 0, cyanMagenta: -5, softLight: SoftLightMode.SOFT },
    recommendedFilm: [
      { ...FILM_PRESETS[7], matchScore: 0.85 },
      { ...FILM_PRESETS[2], matchScore: 0.70 }
    ],
    masterTips: ['冷色调增强雪景纯净感', '注意曝光补偿防止过曝', '寻找有趣的雪景形态'],
    tags: ['风景', '雪景', '纯净', '冷调'],
    bestTime: '雪天或阴天',
    environmentTips: '雪天、阴天或低色温场景'
  },
  {
    id: 'landscape-beach',
    name: '海滩',
    category: SceneCategory.LANDSCAPE,
    subCategory: '海滩',
    description: '海滩场景，追求清新通透',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 10, saturation: 8, contrast: 5, colorTemp: -5, sharpness: 10, vignette: 0, cyanMagenta: -3, softLight: SoftLightMode.NONE },
    recommendedFilm: [
      { ...FILM_PRESETS[1], matchScore: 0.80 },
      { ...FILM_PRESETS[2], matchScore: 0.75 }
    ],
    masterTips: ['冷色调增强海水表现', '注意天空与海面比例', '寻找有趣的海岸线'],
    tags: ['风景', '海滩', '海边', '清新'],
    bestTime: '晴朗天气',
    environmentTips: '晴朗天气或明亮的度假场景'
  },

  // ─── 夜景系列 ───
  {
    id: 'night-city',
    name: '城市夜景',
    category: SceneCategory.NIGHT,
    subCategory: '城市夜景',
    description: '城市夜景拍摄，追求光影层次和氛围感',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: -18, saturation: 12, contrast: 25, colorTemp: -10, sharpness: 20, vignette: 20, cyanMagenta: -15, softLight: SoftLightMode.NONE },
    recommendedFilm: [
      { ...FILM_PRESETS[5], matchScore: 0.90 },
      { ...FILM_PRESETS[6], matchScore: 0.75 }
    ],
    masterTips: ['高对比度增强光影层次', '注意曝光控制', '寻找有灯光的建筑'],
    tags: ['夜景', '城市', '灯光', '氛围'],
    bestTime: '日落后蓝调时刻',
    environmentTips: '城市夜景，灯光璀璨的场景'
  },
  {
    id: 'night-neon',
    name: '霓虹灯',
    category: SceneCategory.NIGHT,
    subCategory: '霓虹灯',
    description: '霓虹灯场景，追求色彩饱和和梦幻感',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 0, saturation: 20, contrast: 15, colorTemp: -10, sharpness: 10, vignette: 5, cyanMagenta: 3, softLight: SoftLightMode.SOFT },
    recommendedFilm: [
      { ...FILM_PRESETS[5], matchScore: 0.95 },
      { ...FILM_PRESETS[7], matchScore: 0.75 }
    ],
    masterTips: ['增强色彩饱和度', '柔光营造梦幻感', '寻找有霓虹灯招牌的场景'],
    tags: ['夜景', '霓虹', '色彩', '梦幻'],
    bestTime: '夜晚繁华街道',
    environmentTips: '夜晚城市、霓虹灯招牌、繁华街道'
  },
  {
    id: 'night-starry',
    name: '星空',
    category: SceneCategory.NIGHT,
    subCategory: '星空',
    description: '星空拍摄，追求深邃神秘感',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: -20, saturation: 10, contrast: 30, colorTemp: -15, sharpness: 25, vignette: 30, cyanMagenta: -10, softLight: SoftLightMode.NONE },
    recommendedFilm: [
      { ...FILM_PRESETS[6], matchScore: 0.85 },
      { ...FILM_PRESETS[5], matchScore: 0.80 }
    ],
    masterTips: ['高对比度增强星空层次', '使用长曝光', '远离城市光污染'],
    tags: ['夜景', '星空', '深邃', '神秘'],
    bestTime: '晴朗夜晚',
    environmentTips: '远离城市光污染的开阔地带'
  },
  {
    id: 'night-candlelight',
    name: '烛光',
    category: SceneCategory.NIGHT,
    subCategory: '烛光',
    description: '烛光环境，追求温馨暖色调',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: -10, saturation: 5, contrast: 5, colorTemp: 15, sharpness: 0, vignette: 10, cyanMagenta: -5, softLight: SoftLightMode.SOFT },
    recommendedFilm: [
      { ...FILM_PRESETS[8], matchScore: 0.90 },
      { ...FILM_PRESETS[3], matchScore: 0.80 }
    ],
    masterTips: ['暖色调增强温馨感', '柔光营造氛围', '注意曝光控制'],
    tags: ['夜景', '烛光', '温馨', '暖调'],
    environmentTips: '烛光环境，温馨室内'
  },

  // ─── 美食系列 ───
  {
    id: 'food-restaurant',
    name: '餐厅美食',
    category: SceneCategory.FOOD,
    subCategory: '餐厅美食',
    description: '餐厅美食拍摄，追求暖色调和质感',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: -5, saturation: 15, contrast: 8, colorTemp: 10, sharpness: 20, vignette: -10, cyanMagenta: 0, softLight: SoftLightMode.NONE },
    recommendedFilm: [
      { ...FILM_PRESETS[8], matchScore: 0.85 },
      { ...FILM_PRESETS[0], matchScore: 0.75 }
    ],
    masterTips: ['暖色调增强食欲感', '注意食物摆放角度', '寻找最佳光线位置'],
    tags: ['美食', '餐厅', '暖调', '质感'],
    environmentTips: '餐厅、厨房、美食拍摄场景'
  },
  {
    id: 'food-dessert',
    name: '甜点',
    category: SceneCategory.FOOD,
    subCategory: '甜点',
    description: '甜点拍摄，追求梦幻温馨感',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 5, saturation: 20, contrast: 5, colorTemp: 5, sharpness: 18, vignette: -15, cyanMagenta: 5, softLight: SoftLightMode.DREAMY },
    recommendedFilm: [
      { ...FILM_PRESETS[8], matchScore: 0.90 },
      { ...FILM_PRESETS[3], matchScore: 0.80 }
    ],
    masterTips: ['梦幻柔光营造温馨感', '注意甜点摆放角度', '寻找简洁背景'],
    tags: ['美食', '甜点', '梦幻', '温馨'],
    environmentTips: '温馨室内，简洁背景'
  },

  // ─── 城市系列 ───
  {
    id: 'urban-street',
    name: '街拍',
    category: SceneCategory.URBAN,
    subCategory: '街拍',
    description: '街头纪实拍摄，追求真实质感和故事性',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: -5, saturation: 5, contrast: 18, colorTemp: 0, sharpness: 22, vignette: 10, cyanMagenta: -5, softLight: SoftLightMode.NONE },
    recommendedFilm: [
      { ...FILM_PRESETS[6], matchScore: 0.85 },
      { ...FILM_PRESETS[0], matchScore: 0.80 }
    ],
    masterTips: ['高对比度增强街头质感', '捕捉真实生活瞬间', '注意构图简洁'],
    tags: ['城市', '街拍', '纪实', '质感'],
    bestTime: '日间户外',
    environmentTips: '自然光或柔和人工光源'
  },
  {
    id: 'urban-architecture',
    name: '建筑',
    category: SceneCategory.URBAN,
    subCategory: '建筑',
    description: '建筑摄影，追求几何线条和光影质感',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 5, saturation: 5, contrast: 25, colorTemp: 0, sharpness: 28, vignette: 0, cyanMagenta: -10, softLight: SoftLightMode.NONE },
    recommendedFilm: [
      { ...FILM_PRESETS[6], matchScore: 0.85 },
      { ...FILM_PRESETS[2], matchScore: 0.80 }
    ],
    masterTips: ['高对比度突出几何线条', '注意构图对称和透视', '寻找有趣的光影角度'],
    tags: ['城市', '建筑', '几何', '线条'],
    bestTime: '晴朗天气',
    environmentTips: '强烈光影对比场景'
  },
  {
    id: 'urban-cafe',
    name: '咖啡馆',
    category: SceneCategory.URBAN,
    subCategory: '咖啡馆',
    description: '咖啡馆场景，追求温馨文艺感',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: -10, saturation: 8, contrast: 5, colorTemp: 15, sharpness: 5, vignette: 5, cyanMagenta: -3, softLight: SoftLightMode.SOFT },
    recommendedFilm: [
      { ...FILM_PRESETS[8], matchScore: 0.90 },
      { ...FILM_PRESETS[3], matchScore: 0.80 }
    ],
    masterTips: ['暖色调营造温馨氛围', '柔光增加文艺感', '寻找有趣的室内元素'],
    tags: ['城市', '咖啡馆', '温馨', '文艺'],
    environmentTips: '温馨室内，柔和光线'
  },

  // ─── 特殊系列 ───
  {
    id: 'special-concert',
    name: '演唱会',
    category: SceneCategory.SPECIAL,
    subCategory: '演唱会',
    description: '演唱会拍摄，追求舞台光影效果',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: -10, saturation: 15, contrast: 20, colorTemp: 0, sharpness: 10, vignette: 10, cyanMagenta: 0, softLight: SoftLightMode.SOFT },
    recommendedFilm: [
      { ...FILM_PRESETS[5], matchScore: 0.85 },
      { ...FILM_PRESETS[6], matchScore: 0.80 }
    ],
    masterTips: ['高对比度增强舞台效果', '注意灯光变化', '遵守拍摄规定'],
    tags: ['特殊', '演唱会', '舞台', '光影'],
    environmentTips: '舞台环境，注意光线变化'
  },
  {
    id: 'special-wedding',
    name: '婚礼',
    category: SceneCategory.SPECIAL,
    subCategory: '婚礼',
    description: '婚礼拍摄，追求温馨浪漫感',
    color: HASSELBLAD_ORANGE,
    confidence: 0,
    hasselbladParams: { tone: 0, saturation: 10, contrast: 5, colorTemp: 10, sharpness: 8, vignette: 5, cyanMagenta: 0, softLight: SoftLightMode.DREAMY },
    recommendedFilm: [
      { ...FILM_PRESETS[3], matchScore: 0.90 },
      { ...FILM_PRESETS[8], matchScore: 0.85 }
    ],
    masterTips: ['梦幻柔光营造浪漫感', '捕捉情感瞬间', '注意光线变化'],
    tags: ['特殊', '婚礼', '温馨', '浪漫'],
    environmentTips: '婚礼现场，注意光线'
  }
]

// 配方数据模型
export interface RecipeProfile {
  id: string;
  name: string;
  description: string;
  author: { name: string; avatarUrl?: string };
  scene: { id: string; displayName: string; category: string; icon: string };
  film: { id: string; displayName: string; series: string; matchScore: number };
  hasselbladParams: HasselbladParams;
  masterTips: string[];
  createdAt: number;
  updatedAt: number;
  usageCount: number;
  isFavorite: boolean;
  tags: string[];
}

// 颜色分析结果
export interface ColorProfile {
  avgRed: number;
  avgGreen: number;
  avgBlue: number;
  warmthRatio: number;
  coolRatio: number;
  greenDominance: number;
  blueDominance: number;
  redDominance: number;
  colorVariance: number;
  dominantTone: 'warm' | 'cool' | 'green' | 'blue' | 'neutral' | 'high_key' | 'low_key';
}

// 亮度等级
export type BrightnessLevel = 'very_dark' | 'dark' | 'normal' | 'bright' | 'very_bright'

// 分析结果
export interface AnalysisResult {
  primaryScene: SceneProfile;
  confidence: number;
  alternativeScenes: SceneProfile[];
  colorProfile: ColorProfile;
  brightnessLevel: BrightnessLevel;
  faceCount: number;
  analysisTimeMs: number;
}