/**
 * LUT资源服务
 * 提供9款哈苏胶片LUT资源，使用统一MasterLUT数据模型
 */

import {
  MasterLUT,
  LUTCategory,
  LUTFormat,
  LUTSize,
  LUTSource,
  LUTParams,
  getCategoryDisplay,
} from '../models/MasterLUT';

// 重新导出类型
export {
  MasterLUT,
  LUTCategory,
  LUTFormat,
  LUTSize,
  LUTSource,
  LUTParams,
  getCategoryDisplay,
};

// LUT分类列表
export const LUT_CATEGORIES = Object.values(LUTCategory).map(cat => ({
  key: cat,
  label: getCategoryDisplay(cat).displayName,
  icon: getCategoryDisplay(cat).icon,
}));

// 9款哈苏胶片LUT资源库 - 使用MasterLUT格式
export const MASTER_LUT_RESOURCES: MasterLUT[] = [
  // === 原生经典系列 ===
  {
    id: 'classic-chrome',
    name: 'Classic Chrome (CC)',
    nameEn: 'Classic Chrome',
    description: '经典铬色，低饱和高对比，复古胶片质感',
    longDescription: '【环境建议】日间户外或充足自然光，避免强逆光场景。【场景推荐】街拍、人文、复古、黑白摄影。【拍摄要点】适合追求经典胶片质感，低饱和度呈现怀旧氛围，建议配合哈苏大师模式使用。',
    category: LUTCategory.HASSELBLAD,
    subCategory: '原生经典',
    tags: ['经典', '铬色', '复古', '低饱和'],
    suitableFor: ['街拍', '人文', '复古', '黑白'],
    format: LUTFormat.CUBE,
    size: LUTSize.SIZE_33,
    fileSize: 12288,
    coverImage: 'https://images.unsplash.com/photo-1507003211169-0a70dd7d80ad?w=400',
    sampleImages: [
      'https://images.unsplash.com/photo-1507003211169-0a70dd7d80ad?w=800',
      'https://images.unsplash.com/photo-1444723121867-c61267198d6c?w=800',
    ],
    downloadUrl: 'https://cdn.hasselblad.com/lut/cc_33.cube',
    mirrorUrls: [
      'https://cdn.jsdelivr.net/gh/hasselblad/luts/cc_33.cube',
    ],
    author: 'Hasselblad Labs',
    authorAvatar: 'https://cdn.hasselblad.com/avatar/hasselblad.png',
    authorUrl: 'https://www.hasselblad.com',
    source: LUTSource.HASSELBLAD,
    isHncsCertified: true,
    filmPresetMapping: 'CC',
    hasselbladCollection: '原生经典',
    isFree: true,
    isHot: true,
    isNew: false,
    isFeatured: true,
    featuredReason: '哈苏经典铬色风格，低饱和高对比的复古质感',
    downloads: 12500,
    likes: 8900,
    rating: 4.9,
    ratingCount: 1256,
    generatedParams: {
      saturation: -0.3,
      contrast: 0.2,
      brightness: 0,
      colorTemperature: 0,
      tint: 0,
      highlightRolloff: 0.1,
      shadowLift: 0,
      skinProtection: true,
    },
    version: 1,
    createdAt: '2024-06-15T00:00:00Z',
    updatedAt: '2024-08-10T00:00:00Z',
    minAppVersion: '1.0',
    usageGuide: '适用于DaVinci Resolve、Premiere Pro、Final Cut Pro等主流软件。建议在户外自然光下使用，可获得最佳复古胶片效果。',
    compatibleSoftware: ['DaVinci Resolve', 'Premiere Pro', 'Final Cut Pro', 'LumaFusion', 'CapCut'],
  },
  {
    id: 'neutral-color',
    name: 'Neutral Color (NC)',
    nameEn: 'Neutral Color',
    description: '中性色彩，自然还原，适合日常拍摄',
    longDescription: '【环境建议】全天候适用，室内外均可。【场景推荐】风景、建筑、日常、旅行摄影。【拍摄要点】中性色彩还原，保持画面真实感，适合追求自然风格的拍摄。',
    category: LUTCategory.HASSELBLAD,
    subCategory: '原生经典',
    tags: ['中性', '自然', '还原', '日常'],
    suitableFor: ['风景', '建筑', '日常', '旅行'],
    format: LUTFormat.CUBE,
    size: LUTSize.SIZE_33,
    fileSize: 11264,
    coverImage: 'https://images.unsplash.com/photo-1493863641943-5b68c26e91bb?w=400',
    sampleImages: [
      'https://images.unsplash.com/photo-1493863641943-5b68c26e91bb?w=800',
      'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800',
    ],
    downloadUrl: 'https://cdn.hasselblad.com/lut/nc_33.cube',
    author: 'Hasselblad Labs',
    source: LUTSource.HASSELBLAD,
    isHncsCertified: true,
    filmPresetMapping: 'NC',
    hasselbladCollection: '原生经典',
    isFree: true,
    isHot: true,
    isNew: false,
    downloads: 9800,
    likes: 7650,
    rating: 4.8,
    ratingCount: 982,
    generatedParams: {
      saturation: 0,
      contrast: 0,
      brightness: 0,
      colorTemperature: 0,
      tint: 0,
      highlightRolloff: 0,
      shadowLift: 0,
      skinProtection: true,
    },
    version: 1,
    createdAt: '2024-07-20T00:00:00Z',
    minAppVersion: '1.0',
    compatibleSoftware: ['DaVinci Resolve', 'Premiere Pro', 'Final Cut Pro'],
  },
  {
    id: 'natural-hue',
    name: 'Natural Hue (NH)',
    nameEn: 'Natural Hue',
    description: '自然色调，肤色优化，人像首选',
    longDescription: '【环境建议】柔和自然光或室内补光。【场景推荐】人像、婚礼、写真、肖像摄影。【拍摄要点】肤色优化算法，自然色调呈现，适合追求真实肤色的人像拍摄。',
    category: LUTCategory.HASSELBLAD,
    subCategory: '原生经典',
    tags: ['自然', '色调', '肤色', '人像'],
    suitableFor: ['人像', '婚礼', '写真', '肖像'],
    format: LUTFormat.CUBE,
    size: LUTSize.SIZE_33,
    fileSize: 13312,
    coverImage: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400',
    sampleImages: [
      'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=800',
      'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800',
    ],
    downloadUrl: 'https://cdn.hasselblad.com/lut/nh_33.cube',
    author: 'Hasselblad Labs',
    source: LUTSource.HASSELBLAD,
    isHncsCertified: true,
    filmPresetMapping: 'NH',
    hasselbladCollection: '原生经典',
    isFree: true,
    isHot: true,
    isNew: false,
    isFeatured: true,
    featuredReason: '哈苏肤色优化算法，自然色调呈现',
    downloads: 8500,
    likes: 5430,
    rating: 4.9,
    ratingCount: 856,
    generatedParams: {
      saturation: 0.1,
      contrast: 0.05,
      brightness: 0,
      colorTemperature: 0.1,
      tint: 0,
      highlightRolloff: 0.05,
      shadowLift: 0,
      skinProtection: true,
    },
    version: 1,
    createdAt: '2024-08-10T00:00:00Z',
    minAppVersion: '1.0',
    compatibleSoftware: ['DaVinci Resolve', 'Premiere Pro', 'Final Cut Pro'],
  },

  // === 情绪表达系列 ===
  {
    id: 'portra-400',
    name: 'Portra 400',
    nameEn: 'Kodak Portra 400',
    description: '专业人像胶片，温暖肤色，户外首选',
    longDescription: '【环境建议】户外自然光，黄金时刻效果最佳。【场景推荐】人像、婚礼、户外、街拍。【拍摄要点】专业人像胶片色彩，温暖肤色呈现，配合哈苏大师模式可获得更自然的肤色表现。',
    category: LUTCategory.HASSELBLAD,
    subCategory: '情绪表达',
    tags: ['人像', '温暖', '胶片', '婚礼'],
    suitableFor: ['人像', '婚礼', '户外', '街拍'],
    format: LUTFormat.CUBE,
    size: LUTSize.SIZE_33,
    fileSize: 15360,
    coverImage: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400',
    sampleImages: [
      'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=800',
      'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=800',
    ],
    downloadUrl: 'https://cdn.hasselblad.com/lut/portra_33.cube',
    author: 'Hasselblad Labs',
    source: LUTSource.HASSELBLAD,
    isHncsCertified: true,
    filmPresetMapping: 'Portra',
    hasselbladCollection: '情绪表达',
    isFree: true,
    isHot: true,
    isNew: false,
    isFeatured: true,
    featuredReason: '柯达Portra 400专业人像胶片风格',
    downloads: 15600,
    likes: 12300,
    rating: 4.9,
    ratingCount: 1562,
    generatedParams: {
      saturation: 0.15,
      contrast: 0.1,
      brightness: 0.05,
      colorTemperature: 0.2,
      tint: 0.05,
      highlightRolloff: 0.1,
      shadowLift: 0,
      skinProtection: true,
    },
    version: 1,
    createdAt: '2024-05-01T00:00:00Z',
    minAppVersion: '1.0',
    compatibleSoftware: ['DaVinci Resolve', 'Premiere Pro', 'Final Cut Pro'],
  },
  {
    id: 'rdp3-fujifilm',
    name: 'RDP3 Fujifilm',
    nameEn: 'Fujifilm RDP3',
    description: '富士反转片，鲜艳通透，风景利器',
    longDescription: '【环境建议】户外自然光，晴天效果最佳。【场景推荐】风景、自然、旅行、户外摄影。【拍摄要点】富士反转片风格，鲜艳通透的色彩呈现，适合追求高饱和度的风景拍摄。',
    category: LUTCategory.HASSELBLAD,
    subCategory: '情绪表达',
    tags: ['富士', '反转片', '鲜艳', '风景'],
    suitableFor: ['风景', '自然', '旅行', '户外'],
    format: LUTFormat.CUBE,
    size: LUTSize.SIZE_33,
    fileSize: 14336,
    coverImage: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400',
    sampleImages: [
      'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800',
      'https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=800',
    ],
    downloadUrl: 'https://cdn.hasselblad.com/lut/rdp3_33.cube',
    author: 'Hasselblad Labs',
    source: LUTSource.HASSELBLAD,
    isHncsCertified: true,
    filmPresetMapping: 'RDP3',
    hasselbladCollection: '情绪表达',
    isFree: true,
    isHot: false,
    isNew: false,
    downloads: 7200,
    likes: 6780,
    rating: 4.7,
    ratingCount: 722,
    generatedParams: {
      saturation: 0.25,
      contrast: 0.15,
      brightness: 0.05,
      colorTemperature: 0,
      tint: 0,
      highlightRolloff: 0.05,
      shadowLift: 0,
      skinProtection: false,
    },
    version: 1,
    createdAt: '2024-06-20T00:00:00Z',
    minAppVersion: '1.0',
    compatibleSoftware: ['DaVinci Resolve', 'Premiere Pro', 'Final Cut Pro'],
  },

  // === 结构时间系列 ===
  {
    id: 'cinestill-800t',
    name: 'CineStill 800T',
    nameEn: 'CineStill 800T',
    description: '电影夜景，钨丝灯暖调，城市夜拍',
    longDescription: '【环境建议】夜景、室内低光、钨丝灯环境。【场景推荐】夜景、城市、室内、电影风格。【拍摄要点】电影灯光片风格，钨丝灯平衡，适合夜景和室内低光拍摄。',
    category: LUTCategory.HASSELBLAD,
    subCategory: '结构时间',
    tags: ['夜景', '电影', '城市', '钨丝灯'],
    suitableFor: ['夜景', '城市', '室内', '电影'],
    format: LUTFormat.CUBE,
    size: LUTSize.SIZE_33,
    fileSize: 10240,
    coverImage: 'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=400',
    sampleImages: [
      'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=800',
      'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=800',
    ],
    downloadUrl: 'https://cdn.hasselblad.com/lut/800t_33.cube',
    author: 'Hasselblad Labs',
    source: LUTSource.HASSELBLAD,
    isHncsCertified: true,
    filmPresetMapping: '800T',
    hasselbladCollection: '结构时间',
    isFree: true,
    isHot: true,
    isNew: false,
    downloads: 11300,
    likes: 8900,
    rating: 4.8,
    ratingCount: 1132,
    generatedParams: {
      saturation: 0.1,
      contrast: 0.2,
      brightness: -0.05,
      colorTemperature: 0.3,
      tint: 0.1,
      highlightRolloff: 0.15,
      shadowLift: 0.05,
      skinProtection: true,
    },
    version: 1,
    createdAt: '2024-07-15T00:00:00Z',
    minAppVersion: '1.0',
    compatibleSoftware: ['DaVinci Resolve', 'Premiere Pro', 'Final Cut Pro'],
  },
  {
    id: 'trix-400',
    name: 'Tri-X 400 (TX400)',
    nameEn: 'Kodak Tri-X 400',
    description: '经典黑白，颗粒质感，人文纪实',
    longDescription: '【环境建议】全天候适用，高对比度场景效果最佳。【场景推荐】黑白、人文、纪实、街拍。【拍摄要点】经典黑白胶片风格，颗粒质感呈现，适合人文纪实摄影。',
    category: LUTCategory.HASSELBLAD,
    subCategory: '结构时间',
    tags: ['黑白', '颗粒', '纪实', '人文'],
    suitableFor: ['黑白', '人文', '纪实', '街拍'],
    format: LUTFormat.CUBE,
    size: LUTSize.SIZE_33,
    fileSize: 8192,
    coverImage: 'https://images.unsplash.com/photo-1444723121867-c61267198d6c?w=400',
    sampleImages: [
      'https://images.unsplash.com/photo-1444723121867-c61267198d6c?w=800',
      'https://images.unsplash.com/photo-1476973422084-e0fa66ff9456?w=800',
    ],
    downloadUrl: 'https://cdn.hasselblad.com/lut/tx400_33.cube',
    author: 'Hasselblad Labs',
    source: LUTSource.HASSELBLAD,
    isHncsCertified: true,
    filmPresetMapping: 'TX400',
    hasselbladCollection: '结构时间',
    isFree: true,
    isHot: true,
    isNew: false,
    isFeatured: true,
    featuredReason: '柯达Tri-X 400经典黑白胶片风格',
    downloads: 9500,
    likes: 15600,
    rating: 4.9,
    ratingCount: 956,
    generatedParams: {
      saturation: -1,
      contrast: 0.25,
      brightness: 0,
      colorTemperature: 0,
      tint: 0,
      highlightRolloff: 0.1,
      shadowLift: 0,
      skinProtection: false,
    },
    version: 1,
    createdAt: '2024-08-01T00:00:00Z',
    minAppVersion: '1.0',
    compatibleSoftware: ['DaVinci Resolve', 'Premiere Pro', 'Final Cut Pro'],
  },

  // === 数字记忆系列 ===
  {
    id: 'ccd-warm',
    name: 'CCD Warm',
    nameEn: 'CCD Warm',
    description: '数码暖调，复古质感，怀旧风格',
    longDescription: '【环境建议】柔和光线，室内外均可。【场景推荐】复古、怀旧、日常、Vlog。【拍摄要点】数码暖调风格，复古质感呈现，适合怀旧风格拍摄。',
    category: LUTCategory.HASSELBLAD,
    subCategory: '数字记忆',
    tags: ['数码', '暖调', '复古', '怀旧'],
    suitableFor: ['复古', '怀旧', '日常', 'Vlog'],
    format: LUTFormat.CUBE,
    size: LUTSize.SIZE_33,
    fileSize: 8192,
    coverImage: 'https://images.unsplash.com/photo-1476224203421-9ac39bcb3327?w=400',
    sampleImages: [
      'https://images.unsplash.com/photo-1476224203421-9ac39bcb3327?w=800',
      'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=800',
    ],
    downloadUrl: 'https://cdn.hasselblad.com/lut/ccd-warm_33.cube',
    author: 'Hasselblad Labs',
    source: LUTSource.HASSELBLAD,
    isHncsCertified: false,
    filmPresetMapping: 'CCD-Warm',
    hasselbladCollection: '数字记忆',
    isFree: true,
    isHot: false,
    isNew: false,
    downloads: 6800,
    likes: 11200,
    rating: 4.6,
    ratingCount: 684,
    generatedParams: {
      saturation: 0.05,
      contrast: 0.1,
      brightness: 0.05,
      colorTemperature: 0.15,
      tint: 0.05,
      highlightRolloff: 0.08,
      shadowLift: 0,
      skinProtection: true,
    },
    version: 1,
    createdAt: '2024-08-15T00:00:00Z',
    minAppVersion: '1.0',
    compatibleSoftware: ['DaVinci Resolve', 'Premiere Pro', 'Final Cut Pro'],
  },
  {
    id: 'ccd-cool',
    name: 'CCD Cool',
    nameEn: 'CCD Cool',
    description: '数码冷调，清透风格，现代感',
    longDescription: '【环境建议】清透光线，室内外均可。【场景推荐】现代、清透、科技、开箱。【拍摄要点】数码冷调风格，清透质感呈现，适合现代风格拍摄。',
    category: LUTCategory.HASSELBLAD,
    subCategory: '数字记忆',
    tags: ['数码', '冷调', '清透', '现代'],
    suitableFor: ['现代', '清透', '科技', '开箱'],
    format: LUTFormat.CUBE,
    size: LUTSize.SIZE_33,
    fileSize: 9216,
    coverImage: 'https://images.unsplash.com/photo-1488426862026-c5e5a0a0a8e1?w=400',
    sampleImages: [
      'https://images.unsplash.com/photo-1488426862026-c5e5a0a0a8e1?w=800',
      'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=800',
    ],
    downloadUrl: 'https://cdn.hasselblad.com/lut/ccd-cool_33.cube',
    author: 'Hasselblad Labs',
    source: LUTSource.HASSELBLAD,
    isHncsCertified: false,
    filmPresetMapping: 'CCD-Cool',
    hasselbladCollection: '数字记忆',
    isFree: true,
    isHot: false,
    isNew: false,
    downloads: 5400,
    likes: 13400,
    rating: 4.7,
    ratingCount: 544,
    generatedParams: {
      saturation: 0,
      contrast: 0.05,
      brightness: 0,
      colorTemperature: -0.1,
      tint: -0.05,
      highlightRolloff: 0.05,
      shadowLift: 0,
      skinProtection: true,
    },
    version: 1,
    createdAt: '2024-09-01T00:00:00Z',
    minAppVersion: '1.0',
    compatibleSoftware: ['DaVinci Resolve', 'Premiere Pro', 'Final Cut Pro'],
  },
];

// 导出兼容旧版本的LUT_RESOURCES
export const LUT_RESOURCES = MASTER_LUT_RESOURCES;

// 获取LUT资源列表
export function getLUTResources(category?: LUTCategory | string): MasterLUT[] {
  if (!category || category === LUTCategory.ALL || category === 'all') {
    return MASTER_LUT_RESOURCES;
  }
  return MASTER_LUT_RESOURCES.filter(lut => lut.category === category || lut.subCategory === category);
}

// 搜索LUT资源
export function searchLUTResources(query: string): MasterLUT[] {
  const q = query.toLowerCase();
  return MASTER_LUT_RESOURCES.filter(lut =>
    lut.name.toLowerCase().includes(q) ||
    lut.nameEn.toLowerCase().includes(q) ||
    lut.description.toLowerCase().includes(q) ||
    lut.tags.some(tag => tag.toLowerCase().includes(q)) ||
    (lut.longDescription && lut.longDescription.toLowerCase().includes(q))
  );
}

// 获取热门LUT
export function getHotLUTs(): MasterLUT[] {
  return MASTER_LUT_RESOURCES.filter(lut => lut.isHot).sort((a, b) => (b.downloads || 0) - (a.downloads || 0));
}

// 获取最新LUT
export function getNewLUTs(): MasterLUT[] {
  return MASTER_LUT_RESOURCES.filter(lut => lut.isNew);
}

// 获取精选LUT
export function getFeaturedLUTs(): MasterLUT[] {
  return MASTER_LUT_RESOURCES.filter(lut => lut.isFeatured);
}

// 按系列获取LUT
export function getLUTsByCollection(collection: string): MasterLUT[] {
  return MASTER_LUT_RESOURCES.filter(lut => lut.hasselbladCollection === collection);
}

// 获取HNCS认证LUT
export function getHncsCertifiedLUTs(): MasterLUT[] {
  return MASTER_LUT_RESOURCES.filter(lut => lut.isHncsCertified);
}

// 下载LUT文件
export async function downloadLUT(lut: MasterLUT): Promise<Blob> {
  const response = await fetch(lut.downloadUrl);
  if (!response.ok) {
    // 尝试备用链接
    if (lut.mirrorUrls && lut.mirrorUrls.length > 0) {
      for (const mirrorUrl of lut.mirrorUrls) {
        const mirrorResponse = await fetch(mirrorUrl);
        if (mirrorResponse.ok) {
          return mirrorResponse.blob();
        }
      }
    }
    throw new Error(`下载失败: ${response.statusText}`);
  }
  return response.blob();
}

// 导出格式化函数（已在models中定义，这里重新导出）
export { formatFileSize, formatDownloads } from '../models/MasterLUT';