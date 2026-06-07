import { CloudPreset, CloudPresetResponse, SyncState } from '../types/cloudPreset';
import { localPresetData } from '../data/localPresetData';

const SYNC_STORAGE_KEY = 'omaster_cloud_preset_cache';
const SYNC_META_KEY = 'omaster_cloud_preset_meta';
const SYNC_INTERVAL = 24 * 60 * 60 * 1000; // 24小时

/**
 * 云端预设同步服务
 * 解析云端JSON样张数据并提供本地缓存
 */
export class CloudPresetService {
  private cache: Map<string, CloudPreset> = new Map();
  private state: SyncState = {
    status: 'idle',
    newCount: 0,
    updatedCount: 0,
    lastSyncTime: 0,
  };

  /**
   * 初始化：从本地缓存加载
   */
  initialize(): void {
    try {
      const cached = localStorage.getItem(SYNC_STORAGE_KEY);
      const meta = localStorage.getItem(SYNC_META_KEY);
      if (cached) {
        const presets: CloudPreset[] = JSON.parse(cached);
        presets.forEach(p => this.cache.set(p.id, p));
      }
      if (meta) {
        const parsed = JSON.parse(meta);
        this.state.lastSyncTime = parsed.lastSyncTime || 0;
      }
    } catch (e) {
      console.warn('[CloudPreset] 缓存加载失败', e);
    }
    // 确保有本地基础数据
    this.mergeLocalData();
  }

  /**
   * 合并本地基础数据
   */
  private mergeLocalData(): void {
    localPresetData.forEach(p => {
      if (!this.cache.has(p.id)) {
        this.cache.set(p.id, p);
      }
    });
  }

  /**
   * 同步云端数据
   * 模拟CDN拉取JSON数据
   */
  async sync(force = false): Promise<SyncState> {
    if (this.state.status === 'syncing') return this.state;

    if (!force && !this.shouldSync()) {
      return this.state;
    }

    this.state = { ...this.state, status: 'syncing' };
    const before = this.cache.size;

    try {
      // 模拟从CDN拉取JSON
      // 真实环境应使用: fetch(CDN_URL).then(r => r.json())
      const response = await this.fetchFromCDN();
      
      let newCount = 0;
      let updatedCount = 0;

      response.presets.forEach(preset => {
        const existing = this.cache.get(preset.id);
        if (!existing) {
          newCount++;
        } else if (existing.build < preset.build) {
          updatedCount++;
        }
        this.cache.set(preset.id, preset);
      });

      this.state = {
        status: 'success',
        newCount,
        updatedCount,
        lastSyncTime: Date.now(),
      };

      this.persist();
      console.log(`[CloudPreset] 同步完成: +${newCount} 新增, ~${updatedCount} 更新 (共 ${this.cache.size} 条)`);
    } catch (e) {
      this.state = {
        status: 'error',
        newCount: 0,
        updatedCount: 0,
        lastSyncTime: Date.now(),
        errorMessage: e instanceof Error ? e.message : '同步失败',
      };
    }

    return this.state;
  }

  /**
   * 模拟从CDN拉取
   * 真实环境会替换为fetch调用
   */
  private async fetchFromCDN(): Promise<CloudPresetResponse> {
    return new Promise(resolve => {
      setTimeout(() => {
        // 模拟从云端获取的新预设
        const newPresets: CloudPreset[] = [
          {
            id: 'cloud_hncs_001',
            name: '哈苏深海蓝',
            author: '@OPPO影像实验室',
            coverPath: 'https://images.unsplash.com/photo-1505142468610-359e7d316be0?w=600&h=800&fit=crop',
            galleryImages: [
              'https://images.unsplash.com/photo-1505142468610-359e7d316be0?w=600&h=800&fit=crop',
            ],
            brand: 'oppo',
            category: 'pro',
            tags: ['哈苏', '蓝色', '风景', '自然'],
            description: '哈苏自然色彩解决方案，呈现深邃海洋蓝调',
            deviceModel: ['OPPO Find X8 Pro', 'OPPO Find X7 Ultra'],
            params: { saturation: 8, contrast: 12, brightness: -2, warmth: -10, sharpness: 18, highlights: -15, shadows: 10, clarity: 20, noiseReduction: 5, vignette: 0, skinSmooth: 0 },
            isHncs: true,
            isPinned: false,
            isFavorite: false,
            isNew: true,
            isSystem: true,
            downloadCount: 12580,
            favoriteCount: 3420,
            rating: 4.8,
            build: 3,
            createdAt: Date.now() - 86400000 * 2,
            updatedAt: Date.now(),
            version: '1.0.3',
          },
          {
            id: 'cloud_night_002',
            name: '蓝调时刻',
            author: '@城市夜行者',
            coverPath: 'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=600&h=800&fit=crop',
            galleryImages: [],
            brand: 'oppo',
            category: 'pro',
            tags: ['夜景', '蓝调', '城市', '霓虹'],
            description: '蓝调时刻城市夜景，保留高光与暗部细节',
            deviceModel: ['OPPO Find X8 Pro'],
            params: { saturation: 5, contrast: 18, brightness: -5, warmth: -15, sharpness: 25, highlights: -20, shadows: 15, clarity: 22, noiseReduction: 30, vignette: 15, skinSmooth: 0 },
            isHncs: false,
            isPinned: false,
            isFavorite: false,
            isNew: true,
            isSystem: false,
            downloadCount: 8920,
            favoriteCount: 2150,
            rating: 4.6,
            build: 2,
            createdAt: Date.now() - 86400000 * 5,
            updatedAt: Date.now(),
            version: '1.0.2',
          },
          {
            id: 'cloud_film_003',
            name: '富士NC胶片',
            author: '@胶片复兴社',
            coverPath: 'https://images.unsplash.com/photo-1502082553048-f009c37129b9?w=600&h=800&fit=crop',
            galleryImages: [],
            brand: 'oppo',
            category: 'auto',
            tags: ['胶片', '复古', '人文', '街拍'],
            description: '致敬富士NC胶片色彩，柔和绿调',
            deviceModel: ['OPPO Find X8 Pro', 'OPPO Find X7 Ultra', 'OnePlus 12'],
            params: { saturation: -8, contrast: 10, brightness: 3, warmth: 8, sharpness: 12, highlights: -10, shadows: 8, clarity: 15, noiseReduction: 0, vignette: 10, skinSmooth: 5 },
            isHncs: false,
            isPinned: false,
            isFavorite: false,
            isNew: true,
            isSystem: false,
            downloadCount: 15600,
            favoriteCount: 4280,
            rating: 4.9,
            build: 5,
            createdAt: Date.now() - 86400000 * 3,
            updatedAt: Date.now(),
            version: '1.0.5',
          },
          {
            id: 'cloud_portrait_004',
            name: '肤若凝脂',
            author: '@人像摄影师Leo',
            coverPath: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=600&h=800&fit=crop',
            galleryImages: [],
            brand: 'oppo',
            category: 'pro',
            tags: ['人像', '美肤', '自然', '柔美'],
            description: '专业人像调色，保留真实肤质',
            deviceModel: ['OPPO Find X8 Pro'],
            params: { saturation: 5, contrast: -3, brightness: 5, warmth: 12, sharpness: 8, highlights: -8, shadows: 5, clarity: 10, noiseReduction: 10, vignette: 0, skinSmooth: 30 },
            isHncs: false,
            isPinned: false,
            isFavorite: false,
            isNew: true,
            isSystem: false,
            downloadCount: 9870,
            favoriteCount: 2890,
            rating: 4.7,
            build: 4,
            createdAt: Date.now() - 86400000,
            updatedAt: Date.now(),
            version: '1.0.4',
          },
        ];

        resolve({
          version: 2,
          build: 10,
          total: newPresets.length,
          updatedAt: Date.now(),
          hasMore: false,
          presets: newPresets,
        });
      }, 1500);
    });
  }

  /**
   * 是否需要同步
   */
  shouldSync(): boolean {
    return Date.now() - this.state.lastSyncTime > SYNC_INTERVAL;
  }

  /**
   * 获取所有预设
   */
  getAll(): CloudPreset[] {
    return Array.from(this.cache.values());
  }

  /**
   * 收藏管理
   */
  toggleFavorite(id: string): boolean {
    const preset = this.cache.get(id);
    if (!preset) return false;
    const updated = { ...preset, isFavorite: !preset.isFavorite };
    this.cache.set(id, updated);
    this.persist();
    return updated.isFavorite;
  }

  /**
   * 置顶管理
   */
  togglePin(id: string): boolean {
    const preset = this.cache.get(id);
    if (!preset) return false;
    const updated = { ...preset, isPinned: !preset.isPinned };
    this.cache.set(id, updated);
    this.persist();
    return updated.isPinned;
  }

  /**
   * 获取收藏列表
   */
  getFavorites(): CloudPreset[] {
    return this.getAll().filter(p => p.isFavorite);
  }

  /**
   * 获取置顶列表
   */
  getPinned(): CloudPreset[] {
    return this.getAll().filter(p => p.isPinned);
  }

  /**
   * 按品牌筛选
   */
  getByBrand(brand: string): CloudPreset[] {
    return this.getAll().filter(p => p.brand === brand);
  }

  /**
   * 按标签筛选
   */
  getByTag(tag: string): CloudPreset[] {
    return this.getAll().filter(p => p.tags.includes(tag));
  }

  /**
   * 搜索
   */
  search(query: string): CloudPreset[] {
    const q = query.toLowerCase();
    return this.getAll().filter(p =>
      p.name.toLowerCase().includes(q) ||
      p.author.toLowerCase().includes(q) ||
      p.tags.some(t => t.toLowerCase().includes(q))
    );
  }

  /**
   * 持久化到本地
   */
  private persist(): void {
    try {
      const data = Array.from(this.cache.values());
      localStorage.setItem(SYNC_STORAGE_KEY, JSON.stringify(data));
      localStorage.setItem(SYNC_META_KEY, JSON.stringify({
        lastSyncTime: this.state.lastSyncTime,
      }));
    } catch (e) {
      console.warn('[CloudPreset] 持久化失败', e);
    }
  }

  /**
   * 获取当前状态
   */
  getState(): SyncState {
    return this.state;
  }
}

export const cloudPresetService = new CloudPresetService();
