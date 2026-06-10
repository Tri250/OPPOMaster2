/**
 * 统一水印模板类型定义
 * 双端统一：Android (Kotlin) ↔ Web (TypeScript)
 */

// ========== 分类枚举 ==========
export type WatermarkCategory = 
  | 'all' 
  | 'brand' 
  | 'minimal' 
  | 'tech' 
  | 'info' 
  | 'personal' 
  | 'social' 
  | 'legal' 
  | 'badge' 
  | 'pro' 
  | 'hasselblad';

export const WATERMARK_CATEGORIES: { key: WatermarkCategory; label: string; icon: string }[] = [
  { key: 'all', label: '全部', icon: '📋' },
  { key: 'brand', label: '品牌认证', icon: '⭐' },
  { key: 'minimal', label: '极简印记', icon: '✨' },
  { key: 'tech', label: '技术参数', icon: '⚙️' },
  { key: 'info', label: '信息记录', icon: '📅' },
  { key: 'personal', label: '个人签名', icon: '✍️' },
  { key: 'social', label: '社交分享', icon: '@' },
  { key: 'legal', label: '版权保护', icon: '©' },
  { key: 'badge', label: '荣誉徽章', icon: '🏆' },
  { key: 'pro', label: '专业防伪', icon: '🔐' },
  { key: 'hasselblad', label: '哈苏大师', icon: '👑' },
];

// ========== 图层类型 ==========
export type WatermarkLayerType = 
  | 'text' 
  | 'brand' 
  | 'device' 
  | 'params' 
  | 'timestamp' 
  | 'location' 
  | 'logo' 
  | 'shape' 
  | 'vignette';

// ========== 内容来源 ==========
export type ContentSource = 'manual' | 'exif' | 'gps' | 'system' | 'device_info';

// ========== 水印位置 ==========
export type WatermarkPosition = 
  | 'top-left' 
  | 'top-center' 
  | 'top-right' 
  | 'center-left' 
  | 'center' 
  | 'center-right' 
  | 'bottom-left' 
  | 'bottom-center' 
  | 'bottom-right';

export const WATERMARK_POSITIONS: { key: WatermarkPosition; label: string }[] = [
  { key: 'top-left', label: '左上' },
  { key: 'top-center', label: '上中' },
  { key: 'top-right', label: '右上' },
  { key: 'center-left', label: '左中' },
  { key: 'center', label: '居中' },
  { key: 'center-right', label: '右中' },
  { key: 'bottom-left', label: '左下' },
  { key: 'bottom-center', label: '下中' },
  { key: 'bottom-right', label: '右下' },
];

// ========== 图层样式 ==========
export interface WatermarkLayerStyle {
  fontSize: number;
  fontFamily: string;
  fontWeight: number;
  color: string;
  opacity: number;
  letterSpacing: number;
  rotation: number;
  shadowEnabled: boolean;
  shadowBlur: number;
  backgroundColor: string;
  backgroundOpacity: number;
  padding: number;
}

export const DEFAULT_LAYER_STYLE: WatermarkLayerStyle = {
  fontSize: 14,
  fontFamily: 'default',
  fontWeight: 400,
  color: '#FFFFFF',
  opacity: 0.8,
  letterSpacing: 0,
  rotation: 0,
  shadowEnabled: true,
  shadowBlur: 4,
  backgroundColor: 'transparent',
  backgroundOpacity: 0,
  padding: 8,
};

// ========== 图层定义 ==========
export interface WatermarkLayerDef {
  id: string;
  type: WatermarkLayerType;
  defaultContent: string;
  defaultPosition: WatermarkPosition;
  defaultStyle: WatermarkLayerStyle;
  isRequired: boolean;
  contentSource: ContentSource;
  isEnabled: boolean;
  sortOrder: number;
}

// ========== 预设样式 ==========
export interface WatermarkStylePreset {
  primaryColor: string;
  secondaryColor: string;
  fontSize: number;
  opacity: number;
  letterSpacing: number;
  shadowEnabled: boolean;
  shadowBlur: number;
  fontFamily: string;
}

// ========== 统一模板 ==========
export interface MasterWatermarkTemplate {
  id: string;
  name: string;
  nameEn: string;
  category: WatermarkCategory;
  description: string;
  previewThumb: string;
  isHasselbladSeries: boolean;
  layers: WatermarkLayerDef[];
  presetStyle: WatermarkStylePreset;
  defaultPosition: WatermarkPosition;
  isPopular: boolean;
  isNew: boolean;
}

// ========== EXIF 数据 ==========
export interface ExifWatermarkData {
  make?: string;
  model?: string;
  aperture?: string;
  shutterSpeed?: string;
  iso?: string;
  focalLength?: string;
  dateTaken?: string;
  gpsLat?: number;
  gpsLng?: number;
  locationName?: string;
  lensModel?: string;
  flashUsed: boolean;
  imageWidth: number;
  imageHeight: number;
}

// ========== 辅助函数 ==========
export function getDeviceInfo(exif: ExifWatermarkData): string {
  if (exif.make && exif.model) {
    return `${exif.make} ${exif.model}`;
  }
  if (exif.model) {
    return exif.model;
  }
  return 'Unknown Device';
}

export function getParamsInfo(exif: ExifWatermarkData): string {
  const parts: string[] = [];
  if (exif.aperture) parts.push(exif.aperture);
  if (exif.shutterSpeed) parts.push(exif.shutterSpeed);
  if (exif.iso) parts.push(exif.iso);
  if (exif.focalLength) parts.push(exif.focalLength);
  return parts.join('  ');
}
