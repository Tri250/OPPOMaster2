/**
 * LUT 类型定义
 * 双端统一类型
 */

// ============================================
// 枚举类型
// ============================================

export type LUTCategory = 
  | 'all' 
  | 'film' 
  | 'cinematic' 
  | 'vlog' 
  | 'color' 
  | 'portrait' 
  | 'night' 
  | 'vintage' 
  | 'hasselblad';

export type LUTFormat = 'cube' | '3dl' | 'mga';
export type LUTSize = '33' | '64';
export type LUTSource = 'omaster' | 'community' | 'hasselblad' | 'partner';
export type LUTSortBy = 'downloads' | 'rating' | 'newest' | 'name';

// ============================================
// 数据模型
// ============================================

export interface LUTParams {
  saturation: number;
  contrast: number;
  brightness: number;
  colorTemperature: number;
  tint: number;
  highlightRolloff: number;
  shadowLift: number;
  skinProtection: boolean;
}

export interface MasterLUT {
  // 基础信息
  id: string;
  name: string;
  nameEn: string;
  description: string;
  longDescription?: string;

  // 分类与标签
  category: LUTCategory;
  subCategory?: string;
  tags: string[];
  suitableFor: string[];

  // 技术规格
  format: LUTFormat;
  size: LUTSize;
  fileSize: number;

  // 视觉资源
  coverImage: string;
  sampleImages?: string[];
  sampleVideo?: string;

  // 下载信息
  downloadUrl: string;
  mirrorUrls?: string[];

  // 作者与来源
  author: string;
  authorAvatar?: string;
  authorUrl?: string;
  source?: LUTSource;

  // 哈苏品牌属性
  isHncsCertified?: boolean;
  filmPresetMapping?: string;
  hasselbladCollection?: string;

  // 运营属性
  isFree: boolean;
  isHot: boolean;
  isNew: boolean;
  isFeatured?: boolean;
  featuredReason?: string;

  // 统计
  downloads: number;
  likes: number;
  rating: number;
  ratingCount?: number;

  // 预设关联
  relatedPresetIds?: string[];
  generatedParams?: LUTParams;

  // 元数据
  version?: number;
  createdAt: string;
  updatedAt?: string;
  minAppVersion?: string;

  // 使用指引
  usageGuide?: string;
  compatibleSoftware?: string[];
}

// ============================================
// 状态类型
// ============================================

export interface Resource<T> {
  type: 'loading' | 'success' | 'error';
  data?: T;
  message?: string;
}

export type DownloadProgress = 
  | { type: 'starting'; lutId: string }
  | { type: 'downloading'; lutId: string; progress: number; bytesDownloaded: number; totalBytes: number }
  | { type: 'completed'; lutId: string; blob?: Blob; filePath?: string }
  | { type: 'error'; lutId: string; message: string };

// ============================================
// 分类配置
// ============================================

export const LUT_CATEGORIES: { key: LUTCategory; label: string; icon: string }[] = [
  { key: 'all', label: '全部', icon: '🎬' },
  { key: 'film', label: '胶片经典', icon: '🎥' },
  { key: 'cinematic', label: '电影感', icon: '🎞️' },
  { key: 'vlog', label: 'Vlog风格', icon: '📹' },
  { key: 'color', label: '色彩风格', icon: '🎨' },
  { key: 'portrait', label: '人像优化', icon: '👤' },
  { key: 'night', label: '夜景', icon: '🌃' },
  { key: 'vintage', label: '复古怀旧', icon: '📻' },
  { key: 'hasselblad', label: '哈苏大师', icon: '👑' },
];

export const LUT_SORT_OPTIONS: { key: LUTSortBy; label: string }[] = [
  { key: 'downloads', label: '最多下载' },
  { key: 'rating', label: '最高评分' },
  { key: 'newest', label: '最新发布' },
  { key: 'name', label: '名称排序' },
];
