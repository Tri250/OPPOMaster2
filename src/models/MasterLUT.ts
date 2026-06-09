/**
 * 哈苏大师色彩配方数据模型
 * 双端统一：Web (TypeScript interface) ↔ Android (Kotlin Serialization)
 */

/**
 * LUT分类枚举
 */
export enum LUTCategory {
  ALL = 'all',
  FILM = 'film',
  CINEMATIC = 'cinematic',
  VLOG = 'vlog',
  COLOR = 'color',
  PORTRAIT = 'portrait',
  NIGHT = 'night',
  VINTAGE = 'vintage',
  HASSELBLAD = 'hasselblad',
}

export const LUT_CATEGORY_META: Record<LUTCategory, { displayName: string; icon: string }> = {
  [LUTCategory.ALL]: { displayName: '全部', icon: '🎬' },
  [LUTCategory.FILM]: { displayName: '胶片经典', icon: '🎥' },
  [LUTCategory.CINEMATIC]: { displayName: '电影感', icon: '🎞️' },
  [LUTCategory.VLOG]: { displayName: 'Vlog风格', icon: '📹' },
  [LUTCategory.COLOR]: { displayName: '色彩风格', icon: '🎨' },
  [LUTCategory.PORTRAIT]: { displayName: '人像优化', icon: '👤' },
  [LUTCategory.NIGHT]: { displayName: '夜景', icon: '🌃' },
  [LUTCategory.VINTAGE]: { displayName: '复古怀旧', icon: '📻' },
  [LUTCategory.HASSELBLAD]: { displayName: '哈苏大师', icon: '👑' },
};

/**
 * LUT文件格式枚举
 */
export enum LUTFormat {
  CUBE = 'cube',
  L3D = '3dl',
  MGA = 'mga',
}

export const LUT_FORMAT_META: Record<LUTFormat, { extension: string; displayName: string }> = {
  [LUTFormat.CUBE]: { extension: 'cube', displayName: 'Cube LUT' },
  [LUTFormat.L3D]: { extension: '3dl', displayName: '3D LUT' },
  [LUTFormat.MGA]: { extension: 'mga', displayName: 'MGA LUT' },
};

/**
 * LUT尺寸枚举
 */
export enum LUTSize {
  SIZE_33 = 33,
  SIZE_64 = 64,
}

export const LUT_SIZE_META: Record<LUTSize, { value: number; displayName: string }> = {
  [LUTSize.SIZE_33]: { value: 33, displayName: '33×33×33 (标准)' },
  [LUTSize.SIZE_64]: { value: 64, displayName: '64×64×64 (高精度)' },
};

/**
 * LUT来源枚举
 */
export enum LUTSource {
  OMASTER = 'OMaster 官方',
  COMMUNITY = '社区上传',
  HASSELBLAD = '哈苏官方',
  PARTNER = '合作摄影师',
}

/**
 * LUT参数近似值
 * 从LUT反推的参数调整建议
 */
export interface LUTParams {
  saturation: number;      // 饱和度偏移 (-1~1)
  contrast: number;        // 对比度偏移
  brightness: number;      // 亮度偏移
  colorTemperature: number; // 色温偏移
  tint: number;            // 色调偏移
  highlightRolloff: number; // 高光衰减
  shadowLift: number;      // 阴影提升
  skinProtection: boolean; // 肤色保护
}

/**
 * 哈苏大师色彩配方数据模型
 * 双端统一：Android (Kotlin Serialization) ↔ Web (TypeScript interface)
 */
export interface MasterLUT {
  // ===== 基础信息 =====
  id: string;                    // 唯一标识 (如 "kodak-portra-400")
  name: string;                  // 中文名称
  nameEn: string;                // 英文名称
  description: string;           // 描述文案
  longDescription?: string;      // 详细描述（含拍摄建议）

  // ===== 分类与标签 =====
  category: LUTCategory;         // 主分类
  subCategory?: string;          // 子分类（如 "人像/户外"）
  tags: string[];                // 标签列表
  suitableFor: string[];         // 适用场景

  // ===== 技术规格 =====
  format: LUTFormat;             // 文件格式
  size: LUTSize;                 // 色彩精度
  fileSize: number;              // 文件大小 (bytes)

  // ===== 视觉资源 =====
  coverImage: string;            // 封面预览图
  sampleImages?: string[];       // 样片列表（Before/After 对比用）
  sampleVideo?: string;          // 视频样片（可选）

  // ===== 下载信息 =====
  downloadUrl: string;           // 下载直链
  mirrorUrls?: string[];         // 备用下载链接

  // ===== 作者与来源 =====
  author: string;                // 作者名
  authorAvatar?: string;         // 作者头像
  authorUrl?: string;            // 作者主页
  source?: LUTSource;            // 来源

  // ===== 哈苏品牌属性 =====
  isHncsCertified?: boolean;     // 是否 HNCS 认证
  filmPresetMapping?: string;    // 关联的胶片风格 (如 "CC"/"NC"/"NH")
  hasselbladCollection?: string; // 所属哈苏系列 (如 "大师赛2024"/"胶片经典")

  // ===== 运营属性 =====
  isFree?: boolean;              // 是否免费
  isHot?: boolean;               // 是否热门
  isNew?: boolean;               // 是否新品
  isFeatured?: boolean;          // 是否精选推荐
  featuredReason?: string;       // 精选理由

  // ===== 统计 =====
  downloads?: number;            // 下载次数
  likes?: number;                // 喜欢次数
  rating?: number;               // 评分 (0-5)
  ratingCount?: number;          // 评分人数

  // ===== 预设关联 =====
  relatedPresetIds?: string[];   // 关联的预设ID
  generatedParams?: LUTParams;   // 从 LUT 反推的参数近似值

  // ===== 元数据 =====
  version?: number;              // 版本号
  createdAt: string;             // 创建时间 (ISO 8601)
  updatedAt?: string;            // 更新时间
  minAppVersion?: string;        // 最低支持版本

  // ===== 使用指引 =====
  usageGuide?: string;           // 使用说明 (Markdown)
  compatibleSoftware?: string[]; // 兼容软件 (如 ["DaVinci Resolve", "Premiere Pro", "Final Cut Pro"])
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/**
 * 格式化下载数
 */
export function formatDownloads(count: number): string {
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}万`;
  }
  return count.toString();
}

/**
 * 格式化评分
 */
export function formatRating(rating: number): string {
  return rating.toFixed(1);
}

/**
 * 获取分类显示信息
 */
export function getCategoryDisplay(category: LUTCategory): { displayName: string; icon: string } {
  return LUT_CATEGORY_META[category] || LUT_CATEGORY_META[LUTCategory.ALL];
}

/**
 * 获取格式显示信息
 */
export function getFormatDisplay(format: LUTFormat): { extension: string; displayName: string } {
  return LUT_FORMAT_META[format] || LUT_FORMAT_META[LUTFormat.CUBE];
}

/**
 * 获取尺寸显示信息
 */
export function getSizeDisplay(size: LUTSize): { value: number; displayName: string } {
  return LUT_SIZE_META[size] || LUT_SIZE_META[LUTSize.SIZE_33];
}