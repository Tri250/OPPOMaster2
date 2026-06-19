// 5.3 场景→哈苏参数映射表 - Web端实现
// 提供三个核心映射功能：场景→哈苏参数、场景→胶片推荐、场景→大师建议

import {
  HasselbladParams,
  FilmPreset,
  FilmSeries,
  SoftLightMode,
  SceneCategory,
  SceneProfile,
  getScenePresetById,
} from '../store/sceneProfile';

/**
 * 场景→哈苏参数映射表
 */
export const SCENE_TO_HASSELBLAD_PARAMS: Record<string, HasselbladParams> = {
  // ─── 人像系列 ───
  'portrait-standard': { tone: -3, saturation: 10, contrast: -15, colorTemp: -5, sharpness: -15, clarity: 5, vignette: 20, cyanMagenta: -5, softLight: SoftLightMode.SOFT },
  'portrait-indoor': { tone: -2, saturation: 5, contrast: -10, colorTemp: 0, sharpness: -5, clarity: 8, vignette: 15, cyanMagenta: -3, softLight: SoftLightMode.SOFT },
  'portrait-backlit': { tone: -5, saturation: 12, contrast: -10, colorTemp: -10, sharpness: -10, vignette: 25, cyanMagenta: -8, softLight: SoftLightMode.DREAMY },
  'portrait-studio': { tone: 0, saturation: 0, contrast: 15, colorTemp: 0, sharpness: 0, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
  'portrait-bw': { tone: -10, saturation: -30, contrast: 25, colorTemp: 0, sharpness: 20, vignette: 15, cyanMagenta: 0, softLight: SoftLightMode.NONE },
  'portrait-group': { tone: 0, saturation: 8, contrast: -10, colorTemp: 0, sharpness: 12, vignette: 0, cyanMagenta: 0, softLight: SoftLightMode.NONE },
  'portrait-child': { tone: 5, saturation: 12, contrast: -8, colorTemp: 5, sharpness: -5, vignette: 10, cyanMagenta: 3, softLight: SoftLightMode.DREAMY },
  'portrait-couple': { tone: 5, saturation: 10, contrast: -5, colorTemp: 10, sharpness: -10, vignette: 15, cyanMagenta: 5, softLight: SoftLightMode.SOFT },
  'portrait-senior': { tone: -5, saturation: 5, contrast: 20, colorTemp: 0, sharpness: 8, vignette: 10, cyanMagenta: -5, softLight: SoftLightMode.NONE },

  // ─── 风景系列 ───
  'landscape-standard': { tone: 5, saturation: 15, contrast: 12, colorTemp: 0, sharpness: 18, vignette: -5, cyanMagenta: -3, softLight: SoftLightMode.NONE },
  'landscape-sunset': { tone: -5, saturation: 25, contrast: 10, colorTemp: 20, sharpness: 12, clarity: 15, vignette: 0, cyanMagenta: 5, softLight: SoftLightMode.NONE },
  'landscape-sky': { tone: 10, saturation: 10, contrast: 5, colorTemp: -15, sharpness: 15, vignette: -10, cyanMagenta: -10, softLight: SoftLightMode.NONE },
  'landscape-forest': { tone: 3, saturation: 20, contrast: 10, colorTemp: 5, sharpness: 15, vignette: -8, cyanMagenta: -5, softLight: SoftLightMode.NONE },
  'landscape-autumn': { tone: 0, saturation: 25, contrast: 10, colorTemp: 15, sharpness: 12, vignette: 0, cyanMagenta: 8, softLight: SoftLightMode.NONE },
  'landscape-snow': { tone: 15, saturation: -5, contrast: -10, colorTemp: -5, sharpness: 10, vignette: -15, cyanMagenta: -8, softLight: SoftLightMode.NONE },
  'landscape-beach': { tone: 10, saturation: 8, contrast: 5, colorTemp: -5, sharpness: 15, vignette: -10, cyanMagenta: -5, softLight: SoftLightMode.NONE },
  'landscape-waterfall': { tone: 0, saturation: 10, contrast: 5, colorTemp: 0, sharpness: 20, vignette: -10, cyanMagenta: -3, softLight: SoftLightMode.NONE },
  'landscape-mountain': { tone: 8, saturation: 12, contrast: 15, colorTemp: 0, sharpness: 20, vignette: 5, cyanMagenta: -5, softLight: SoftLightMode.NONE },
  'landscape-desert': { tone: -5, saturation: 15, contrast: 10, colorTemp: 15, sharpness: 18, vignette: 10, cyanMagenta: 10, softLight: SoftLightMode.NONE },

  // ─── 夜景系列 ───
  'night-city': { tone: -15, saturation: -5, contrast: 20, colorTemp: -5, sharpness: 15, vignette: 25, cyanMagenta: -10, softLight: SoftLightMode.NONE },
  'night-neon': { tone: -18, saturation: 12, contrast: 25, colorTemp: -10, sharpness: 20, vignette: 20, cyanMagenta: -15, softLight: SoftLightMode.NONE },
  'night-starry': { tone: -20, saturation: 10, contrast: 30, colorTemp: -15, sharpness: 25, vignette: 30, cyanMagenta: -10, softLight: SoftLightMode.NONE },
  'night-candle': { tone: -10, saturation: 5, contrast: 10, colorTemp: 15, sharpness: -5, vignette: 20, cyanMagenta: 5, softLight: SoftLightMode.SOFT },
  'night-fireworks': { tone: -5, saturation: 15, contrast: 20, colorTemp: 0, sharpness: 15, vignette: 10, cyanMagenta: 0, softLight: SoftLightMode.NONE },

  // ─── 美食系列 ───
  'food-restaurant': { tone: -5, saturation: 15, contrast: 8, colorTemp: 10, sharpness: 20, vignette: -10, cyanMagenta: 0, softLight: SoftLightMode.NONE },
  'food-dessert': { tone: 5, saturation: 20, contrast: 5, colorTemp: 5, sharpness: 18, vignette: -15, cyanMagenta: 5, softLight: SoftLightMode.SOFT },
  'food-drink': { tone: 0, saturation: 10, contrast: 12, colorTemp: 5, sharpness: 15, vignette: -10, cyanMagenta: 0, softLight: SoftLightMode.NONE },
  'food-coffee': { tone: -5, saturation: 8, contrast: 5, colorTemp: 10, sharpness: 12, vignette: -5, cyanMagenta: 3, softLight: SoftLightMode.SOFT },
  'food-bbq': { tone: 0, saturation: 15, contrast: 10, colorTemp: 8, sharpness: 18, vignette: 0, cyanMagenta: 5, softLight: SoftLightMode.NONE },

  // ─── 城市/街拍系列 ───
  'urban-street': { tone: -5, saturation: 5, contrast: 18, colorTemp: 0, sharpness: 22, vignette: 10, cyanMagenta: -5, softLight: SoftLightMode.NONE },
  'urban-architecture': { tone: 5, saturation: 5, contrast: 25, colorTemp: 0, sharpness: 28, vignette: 0, cyanMagenta: -10, softLight: SoftLightMode.NONE },
  'urban-cafe': { tone: -10, saturation: 8, contrast: 5, colorTemp: 15, sharpness: 10, vignette: 5, cyanMagenta: 8, softLight: SoftLightMode.SOFT },
  'urban-museum': { tone: -5, saturation: 0, contrast: 15, colorTemp: 0, sharpness: 15, vignette: 0, cyanMagenta: -5, softLight: SoftLightMode.NONE },
  'urban-market': { tone: 0, saturation: 12, contrast: 10, colorTemp: 5, sharpness: 18, vignette: 5, cyanMagenta: 0, softLight: SoftLightMode.NONE },
  'urban-station': { tone: -8, saturation: 5, contrast: 15, colorTemp: -5, sharpness: 20, vignette: 10, cyanMagenta: -8, softLight: SoftLightMode.NONE },
  'urban-park': { tone: 5, saturation: 10, contrast: 8, colorTemp: 5, sharpness: 15, vignette: -5, cyanMagenta: -3, softLight: SoftLightMode.NONE },

  // ─── 静物系列 ───
  'still-flower': { tone: 5, saturation: 15, contrast: 5, colorTemp: 0, sharpness: 10, vignette: -10, cyanMagenta: 0, softLight: SoftLightMode.SOFT },
  'still-product': { tone: 0, saturation: 5, contrast: 10, colorTemp: 0, sharpness: 15, vignette: -5, cyanMagenta: 0, softLight: SoftLightMode.NONE },
  'still-book': { tone: -5, saturation: 5, contrast: 5, colorTemp: 5, sharpness: 10, vignette: 0, cyanMagenta: 3, softLight: SoftLightMode.SOFT },
  'still-art': { tone: 0, saturation: 8, contrast: 12, colorTemp: 0, sharpness: 12, vignette: 0, cyanMagenta: -5, softLight: SoftLightMode.NONE },

  // ─── 微距系列 ───
  'macro-insect': { tone: 0, saturation: 10, contrast: 15, colorTemp: 0, sharpness: 25, vignette: -5, cyanMagenta: 0, softLight: SoftLightMode.NONE },
  'macro-water': { tone: 10, saturation: 5, contrast: 10, colorTemp: -5, sharpness: 20, vignette: -10, cyanMagenta: -5, softLight: SoftLightMode.NONE },
  'macro-texture': { tone: 5, saturation: 0, contrast: 25, colorTemp: 0, sharpness: 30, vignette: 0, cyanMagenta: -8, softLight: SoftLightMode.NONE },

  // ─── 活动系列 ───
  'event-wedding': { tone: 5, saturation: 10, contrast: -5, colorTemp: 10, sharpness: -10, vignette: 15, cyanMagenta: 5, softLight: SoftLightMode.SOFT },
  'event-party': { tone: 0, saturation: 15, contrast: 10, colorTemp: 5, sharpness: 15, vignette: 5, cyanMagenta: 0, softLight: SoftLightMode.NONE },
  'event-concert': { tone: -10, saturation: 15, contrast: 20, colorTemp: 15, sharpness: 20, vignette: 15, cyanMagenta: -10, softLight: SoftLightMode.NONE },
  'event-sports': { tone: 0, saturation: 10, contrast: 15, colorTemp: 0, sharpness: 20, vignette: 0, cyanMagenta: -5, softLight: SoftLightMode.NONE },
};

/**
 * 默认哈苏参数
 */
export const DEFAULT_HASSELBLAD_PARAMS: HasselbladParams = {
  tone: 0,
  saturation: 0,
  contrast: 0,
  colorTemp: 0,
  sharpness: 0,
  vignette: 0,
  cyanMagenta: 0,
  softLight: SoftLightMode.NONE,
};

/**
 * 获取场景对应的哈苏参数
 */
export function getHasselbladParams(sceneId: string): HasselbladParams {
  return SCENE_TO_HASSELBLAD_PARAMS[sceneId] ?? DEFAULT_HASSELBLAD_PARAMS;
}

/**
 * 场景→胶片推荐映射
 */
export const SCENE_TO_FILM_RECOMMENDATIONS: Record<string, FilmPreset[]> = {
  // 人像系列
  'portrait': [
    { id: 'portra', name: 'Portra 400', series: FilmSeries.EMOTION, matchScore: 0.95, description: '柔和肤色，人像首选' },
    { id: 'cc', name: 'CC 经典负片', series: FilmSeries.CLASSIC, matchScore: 0.82, description: '经典胶片质感' },
    { id: 'ccd_warm', name: '暖 CCD', series: FilmSeries.DIGITAL, matchScore: 0.70, description: '温馨氛围' },
  ],
  'portrait-bw': [
    { id: 'tx400', name: 'TX400 黑白', series: FilmSeries.STRUCTURE, matchScore: 0.98, description: '经典黑白颗粒' },
    { id: 'nh', name: 'NH 浓郁', series: FilmSeries.CLASSIC, matchScore: 0.65, description: '高对比黑白' },
  ],
  // 风景系列
  'landscape': [
    { id: 'rdp3', name: 'RDP3 正片', series: FilmSeries.EMOTION, matchScore: 0.93, description: '反转片质感，高饱和' },
    { id: 'nh', name: 'NH 浓郁', series: FilmSeries.CLASSIC, matchScore: 0.88, description: '浓郁色彩' },
    { id: 'nc', name: 'NC 自然', series: FilmSeries.CLASSIC, matchScore: 0.75, description: '自然柔和' },
  ],
  'landscape-sunset': [
    { id: 'rdp3', name: 'RDP3 正片', series: FilmSeries.EMOTION, matchScore: 0.95, description: '反转片质感' },
    { id: 'portra', name: 'Portra 400', series: FilmSeries.EMOTION, matchScore: 0.88, description: '柔和暖调' },
    { id: 'ccd_warm', name: '暖 CCD', series: FilmSeries.DIGITAL, matchScore: 0.80, description: '数字暖调' },
  ],
  'landscape-snow': [
    { id: 'ccd_cool', name: '冷 CCD', series: FilmSeries.DIGITAL, matchScore: 0.90, description: '清冷质感' },
    { id: 'nh', name: 'NH 浓郁', series: FilmSeries.CLASSIC, matchScore: 0.82, description: '纯净高对比' },
    { id: 'nc', name: 'NC 自然', series: FilmSeries.CLASSIC, matchScore: 0.70, description: '自然柔和' },
  ],
  // 夜景系列
  'night': [
    { id: '800t', name: '800T 夜景', series: FilmSeries.STRUCTURE, matchScore: 0.96, description: '夜景电影感' },
    { id: 'tx400', name: 'TX400 黑白', series: FilmSeries.STRUCTURE, matchScore: 0.78, description: '黑白夜景' },
    { id: 'ccd_cool', name: '冷 CCD', series: FilmSeries.DIGITAL, matchScore: 0.65, description: '冷调数字' },
  ],
  'night-starry': [
    { id: 'tx400', name: 'TX400 黑白', series: FilmSeries.STRUCTURE, matchScore: 0.92, description: '星空黑白' },
    { id: '800t', name: '800T 夜景', series: FilmSeries.STRUCTURE, matchScore: 0.85, description: '电影感星空' },
    { id: 'ccd_cool', name: '冷 CCD', series: FilmSeries.DIGITAL, matchScore: 0.70, description: '冷调星空' },
  ],
  // 美食系列
  'food': [
    { id: 'ccd_warm', name: '暖 CCD', series: FilmSeries.DIGITAL, matchScore: 0.90, description: '食欲感暖调' },
    { id: 'cc', name: 'CC 经典负片', series: FilmSeries.CLASSIC, matchScore: 0.85, description: '经典质感' },
    { id: 'portra', name: 'Portra 400', series: FilmSeries.EMOTION, matchScore: 0.72, description: '柔和自然' },
  ],
  // 街拍/建筑
  'urban-street': [
    { id: 'tx400', name: 'TX400 黑白', series: FilmSeries.STRUCTURE, matchScore: 0.92, description: '街头黑白' },
    { id: 'nh', name: 'NH 浓郁', series: FilmSeries.CLASSIC, matchScore: 0.80, description: '高对比' },
    { id: 'cc', name: 'CC 经典负片', series: FilmSeries.CLASSIC, matchScore: 0.74, description: '经典质感' },
  ],
  'urban-architecture': [
    { id: 'tx400', name: 'TX400 黑白', series: FilmSeries.STRUCTURE, matchScore: 0.92, description: '建筑黑白' },
    { id: 'nh', name: 'NH 浓郁', series: FilmSeries.CLASSIC, matchScore: 0.80, description: '高对比' },
    { id: 'cc', name: 'CC 经典负片', series: FilmSeries.CLASSIC, matchScore: 0.74, description: '经典质感' },
  ],
  // 静物系列
  'still': [
    { id: 'nc', name: 'NC 自然', series: FilmSeries.CLASSIC, matchScore: 0.88, description: '自然质感' },
    { id: 'rdp3', name: 'RDP3 正片', series: FilmSeries.EMOTION, matchScore: 0.82, description: '高饱和' },
    { id: 'nh', name: 'NH 浓郁', series: FilmSeries.CLASSIC, matchScore: 0.75, description: '浓郁色彩' },
  ],
  // 微距系列
  'macro': [
    { id: 'rdp3', name: 'RDP3 正片', series: FilmSeries.EMOTION, matchScore: 0.90, description: '细节高饱和' },
    { id: 'nh', name: 'NH 浓郁', series: FilmSeries.CLASSIC, matchScore: 0.85, description: '浓郁质感' },
    { id: 'tx400', name: 'TX400 黑白', series: FilmSeries.STRUCTURE, matchScore: 0.78, description: '黑白纹理' },
  ],
  // 活动系列
  'event-wedding': [
    { id: 'portra', name: 'Portra 400', series: FilmSeries.EMOTION, matchScore: 0.95, description: '婚礼首选' },
    { id: 'ccd_warm', name: '暖 CCD', series: FilmSeries.DIGITAL, matchScore: 0.88, description: '温馨氛围' },
    { id: 'nc', name: 'NC 自然', series: FilmSeries.CLASSIC, matchScore: 0.80, description: '自然记录' },
  ],
  'event-concert': [
    { id: 'tx400', name: 'TX400 黑白', series: FilmSeries.STRUCTURE, matchScore: 0.90, description: '舞台黑白' },
    { id: '800t', name: '800T 夜景', series: FilmSeries.STRUCTURE, matchScore: 0.85, description: '灯光效果' },
    { id: 'nh', name: 'NH 浓郁', series: FilmSeries.CLASSIC, matchScore: 0.78, description: '浓郁色彩' },
  ],
};

/**
 * 获取场景推荐的胶片列表
 */
export function getRecommendedFilms(sceneId: string): FilmPreset[] {
  // 尝试精确匹配
  if (SCENE_TO_FILM_RECOMMENDATIONS[sceneId]) {
    return SCENE_TO_FILM_RECOMMENDATIONS[sceneId];
  }

  // 尝试前缀匹配
  const prefix = sceneId.split('-')[0];
  if (SCENE_TO_FILM_RECOMMENDATIONS[prefix]) {
    return SCENE_TO_FILM_RECOMMENDATIONS[prefix];
  }

  // 默认推荐
  return [
    { id: 'cc', name: 'CC 经典负片', series: FilmSeries.CLASSIC, matchScore: 0.85, description: '经典胶片质感' },
    { id: 'nc', name: 'NC 自然', series: FilmSeries.CLASSIC, matchScore: 0.80, description: '自然柔和' },
  ];
}

/**
 * 场景→大师拍摄建议映射
 */
export const SCENE_TO_MASTER_TIPS: Record<string, string[]> = {
  // 人像系列
  'portrait': [
    '📐 使用 2× 或 3× 长焦避免广角畸变，让人脸比例更自然',
    '☀️ 寻找柔和的侧光或窗户光，哈苏风格偏爱自然光影',
    '🎨 肤色还原是 HNCS 的核心——不过度美白，保留真实肤色',
    '📷 试试 Portra 400 胶片风格，温柔叙事感拉满',
  ],
  'portrait-backlit': [
    '🌅 逆光拍摄保留高光细节，使用 HDR 或手动提升阴影',
    '✨ 柔光模式让轮廓光更加梦幻，哈苏特色效果',
    '📐 对焦眼睛，让逆光产生柔和的轮廓光效果',
    '📷 Portra 400 + 暗角 +20，逆光人像完美配方',
  ],
  'portrait-bw': [
    '⚫ 关注光影对比，黑白人像的核心是明暗层次',
    '📐 简化背景元素，让人物轮廓更加突出',
    '☀️ 寻找硬光或侧光，增强黑白对比效果',
    '📷 TX400 胶片风格，经典黑白颗粒质感',
  ],
  // 风景系列
  'landscape': [
    '🌅 黄金时刻（日出后/日落前 30 分钟）出片率最高',
    '📐 利用前景（岩石/树枝/花草）增加画面层次感',
    '🖼️ 试试 XPAN 宽幅模式，电影感构图一步到位',
    '🎨 浓郁胶片风格让蓝天更澄澈、绿植更鲜活',
  ],
  'landscape-sunset': [
    '☀️ 等待太阳接触地平线的瞬间，色彩最丰富',
    '📐 使用渐变滤镜平衡天空与地面曝光',
    '🎨 色温 +20 + 饱和度 +25，日落完美配方',
    '📷 RDP3 正片风格，反转片质感让日落更壮观',
  ],
  // 夜景系列
  'night': [
    '📷 ISO 控制在 400 以内，手持拍摄找支撑点',
    '💡 寻找点光源（路灯/霓虹/橱窗）作为画面视觉锚点',
    '🌊 寻找水面拍摄，倒影让夜景层次翻倍',
    '🎞️ 800T 胶片专为夜景优化——不糊不噪',
  ],
  'night-starry': [
    '📍 远离城市光污染，寻找暗夜保护区',
    '📷 使用三脚架 + 高 ISO + 长曝光（15-30秒）',
    '🎨 对比度 +30 + 锐度 +25，星空细节最大化',
    '🎞️ TX400 黑白风格，星空摄影的经典选择',
  ],
  // 美食系列
  'food': [
    '📐 45° 俯拍或平视 0° 特写，两种角度切换拍',
    '💡 寻找自然光或暖光，避免顶光造成难看的阴影',
    '🎨 暖 CCD 胶片风格让食物更有食欲',
    '📷 对焦在食物的纹理细节上——肉汁、糖霜、气泡',
  ],
  // 街拍/建筑
  'urban-street': [
    '🚶 等待决定性瞬间——一个人、一束光、一个故事',
    '⚫ TX400 黑白胶片让街头故事感翻倍',
    '📐 寻找几何线条和光影对比，哈苏黑白风格强调明暗反差',
    '🎯 预设对焦点在画面 1/3 处，抬手即拍',
  ],
  'urban-architecture': [
    '📐 寻找建筑的几何线条和对称结构',
    '☀️ 等待光线创造阴影，增强建筑立体感',
    '⚫ TX400 黑白风格让建筑更有力量感',
    '🎯 对比度 +25 + 锐度 +28，建筑细节最大化',
  ],
  'urban-cafe': [
    '☕ 捕捉咖啡馆的氛围——咖啡杯、书本、光影',
    '💡 利用窗户光线，创造柔和温馨的氛围',
    '🎨 暖 CCD + 柔光模式，咖啡馆完美配方',
    '📷 45° 角度拍摄，展现空间层次',
  ],
  // 静物系列
  'still': [
    '📐 简化背景，让主体更加突出',
    '💡 使用柔和的侧光或逆光，增强质感',
    '🎨 NC 自然风格保留静物的真实色彩',
    '📷 关注细节纹理——花瓣、叶片、质感',
  ],
  // 微距系列
  'macro': [
    '🔍 使用微距镜头或长焦 + 近摄滤镜',
    '💡 柔和光线避免过曝，保留细节层次',
    '🎨 锐度 +25 + 对比度 +15，细节最大化',
    '📷 景深很浅，注意对焦精度',
  ],
  // 活动系列
  'event-wedding': [
    '💒 捕捉重要瞬间——仪式、誓言、亲吻',
    '💡 注意光线柔和，避免硬光',
    '🎨 Portra 400 + 柔光，婚礼完美配方',
    '📷 多拍细节——戒指、花束、表情',
  ],
  'event-concert': [
    '🎤 使用快速镜头 + 高 ISO，捕捉动态',
    '💡 注意舞台灯光变化，预判高潮时刻',
    '⚫ TX400 黑白风格让舞台更有戏剧感',
    '📷 连拍模式，捕捉最佳瞬间',
  ],
};

/**
 * 获取场景的大师拍摄建议
 */
export function getMasterTips(sceneId: string): string[] {
  // 尝试精确匹配
  if (SCENE_TO_MASTER_TIPS[sceneId]) {
    return SCENE_TO_MASTER_TIPS[sceneId];
  }

  // 尝试前缀匹配
  const prefix = sceneId.split('-')[0];
  if (SCENE_TO_MASTER_TIPS[prefix]) {
    return SCENE_TO_MASTER_TIPS[prefix];
  }

  // 默认建议
  return [
    '📷 哈苏大师模式让每一张照片都有故事',
    '🎨 试试不同胶片风格，找到你的专属配方',
    '💡 关注光影变化，哈苏风格偏爱自然光',
    '📐 注意构图，三分法则永不过时',
  ];
}

/**
 * 获取完整的场景画像（包含参数、胶片、建议）
 */
export function getFullSceneProfile(sceneId: string, confidence: number = 0.85): SceneProfile {
  const preset = getScenePresetById(sceneId);
  const params = getHasselbladParams(sceneId);
  const films = getRecommendedFilms(sceneId);
  const tips = getMasterTips(sceneId);

  if (preset) {
    return {
      ...preset,
      confidence,
      hasselbladParams: params,
      recommendedFilm: films,
      masterTips: tips,
    };
  }

  // 创建默认场景画像
  const category = inferCategory(sceneId);
  const categoryMeta = {
    [SceneCategory.PORTRAIT]: { name: '人像', color: '#FF6B35' },
    [SceneCategory.LANDSCAPE]: { name: '风景', color: '#4CAF50' },
    [SceneCategory.NIGHT]: { name: '夜景', color: '#2196F3' },
    [SceneCategory.FOOD]: { name: '美食', color: '#FF9800' },
    [SceneCategory.URBAN]: { name: '城市', color: '#9C27B0' },
    [SceneCategory.STILL_LIFE]: { name: '静物', color: '#00BCD4' },
    [SceneCategory.MACRO]: { name: '微距', color: '#E91E63' },
    [SceneCategory.EVENT]: { name: '活动', color: '#FF5722' },
  };

  const meta = categoryMeta[category] || { name: '通用', color: '#FF6B35' };

  return {
    id: sceneId,
    name: sceneId.replace(/-/g, ' ').replace(/\b\w/g, c => c.toUpperCase()),
    category,
    description: `${meta.name}场景`,
    color: meta.color,
    confidence,
    hasselbladParams: params,
    recommendedFilm: films,
    masterTips: tips,
  };
}

/**
 * 根据场景ID推断类别
 */
function inferCategory(sceneId: string): SceneCategory {
  if (sceneId.startsWith('portrait')) return SceneCategory.PORTRAIT;
  if (sceneId.startsWith('landscape')) return SceneCategory.LANDSCAPE;
  if (sceneId.startsWith('night')) return SceneCategory.NIGHT;
  if (sceneId.startsWith('food')) return SceneCategory.FOOD;
  if (sceneId.startsWith('urban') || sceneId === 'street' || sceneId === 'architecture') return SceneCategory.URBAN;
  if (sceneId.startsWith('still')) return SceneCategory.STILL_LIFE;
  if (sceneId.startsWith('macro')) return SceneCategory.MACRO;
  if (sceneId.startsWith('event')) return SceneCategory.EVENT;
  return SceneCategory.PORTRAIT;
}

/**
 * 参数调整建议
 */
export interface ParamAdjustment {
  param: string;
  displayName: string;
  currentValue: number;
  targetValue: number;
  delta: number;
  advice: string;
}

/**
 * 获取参数调整建议
 */
export function getParamAdjustmentAdvice(
  currentParams: HasselbladParams,
  targetSceneId: string
): ParamAdjustment[] {
  const targetParams = getHasselbladParams(targetSceneId);
  const adjustments: ParamAdjustment[] = [];

  const paramNames: Record<string, { name: string; key: keyof HasselbladParams }> = {
    tone: { name: '影调', key: 'tone' },
    saturation: { name: '饱和度', key: 'saturation' },
    contrast: { name: '对比度', key: 'contrast' },
    colorTemp: { name: '色温', key: 'colorTemp' },
    sharpness: { name: '锐度', key: 'sharpness' },
    vignette: { name: '暗角', key: 'vignette' },
    cyanMagenta: { name: '青品调', key: 'cyanMagenta' },
  };

  for (const [param, { name, key }] of Object.entries(paramNames)) {
    const current = currentParams[key] as number;
    const target = targetParams[key] as number;

    if (current !== target) {
      const delta = target - current;
      adjustments.push({
        param,
        displayName: name,
        currentValue: current,
        targetValue: target,
        delta,
        advice: delta > 0
          ? `建议提升 ${name} ${Math.abs(delta)} 点`
          : `建议降低 ${name} ${Math.abs(delta)} 点`,
      });
    }
  }

  return adjustments.sort((a, b) => Math.abs(b.delta) - Math.abs(a.delta));
}