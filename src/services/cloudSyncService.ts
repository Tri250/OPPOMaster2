// 云同步服务层 - 支持自动刷新和状态订阅
// 影像参数名称与Android端 MasterPreset.kt 保持一致

export interface SyncModule {
  id: string;
  name: string;
  status: 'pending' | 'syncing' | 'completed' | 'error';
  progress: number;
  lastSyncTime?: number;
}

export interface BrandSyncState {
  id: string;
  name: string;
  color: string;
  isConnected: boolean;
  modules: SyncModule[];
  lastSyncTime: number;
}

export interface CloudSyncState {
  isConnected: boolean;
  brands: BrandSyncState[];
  modules: SyncModule[];
  lastFullSyncTime: number;
}

// 大师模式预设参数 - 与Android端 MasterPreset.kt 一致
export interface Preset {
  id: string;
  name: string;
  coverPath: string;
  galleryImages?: string[];
  author: string;
  brand: string;
  brandId: string;
  tags: string[];
  isNew: boolean;
  isHncs: boolean;
  mode?: 'auto' | 'pro';
  
  // 基础调色参数
  filter?: string;           // 滤镜类型：原图、胶片、黑白等
  softLight?: number;        // 柔光强度：0-100
  tone?: number;             // 影调：-100 到 +100，控制整体明暗对比
  saturation?: number;       // 饱和度：-100 到 +100
  warmCool?: number;         // 冷暖色调：-100 到 +100，负值偏冷，正值偏暖
  cyanMagenta?: number;      // 青品色调：-100 到 +100，负值偏青，正值偏品红
  sharpness?: number;        // 锐度：0-100
  vignette?: string;         // 暗角：开/关
  
  // 专业参数（Pro模式）
  iso?: string;              // ISO感光度：100, 200-400等
  shutterSpeed?: string;     // 快门速度：1/125, 1/60等
  exposureCompensation?: string; // 曝光补偿：-1.0, +0.7等
  colorTemperature?: number; // 色温数值：2000-8000
  colorHue?: number;         // 色调数值：-150 到 150
  whiteBalance?: string;     // 白平衡：2000K, 阴天, 日光等
  colorTone?: string;        // 色调：暖调, 冷调等
  
  // 其他信息
  description?: string;
  downloadCount?: number;
  rating?: number;
  version?: number;
  build?: number;
  createdAt?: number;
}

// 模块定义
export const syncModules: SyncModule[] = [
  { id: 'presets', name: '预设参数', status: 'pending', progress: 0 },
  { id: 'watermarks', name: '水印模板', status: 'pending', progress: 0 },
  { id: 'favorites', name: '收藏列表', status: 'pending', progress: 0 },
  { id: 'settings', name: '应用设置', status: 'pending', progress: 0 },
];

// CDN数据源
export const cdnSources: Record<string, { name: string; baseUrl: string; color: string }> = {
  oppo: { name: 'OPPO', baseUrl: 'https://cdn.oppo.com/omaster', color: '#1BAA52' },
  realme: { name: 'realme', baseUrl: 'https://cdn.realme.com/omaster', color: '#FFC107' },
  vivo: { name: 'vivo', baseUrl: 'https://cdn.vivo.com/omaster', color: '#415FFF' },
  honor: { name: '荣耀', baseUrl: 'https://cdn.honor.com/omaster', color: '#00BFFF' },
  xiaomi: { name: '小米', baseUrl: 'https://cdn.mi.com/omaster', color: '#FF6900' },
};

// 预设数据模板（各品牌共享）- 使用正确的参数名称
const presetTemplates: Partial<Preset>[] = [
  {
    name: '经典人像',
    coverPath: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=500&fit=crop',
    author: '影像',
    tags: ['人像', '经典'],
    isHncs: true,
    mode: 'auto',
    filter: '原图',
    softLight: 30,
    tone: 10,
    saturation: 10,
    warmCool: 8,
    cyanMagenta: 0,
    sharpness: 15,
    vignette: '关',
    description: '适合人像拍摄，肤色自然柔和，细节丰富',
    downloadCount: 12500,
    rating: 4.8,
  },
  {
    name: '夜景大师',
    coverPath: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop',
    author: '夜景专家',
    tags: ['夜景', '大师'],
    isHncs: true,
    mode: 'pro',
    filter: '胶片',
    softLight: 20,
    tone: 20,
    saturation: 35,
    warmCool: -10,
    cyanMagenta: 5,
    sharpness: 25,
    vignette: '开',
    iso: '400-800',
    shutterSpeed: '1/30',
    exposureCompensation: '-0.3',
    colorTemperature: 4500,
    description: '专为夜景优化，降噪增强，色彩饱满',
    downloadCount: 8900,
    rating: 4.6,
  },
  {
    name: '风景通透',
    coverPath: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
    author: '风光摄影',
    tags: ['风景', '通透'],
    isHncs: false,
    mode: 'auto',
    filter: '原图',
    softLight: 10,
    tone: 15,
    saturation: 20,
    warmCool: -10,
    cyanMagenta: -5,
    sharpness: 25,
    vignette: '关',
    description: '风景专用，通透感强，色彩自然',
    downloadCount: 7200,
    rating: 4.5,
  },
  {
    name: '美食暖调',
    coverPath: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=280&fit=crop',
    author: '美食摄影',
    tags: ['美食', '暖调'],
    isHncs: false,
    mode: 'auto',
    filter: '原图',
    softLight: 40,
    tone: 5,
    saturation: 15,
    warmCool: 20,
    cyanMagenta: 0,
    sharpness: 12,
    vignette: '关',
    description: '美食拍摄专用，暖色调，食欲感强',
    downloadCount: 5600,
    rating: 4.4,
  },
  {
    name: '街拍黑白',
    coverPath: 'https://images.unsplash.com/photo-1444723121867-c61267198d6c?w=400&h=320&fit=crop',
    author: '街拍大师',
    tags: ['街拍', '黑白'],
    isHncs: false,
    mode: 'auto',
    filter: '黑白',
    softLight: 15,
    tone: 25,
    saturation: -100,
    warmCool: 0,
    cyanMagenta: 0,
    sharpness: 20,
    vignette: '开',
    description: '经典黑白风格，高对比度，艺术感强',
    downloadCount: 4300,
    rating: 4.3,
  },
  {
    name: '胶片复古',
    coverPath: 'https://images.unsplash.com/photo-1502893425584-3b5d3f3f3f3f?w=400&h=300&fit=crop',
    author: '胶片爱好者',
    tags: ['胶片', '复古'],
    isHncs: true,
    mode: 'auto',
    filter: '胶片',
    softLight: 50,
    tone: -10,
    saturation: 5,
    warmCool: 15,
    cyanMagenta: 10,
    sharpness: 10,
    vignette: '开',
    description: '复古胶片质感，柔光梦幻',
    downloadCount: 6800,
    rating: 4.7,
  },
];

// 云同步服务类
export class CloudSyncService {
  private state: CloudSyncState;
  private presets: Preset[] = [];
  private subscribers: Set<(state: CloudSyncState, presets: Preset[]) => void> = new Set();
  private autoRefreshInterval: number | null = null;
  private isAutoRefreshEnabled: boolean = true;

  constructor() {
    // 初始化所有品牌默认已连接
    const brands: BrandSyncState[] = Object.entries(cdnSources).map(([id, source]) => ({
      id,
      name: source.name,
      color: source.color,
      isConnected: true, // 默认已连接
      modules: syncModules.map(m => ({ ...m, status: 'completed', progress: 100 })),
      lastSyncTime: Date.now(),
    }));

    this.state = {
      isConnected: true,
      brands,
      modules: syncModules.map(m => ({ ...m, status: 'completed', progress: 100 })),
      lastFullSyncTime: Date.now(),
    };

    // 初始化预设数据
    this.refreshPresets();

    // 启动自动刷新（每30秒）
    this.startAutoRefresh();
  }

  // 订阅状态变化
  subscribe(callback: (state: CloudSyncState, presets: Preset[]) => void): () => void {
    this.subscribers.add(callback);
    // 立即通知当前状态
    callback(this.state, this.presets);
    // 返回取消订阅函数
    return () => this.subscribers.delete(callback);
  }

  // 通知所有订阅者
  private notifySubscribers(): void {
    this.subscribers.forEach(callback => callback(this.state, this.presets));
  }

  // 启动自动刷新
  startAutoRefresh(intervalMs: number = 30000): void {
    if (this.autoRefreshInterval) {
      clearInterval(this.autoRefreshInterval);
    }
    this.isAutoRefreshEnabled = true;
    this.autoRefreshInterval = window.setInterval(() => {
      if (this.isAutoRefreshEnabled) {
        this.refreshPresets();
      }
    }, intervalMs);
  }

  // 停止自动刷新
  stopAutoRefresh(): void {
    if (this.autoRefreshInterval) {
      clearInterval(this.autoRefreshInterval);
      this.autoRefreshInterval = null;
    }
    this.isAutoRefreshEnabled = false;
  }

  // 刷新预设数据（自动合并重复内容）
  async refreshPresets(): Promise<Preset[]> {
    // 模拟网络请求延迟
    await new Promise(r => setTimeout(r, 100));
    
    const allPresets: Preset[] = [];
    
    // 从所有已连接品牌获取预设
    for (const brandState of this.state.brands) {
      if (brandState.isConnected) {
        const source = cdnSources[brandState.id];
        // 为每个品牌生成预设
        presetTemplates.forEach((template, index) => {
          allPresets.push({
            ...template,
            id: `${brandState.id}_${index}`,
            name: `${source.name} ${template.name}`,
            coverPath: template.coverPath!,
            author: `@${source.name}${template.author}`,
            brand: source.name,
            brandId: brandState.id,
            tags: template.tags!,
            isNew: true,
            isHncs: template.isHncs!,
            description: template.description,
            downloadCount: template.downloadCount,
            rating: template.rating,
          } as Preset);
        });
      }
    }

    // 合并重复预设（相同名称和参数的预设合并为一个，显示多个品牌标签）
    this.presets = this.mergeDuplicatePresets(allPresets);
    
    // 通知订阅者
    this.notifySubscribers();
    
    return this.presets;
  }

  // 合并重复预设
  private mergeDuplicatePresets(presets: Preset[]): Preset[] {
    const mergedMap = new Map<string, Preset>();
    
    presets.forEach(preset => {
      // 使用预设名称作为合并键（相同名称的预设合并）
      const key = preset.name.split(' ').slice(1).join(' '); // 提取基础名称
      
      if (mergedMap.has(key)) {
        const existing = mergedMap.get(key)!;
        // 合并品牌信息
        if (!existing.brand.includes(preset.brand)) {
          existing.brand = `${existing.brand}, ${preset.brand}`;
        }
        // 累加下载量
        existing.downloadCount = (existing.downloadCount || 0) + (preset.downloadCount || 0);
        // 取最高评分
        existing.rating = Math.max(existing.rating || 0, preset.rating || 0);
      } else {
        // 创建合并后的预设
        mergedMap.set(key, {
          ...preset,
          id: `merged_${key}`,
          name: key,
        });
      }
    });
    
    return Array.from(mergedMap.values());
  }

  // 获取当前状态
  getState(): CloudSyncState {
    return this.state;
  }

  // 获取当前预设
  getPresets(): Preset[] {
    return this.presets;
  }

  // 获取品牌状态
  getBrandState(brandId: string): BrandSyncState | undefined {
    return this.state.brands.find(b => b.id === brandId);
  }

  // 连接品牌
  connectBrand(brandId: string): Promise<boolean> {
    return new Promise((resolve) => {
      const brand = this.state.brands.find(b => b.id === brandId);
      if (brand) {
        brand.isConnected = true;
        brand.lastSyncTime = Date.now();
      }
      this.refreshPresets();
      resolve(true);
    });
  }

  // 断开品牌
  disconnectBrand(brandId: string): void {
    const brand = this.state.brands.find(b => b.id === brandId);
    if (brand) {
      brand.isConnected = false;
    }
    this.refreshPresets();
  }

  // 连接所有品牌
  connectAllBrands(): Promise<boolean> {
    return new Promise((resolve) => {
      this.state.brands.forEach(b => {
        b.isConnected = true;
        b.lastSyncTime = Date.now();
      });
      this.state.isConnected = true;
      this.refreshPresets();
      resolve(true);
    });
  }

  // 同步单个品牌的所有模块
  syncBrandModules(brandId: string, onProgress?: (moduleId: string, progress: number) => void): Promise<boolean> {
    return new Promise(async (resolve) => {
      const brand = this.state.brands.find(b => b.id === brandId);
      if (!brand) {
        resolve(false);
        return;
      }

      for (const module of brand.modules) {
        await this.syncModuleForBrand(brandId, module.id, onProgress);
      }
      brand.lastSyncTime = Date.now();
      this.refreshPresets();
      resolve(true);
    });
  }

  // 同步品牌的单个模块
  syncModuleForBrand(brandId: string, moduleId: string, onProgress?: (moduleId: string, progress: number) => void): Promise<boolean> {
    return new Promise((resolve) => {
      const brand = this.state.brands.find(b => b.id === brandId);
      if (!brand) {
        resolve(false);
        return;
      }

      const module = brand.modules.find(m => m.id === moduleId);
      if (!module) {
        resolve(false);
        return;
      }

      module.status = 'syncing';
      module.progress = 0;

      let progress = 0;
      const interval = setInterval(() => {
        progress += 10;
        module.progress = progress;
        onProgress?.(moduleId, progress);

        if (progress >= 100) {
          clearInterval(interval);
          module.status = 'completed';
          module.lastSyncTime = Date.now();
          this.notifySubscribers();
          resolve(true);
        }
      }, 200);
    });
  }

  // 同步所有品牌的所有模块
  syncAllBrands(onBrandProgress?: (brandId: string, moduleId: string, progress: number) => void): Promise<boolean> {
    return new Promise(async (resolve) => {
      for (const brand of this.state.brands) {
        if (brand.isConnected) {
          await this.syncBrandModules(brand.id, (moduleId, progress) => {
            onBrandProgress?.(brand.id, moduleId, progress);
          });
        }
      }
      this.state.lastFullSyncTime = Date.now();
      this.refreshPresets();
      resolve(true);
    });
  }

  // 同步单个模块（全局）
  syncModule(moduleId: string, onProgress?: (progress: number) => void): Promise<boolean> {
    return new Promise(async (resolve) => {
      // 同步所有已连接品牌的该模块
      for (const brand of this.state.brands) {
        if (brand.isConnected) {
          await this.syncModuleForBrand(brand.id, moduleId, (mId, progress) => {
            onProgress?.(progress);
          });
        }
      }
      this.refreshPresets();
      resolve(true);
    });
  }

  // 同步所有模块
  syncAllModules(onModuleProgress?: (moduleId: string, progress: number) => void): Promise<boolean> {
    return new Promise(async (resolve) => {
      for (const module of this.state.modules) {
        await this.syncModule(module.id, (progress) => {
          onModuleProgress?.(module.id, progress);
        });
      }
      this.state.lastFullSyncTime = Date.now();
      this.notifySubscribers();
      resolve(true);
    });
  }

  // 获取预设详情
  getPresetDetail(presetId: string): Preset | undefined {
    return this.presets.find(p => p.id === presetId);
  }

  // 获取预设数据（从所有已连接品牌获取）
  async fetchPresets(brand?: string): Promise<Preset[]> {
    await new Promise(r => setTimeout(r, 100));
    
    if (brand) {
      // 返回指定品牌的预设
      return this.presets.filter(p => p.brandId === brand || p.brand.includes(cdnSources[brand]?.name));
    }
    
    return this.presets;
  }
}

// 全局云同步服务实例
export const cloudSyncService = new CloudSyncService();