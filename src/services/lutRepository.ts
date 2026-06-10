/**
 * LUT 资源服务层
 * Web端服务架构 - 对齐 Android Repository 模式
 */

import { MasterLUT, LUTCategory, LUTSortBy, DownloadProgress, Resource } from '../types/lut';

// ============================================
// 类型定义
// ============================================

export type { MasterLUT, LUTCategory, LUTSortBy, DownloadProgress, Resource };

// ============================================
// 本地存储管理
// ============================================

const STORAGE_KEYS = {
  CACHED_LUTS: 'omaster_lut_cache',
  FAVORITES: 'omaster_lut_favorites',
  DOWNLOADS: 'omaster_lut_downloads',
  RATINGS: 'omaster_lut_ratings',
};

/**
 * LUT 本地数据源
 */
class LUTLocalDataSource {
  private lutCache: MasterLUT[] = [];

  /**
   * 获取 LUT 列表
   */
  getLUTs(category: LUTCategory, query: string, sortBy: LUTSortBy): MasterLUT[] {
    let result = [...this.lutCache];

    // 分类过滤
    if (category !== 'all') {
      result = result.filter(lut => lut.category === category);
    }

    // 搜索过滤
    if (query) {
      const q = query.toLowerCase();
      result = result.filter(lut =>
        lut.name.toLowerCase().includes(q) ||
        lut.nameEn.toLowerCase().includes(q) ||
        lut.tags.some(tag => tag.toLowerCase().includes(q))
      );
    }

    // 排序
    switch (sortBy) {
      case 'downloads':
        result.sort((a, b) => b.downloads - a.downloads);
        break;
      case 'rating':
        result.sort((a, b) => b.rating - a.rating);
        break;
      case 'newest':
        result.sort((a, b) => b.createdAt.localeCompare(a.createdAt));
        break;
      case 'name':
        result.sort((a, b) => a.name.localeCompare(b.name));
        break;
    }

    return result;
  }

  /**
   * 缓存 LUT 列表
   */
  cacheLUTs(luts: MasterLUT[]): void {
    this.lutCache = luts;
    try {
      localStorage.setItem(STORAGE_KEYS.CACHED_LUTS, JSON.stringify(luts));
    } catch (_: unknown) {
      // 存储空间不足时忽略
    }
  }

  /**
   * 从持久化缓存加载
   */
  loadCachedLUTs(): MasterLUT[] {
    try {
      const cached = localStorage.getItem(STORAGE_KEYS.CACHED_LUTS);
      if (cached) {
        this.lutCache = JSON.parse(cached);
        return this.lutCache;
      }
    } catch (_: unknown) {
      // 解析失败时忽略
    }
    return [];
  }

  /**
   * 获取热门 LUT
   */
  getHotLUTs(): MasterLUT[] {
    return this.lutCache
      .filter(lut => lut.isHot)
      .sort((a, b) => b.downloads - a.downloads)
      .slice(0, 10);
  }

  /**
   * 获取新品 LUT
   */
  getNewLUTs(): MasterLUT[] {
    return this.lutCache
      .filter(lut => lut.isNew)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .slice(0, 10);
  }

  /**
   * 切换收藏状态
   */
  toggleFavorite(id: string): boolean {
    const favorites = this.getFavoriteIds();
    const isFavorite = favorites.has(id);
    
    if (isFavorite) {
      favorites.delete(id);
    } else {
      favorites.add(id);
    }
    
    try {
      localStorage.setItem(STORAGE_KEYS.FAVORITES, JSON.stringify([...favorites]));
    } catch (_: unknown) {
      // 存储失败时忽略
    }
    
    return !isFavorite;
  }

  /**
   * 获取收藏 ID 集合
   */
  getFavoriteIds(): Set<string> {
    try {
      const favorites = localStorage.getItem(STORAGE_KEYS.FAVORITES);
      if (favorites) {
        return new Set(JSON.parse(favorites));
      }
    } catch (_: unknown) {
      // 解析失败时忽略
    }
    return new Set();
  }

  /**
   * 获取收藏列表
   */
  getFavorites(): MasterLUT[] {
    const favoriteIds = this.getFavoriteIds();
    return this.lutCache.filter(lut => favoriteIds.has(lut.id));
  }

  /**
   * 记录下载
   */
  recordDownload(id: string): void {
    try {
      const downloads = this.getDownloadIds();
      downloads.add(id);
      localStorage.setItem(STORAGE_KEYS.DOWNLOADS, JSON.stringify([...downloads]));
    } catch (_: unknown) {
      // 存储失败时忽略
    }
  }

  /**
   * 获取已下载 ID 集合
   */
  getDownloadIds(): Set<string> {
    try {
      const downloads = localStorage.getItem(STORAGE_KEYS.DOWNLOADS);
      if (downloads) {
        return new Set(JSON.parse(downloads));
      }
    } catch (_: unknown) {
      // 解析失败时忽略
    }
    return new Set();
  }

  /**
   * 更新评分
   */
  updateRating(id: string, rating: number): void {
    try {
      const ratings = this.getAllRatings();
      ratings[id] = rating;
      localStorage.setItem(STORAGE_KEYS.RATINGS, JSON.stringify(ratings));
    } catch (_: unknown) {
      // 存储失败时忽略
    }
  }

  /**
   * 获取所有评分
   */
  private getAllRatings(): Record<string, number> {
    try {
      const ratings = localStorage.getItem(STORAGE_KEYS.RATINGS);
      if (ratings) {
        return JSON.parse(ratings);
      }
    } catch (_: unknown) {
      // 解析失败时忽略
    }
    return {};
  }

  /**
   * 获取评分
   */
  getRating(id: string): number {
    return this.getAllRatings()[id] || 0;
  }
}

// ============================================
// 远程数据源
// ============================================

const API_BASE_URL = 'https://api.omaster.app/v1';
const CDN_BASE_URL = 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main';

/**
 * LUT 远程数据源
 */
class LUTRemoteDataSource {
  /**
   * 获取 LUT 列表
   */
  async fetchLUTList(
    category: string,
    query: string,
    sortBy: string
  ): Promise<MasterLUT[]> {
    try {
      const params = new URLSearchParams();
      if (category !== 'all') params.append('category', category);
      if (query) params.append('q', query);
      if (sortBy) params.append('sort', sortBy);

      const response = await fetch(`${API_BASE_URL}/luts?${params}`);
      if (response.ok) {
        return await response.json();
      }
    } catch (_: unknown) {
      // 降级到 CDN
    }
    
    return this.fetchFromCDN();
  }

  /**
   * 从 CDN 获取静态数据
   */
  private async fetchFromCDN(): Promise<MasterLUT[]> {
    try {
      const response = await fetch(`${CDN_BASE_URL}/data/luts.json`);
      if (response.ok) {
        return await response.json();
      }
    } catch (_: unknown) {
      // 使用内置数据
    }
    
    return this.getDefaultLUTs();
  }

  /**
   * 下载 LUT 文件
   */
  async downloadLUTFile(
    url: string,
    onProgress?: (progress: DownloadProgress) => void
  ): Promise<Blob> {
    onProgress?.({ type: 'starting', lutId: '' });

    try {
      const response = await fetch(url);
      if (!response.ok) throw new Error('下载失败');

      const contentLength = parseInt(response.headers.get('content-length') || '0', 10);
      const reader = response.body?.getReader();
      
      if (!reader) {
        onProgress?.({ type: 'completed', lutId: '', blob: await response.blob() });
        return await response.blob();
      }

      const chunks: Uint8Array[] = [];
      let bytesDownloaded = 0;

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        chunks.push(value);
        bytesDownloaded += value.length;

        if (contentLength > 0) {
          onProgress?.({
            type: 'downloading',
            lutId: '',
            progress: bytesDownloaded / contentLength,
            bytesDownloaded,
            totalBytes: contentLength,
          });
        }
      }

      const blob = new Blob(chunks);
      onProgress?.({ type: 'completed', lutId: '', blob });
      return blob;
    } catch (e) {
      onProgress?.({ type: 'error', lutId: '', message: String(e) });
      throw e;
    }
  }

  /**
   * 提交评分
   */
  async submitRating(lutId: string, rating: number): Promise<boolean> {
    try {
      const response = await fetch(`${API_BASE_URL}/luts/${lutId}/rating`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ rating }),
      });
      return response.ok;
    } catch (_: unknown) {
      return false;
    }
  }

  /**
   * 获取默认 LUT 数据
   */
  private getDefaultLUTs(): MasterLUT[] {
    // 返回内置默认数据
    return [];
  }
}

// ============================================
// Repository
// ============================================

const localDataSource = new LUTLocalDataSource();
const remoteDataSource = new LUTRemoteDataSource();

/**
 * LUT Repository
 * 统一数据访问层
 */
export const LUTRepository = {
  /**
   * 获取 LUT 列表
   */
  async getLUTs(
    category: LUTCategory = 'all',
    query: string = '',
    sortBy: LUTSortBy = 'downloads'
  ): Promise<Resource<MasterLUT[]>> {
    // 1. 先返回缓存数据
    const cached = localDataSource.getLUTs(category, query, sortBy);

    // 2. 网络刷新
    try {
      const remote = await remoteDataSource.fetchLUTList(category, query, sortBy);
      localDataSource.cacheLUTs(remote);
      const merged = localDataSource.getLUTs(category, query, sortBy);
      return { type: 'success', data: merged };
    } catch (e) {
      if (cached.length > 0) {
        return { type: 'success', data: cached };
      }
      return { type: 'error', message: String(e) };
    }
  },

  /**
   * 获取热门 LUT
   */
  getHotLUTs(): MasterLUT[] {
    return localDataSource.getHotLUTs();
  },

  /**
   * 获取新品 LUT
   */
  getNewLUTs(): MasterLUT[] {
    return localDataSource.getNewLUTs();
  },

  /**
   * 下载 LUT
   */
  async downloadLUT(
    lut: MasterLUT,
    onProgress?: (progress: DownloadProgress) => void
  ): Promise<Blob> {
    const blob = await remoteDataSource.downloadLUTFile(lut.downloadUrl, onProgress);
    localDataSource.recordDownload(lut.id);
    return blob;
  },

  /**
   * 切换收藏
   */
  toggleFavorite(id: string): boolean {
    return localDataSource.toggleFavorite(id);
  },

  /**
   * 获取收藏列表
   */
  getFavorites(): MasterLUT[] {
    return localDataSource.getFavorites();
  },

  /**
   * 获取收藏 ID 集合
   */
  getFavoriteIds(): Set<string> {
    return localDataSource.getFavoriteIds();
  },

  /**
   * 获取已下载 ID 集合
   */
  getDownloadIds(): Set<string> {
    return localDataSource.getDownloadIds();
  },

  /**
   * 提交评分
   */
  async submitRating(lutId: string, rating: number): Promise<void> {
    localDataSource.updateRating(lutId, rating);
    await remoteDataSource.submitRating(lutId, rating);
  },

  /**
   * 初始化缓存
   */
  init(): void {
    localDataSource.loadCachedLUTs();
  },
};

// 初始化
LUTRepository.init();
