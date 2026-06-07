import { create } from 'zustand';

export type PageType = 'home' | 'featured' | 'features' | 'about';
export type SubPageType = 
  | 'ai-scene' 
  | 'ai-fine-tune' 
  | 'smart-optimize' 
  | 'watermark' 
  | 'param-adjust' 
  | 'preset-manager'
  | 'hasselblad'
  | 'cloud-sync'
  | 'theme-settings'
  | 'dark-mode'
  | 'update-channel'
  | 'notification'
  | 'privacy'
  | 'terms'
  | 'hsl-adjustment'
  | 'batch-processing'
  | 'raw-processing'
  | 'tone-curve'
  | 'histogram'
  | 'favorites'
  | 'trend-2026'
  | 'scene-detail'
  | 'lut-share'
  | null;

export interface Preset {
  id: string;
  name: string;
  coverPath: string;
  author: string;
  brand: string;
  tags: string[];
  isNew: boolean;
  isHncs: boolean;
  saturation: number;
  contrast: number;
  warmth: number;
  sharpness: number;
  clarity?: number;
  brightness?: number;
}

export interface SubFeature {
  id: string;
  name: string;
  desc: string;
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
  category?: string;
  subFeatures?: SubFeature[];
}

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
    highlights: number;
    shadows: number;
    clarity: number;
    noiseReduction: number;
    skinSmooth: number;
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
}

export const useAppStore = create<AppState>((set, get) => ({
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
    // ========== AI 智能引擎 ==========
    {
      id: 'ai-engine',
      title: 'AI 智能引擎',
      subtitle: '场景识别 · 微调 · 增强 · 细分',
      icon: 'Cpu',
      color: '#4CAF50',
      gradientColors: ['#1B5E20', '#2E7D32'],
      enabled: true,
      showToggle: true,
      category: 'ai',
      subFeatures: [
        { id: 'scene-recognition', name: '场景识别', desc: '36+场景智能识别' },
        { id: 'one-click-tune', name: '一键微调', desc: 'AI色彩风格优化' },
        { id: 'smart-enhance', name: '智能增强', desc: 'HDR/降噪/锐化' },
        { id: 'scene-detail', name: '场景细分', desc: '细分参数优化' },
      ],
    },
    // ========== 专业调色 ==========
    {
      id: 'pro-color',
      title: '专业调色',
      subtitle: 'HSL · 曲线 · 直方图',
      icon: 'Palette',
      color: '#FF6B35',
      gradientColors: ['#D84315', '#FF5722'],
      enabled: true,
      showToggle: false,
      category: 'color',
      subFeatures: [
        { id: 'hsl', name: 'HSL调节', desc: '8色独立控制' },
        { id: 'curve', name: '色调曲线', desc: 'RGB曲线调节' },
        { id: 'histogram', name: '直方图', desc: '曝光分析' },
      ],
    },
    // ========== 工作流 ==========
    {
      id: 'workflow',
      title: '工作流',
      subtitle: 'RAW · 批量 · 水印',
      icon: 'Layers',
      color: '#795548',
      gradientColors: ['#3E2723', '#5D4037'],
      enabled: true,
      showToggle: false,
      category: 'workflow',
      subFeatures: [
        { id: 'raw', name: 'RAW处理', desc: 'DNG/CR2/NEF' },
        { id: 'batch', name: '批量处理', desc: '多图同时调节' },
        { id: 'watermark', name: '水印编辑', desc: '14+专业模板' },
      ],
    },
    // ========== 预设中心 ==========
    {
      id: 'preset-center',
      title: '预设中心',
      subtitle: '收藏 · 趋势 · 品牌',
      icon: 'Sparkles',
      color: '#FFD700',
      gradientColors: ['#FFA000', '#FFC107'],
      enabled: true,
      showToggle: false,
      category: 'preset',
      subFeatures: [
        { id: 'favorites', name: '我的收藏', desc: '收藏夹管理' },
        { id: 'trend', name: '2026趋势', desc: '年度流行风格' },
        { id: 'brand', name: '品牌预设', desc: '哈苏/富士/徕卡' },
      ],
    },
    // ========== LUT资源与拍摄分享 ==========
    {
      id: 'lut-share',
      title: 'LUT资源与分享',
      subtitle: 'LUT库 · 拍摄分享 · 社区',
      icon: 'Share2',
      color: '#3B82F6',
      gradientColors: ['#1E40AF', '#3B82F6'],
      enabled: true,
      showToggle: false,
      category: 'community',
      subFeatures: [
        { id: 'lut-library', name: 'LUT资源库', desc: '3D LUT/CUBE文件' },
        { id: 'shot-share', name: '拍摄分享', desc: '作品展示交流' },
        { id: 'community', name: '创作者社区', desc: '关注/点赞/评论' },
      ],
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
    highlights: 0,
    shadows: 0,
    clarity: 10,
    noiseReduction: 0,
    skinSmooth: 0,
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
}));

export const featuredPresets: Preset[] = [
  {
    id: 'featured_1',
    name: '清新人像',
    coverPath: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&h=300&fit=crop',
    author: '@OPPO影像',
    brand: 'OPPO',
    tags: ['人像', '清新'],
    isNew: true,
    isHncs: true,
    saturation: 10,
    contrast: 5,
    warmth: 8,
    sharpness: 15,
  },
  {
    id: 'featured_2',
    name: '夜景霓虹',
    coverPath: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop',
    author: '@Find摄影',
    brand: 'OPPO',
    tags: ['夜景', '霓虹'],
    isNew: false,
    isHncs: true,
    saturation: 35,
    contrast: 20,
    warmth: -10,
    sharpness: 25,
  },
  {
    id: 'featured_3',
    name: '美食暖调',
    coverPath: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=280&fit=crop',
    author: '@美食摄影师',
    brand: 'realme',
    tags: ['美食', '暖调'],
    isNew: true,
    isHncs: false,
    saturation: 15,
    contrast: 10,
    warmth: 20,
    sharpness: 12,
  },
  {
    id: 'featured_4',
    name: '街拍黑白',
    coverPath: 'https://images.unsplash.com/photo-1444723121867-c61267198d6c?w=400&h=320&fit=crop',
    author: '@街拍大师',
    brand: 'vivo',
    tags: ['街拍', '黑白'],
    isNew: false,
    isHncs: false,
    saturation: -100,
    contrast: 25,
    warmth: 0,
    sharpness: 20,
  },
  {
    id: 'featured_5',
    name: '风景通透',
    coverPath: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
    author: '@风光摄影',
    brand: '荣耀',
    tags: ['风景', '通透'],
    isNew: true,
    isHncs: true,
    saturation: 20,
    contrast: 10,
    warmth: -10,
    sharpness: 25,
  },
  {
    id: 'featured_6',
    name: '建筑几何',
    coverPath: 'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=400&h=340&fit=crop',
    author: '@建筑摄影',
    brand: '小米',
    tags: ['建筑', '几何'],
    isNew: false,
    isHncs: false,
    saturation: 8,
    contrast: 15,
    warmth: 0,
    sharpness: 30,
  },
];

export const homePresets: Preset[] = [
  {
    id: 'home_1',
    name: '清新CC胶片',
    coverPath: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=500&fit=crop',
    author: '@大师预设',
    brand: 'OPPO',
    tags: ['胶片', '清新'],
    isNew: false,
    isHncs: true,
    saturation: 5,
    contrast: 8,
    warmth: 3,
    sharpness: 10,
  },
  {
    id: 'home_2',
    name: '夜景氛围',
    coverPath: 'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=400&h=280&fit=crop',
    author: '@夜景专家',
    brand: 'vivo',
    tags: ['夜景', '氛围'],
    isNew: true,
    isHncs: false,
    saturation: 15,
    contrast: 20,
    warmth: -5,
    sharpness: 18,
  },
  {
    id: 'home_3',
    name: '人像柔美',
    coverPath: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&h=450&fit=crop',
    author: '@人像摄影',
    brand: 'realme',
    tags: ['人像', '柔美'],
    isNew: false,
    isHncs: true,
    saturation: 10,
    contrast: -5,
    warmth: 8,
    sharpness: 12,
  },
  {
    id: 'home_4',
    name: '风景鲜明',
    coverPath: 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=400&h=320&fit=crop',
    author: '@风光大师',
    brand: '荣耀',
    tags: ['风景', '鲜明'],
    isNew: false,
    isHncs: false,
    saturation: 20,
    contrast: 15,
    warmth: 5,
    sharpness: 22,
  },
  {
    id: 'home_5',
    name: '美食诱惑',
    coverPath: 'https://images.unsplash.com/photo-1476224203421-9ac39bcb3327?w=400&h=380&fit=crop',
    author: '@美食摄影',
    brand: '小米',
    tags: ['美食', '暖调'],
    isNew: true,
    isHncs: false,
    saturation: 18,
    contrast: 12,
    warmth: 22,
    sharpness: 15,
  },
  {
    id: 'home_6',
    name: '街拍纪实',
    coverPath: 'https://images.unsplash.com/photo-1476973422084-e0fa66ff9456?w=400&h=300&fit=crop',
    author: '@街拍摄影',
    brand: 'OPPO',
    tags: ['街拍', '纪实'],
    isNew: false,
    isHncs: true,
    saturation: 12,
    contrast: 18,
    warmth: 2,
    sharpness: 20,
  },
];
