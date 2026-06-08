// 云同步服务层 - 支持自动刷新和状态订阅
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

export interface Preset {
  id: string;
  name: string;
  coverPath: string;
  author: string;
  brand: string;
  brandId: string;
  tags: string[];
  isNew: boolean;
  isHncs: boolean;
  saturation: number;
  contrast: number;
  warmth: number;
  sharpness: number;
  clarity?: number;
  brightness?: number;
  description?: string;
  downloadCount?: number;
  rating?: number;
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

// 预设数据模板（各品牌共享）
const presetTemplates = [
  {
    baseName: '经典人像',
    coverPath: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=500&fit=crop',
    author: '影像',
    tags: ['人像', '经典'],
    isHncs: true,
    saturation: 10,
    contrast: 5,
    warmth: 8,
    sharpness: 15,
    description: '适合人像拍摄，肤色自然柔和，细节丰富',
    downloadCount: 12500,
    rating: 4.8,
  },
  {
    baseName: '夜景大师',
    coverPath: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop',
    author: '夜景专家',
    tags: ['夜景', '大师'],
    isHncs: true,
    saturation: 35,
    contrast: 20,
    warmth: -10,
    sharpness: 25,
    description: '专为夜景优化，降噪增强，色彩饱满',
    downloadCount: 8900,
    rating: 4.6,
  },
  {
    baseName: '风景通透',
    coverPath: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
    author: '风光摄影',
    tags: ['风景', '通透'],
    isHncs: false,
    saturation: 20,
    contrast: 10,
    warmth: -10,
    sharpness: 25,
    description: '风景专用，通透感强，色彩自然',
    downloadCount: 7200,
    rating: 4.5,
  },
  {
    baseName: '美食暖调',
    coverPath: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=280&fit=crop',
    author: '美食摄影',
    tags: ['美食', '暖调'],
    isHncs: false,
    saturation: 15,
    contrast: 10,
    warmth: 20,
    sharpness: 12,
    description: '美食拍摄专用，暖色调，食欲感强',
    downloadCount: 5600,
    rating: 4.4,
  },
  {
    baseName: '街拍黑白',
    coverPath: 'https://images.unsplash.com/photo-1444723121867-c61267198d6c?w=400&h=320&fit=crop',
    author: '街拍大师',
    tags: ['街拍', '黑白'],
    isHncs: false,
    saturation: -100,
    contrast: 25,
    warmth: 0,
    sharpness: 20,
    description: '经典黑白风格，高对比度，艺术感强',
    downloadCount: 4300,
    rating: 4.3,
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
            id: `${brandState.id}_${index}`,
            name: `${source.name} ${template.baseName}`,
            coverPath: template.coverPath,
            author: `@${source.name}${template.author}`,
            brand: source.name,
            brandId: brandState.id,
            tags: template.tags,
            isNew: true,
            isHncs: template.isHncs,
            saturation: template.saturation,
            contrast: template.contrast,
            warmth: template.warmth,
            sharpness: template.sharpness,
            description: template.description,
            downloadCount: template.downloadCount,
            rating: template.rating,
          });
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