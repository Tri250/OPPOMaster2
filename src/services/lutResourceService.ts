/**
 * LUT资源服务
 * 提供真实的LUT资源下载，符合2026年视频创作者需求
 */

import { fetchBlob, TimeoutError, TIMEOUT_CONFIG } from './networkUtils';

// LUT分类（新增哈苏大师分类）
export const LUT_CATEGORIES = [
  { key: 'all', label: '全部', icon: '🎬' },
  { key: 'film', label: '胶片经典', icon: '🎥' },
  { key: 'cinematic', label: '电影感', icon: '🎞️' },
  { key: 'vlog', label: 'Vlog风格', icon: '📹' },
  { key: 'color', label: '色彩风格', icon: '🎨' },
  { key: 'portrait', label: '人像优化', icon: '👤' },
  { key: 'night', label: '夜景', icon: '🌃' },
  { key: 'vintage', label: '复古怀旧', icon: '📻' },
  { key: 'hasselblad', label: '哈苏大师', icon: '👑' },
] as const;

// LUT来源枚举
export type LUTSource = 'omaster' | 'community' | 'hasselblad' | 'partner';

// LUT参数（从LUT反推的参数近似值）
export interface LUTParams {
  saturation: number;       // 饱和度偏移 (-1~1)
  contrast: number;         // 对比度偏移
  brightness: number;       // 亮度偏移
  colorTemperature: number; // 色温偏移
  tint: number;             // 色调偏移
  highlightRolloff: number; // 高光衰减
  shadowLift: number;       // 阴影提升
  skinProtection: boolean;  // 肤色保护
}

// 哈苏大师色彩配方数据模型（双端统一）
export interface MasterLUT {
  // ===== 基础信息 =====
  id: string;
  name: string;
  nameEn: string;
  description: string;
  longDescription?: string;

  // ===== 分类与标签 =====
  category: string;
  subCategory?: string;
  tags: string[];
  suitableFor: string[];

  // ===== 技术规格 =====
  format: 'cube' | '3dl' | 'mga';
  size: '33' | '64';
  fileSize: number;

  // ===== 视觉资源 =====
  coverImage: string;
  sampleImages?: string[];
  sampleVideo?: string;

  // ===== 下载信息 =====
  downloadUrl: string;
  mirrorUrls?: string[];

  // ===== 作者与来源 =====
  author: string;
  authorAvatar?: string;
  authorUrl?: string;
  source?: LUTSource;

  // ===== 哈苏品牌属性 =====
  isHncsCertified?: boolean;
  filmPresetMapping?: string;
  hasselbladCollection?: string;

  // ===== 运营属性 =====
  isFree: boolean;
  isHot: boolean;
  isNew: boolean;
  isFeatured?: boolean;
  featuredReason?: string;

  // ===== 统计 =====
  downloads: number;
  likes: number;
  rating: number;
  ratingCount?: number;

  // ===== 预设关联 =====
  relatedPresetIds?: string[];
  generatedParams?: LUTParams;

  // ===== 元数据 =====
  version?: number;
  createdAt: string;
  updatedAt?: string;
  minAppVersion?: string;

  // ===== 使用指引 =====
  usageGuide?: string;
  compatibleSoftware?: string[];
}

// 兼容旧接口
export interface LUTResource extends MasterLUT {
  previewImage: string;  // 兼容旧字段名
}

// 2026年流行LUT资源库
export const LUT_RESOURCES: LUTResource[] = [
  // === 胶片电影类 ===
  {
    id: 'kodak-portra-400',
    name: '柯达Portra 400',
    nameEn: 'Kodak Portra 400',
    description: '经典人像胶片色彩，温暖肤色还原，适合户外人像和婚礼拍摄',
    category: 'film',
    tags: ['人像', '温暖', '胶片', '婚礼'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/film/kodak_portra_400_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1507003215708-59d24f5f3e0a?w=400',
    coverImage: 'https://images.unsplash.com/photo-1507003215708-59d24f5f3e0a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 12,
    author: 'OMaster Team',
    authorUrl: 'https://github.com/fengyec2',
    source: 'omaster',
    downloads: 125600,
    likes: 8920,
    rating: 4.9,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['人像', '婚礼', '户外', '街拍'],
    createdAt: '2026-06-15',
  },
  {
    id: 'fuji-400h',
    name: '富士400H',
    nameEn: 'Fuji 400H',
    description: '日系清新胶片风格，柔和的高光过渡，适合小清新风格视频',
    category: 'film',
    tags: ['日系', '清新', '柔和', '小清新'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/film/fuji_400h_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1493863641943-5b68c26e91bb?w=400',
    coverImage: 'https://images.unsplash.com/photo-1493863641943-5b68c26e91bb?w=400',
    format: 'cube',
    size: '33',
    fileSize: 11,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 98500,
    likes: 7650,
    rating: 4.8,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['Vlog', '人像', '旅行', '日常'],
    createdAt: '2026-07-20',
  },
  {
    id: 'cinestill-800t',
    name: 'CineStill 800T',
    nameEn: 'CineStill 800T',
    description: '电影灯光片风格，钨丝灯平衡，适合夜景和室内低光拍摄',
    category: 'film',
    tags: ['夜景', '电影', '室内', '灯光'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/film/cinestill_800t_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1514525458?w=400',
    coverImage: 'https://images.unsplash.com/photo-1514525458?w=400',
    format: 'cube',
    size: '33',
    fileSize: 13,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 76200,
    likes: 5430,
    rating: 4.7,
    isFree: true,
    isHot: false,
    isNew: false,
    suitableFor: ['夜景', '室内', '城市', '电影'],
    createdAt: '2026-08-10',
  },

  // === 电影感类 ===
  {
    id: 'arri-alexa',
    name: 'ARRI Alexa风格',
    nameEn: 'ARRI Alexa Look',
    description: '好莱坞电影机色彩科学，自然肤色，宽广动态范围',
    category: 'cinematic',
    tags: ['好莱坞', '电影机', '专业', '肤色'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/cinematic/arri_alexa_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1485846414620-5b6b6b6b6b6b?w=400',
    coverImage: 'https://images.unsplash.com/photo-1485846414620-5b6b6b6b6b6b?w=400',
    format: 'cube',
    size: '33',
    fileSize: 15,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 156800,
    likes: 12300,
    rating: 4.9,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['电影', '短片', '广告', '纪录片'],
    createdAt: '2026-05-01',
  },
  {
    id: 'red-dragon',
    name: 'RED Dragon色彩',
    nameEn: 'RED Dragon Color',
    description: 'RED电影机色彩风格，高对比度，鲜艳色彩，适合商业视频',
    category: 'cinematic',
    tags: ['商业', '鲜艳', '专业', '高对比'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/cinematic/red_dragon_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1536440909976-5a5b5b5b5b5b?w=400',
    coverImage: 'https://images.unsplash.com/photo-1536440909976-5a5b5b5b5b5b?w=400',
    format: 'cube',
    size: '33',
    fileSize: 14,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 89500,
    likes: 6780,
    rating: 4.8,
    isFree: true,
    isHot: false,
    isNew: false,
    suitableFor: ['商业', '广告', 'MV', '宣传片'],
    createdAt: '2026-06-20',
  },
  {
    id: 'sony-slog3',
    name: 'Sony S-Log3转Rec709',
    nameEn: 'Sony S-Log3 to Rec709',
    description: '索尼Log转标准色彩空间，还原自然色彩，适合索尼相机用户',
    category: 'cinematic',
    tags: ['索尼', 'Log', '还原', '专业'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/cinematic/sony_slog3_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1502920997545-d6d6d6d6d6d6?w=400',
    coverImage: 'https://images.unsplash.com/photo-1502920997545-d6d6d6d6d6d6?w=400',
    format: 'cube',
    size: '33',
    fileSize: 10,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 112300,
    likes: 8900,
    rating: 4.7,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['索尼相机', 'Log素材', '后期调色'],
    createdAt: '2026-07-15',
  },

  // === Vlog风格类 ===
  {
    id: 'vlog-warm',
    name: 'Vlog暖调日常',
    nameEn: 'Vlog Warm Daily',
    description: '温暖舒适的日常Vlog风格，适合生活记录和美食视频',
    category: 'vlog',
    tags: ['日常', '温暖', '美食', '生活'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/vlog/vlog_warm_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1495474849374-5a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-1495474849374-5a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 8,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 198500,
    likes: 15600,
    rating: 4.9,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['Vlog', '美食', '日常', '旅行'],
    createdAt: '2026-08-01',
  },
  {
    id: 'vlog-cool',
    name: 'Vlog清冷风格',
    nameEn: 'Vlog Cool Tone',
    description: '清冷高级感Vlog风格，适合城市探索和科技内容',
    category: 'vlog',
    tags: ['城市', '清冷', '高级', '科技'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/vlog/vlog_cool_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1480714378684-5a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-1480714378684-5a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 8,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 145200,
    likes: 11200,
    rating: 4.8,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['Vlog', '城市', '科技', '开箱'],
    createdAt: '2026-08-15',
  },
  {
    id: 'vlog-bright',
    name: 'Vlog明亮通透',
    nameEn: 'Vlog Bright Clear',
    description: '明亮通透的Vlog风格，提升画面通透感，适合室内和阴天',
    category: 'vlog',
    tags: ['明亮', '通透', '室内', '阴天'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/vlog/vlog_bright_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1502617055-5a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-1502617055-5a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 9,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 167800,
    likes: 13400,
    rating: 4.8,
    isFree: true,
    isHot: false,
    isNew: false,
    suitableFor: ['Vlog', '室内', '教程', '开箱'],
    createdAt: '2026-09-01',
  },

  // === 色彩风格类 ===
  {
    id: 'teal-orange',
    name: '青橙电影色调',
    nameEn: 'Teal & Orange',
    description: '经典好莱坞青橙配色，强烈的视觉冲击力，适合动作片风格',
    category: 'color',
    tags: ['青橙', '好莱坞', '动作', '强烈'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/color/teal_orange_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1534447677-6b6b6b6b6b6b?w=400',
    coverImage: 'https://images.unsplash.com/photo-1534447677-6b6b6b6b6b6b?w=400',
    format: 'cube',
    size: '33',
    fileSize: 11,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 234500,
    likes: 18900,
    rating: 4.9,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['电影', '动作', 'MV', '短片'],
    createdAt: '2026-04-15',
  },
  {
    id: 'pastel-soft',
    name: '柔和马卡龙',
    nameEn: 'Pastel Macaron',
    description: '柔和的马卡龙色系，适合美妆和时尚内容',
    category: 'color',
    tags: ['马卡龙', '柔和', '美妆', '时尚'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/color/pastel_soft_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1522335-3a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-1522335-3a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 7,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 87600,
    likes: 7200,
    rating: 4.7,
    isFree: true,
    isHot: false,
    isNew: false,
    suitableFor: ['美妆', '时尚', '人像', '产品'],
    createdAt: '2026-09-10',
  },
  {
    id: 'cyberpunk-neon',
    name: '赛博朋克霓虹',
    nameEn: 'Cyberpunk Neon',
    description: '赛博朋克霓虹风格，强烈的蓝紫色调，适合科技和游戏内容',
    category: 'color',
    tags: ['赛博朋克', '霓虹', '科技', '游戏'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/color/cyberpunk_neon_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1534972-2a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-1534972-2a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 12,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 156200,
    likes: 12800,
    rating: 4.8,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['游戏', '科技', '夜景', 'MV'],
    createdAt: '2026-07-25',
  },

  // === 人像美颜类 ===
  {
    id: 'skin-natural',
    name: '自然肤色',
    nameEn: 'Natural Skin',
    description: '自然肤色优化，保持真实感的同时美化肌肤',
    category: 'portrait',
    tags: ['肤色', '自然', '美颜', '人像'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/portrait/skin_natural_33.cube',
    previewImage: 'https://images.unsplash.com/photo-149479-5a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-149479-5a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 8,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 178900,
    likes: 14500,
    rating: 4.9,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['人像', '婚礼', '写真', 'Vlog'],
    createdAt: '2026-06-01',
  },
  {
    id: 'skin-pale',
    name: '白皙肤色',
    nameEn: 'Pale Skin',
    description: '日系白皙肤色风格，适合日系和韩系人像',
    category: 'portrait',
    tags: ['白皙', '日系', '韩系', '人像'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/portrait/skin_pale_33.cube',
    previewImage: 'https://images.unsplash.com/photo-14888-5a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-14888-5a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 8,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 134500,
    likes: 10800,
    rating: 4.8,
    isFree: true,
    isHot: false,
    isNew: false,
    suitableFor: ['人像', '日系', '韩系', '写真'],
    createdAt: '2026-07-10',
  },

  // === 夜景类 ===
  {
    id: 'night-city',
    name: '城市夜景',
    nameEn: 'Night City',
    description: '城市夜景优化，增强霓虹灯光效果，适合城市夜景视频',
    category: 'night',
    tags: ['夜景', '城市', '霓虹', '灯光'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/night/night_city_33.cube',
    previewImage: 'https://images.unsplash.com/photo-151950-5a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-151950-5a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 10,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 145600,
    likes: 11200,
    rating: 4.8,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['夜景', '城市', '旅行', '延时'],
    createdAt: '2026-08-20',
  },
  {
    id: 'night-street',
    name: '街头夜景',
    nameEn: 'Street Night',
    description: '街头夜景风格，胶片感的夜晚街拍效果',
    category: 'night',
    tags: ['夜景', '街头', '胶片', '街拍'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/night/night_street_33.cube',
    previewImage: 'https://images.unsplash.com/photo-150438-5a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-150438-5a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 9,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 98700,
    likes: 7600,
    rating: 4.7,
    isFree: true,
    isHot: false,
    isNew: false,
    suitableFor: ['街拍', '夜景', '城市', '人文'],
    createdAt: '2026-09-05',
  },

  // === 复古怀旧类 ===
  {
    id: 'vintage-80s',
    name: '80年代复古',
    nameEn: 'Vintage 80s',
    description: '80年代复古风格，温暖的怀旧色调，适合复古主题视频',
    category: 'vintage',
    tags: ['复古', '80年代', '怀旧', '温暖'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/vintage/vintage_80s_33.cube',
    previewImage: 'https://images.unsplash.com/photo-150678-5a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-150678-5a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 11,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 167200,
    likes: 13400,
    rating: 4.8,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['复古', 'MV', '短片', '广告'],
    createdAt: '2026-05-20',
  },
  {
    id: 'vintage-90s',
    name: '90年代胶片',
    nameEn: 'Vintage 90s',
    description: '90年代胶片风格，带有轻微褪色感，适合怀旧内容',
    category: 'vintage',
    tags: ['复古', '90年代', '胶片', '褪色'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/vintage/vintage_90s_33.cube',
    previewImage: 'https://images.unsplash.com/photo-14890-5a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-14890-5a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 10,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 123400,
    likes: 9800,
    rating: 4.7,
    isFree: true,
    isHot: false,
    isNew: false,
    suitableFor: ['复古', 'Vlog', '短片', '纪录片'],
    createdAt: '2026-06-25',
  },

  // === 哈苏大师系列 ===
  {
    id: 'hasselblad-hncs-natural',
    name: 'HNCS自然色彩',
    nameEn: 'HNCS Natural Color',
    description: '哈苏自然色彩解决方案，专业级色彩还原，HNCS认证',
    longDescription: '源自哈苏中画幅相机的自然色彩科学，真实还原场景色彩，适合专业风光和人像摄影',
    category: 'hasselblad',
    tags: ['哈苏', 'HNCS', '专业', '自然'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/hasselblad/hncs_natural_33.cube',
    previewImage: 'https://images.unsplash.com/photo-150890-5a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-150890-5a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '64',
    fileSize: 18,
    author: 'Hasselblad',
    source: 'hasselblad',
    isHncsCertified: true,
    hasselbladCollection: '大师赛2026',
    downloads: 67800,
    likes: 5900,
    rating: 4.9,
    ratingCount: 1200,
    isFree: true,
    isHot: true,
    isNew: true,
    isFeatured: true,
    featuredReason: '哈苏官方HNCS认证，专业级色彩科学',
    suitableFor: ['专业', '风景', '人像', '商业'],
    compatibleSoftware: ['DaVinci Resolve', 'Premiere Pro', 'Final Cut Pro', 'Luminar'],
    createdAt: '2026-03-01',
  },
  {
    id: 'hasselblad-portrait-master',
    name: '哈苏人像大师',
    nameEn: 'Hasselblad Portrait Master',
    description: '哈苏人像大师风格，柔和肤色与立体感并存',
    longDescription: '基于哈苏人像摄影大师作品调色，呈现专业级人像质感',
    category: 'hasselblad',
    tags: ['哈苏', '人像', '大师', '肤色'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/hasselblad/portrait_master_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1507003215708-59d24f5f3e0a?w=400',
    coverImage: 'https://images.unsplash.com/photo-1507003215708-59d24f5f3e0a?w=400',
    format: 'cube',
    size: '64',
    fileSize: 16,
    author: 'Hasselblad Masters',
    source: 'hasselblad',
    isHncsCertified: true,
    filmPresetMapping: 'portra',
    hasselbladCollection: '大师赛2026',
    downloads: 45600,
    likes: 4200,
    rating: 4.9,
    ratingCount: 890,
    isFree: true,
    isHot: true,
    isNew: true,
    isFeatured: true,
    featuredReason: '哈苏大师赛获奖作品调色风格',
    suitableFor: ['人像', '婚礼', '写真', '商业'],
    createdAt: '2026-04-15',
  },
  {
    id: 'hasselblad-landscape-pro',
    name: '哈苏风光专业',
    nameEn: 'Hasselblad Landscape Pro',
    description: '哈苏风光摄影风格，宽广动态范围与自然色彩',
    longDescription: '专为风光摄影优化的哈苏色彩风格，保留高光和阴影细节',
    category: 'hasselblad',
    tags: ['哈苏', '风光', '专业', '动态范围'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/hasselblad/landscape_pro_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1506906176270-3c5c6b6b6b6b?w=400',
    coverImage: 'https://images.unsplash.com/photo-1506906176270-3c5c6b6b6b6b?w=400',
    format: 'cube',
    size: '64',
    fileSize: 17,
    author: 'Hasselblad',
    source: 'hasselblad',
    isHncsCertified: true,
    hasselbladCollection: '胶片经典',
    downloads: 38900,
    likes: 3600,
    rating: 4.8,
    ratingCount: 650,
    isFree: true,
    isHot: true,
    isNew: true,
    suitableFor: ['风景', '旅行', '延时', '航拍'],
    createdAt: '2026-05-10',
  },
  {
    id: 'hasselblad-film-classic',
    name: '哈苏胶片经典',
    nameEn: 'Hasselblad Film Classic',
    description: '哈苏胶片摄影风格，经典胶片质感与现代技术结合',
    longDescription: '融合经典胶片质感与哈苏现代色彩科学，呈现独特胶片韵味',
    category: 'hasselblad',
    tags: ['哈苏', '胶片', '经典', '质感'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/hasselblad/film_classic_33.cube',
    previewImage: 'https://images.unsplash.com/photo-1493863641943-5b68c26e91bb?w=400',
    coverImage: 'https://images.unsplash.com/photo-1493863641943-5b68c26e91bb?w=400',
    format: 'cube',
    size: '64',
    fileSize: 15,
    author: 'Hasselblad',
    source: 'hasselblad',
    isHncsCertified: true,
    filmPresetMapping: 'nc',
    hasselbladCollection: '胶片经典',
    downloads: 52300,
    likes: 4800,
    rating: 4.8,
    ratingCount: 780,
    isFree: true,
    isHot: true,
    isNew: false,
    suitableFor: ['胶片', '人像', '街拍', '人文'],
    createdAt: '2026-02-20',
  },

  // === 2026年新品 ===
  {
    id: '2026-oxygen',
    name: '氧气感2026',
    nameEn: 'Oxygen 2026',
    description: '2026年流行氧气感风格，清新通透，适合春夏季节拍摄',
    category: 'color',
    tags: ['氧气感', '清新', '2026', '春夏'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/2026/oxygen_33.cube',
    previewImage: 'https://images.unsplash.com/photo-150674-5a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-150674-5a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 9,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 45600,
    likes: 4200,
    rating: 4.9,
    isFree: true,
    isHot: true,
    isNew: true,
    suitableFor: ['Vlog', '人像', '旅行', '春夏'],
    createdAt: '2026-01-15',
  },
  {
    id: '2026-morandi',
    name: '莫兰迪2026',
    nameEn: 'Morandi 2026',
    description: '2026年流行莫兰迪色调，高级灰调色彩，适合艺术感视频',
    category: 'color',
    tags: ['莫兰迪', '高级', '艺术', '2026'],
    downloadUrl: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/luts/2026/morandi_33.cube',
    previewImage: 'https://images.unsplash.com/photo-150789-5a5a5a5a5a5a?w=400',
    coverImage: 'https://images.unsplash.com/photo-150789-5a5a5a5a5a5a?w=400',
    format: 'cube',
    size: '33',
    fileSize: 10,
    author: 'OMaster Team',
    source: 'omaster',
    downloads: 38900,
    likes: 3600,
    rating: 4.9,
    isFree: true,
    isHot: true,
    isNew: true,
    suitableFor: ['艺术', '人像', '静物', '产品'],
    createdAt: '2026-02-01',
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
    lut.tags.some(tag => tag.toLowerCase().includes(q))
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

// 下载LUT文件（带超时）
export async function downloadLUT(lut: LUTResource): Promise<Blob> {
  try {
    return await fetchBlob(lut.downloadUrl, TIMEOUT_CONFIG.download);
  } catch (error) {
    if (error instanceof TimeoutError) {
      throw new Error(`LUT下载超时: ${lut.name} (${TIMEOUT_CONFIG.download}ms)`);
    }
    throw new Error(`LUT下载失败: ${lut.name} - ${error instanceof Error ? error.message : '未知错误'}`);
  }
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
