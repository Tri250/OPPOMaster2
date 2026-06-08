// 云同步服务层
export interface SyncModule {
  id: string;
  name: string;
  status: 'pending' | 'syncing' | 'completed' | 'error';
  progress: number;
  lastSyncTime?: number;
}

export interface CloudSyncState {
  isConnected: boolean;
  connectedBrand: string | null;
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
  private state: CloudSyncState = {
    isConnected: true, // 默认已连接
    connectedBrand: 'oppo', // 默认连接OPPO
    modules: syncModules.map(m => ({ ...m, status: 'completed', progress: 100 })),
    lastFullSyncTime: Date.now(),
  };

  // 获取当前状态
  getState(): CloudSyncState {
    return this.state;
  }

  // 连接到品牌CDN
  connect(brandId: string): Promise<boolean> {
    return new Promise((resolve) => {
      setTimeout(() => {
        this.state.isConnected = true;
        this.state.connectedBrand = brandId;
        resolve(true);
      }, 500);
    });
  }

  // 断开连接
  disconnect(): void {
    this.state.isConnected = false;
    this.state.connectedBrand = null;
  }

  // 同步单个模块
  syncModule(moduleId: string, onProgress?: (progress: number) => void): Promise<boolean> {
    return new Promise((resolve) => {
      const module = this.state.modules.find(m => m.id === moduleId);
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
        onProgress?.(progress);

        if (progress >= 100) {
          clearInterval(interval);
          module.status = 'completed';
          module.lastSyncTime = Date.now();
          resolve(true);
        }
      }, 200);
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

  // 获取预设数据（模拟从CDN获取）
  async fetchPresets(brand?: string): Promise<any[]> {
    const source = brand || this.state.connectedBrand || 'oppo';
    
    // 模拟网络请求延迟
    await new Promise(r => setTimeout(r, 300));
    
    // 返回预设数据
    return [
      {
        id: `preset_${source}_1`,
        name: `${cdnSources[source]?.name || source} 经典人像`,
        coverPath: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=500&fit=crop',
        author: `@${cdnSources[source]?.name || source}影像`,
        brand: cdnSources[source]?.name || source,
        tags: ['人像', '经典'],
        isNew: true,
        isHncs: true,
        saturation: 10,
        contrast: 5,
        warmth: 8,
        sharpness: 15,
      },
      {
        id: `preset_${source}_2`,
        name: `${cdnSources[source]?.name || source} 夜景大师`,
        coverPath: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop',
        author: '@夜景专家',
        brand: cdnSources[source]?.name || source,
        tags: ['夜景', '大师'],
        isNew: false,
        isHncs: true,
        saturation: 35,
        contrast: 20,
        warmth: -10,
        sharpness: 25,
      },
      {
        id: `preset_${source}_3`,
        name: `${cdnSources[source]?.name || source} 风景通透`,
        coverPath: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
        author: '@风光摄影',
        brand: cdnSources[source]?.name || source,
        tags: ['风景', '通透'],
        isNew: true,
        isHncs: false,
        saturation: 20,
        contrast: 10,
        warmth: -10,
        sharpness: 25,
      },
    ];
  }
}

// 全局云同步服务实例
export const cloudSyncService = new CloudSyncService();