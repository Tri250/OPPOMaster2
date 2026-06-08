// 云同步服务层
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

// 云同步服务类
export class CloudSyncService {
  private state: CloudSyncState;

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
  }

  // 获取当前状态
  getState(): CloudSyncState {
    return this.state;
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
      resolve(true);
    });
  }

  // 断开品牌
  disconnectBrand(brandId: string): void {
    const brand = this.state.brands.find(b => b.id === brandId);
    if (brand) {
      brand.isConnected = false;
    }
  }

  // 连接所有品牌
  connectAllBrands(): Promise<boolean> {
    return new Promise((resolve) => {
      this.state.brands.forEach(b => {
        b.isConnected = true;
        b.lastSyncTime = Date.now();
      });
      this.state.isConnected = true;
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
      resolve(true);
    });
  }

  // 获取预设数据（从所有已连接品牌获取）
  async fetchPresets(brand?: string): Promise<any[]> {
    // 模拟网络请求延迟
    await new Promise(r => setTimeout(r, 300));
    
    const presets: any[] = [];
    
    // 如果指定了品牌，只获取该品牌的预设
    if (brand) {
      const source = cdnSources[brand];
      if (source) {
        presets.push(
          {
            id: `preset_${brand}_1`,
            name: `${source.name} 经典人像`,
            coverPath: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=500&fit=crop',
            author: `@${source.name}影像`,
            brand: source.name,
            tags: ['人像', '经典'],
            isNew: true,
            isHncs: true,
            saturation: 10,
            contrast: 5,
            warmth: 8,
            sharpness: 15,
          },
          {
            id: `preset_${brand}_2`,
            name: `${source.name} 夜景大师`,
            coverPath: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop',
            author: '@夜景专家',
            brand: source.name,
            tags: ['夜景', '大师'],
            isNew: false,
            isHncs: true,
            saturation: 35,
            contrast: 20,
            warmth: -10,
            sharpness: 25,
          }
        );
      }
    } else {
      // 获取所有已连接品牌的预设
      for (const brandState of this.state.brands) {
        if (brandState.isConnected) {
          const source = cdnSources[brandState.id];
          presets.push(
            {
              id: `preset_${brandState.id}_1`,
              name: `${source.name} 经典人像`,
              coverPath: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=500&fit=crop',
              author: `@${source.name}影像`,
              brand: source.name,
              tags: ['人像', '经典'],
              isNew: true,
              isHncs: true,
              saturation: 10,
              contrast: 5,
              warmth: 8,
              sharpness: 15,
            },
            {
              id: `preset_${brandState.id}_2`,
              name: `${source.name} 夜景大师`,
              coverPath: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop',
              author: '@夜景专家',
              brand: source.name,
              tags: ['夜景', '大师'],
              isNew: false,
              isHncs: true,
              saturation: 35,
              contrast: 20,
              warmth: -10,
              sharpness: 25,
            }
          );
        }
      }
    }
    
    return presets;
  }
}

// 全局云同步服务实例
export const cloudSyncService = new CloudSyncService();