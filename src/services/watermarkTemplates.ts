/**
 * 统一水印模板预设
 * 20个专业水印模板体系 - 与Android端完全同步
 */

import {
  MasterWatermarkTemplate,
  WatermarkLayerDef,
  WatermarkLayerStyle,
  WatermarkStylePreset,
  DEFAULT_LAYER_STYLE,
} from '../types/watermark';

// ========== 辅助函数 ==========
const createLayer = (
  id: string,
  type: WatermarkLayerDef['type'],
  options: Partial<WatermarkLayerDef> = {}
): WatermarkLayerDef => ({
  id,
  type,
  defaultContent: options.defaultContent ?? '',
  defaultPosition: options.defaultPosition ?? 'bottom-left',
  defaultStyle: options.defaultStyle ?? DEFAULT_LAYER_STYLE,
  isRequired: options.isRequired ?? false,
  contentSource: options.contentSource ?? 'manual',
  isEnabled: options.isEnabled ?? true,
  sortOrder: options.sortOrder ?? 0,
});

const createStyle = (options: Partial<WatermarkLayerStyle> = {}): WatermarkLayerStyle => ({
  ...DEFAULT_LAYER_STYLE,
  ...options,
});

// ========== 统一模板列表 ==========
export const MASTER_WATERMARK_TEMPLATES: MasterWatermarkTemplate[] = [
  // === 哈苏大师系列 (3个) ===
  {
    id: 'hasselblad-master',
    name: '哈苏大师',
    nameEn: 'Hasselblad Master',
    category: 'hasselblad',
    description: '哈苏大师赛官方水印风格',
    previewThumb: '',
    isHasselbladSeries: true,
    isPopular: true,
    isNew: false,
    layers: [
      createLayer('brand', 'brand', {
        defaultContent: 'HASSELBLAD',
        defaultPosition: 'bottom-center',
        defaultStyle: createStyle({ fontSize: 14, fontWeight: 700, letterSpacing: 2, opacity: 0.9 }),
        isRequired: true,
        sortOrder: 0,
      }),
      createLayer('subtitle', 'text', {
        defaultContent: 'Natural Color Solution',
        defaultPosition: 'bottom-center',
        defaultStyle: createStyle({ fontSize: 10, opacity: 0.6 }),
        sortOrder: 1,
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 14,
      opacity: 0.9,
      letterSpacing: 2,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-center',
  },
  {
    id: 'hasselblad-hncs',
    name: 'HNCS认证',
    nameEn: 'HNCS Certified',
    category: 'hasselblad',
    description: '哈苏自然色彩解决方案认证',
    previewThumb: '',
    isHasselbladSeries: true,
    isPopular: false,
    isNew: true,
    layers: [
      createLayer('brand', 'brand', {
        defaultContent: 'HNCS',
        defaultStyle: createStyle({ fontSize: 16, fontWeight: 700, letterSpacing: 4 }),
        isRequired: true,
      }),
      createLayer('cert', 'text', {
        defaultContent: 'CERTIFIED',
        defaultStyle: createStyle({ fontSize: 8, opacity: 0.5 }),
        sortOrder: 1,
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 16,
      opacity: 0.9,
      letterSpacing: 4,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-center',
  },
  {
    id: 'hasselblad-xpan',
    name: 'XPAN宽幅',
    nameEn: 'XPAN Format',
    category: 'hasselblad',
    description: '哈苏XPAN宽幅相机风格',
    previewThumb: '',
    isHasselbladSeries: true,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('brand', 'brand', {
        defaultContent: 'XPAN',
        defaultStyle: createStyle({ fontSize: 18, fontWeight: 700, letterSpacing: 6 }),
        isRequired: true,
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 18,
      opacity: 0.8,
      letterSpacing: 6,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-center',
  },

  // === 品牌认证系列 (5个) ===
  {
    id: 'classic-camera',
    name: '经典相机',
    nameEn: 'Classic Camera',
    category: 'brand',
    description: '经典相机水印风格',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: true,
    isNew: false,
    layers: [
      createLayer('shot-on', 'text', {
        defaultContent: 'Shot on',
        defaultStyle: createStyle({ fontSize: 12, opacity: 0.6 }),
        sortOrder: 0,
      }),
      createLayer('brand', 'brand', {
        defaultContent: 'OMaster',
        defaultStyle: createStyle({ fontSize: 16, fontWeight: 700 }),
        isRequired: true,
        sortOrder: 1,
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 16,
      opacity: 0.8,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-left',
  },
  {
    id: 'leica-style',
    name: '徕卡风格',
    nameEn: 'Leica Style',
    category: 'brand',
    description: '徕卡相机经典风格',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: true,
    isNew: false,
    layers: [
      createLayer('brand', 'brand', {
        defaultContent: 'Leica',
        defaultStyle: createStyle({ fontSize: 18, fontWeight: 700, letterSpacing: 3 }),
        isRequired: true,
      }),
      createLayer('subtitle', 'text', {
        defaultContent: 'Camera AG',
        defaultStyle: createStyle({ fontSize: 10, opacity: 0.5 }),
        sortOrder: 1,
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 18,
      opacity: 0.8,
      letterSpacing: 3,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-right',
  },
  {
    id: 'oppo-find',
    name: 'Find系列',
    nameEn: 'OPPO Find',
    category: 'brand',
    description: 'OPPO Find系列手机风格',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('brand', 'brand', {
        defaultContent: 'OPPO Find',
        defaultStyle: createStyle({ fontSize: 14, fontWeight: 600 }),
        isRequired: true,
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 14,
      opacity: 0.8,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-left',
  },
  {
    id: 'oneplus-hasselblad',
    name: '一加哈苏',
    nameEn: 'OnePlus Hasselblad',
    category: 'brand',
    description: '一加哈苏联合水印',
    previewThumb: '',
    isHasselbladSeries: true,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('brand', 'brand', {
        defaultContent: 'OnePlus | HASSELBLAD',
        defaultStyle: createStyle({ fontSize: 12, fontWeight: 600 }),
        isRequired: true,
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 12,
      opacity: 0.8,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-center',
  },
  {
    id: 'film-strip',
    name: '胶片条',
    nameEn: 'Film Strip',
    category: 'brand',
    description: '胶片边框风格',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('brand', 'brand', {
        defaultContent: 'FILM',
        defaultStyle: createStyle({ fontSize: 10, letterSpacing: 8, opacity: 0.7 }),
        isRequired: true,
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 10,
      opacity: 0.7,
      letterSpacing: 8,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-center',
  },

  // === 极简印记系列 (2个) ===
  {
    id: 'minimal-mark',
    name: '极简印记',
    nameEn: 'Minimal Mark',
    category: 'minimal',
    description: '极简风格水印',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: true,
    isNew: false,
    layers: [
      createLayer('brand', 'brand', {
        defaultContent: 'OM',
        defaultStyle: createStyle({ fontSize: 20, fontWeight: 700 }),
        isRequired: true,
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 20,
      opacity: 0.8,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-right',
  },
  {
    id: 'brand-logo',
    name: '品牌Logo',
    nameEn: 'Brand Logo',
    category: 'minimal',
    description: '仅显示品牌Logo',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('logo', 'logo', {
        defaultPosition: 'bottom-right',
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 14,
      opacity: 0.8,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-right',
  },

  // === 技术参数系列 (2个) ===
  {
    id: 'detailed-params',
    name: '详细参数',
    nameEn: 'Detailed Parameters',
    category: 'tech',
    description: '显示完整拍摄参数',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('device', 'device', {
        contentSource: 'device_info',
        defaultStyle: createStyle({ fontSize: 12 }),
      }),
      createLayer('params', 'params', {
        contentSource: 'exif',
        defaultStyle: createStyle({ fontSize: 10, opacity: 0.7 }),
        sortOrder: 1,
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 12,
      opacity: 0.8,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-left',
  },
  {
    id: 'exif-info',
    name: 'EXIF信息',
    nameEn: 'EXIF Info',
    category: 'tech',
    description: '从照片读取EXIF信息',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('params', 'params', {
        contentSource: 'exif',
        defaultStyle: createStyle({ fontSize: 11 }),
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 11,
      opacity: 0.8,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-left',
  },

  // === 信息记录系列 (2个) ===
  {
    id: 'location-tag',
    name: '地理位置',
    nameEn: 'Location Tag',
    category: 'info',
    description: '显示拍摄地点',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('location', 'location', {
        contentSource: 'gps',
        defaultStyle: createStyle({ fontSize: 12 }),
      }),
      createLayer('date', 'timestamp', {
        contentSource: 'exif',
        defaultStyle: createStyle({ fontSize: 10, opacity: 0.6 }),
        sortOrder: 1,
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 12,
      opacity: 0.8,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-left',
  },
  {
    id: 'timestamp',
    name: '时间戳',
    nameEn: 'Timestamp',
    category: 'info',
    description: '显示拍摄时间',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('date', 'timestamp', {
        contentSource: 'system',
        defaultStyle: createStyle({ fontSize: 12 }),
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 12,
      opacity: 0.8,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-left',
  },

  // === 个人签名系列 (2个) ===
  {
    id: 'photographer-sign',
    name: '摄影师签名',
    nameEn: 'Photographer Signature',
    category: 'personal',
    description: '摄影师个人署名',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: true,
    isNew: false,
    layers: [
      createLayer('signature', 'text', {
        defaultContent: 'Photographer',
        defaultStyle: createStyle({ fontSize: 14, fontFamily: 'italic' }),
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 14,
      opacity: 0.8,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'italic',
    },
    defaultPosition: 'bottom-right',
  },
  {
    id: 'art-signature',
    name: '艺术签名',
    nameEn: 'Art Signature',
    category: 'personal',
    description: '艺术风格签名',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('signature', 'text', {
        defaultContent: 'Artist',
        defaultStyle: createStyle({ fontSize: 16, fontWeight: 300, letterSpacing: 4 }),
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 16,
      opacity: 0.8,
      letterSpacing: 4,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-right',
  },

  // === 社交分享系列 (1个) ===
  {
    id: 'social-share',
    name: '社交分享',
    nameEn: 'Social Share',
    category: 'social',
    description: '社交媒体账号',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('social', 'text', {
        defaultContent: '@omaster',
        defaultStyle: createStyle({ fontSize: 14 }),
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 14,
      opacity: 0.8,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-center',
  },

  // === 版权保护系列 (1个) ===
  {
    id: 'copyright',
    name: '版权声明',
    nameEn: 'Copyright',
    category: 'legal',
    description: '版权保护声明',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('copyright', 'text', {
        defaultContent: '© 2026 OMaster. All rights reserved.',
        defaultStyle: createStyle({ fontSize: 10, opacity: 0.6 }),
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 10,
      opacity: 0.6,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-center',
  },

  // === 荣誉徽章系列 (1个) ===
  {
    id: 'award-badge',
    name: '获奖作品',
    nameEn: 'Award Badge',
    category: 'badge',
    description: '获奖作品标识',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('award', 'text', {
        defaultContent: '🏆 Award Winning',
        defaultStyle: createStyle({ fontSize: 12 }),
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 12,
      opacity: 0.8,
      letterSpacing: 0,
      shadowEnabled: true,
      shadowBlur: 4,
      fontFamily: 'default',
    },
    defaultPosition: 'bottom-center',
  },

  // === 专业防伪系列 (1个) ===
  {
    id: 'pro-protect',
    name: '专业防伪',
    nameEn: 'Pro Protection',
    category: 'pro',
    description: '满屏半透明防伪水印',
    previewThumb: '',
    isHasselbladSeries: false,
    isPopular: false,
    isNew: false,
    layers: [
      createLayer('protect', 'text', {
        defaultContent: 'OMASTER',
        defaultStyle: createStyle({ fontSize: 48, opacity: 0.1, rotation: -30 }),
      }),
    ],
    presetStyle: {
      primaryColor: '#FFFFFF',
      secondaryColor: '#FFFFFF',
      fontSize: 48,
      opacity: 0.1,
      letterSpacing: 0,
      shadowEnabled: false,
      shadowBlur: 0,
      fontFamily: 'default',
    },
    defaultPosition: 'center',
  },
];

// ========== 获取模板函数 ==========
export function getWatermarkTemplateById(id: string): MasterWatermarkTemplate | undefined {
  return MASTER_WATERMARK_TEMPLATES.find(t => t.id === id);
}

export function getWatermarkTemplatesByCategory(category: string): MasterWatermarkTemplate[] {
  if (category === 'all') {
    return MASTER_WATERMARK_TEMPLATES;
  }
  return MASTER_WATERMARK_TEMPLATES.filter(t => t.category === category);
}

export function getPopularWatermarkTemplates(): MasterWatermarkTemplate[] {
  return MASTER_WATERMARK_TEMPLATES.filter(t => t.isPopular);
}

export function getNewWatermarkTemplates(): MasterWatermarkTemplate[] {
  return MASTER_WATERMARK_TEMPLATES.filter(t => t.isNew);
}

export function getHasselbladWatermarkTemplates(): MasterWatermarkTemplate[] {
  return MASTER_WATERMARK_TEMPLATES.filter(t => t.isHasselbladSeries);
}
