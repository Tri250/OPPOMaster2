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
  | 'dark-mode'
  | 'update-channel'
  | 'notification'
  | 'privacy'
  | 'terms'
  | 'android-showcase'
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
      title: 'AI 场景识别',
      subtitle: '智能识别36+拍摄场景，自动推荐最佳参数',
      icon: 'Camera',
      color: '#4CAF50',
      gradientColors: ['#1B5E20', '#2E7D32'],
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
      color: '#FF9800',
      gradientColors: ['#E65100', '#F57C00'],
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
      color: '#FF6B35',
      gradientColors: ['#CC5500', '#E86A17'],
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
