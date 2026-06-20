import { create } from 'zustand';

export type PageType = 'home' | 'featured' | 'features' | 'about';
export type SubPageType =
  | 'ai-scene'
  | 'ai-fine-tune'
  | 'smart-optimize'
  | 'watermark'
  | 'param-adjust'
  | 'preset-manager'
  | 'lut-share'
  | 'hasselblad'
  | 'cloud-sync'
  | 'theme-settings'
  | 'update-channel'
  | 'notification'
  | 'privacy'
  | 'terms'
  | 'preset-sources'
  | null;

// ============================================
// 哈苏品牌色彩系统
// ============================================
export const HasselbladColors = {
  // 主色
  primary: '#FF6B35',           // 哈苏橙
  primaryLight: '#FF8C42',      // 浅哈苏橙
  primaryGradient: ['#FF6B35', '#FF8C42'] as [string, string],
  
  // 背景
  background: '#0A0A0A',        // PureBlack
  card: '#1A1A1A',              // 卡片背景
  surface: 'rgba(255,255,255,0.05)', // 表面背景
  
  // 边框
  border: 'rgba(255,255,255,0.05)',
  borderLight: 'rgba(255,255,255,0.1)',
  
  // 文字
  textPrimary: '#FFFFFF',
  textSecondary: 'rgba(255,255,255,0.6)',
  textTertiary: 'rgba(255,255,255,0.4)',
  
  // 角标
  badgeHncs: ['#FF6B35', '#FF8C42'] as [string, string],  // HNCS 橙色渐变
  badgeNew: '#4CAF50',          // NEW 绿色
  badgePro: '#FFD700',          // PRO 金色
  badgeHot: '#FF5722',          // HOT 红色
} as const;

// ============================================
// 统一 Preset 数据模型（对齐 Android MasterPreset）
// ============================================

/**
 * 预设参数项
 * 对应 Android 的 PresetParamItem
 */
export interface PresetParamItem {
  label: string;       // 参数标签，如 "滤镜"
  value: string;       // 参数值，如 "复古 100%"
  span: 1 | 2;        // 1=半宽, 2=全宽
}

/**
 * 预设参数区块
 * 对应 Android 的 PresetSection
 */
export interface PresetSection {
  title: string;       // 区块标题，如 "🎨 调色参数"
  items: PresetParamItem[];
}

/**
 * 预设评论
 * 对应 Android 的 PresetComment
 */
export interface PresetComment {
  id: string;
  user: string;
  avatar?: string;
  content: string;
  rating: number;
  timestamp: number;
  likes: number;
}

/**
 * 预设描述
 * 对应 Android 的 PresetDescription
 */
export interface PresetDescription {
  title: string;
  content: string;
}

/**
 * 统一 Preset 接口（对齐 Android MasterPreset）
 */
export interface Preset {
  // ========== 基础信息 ==========
  id: string;
  name: string;
  coverPath: string;
  author: string;
  brand: string;
  tags: string[];
  isNew: boolean;
  isHncs: boolean;
  
  // ========== 画廊 ==========
  galleryImages?: string[];
  
  // ========== 模式 ==========
  mode?: 'auto' | 'pro';
  
  // ========== 描述 ==========
  description?: PresetDescription;
  
  // ========== 动态参数 sections（对齐 Android）==========
  sections: PresetSection[];
  
  // ========== 社区数据 ==========
  downloads?: number;
  rating?: number;
  ratingCount?: number;
  comments?: PresetComment[];
  
  // ========== 兼容旧字段（从 sections 派生）==========
  saturation?: number;
  contrast?: number;
  warmth?: number;
  sharpness?: number;
  clarity?: number;
  brightness?: number;
}

/**
 * Android 原始预设 JSON 结构
 */
export interface AndroidPresetJson {
  name: string;
  coverPath: string;
  galleryImages?: string[];
  author: string;
  tags?: string[];
  isNew?: boolean;
  description?: PresetDescription;
  sections?: PresetSection[];
  // 兼容字段
  saturation?: number;
  contrast?: number;
  warmth?: number;
  sharpness?: number;
  clarity?: number;
  brightness?: number;
}

export interface PresetSource {
  id: string;
  name: string;
  url: string;
  enabled: boolean;
  lastUpdated?: Date;
}

export interface Feature {
  id: string;
  title: string;
  subtitle: string;
  icon: string;
  color: string;
  gradientColors: [string, string];
  enabled: boolean;
  showToggle: boolean;
}

// ============================================
// 预设数据转换工具函数
// ============================================

/**
 * 从 Android presets.json 结构转换为 React Preset 结构
 */
export const convertAndroidPresetToReact = (
  androidPreset: AndroidPresetJson,
  index: number = 0
): Preset => {
  const id = generatePresetId(androidPreset.name, index);
  
  // 从 sections 提取基础参数作为兼容字段
  const extractParamValue = (paramName: string): number | undefined => {
    if (!androidPreset.sections) return undefined;
    
    for (const section of androidPreset.sections) {
      for (const item of section.items) {
        if (item.label.toLowerCase().includes(paramName.toLowerCase())) {
          const match = item.value.match(/([+-]?\d+)/);
          return match ? parseInt(match[1], 10) : undefined;
        }
      }
    }
    return undefined;
  };

  return {
    id,
    name: androidPreset.name,
    coverPath: androidPreset.coverPath,
    galleryImages: androidPreset.galleryImages || [androidPreset.coverPath],
    author: androidPreset.author,
    brand: extractBrandFromAuthor(androidPreset.author),
    tags: androidPreset.tags || [],
    isNew: androidPreset.isNew || false,
    isHncs: androidPreset.tags?.includes('hncs') || androidPreset.tags?.includes('HNCS') || false,
    mode: androidPreset.tags?.includes('Pro') || androidPreset.tags?.includes('pro') ? 'pro' : 'auto',
    description: androidPreset.description || {
      title: '拍摄建议',
      content: '【环境建议】日间户外或充足自然光【场景推荐】街拍、人像、风景、建筑【拍摄要点】适合追求经典胶片质感，建议使用黄金时刻拍摄'
    },
    sections: androidPreset.sections || convertLegacyParamsToSections(androidPreset),
    // 社区数据（默认值）
    downloads: Math.floor(Math.random() * 20000) + 1000,
    rating: 4.5 + Math.random() * 0.5,
    ratingCount: Math.floor(Math.random() * 1000) + 50,
    comments: [],
    // 兼容旧字段
    saturation: androidPreset.saturation ?? extractParamValue('saturation') ?? extractParamValue('饱和度'),
    contrast: androidPreset.contrast ?? extractParamValue('contrast') ?? extractParamValue('对比度'),
    warmth: androidPreset.warmth ?? extractParamValue('warmth') ?? extractParamValue('色温'),
    sharpness: androidPreset.sharpness ?? extractParamValue('sharpness') ?? extractParamValue('锐度'),
    clarity: androidPreset.clarity ?? extractParamValue('clarity') ?? extractParamValue('清晰度'),
    brightness: androidPreset.brightness ?? extractParamValue('brightness') ?? extractParamValue('亮度'),
  };
};

/**
 * 生成预设 ID
 */
const generatePresetId = (name: string, index: number): string => {
  const timestamp = Date.now();
  const nameHash = name.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
  return `preset_${timestamp}_${nameHash}_${index}`;
};

/**
 * 从作者信息提取品牌
 */
const extractBrandFromAuthor = (author: string): string => {
  const brandMap: Record<string, string> = {
    'OPPO': 'OPPO',
    'oppo': 'OPPO',
    'realme': 'realme',
    '真我': 'realme',
    'vivo': 'vivo',
    '荣耀': '荣耀',
    'honor': '荣耀',
    '小米': '小米',
    'xiaomi': '小米',
    '哈苏': '哈苏',
    'hasselblad': '哈苏',
  };

  for (const [key, brand] of Object.entries(brandMap)) {
    if (author.includes(key)) return brand;
  }
  
  return 'OMaster';
};

/**
 * 将旧版参数转换为 sections 格式
 */
const convertLegacyParamsToSections = (preset: AndroidPresetJson): PresetSection[] => {
  const items: PresetParamItem[] = [];
  
  if (preset.saturation !== undefined) {
    items.push({ label: '饱和度', value: `${preset.saturation >= 0 ? '+' : ''}${preset.saturation}`, span: 1 });
  }
  if (preset.contrast !== undefined) {
    items.push({ label: '对比度', value: `${preset.contrast >= 0 ? '+' : ''}${preset.contrast}`, span: 1 });
  }
  if (preset.warmth !== undefined) {
    items.push({ label: '色温', value: `${preset.warmth >= 0 ? '+' : ''}${preset.warmth}`, span: 1 });
  }
  if (preset.sharpness !== undefined) {
    items.push({ label: '锐度', value: `${preset.sharpness >= 0 ? '+' : ''}${preset.sharpness}`, span: 1 });
  }
  if (preset.clarity !== undefined) {
    items.push({ label: '清晰度', value: `${preset.clarity >= 0 ? '+' : ''}${preset.clarity}`, span: 1 });
  }
  if (preset.brightness !== undefined) {
    items.push({ label: '亮度', value: `${preset.brightness >= 0 ? '+' : ''}${preset.brightness}`, span: 1 });
  }
  
  if (items.length === 0) {
    items.push({ label: '滤镜', value: '复古 100%', span: 2 });
  }
  
  return [{ title: '🎨 大师调色参数', items }];
};

// ============================================
// Zustand Store
// ============================================

interface AppState {
  currentPage: PageType;
  setCurrentPage: (page: PageType) => void;
  currentSubPage: SubPageType;
  setCurrentSubPage: (page: SubPageType) => void;
  navigateToSubPage: (page: SubPageType) => void;
  goBack: () => void;
  selectedTab: number;
  setSelectedTab: (tab: number) => void;
  selectedBrand: string | null;
  setSelectedBrand: (brand: string | null) => void;
  selectedScene: string | null;
  setSelectedScene: (scene: string | null) => void;
  features: Feature[];
  toggleFeature: (id: string) => void;
  // AI 微调参数
  aiParams: {
    saturation: number;
    contrast: number;
    brightness: number;
    warmth: number;
    sharpness: number;
  };
  setAiParam: (key: string, value: number) => void;
  // 参数调节
  cameraParams: {
    iso: number;
    shutter: number;
    aperture: number;
    wb: number;
  };
  setCameraParam: (key: string, value: number) => void;
  // 水印设置
  watermarkSettings: {
    enabled: boolean;
    template: string;
    customText: string;
    position: string;
  };
  setWatermarkSetting: (key: string, value: string | boolean) => void;
  // 主题设置
  theme: 'hasselblad' | 'oppo' | 'vivo' | 'realme' | 'honor' | 'xiaomi';
  setTheme: (theme: AppState['theme']) => void;
  darkMode: 'system' | 'light' | 'dark';
  setDarkMode: (mode: AppState['darkMode']) => void;
  // 通知设置
  notifications: {
    enabled: boolean;
    updates: boolean;
    promotions: boolean;
  };
  setNotification: (key: string, value: boolean) => void;
  // 预设源管理
  presetSources: PresetSource[];
  addPresetSource: (source: Omit<PresetSource, 'id' | 'lastUpdated'>) => void;
  updatePresetSource: (id: string, source: Partial<PresetSource>) => void;
  removePresetSource: (id: string) => void;
  togglePresetSource: (id: string) => void;
  // 从源获取的预设
  fetchedPresets: Preset[];
  setFetchedPresets: (presets: Preset[]) => void;
  // AI 微调图像源（跨页面共享）
  tuneImageSource: string | null;
  setTuneImageSource: (source: string | null) => void;
  // 批量转换 Android 预设
  convertAndSetPresets: (androidPresets: AndroidPresetJson[]) => void;
}

export const useAppStore = create<AppState>((set) => ({
  currentPage: 'home',
  setCurrentPage: (page) => set({ currentPage: page }),
  currentSubPage: null,
  setCurrentSubPage: (page) => set({ currentSubPage: page }),
  navigateToSubPage: (page) => set({ currentSubPage: page }),
  goBack: () => set({ currentSubPage: null }),
  selectedTab: 0,
  setSelectedTab: (tab) => set({ selectedTab: tab }),
  selectedBrand: null,
  setSelectedBrand: (brand) => set({ selectedBrand: brand }),
  selectedScene: null,
  setSelectedScene: (scene) => set({ selectedScene: scene }),
  features: [
    {
      id: 'ai-scene',
      title: '哈苏之眼',
      subtitle: '智能识别50+拍摄场景，自动推荐最佳参数',
      icon: 'Camera',
      color: HasselbladColors.primary,
      gradientColors: HasselbladColors.primaryGradient,
      enabled: true,
      showToggle: true,
    },
    {
      id: 'ai-fine-tune',
      title: 'AI 微调',
      subtitle: '一键智能微调，色彩风格精准控制',
      icon: 'Palette',
      color: '#9C27B0',
      gradientColors: ['#4A148C', '#6A1B9A'],
      enabled: true,
      showToggle: true,
    },
    {
      id: 'smart-optimize',
      title: '智能优化',
      subtitle: '一键HDR增强、降噪、锐化优化',
      icon: 'Cpu',
      color: '#2196F3',
      gradientColors: ['#0D47A1', '#1565C0'],
      enabled: true,
      showToggle: true,
    },
    {
      id: 'watermark',
      title: '水印编辑器',
      subtitle: '14+专业水印模板，品牌认证水印',
      icon: 'Droplets',
      color: '#00BCD4',
      gradientColors: ['#006064', '#00838F'],
      enabled: true,
      showToggle: true,
    },
    {
      id: 'param-adjust',
      title: '参数精细调节',
      subtitle: 'ISO、快门、光圈、白平衡精确控制',
      icon: 'SlidersHorizontal',
      color: '#E91E63',
      gradientColors: ['#880E4F', '#AD1457'],
      enabled: true,
      showToggle: false,
    },
    {
      id: 'preset-manager',
      title: '预设管理',
      subtitle: '云端预设库，收藏、创建、分享',
      icon: 'Images',
      color: HasselbladColors.primary,
      gradientColors: HasselbladColors.primaryGradient,
      enabled: true,
      showToggle: false,
    },
    {
      id: 'lut-share',
      title: 'LUT 资源分享',
      subtitle: '20+专业 LUT 滤镜，一键下载使用',
      icon: 'Palette',
      color: '#9C27B0',
      gradientColors: ['#6A1B9A', '#8E24AA'],
      enabled: true,
      showToggle: false,
    },
    {
      id: 'hasselblad',
      title: '哈苏色彩科学',
      subtitle: 'HNCS 3.0 自然色彩解决方案',
      icon: 'Aperture',
      color: HasselbladColors.primary,
      gradientColors: HasselbladColors.primaryGradient,
      enabled: true,
      showToggle: true,
    },
    {
      id: 'cloud-sync',
      title: '云同步',
      subtitle: 'OPPO/realme/vivo/荣耀 CDN数据同步',
      icon: 'Cloud',
      color: '#3F51B5',
      gradientColors: ['#1A237E', '#303F9F'],
      enabled: false,
      showToggle: true,
    },
  ],
  toggleFeature: (id) =>
    set((state) => ({
      features: state.features.map((f) =>
        f.id === id ? { ...f, enabled: !f.enabled } : f
      ),
    })),
  // AI 微调参数
  aiParams: {
    saturation: 10,
    contrast: 5,
    brightness: 0,
    warmth: 8,
    sharpness: 15,
  },
  setAiParam: (key, value) =>
    set((state) => ({
      aiParams: { ...state.aiParams, [key]: value },
    })),
  // 参数调节
  cameraParams: {
    iso: 100,
    shutter: 125,
    aperture: 2.8,
    wb: 5500,
  },
  setCameraParam: (key, value) =>
    set((state) => ({
      cameraParams: { ...state.cameraParams, [key]: value },
    })),
  // 水印设置
  watermarkSettings: {
    enabled: true,
    template: 'default',
    customText: 'Shot on OMaster',
    position: 'bottom-right',
  },
  setWatermarkSetting: (key, value) =>
    set((state) => ({
      watermarkSettings: { ...state.watermarkSettings, [key]: value },
    })),
  // 主题设置
  theme: 'hasselblad',
  setTheme: (theme) => set({ theme }),
  darkMode: 'system',
  setDarkMode: (mode) => set({ darkMode: mode }),
  // 通知设置
  notifications: {
    enabled: true,
    updates: true,
    promotions: false,
  },
  setNotification: (key, value) =>
    set((state) => ({
      notifications: { ...state.notifications, [key]: value },
    })),
  // 默认预设源
  presetSources: [
    {
      id: 'oppo',
      name: 'OPPO 预设库',
      url: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json',
      enabled: true,
    },
    {
      id: 'realme',
      name: 'realme 预设库',
      url: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json',
      enabled: true,
    },
    {
      id: 'vivo',
      name: 'vivo 预设库',
      url: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/vivo.json',
      enabled: true,
    },
    {
      id: 'honor',
      name: '荣耀 预设库',
      url: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/honor.json',
      enabled: true,
    },
  ],
  addPresetSource: (source) =>
    set((state) => ({
      presetSources: [...state.presetSources, { ...source, id: Date.now().toString() }],
    })),
  updatePresetSource: (id, source) =>
    set((state) => ({
      presetSources: state.presetSources.map((s) =>
        s.id === id ? { ...s, ...source } : s
      ),
    })),
  removePresetSource: (id) =>
    set((state) => ({
      presetSources: state.presetSources.filter((s) => s.id !== id),
    })),
  togglePresetSource: (id) =>
    set((state) => ({
      presetSources: state.presetSources.map((s) =>
        s.id === id ? { ...s, enabled: !s.enabled } : s
      ),
    })),
  fetchedPresets: [],
  setFetchedPresets: (presets) => set({ fetchedPresets: presets }),
  // AI 微调图像源（跨页面共享）
  tuneImageSource: null,
  setTuneImageSource: (source) => set({ tuneImageSource: source }),
  // 批量转换 Android 预设
  convertAndSetPresets: (androidPresets) => {
    const converted = androidPresets.map((p, i) => convertAndroidPresetToReact(p, i));
    set({ fetchedPresets: converted });
  },
}));

// ============================================
// 示例预设数据（使用新数据模型）
// ============================================

const createSamplePreset = (
  id: string,
  name: string,
  coverPath: string,
  author: string,
  tags: string[],
  options: {
    isNew?: boolean;
    isHncs?: boolean;
    saturation?: number;
    contrast?: number;
    warmth?: number;
    sharpness?: number;
    clarity?: number;
  } = {}
): Preset => {
  const {
    isNew = false,
    isHncs = false,
    saturation = 10,
    contrast = 5,
    warmth = 8,
    sharpness = 15,
    clarity = 10,
  } = options;

  return {
    id,
    name,
    coverPath,
    galleryImages: [coverPath, coverPath + '?v=2', coverPath + '?v=3'],
    author,
    brand: extractBrandFromAuthor(author),
    tags,
    isNew,
    isHncs,
    mode: tags.includes('Pro') ? 'pro' : 'auto',
    description: {
      title: '哈苏大师拍摄建议',
      content: '【环境建议】日间户外或充足自然光【场景推荐】街拍、人像、风景、建筑【拍摄要点】适合追求经典胶片质感，建议使用黄金时刻拍摄'
    },
    sections: [
      {
        title: '🎨 大师调色参数',
        items: [
          { label: '滤镜', value: '复古 100%', span: 2 },
          { label: '饱和度', value: `${saturation >= 0 ? '+' : ''}${saturation}`, span: 1 },
          { label: '对比度', value: `${contrast >= 0 ? '+' : ''}${contrast}`, span: 1 },
          { label: '锐度', value: `${sharpness >= 0 ? '+' : ''}${sharpness}`, span: 1 },
          { label: '清晰度', value: `${clarity >= 0 ? '+' : ''}${clarity}`, span: 1 },
        ]
      }
    ],
    downloads: Math.floor(Math.random() * 20000) + 1000,
    rating: 4.5 + Math.random() * 0.5,
    ratingCount: Math.floor(Math.random() * 1000) + 50,
    comments: [],
    saturation,
    contrast,
    warmth,
    sharpness,
    clarity,
  };
};

export const featuredPresets: Preset[] = [
  createSamplePreset(
    'featured_1',
    '清新人像',
    'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&h=300&fit=crop',
    '@OPPO影像',
    ['人像', '清新', 'hncs'],
    { isNew: true, isHncs: true, saturation: 10, contrast: 5, warmth: 8, sharpness: 15 }
  ),
  createSamplePreset(
    'featured_2',
    '夜景霓虹',
    'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop',
    '@Find摄影',
    ['夜景', '霓虹', 'hncs'],
    { isNew: false, isHncs: true, saturation: 35, contrast: 20, warmth: -10, sharpness: 25 }
  ),
  createSamplePreset(
    'featured_3',
    '美食暖调',
    'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=280&fit=crop',
    '@美食摄影师',
    ['美食', '暖调'],
    { isNew: true, isHncs: false, saturation: 15, contrast: 10, warmth: 20, sharpness: 12 }
  ),
  createSamplePreset(
    'featured_4',
    '街拍黑白',
    'https://images.unsplash.com/photo-1444723121867-c61267198d6c?w=400&h=320&fit=crop',
    '@街拍大师',
    ['街拍', '黑白'],
    { isNew: false, isHncs: false, saturation: -100, contrast: 25, warmth: 0, sharpness: 20 }
  ),
  createSamplePreset(
    'featured_5',
    '风景通透',
    'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
    '@风光摄影',
    ['风景', '通透', 'hncs'],
    { isNew: true, isHncs: true, saturation: 20, contrast: 10, warmth: -10, sharpness: 25 }
  ),
  createSamplePreset(
    'featured_6',
    '建筑几何',
    'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=400&h=340&fit=crop',
    '@建筑摄影',
    ['建筑', '几何'],
    { isNew: false, isHncs: false, saturation: 8, contrast: 15, warmth: 0, sharpness: 30 }
  ),
];

export const homePresets: Preset[] = [
  createSamplePreset(
    'home_1',
    '清新CC胶片',
    'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=500&fit=crop',
    '@大师预设',
    ['胶片', '清新', 'hncs'],
    { isNew: false, isHncs: true, saturation: 5, contrast: 8, warmth: 3, sharpness: 10 }
  ),
  createSamplePreset(
    'home_2',
    '夜景氛围',
    'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=400&h=280&fit=crop',
    '@夜景专家',
    ['夜景', '氛围'],
    { isNew: true, isHncs: false, saturation: 15, contrast: 20, warmth: -5, sharpness: 18 }
  ),
  createSamplePreset(
    'home_3',
    '人像柔美',
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&h=450&fit=crop',
    '@人像摄影',
    ['人像', '柔美', 'hncs'],
    { isNew: false, isHncs: true, saturation: 10, contrast: -5, warmth: 8, sharpness: 12 }
  ),
  createSamplePreset(
    'home_4',
    '风景鲜明',
    'https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=400&h=320&fit=crop',
    '@风光大师',
    ['风景', '鲜明'],
    { isNew: false, isHncs: false, saturation: 20, contrast: 15, warmth: 5, sharpness: 22 }
  ),
  createSamplePreset(
    'home_5',
    '美食诱惑',
    'https://images.unsplash.com/photo-1476224203421-9ac39bcb3327?w=400&h=380&fit=crop',
    '@美食摄影',
    ['美食', '暖调'],
    { isNew: true, isHncs: false, saturation: 18, contrast: 12, warmth: 22, sharpness: 15 }
  ),
  createSamplePreset(
    'home_6',
    '街拍纪实',
    'https://images.unsplash.com/photo-1476973422084-e0fa66ff9456?w=400&h=300&fit=crop',
    '@街拍摄影',
    ['街拍', '纪实', 'hncs'],
    { isNew: false, isHncs: true, saturation: 12, contrast: 18, warmth: 2, sharpness: 20 }
  ),
];

// 导出品牌文案规范
export const HasselbladCopy = {
  // 按钮文案
  applyPreset: '一键应用哈苏配方',
  savePreset: '收藏配方',
  sharePreset: '分享配方',
  exportPreset: '导出配方',
  
  // 标签文案
  hncsBadge: 'HNCS 自然色彩认证',
  newBadge: 'NEW',
  proBadge: 'PRO',
  hotBadge: 'HOT',
  
  // 标题文案
  heroTitle: '今日哈苏大师推荐',
  presetDetailTitle: (name: string) => `哈苏大师配方 · ${name}`,
  paramsTitle: '大师调色参数',
  shootingTipsTitle: '哈苏大师拍摄建议',
  relatedTitle: '哈苏大师也爱用',
  
  // 空状态文案
  emptyState: '探索哈苏大师配方库',
  noPresets: '暂无预设，开始探索哈苏大师配方',
  
  // 场景文案
  sceneRecognition: '哈苏之眼',
  sceneAnalysis: '场景分析',
  
  // 胶片文案
  filmRecommendation: '胶片推荐',
  filmMatch: '胶片匹配度',
} as const;
