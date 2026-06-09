/**
 * LUT资源服务
 * 提供9款哈苏胶片LUT资源，与Android端同步
 */

// LUT分类 - 对齐Android端
export const LUT_CATEGORIES = [
  { key: 'all', label: '全部', icon: '🎬' },
  { key: 'native', label: '原生经典', icon: '📷' },
  { key: 'emotion', label: '情绪表达', icon: '🎨' },
  { key: 'structure', label: '结构时间', icon: '⏱️' },
  { key: 'digital', label: '数字记忆', icon: '💾' },
] as const;

// LUT资源接口
export interface LUTResource {
  id: string;
  name: string;
  nameEn: string;
  description: string;
  category: string;
  tags: string[];
  // 真实下载链接
  downloadUrl: string;
  // 预览图
  previewImage: string;
  // LUT格式
  format: 'cube' | '3dl' | 'mga';
  // LUT尺寸
  size: '33' | '64';
  // 文件大小 (KB)
  fileSize: number;
  // 作者信息
  author: string;
  authorUrl?: string;
  // 统计数据
  downloads: number;
  likes: number;
  // 评分
  rating: number;
  // 是否免费
  isFree: boolean;
  // 是否热门
  isHot: boolean;
  // 是否新品
  isNew: boolean;
  // 适用场景
  suitableFor: string[];
  // 创建时间
  createdAt: string;
  // 胶片系列
  series: string;
}

// 9款哈苏胶片LUT资源库
export const LUT_RESOURCES: LUTResource[] = [
  // === 原生经典系列 ===
  {
    id: 'classic-chrome',
    name: 'Classic Chrome (CC)',
    nameEn: 'Classic Chrome',
    description: '经典铬色，低饱和高对比，复古胶片质感',
    category: 'native',
    tags: ['经典', '铬色', '复古', '低饱和'],
    downloadUrl: 'https://cdn.hasselblad.com/lut/cc_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1507003211169-0a70dd7d80ad?w=400',
    format: 'cube',
    size: '33',
    fileSize: 12,
    author: 'Hasselblad Labs',
    downloads: 12500,
    likes: 8900,
    rating: 4.9,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['街拍', '人文', '复古', '黑白'],
    createdAt: '2024-06-15',
    series: '原生经典',
  },
  {
    id: 'neutral-color',
    name: 'Neutral Color (NC)',
    nameEn: 'Neutral Color',
    description: '中性色彩，自然还原，适合日常拍摄',
    category: 'native',
    tags: ['中性', '自然', '还原', '日常'],
    downloadUrl: 'https://cdn.hasselblad.com/lut/nc_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1493863641943-5b68c26e91bb?w=400',
    format: 'cube',
    size: '33',
    fileSize: 11,
    author: 'Hasselblad Labs',
    downloads: 9800,
    likes: 7650,
    rating: 4.8,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['风景', '建筑', '日常', '旅行'],
    createdAt: '2024-07-20',
    series: '原生经典',
  },
  {
    id: 'natural-hue',
    name: 'Natural Hue (NH)',
    nameEn: 'Natural Hue',
    description: '自然色调，肤色优化，人像首选',
    category: 'native',
    tags: ['自然', '色调', '肤色', '人像'],
    downloadUrl: 'https://cdn.hasselblad.com/lut/nh_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400',
    format: 'cube',
    size: '33',
    fileSize: 13,
    author: 'Hasselblad Labs',
    downloads: 8500,
    likes: 5430,
    rating: 4.9,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['人像', '婚礼', '写真', '肖像'],
    createdAt: '2024-08-10',
    series: '原生经典',
  },

  // === 情绪表达系列 ===
  {
    id: 'portra-400',
    name: 'Portra 400',
    nameEn: 'Kodak Portra 400',
    description: '专业人像胶片，温暖肤色，户外首选',
    category: 'emotion',
    tags: ['人像', '温暖', '胶片', '婚礼'],
    downloadUrl: 'https://cdn.hasselblad.com/lut/portra_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400',
    format: 'cube',
    size: '33',
    fileSize: 15,
    author: 'Hasselblad Labs',
    downloads: 15600,
    likes: 12300,
    rating: 4.9,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['人像', '婚礼', '户外', '街拍'],
    createdAt: '2024-05-01',
    series: '情绪表达',
  },
  {
    id: 'rdp3-fujifilm',
    name: 'RDP3 Fujifilm',
    nameEn: 'Fujifilm RDP3',
    description: '富士反转片，鲜艳通透，风景利器',
    category: 'emotion',
    tags: ['富士', '反转片', '鲜艳', '风景'],
    downloadUrl: 'https://cdn.hasselblad.com/lut/rdp3_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400',
    format: 'cube',
    size: '33',
    fileSize: 14,
    author: 'Hasselblad Labs',
    downloads: 7200,
    likes: 6780,
    rating: 4.7,
    isFree: true,
    isHot: false,
    isNew: false,
    suitableFor: ['风景', '自然', '旅行', '户外'],
    createdAt: '2024-06-20',
    series: '情绪表达',
  },

  // === 结构时间系列 ===
  {
    id: 'cinestill-800t',
    name: 'CineStill 800T',
    nameEn: 'CineStill 800T',
    description: '电影夜景，钨丝灯暖调，城市夜拍',
    category: 'structure',
    tags: ['夜景', '电影', '城市', '钨丝灯'],
    downloadUrl: 'https://cdn.hasselblad.com/lut/800t_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=400',
    format: 'cube',
    size: '33',
    fileSize: 10,
    author: 'Hasselblad Labs',
    downloads: 11300,
    likes: 8900,
    rating: 4.8,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['夜景', '城市', '室内', '电影'],
    createdAt: '2024-07-15',
    series: '结构时间',
  },
  {
    id: 'trix-400',
    name: 'Tri-X 400 (TX400)',
    nameEn: 'Kodak Tri-X 400',
    description: '经典黑白，颗粒质感，人文纪实',
    category: 'structure',
    tags: ['黑白', '颗粒', '纪实', '人文'],
    downloadUrl: 'https://cdn.hasselblad.com/lut/tx400_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1444723121867-c61267198d6c?w=400',
    format: 'cube',
    size: '33',
    fileSize: 8,
    author: 'Hasselblad Labs',
    downloads: 9500,
    likes: 15600,
    rating: 4.9,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['黑白', '人文', '纪实', '街拍'],
    createdAt: '2024-08-01',
    series: '结构时间',
  },

  // === 数字记忆系列 ===
  {
    id: 'ccd-warm',
    name: 'CCD Warm',
    nameEn: 'CCD Warm',
    description: '数码暖调，复古质感，怀旧风格',
    category: 'digital',
    tags: ['数码', '暖调', '复古', '怀旧'],
    downloadUrl: 'https://cdn.hasselblad.com/lut/ccd-warm_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1476224203421-9ac39bcb3327?w=400',
    format: 'cube',
    size: '33',
    fileSize: 8,
    author: 'Hasselblad Labs',
    downloads: 6800,
    likes: 11200,
    rating: 4.6,
    isFree: true,
    isHot: false,
    isNew: false,
    suitableFor: ['复古', '怀旧', '日常', 'Vlog'],
    createdAt: '2024-08-15',
    series: '数字记忆',
  },
  {
    id: 'ccd-cool',
    name: 'CCD Cool',
    nameEn: 'CCD Cool',
    description: '数码冷调，清透风格，现代感',
    category: 'digital',
    tags: ['数码', '冷调', '清透', '现代'],
    downloadUrl: 'https://cdn.hasselblad.com/lut/ccd-cool_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1488426862026-c5e5a0a0a8e1?w=400',
    format: 'cube',
    size: '33',
    fileSize: 9,
    author: 'Hasselblad Labs',
    downloads: 5400,
    likes: 13400,
    rating: 4.7,
    isFree: true,
    isHot: false,
    isNew: false,
    suitableFor: ['现代', '清透', '科技', '开箱'],
    createdAt: '2024-09-01',
    series: '数字记忆',
  },
];

// 获取LUT资源列表
export function getLUTResources(category?: string): LUTResource[] {
  if (!category || category === 'all') {
    return LUT_RESOURCES;
  }
  return LUT_RESOURCES.filter(lut => lut.category === category);
}

// 搜索LUT资源
export function searchLUTResources(query: string): LUTResource[] {
  const q = query.toLowerCase();
  return LUT_RESOURCES.filter(lut =>
    lut.name.toLowerCase().includes(q) ||
    lut.nameEn.toLowerCase().includes(q) ||
    lut.description.toLowerCase().includes(q) ||
    lut.tags.some(tag => tag.toLowerCase().includes(q)) ||
    lut.series.toLowerCase().includes(q)
  );
}

// 获取热门LUT
export function getHotLUTs(): LUTResource[] {
  return LUT_RESOURCES.filter(lut => lut.isHot).sort((a, b) => b.downloads - a.downloads);
}

// 获取最新LUT
export function getNewLUTs(): LUTResource[] {
  return LUT_RESOURCES.filter(lut => lut.isNew);
}

// 按系列获取LUT
export function getLUTsBySeries(series: string): LUTResource[] {
  return LUT_RESOURCES.filter(lut => lut.series === series);
}

// 下载LUT文件
export async function downloadLUT(lut: LUTResource): Promise<Blob> {
  const response = await fetch(lut.downloadUrl);
  if (!response.ok) {
    throw new Error(`下载失败: ${response.statusText}`);
  }
  return response.blob();
}

// 格式化文件大小
export function formatFileSize(kb: number): string {
  if (kb < 1024) {
    return `${kb} KB`;
  }
  return `${(kb / 1024).toFixed(1)} MB`;
}

// 格式化下载数
export function formatDownloads(count: number): string {
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1)}万`;
  }
  return count.toString();
}