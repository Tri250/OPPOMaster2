/**
 * Web端LUT服务层
 * 对应Android端LUTRepository架构
 */

import {
  MasterLUT,
  LUTCategory,
  LUTSortBy,
  DownloadProgress,
  DownloadState,
  Resource,
} from '../models/MasterLUT';
import { MASTER_LUT_RESOURCES, getLUTResources, searchLUTResources } from './lutResourceService';

// ===== 类型定义 =====

export interface LUTFilterOptions {
  category?: LUTCategory | string;
  query?: string;
  sortBy?: LUTSortBy;
}

export interface LUTDownloadOptions {
  lutId: string;
  onProgress?: (progress: DownloadProgress) => void;
  onComplete?: (filePath: string) => void;
  onError?: (error: string) => void;
}

// ===== 本地存储键 =====

const STORAGE_KEYS = {
  FAVORITES: 'lut_favorites',
  DOWNLOADS: 'lut_downloads',
  RATINGS: 'lut_ratings',
};

// ===== LUT Repository (Web端) =====

class LUTRepositoryWeb {
  private favorites: Set<string> = new Set();
  private downloads: Map<string, DownloadState> = new Map();
  private ratings: Map<string, number> = new Map();

  constructor() {
    this.loadFromStorage();
  }

  // ===== 数据获取 =====

  /**
   * 获取LUT列表（缓存优先）
   */
  async getLUTs(options: LUTFilterOptions = {}): Promise<Resource<MasterLUT[]>> {
    try {
      const { category, query, sortBy } = options;
      
      // 获取数据
      let luts = query 
        ? searchLUTResources(query)
        : getLUTResources(category);
      
      // 排序
      luts = this.sortLUTs(luts, sortBy || LUTSortBy.DOWNLOADS);
      
      return { data: luts, message: null };
    } catch (e) {
      return { data: null, message: e instanceof Error ? e.message : '加载失败' };
    }
  }

  /**
   * 获取单个LUT
   */
  async getLUTById(id: string): Promise<MasterLUT | null> {
    return MASTER_LUT_RESOURCES.find(lut => lut.id === id) || null;
  }

  /**
   * 获取热门LUT
   */
  async getHotLUTs(): Promise<MasterLUT[]> {
    return MASTER_LUT_RESOURCES.filter(lut => lut.isHot)
      .sort((a, b) => (b.downloads || 0) - (a.downloads || 0));
  }

  /**
   * 获取新品LUT
   */
  async getNewLUTs(): Promise<MasterLUT[]> {
    return MASTER_LUT_RESOURCES.filter(lut => lut.isNew);
  }

  /**
   * 获取精选LUT
   */
  async getFeaturedLUTs(): Promise<MasterLUT[]> {
    return MASTER_LUT_RESOURCES.filter(lut => lut.isFeatured);
  }

  /**
   * 搜索LUT
   */
  async searchLUTs(query: string): Promise<MasterLUT[]> {
    return searchLUTResources(query);
  }

  /**
   * 按系列获取LUT
   */
  async getLUTsByCollection(collection: string): Promise<MasterLUT[]> {
    return MASTER_LUT_RESOURCES.filter(lut => lut.hasselbladCollection === collection);
  }

  /**
   * 获取HNCS认证LUT
   */
  async getHncsCertifiedLUTs(): Promise<MasterLUT[]> {
    return MASTER_LUT_RESOURCES.filter(lut => lut.isHncsCertified);
  }

  // ===== 下载管理 =====

  /**
   * 下载LUT（模拟）
   */
  async downloadLUT(options: LUTDownloadOptions): Promise<void> {
    const { lutId, onProgress, onComplete, onError } = options;
    const lut = await this.getLUTById(lutId);
    
    if (!lut) {
      onError?.('LUT不存在');
      return;
    }

    // 模拟下载进度
    onProgress?.({ type: 'starting', lutId });
    
    try {
      // 实际项目中这里应该调用fetch下载文件
      // 这里模拟下载过程
      for (let i = 0; i <= 100; i += 10) {
        await new Promise(resolve => setTimeout(resolve, 100));
        onProgress?.({
          type: 'downloading',
          lutId,
          progress: i,
          bytesDownloaded: Math.floor(lut.fileSize * i / 100),
          totalBytes: lut.fileSize,
        });
      }

      // 记录下载完成
      const filePath = `/downloads/${lutId}.cube`;
      this.downloads.set(lutId, {
        lutId,
        isCompleted: true,
        filePath,
        downloadedAt: Date.now(),
      });
      this.saveToStorage();
      
      onComplete?.(filePath);
    } catch (e) {
      onError?.(e instanceof Error ? e.message : '下载失败');
    }
  }

  /**
   * 获取已下载的LUT列表
   */
  async getDownloadedLUTs(): Promise<MasterLUT[]> {
    const downloadedIds = Array.from(this.downloads.keys());
    return MASTER_LUT_RESOURCES.filter(lut => downloadedIds.includes(lut.id));
  }

  /**
   * 删除下载记录
   */
  async deleteDownloadedLUT(lutId: string): Promise<void> {
    this.downloads.delete(lutId);
    this.saveToStorage();
  }

  /**
   * 检查是否已下载
   */
  isDownloaded(lutId: string): boolean {
    return this.downloads.has(lutId) && this.downloads.get(lutId)?.isCompleted;
  }

  /**
   * 获取下载状态
   */
  getDownloadState(lutId: string): DownloadState | undefined {
    return this.downloads.get(lutId);
  }

  // ===== 收藏管理 =====

  /**
   * 切换收藏
   */
  toggleFavorite(lutId: string): void {
    if (this.favorites.has(lutId)) {
      this.favorites.delete(lutId);
    } else {
      this.favorites.add(lutId);
    }
    this.saveToStorage();
  }

  /**
   * 获取收藏列表
   */
  async getFavorites(): Promise<MasterLUT[]> {
    const favoriteIds = Array.from(this.favorites);
    return MASTER_LUT_RESOURCES.filter(lut => favoriteIds.includes(lut.id));
  }

  /**
   * 检查是否收藏
   */
  isFavorite(lutId: string): boolean {
    return this.favorites.has(lutId);
  }

  // ===== 评分管理 =====

  /**
   * 提交评分
   */
  submitRating(lutId: string, rating: number): void {
    this.ratings.set(lutId, rating);
    this.saveToStorage();
  }

  /**
   * 获取用户评分
   */
  getUserRating(lutId: string): number | undefined {
    return this.ratings.get(lutId);
  }

  // ===== 排序 =====

  private sortLUTs(luts: MasterLUT[], sortBy: LUTSortBy): MasterLUT[] {
    const sorted = [...luts];
    switch (sortBy) {
      case LUTSortBy.DOWNLOADS:
        sorted.sort((a, b) => (b.downloads || 0) - (a.downloads || 0));
        break;
      case LUTSortBy.RATING:
        sorted.sort((a, b) => (b.rating || 0) - (a.rating || 0));
        break;
      case LUTSortBy.NEWEST:
        sorted.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        break;
      case LUTSortBy.NAME:
        sorted.sort((a, b) => a.name.localeCompare(b.name));
        break;
    }
    return sorted;
  }

  // ===== 本地存储 =====

  private loadFromStorage(): void {
    try {
      // 加载收藏
      const favoritesStr = localStorage.getItem(STORAGE_KEYS.FAVORITES);
      if (favoritesStr) {
        this.favorites = new Set(JSON.parse(favoritesStr));
      }

      // 加载下载记录
      const downloadsStr = localStorage.getItem(STORAGE_KEYS.DOWNLOADS);
      if (downloadsStr) {
        const downloadsObj = JSON.parse(downloadsStr);
        this.downloads = new Map(Object.entries(downloadsObj));
      }

      // 加载评分
      const ratingsStr = localStorage.getItem(STORAGE_KEYS.RATINGS);
      if (ratingsStr) {
        const ratingsObj = JSON.parse(ratingsStr);
        this.ratings = new Map(Object.entries(ratingsObj));
      }
    } catch (e) {
      console.error('Failed to load from storage:', e);
    }
  }

  private saveToStorage(): void {
    try {
      localStorage.setItem(STORAGE_KEYS.FAVORITES, JSON.stringify(Array.from(this.favorites)));
      localStorage.setItem(STORAGE_KEYS.DOWNLOADS, JSON.stringify(Object.fromEntries(this.downloads)));
      localStorage.setItem(STORAGE_KEYS.RATINGS, JSON.stringify(Object.fromEntries(this.ratings)));
    } catch (e) {
      console.error('Failed to save to storage:', e);
    }
  }
}

// ===== 导出单例 =====

export const lutRepository = new LUTRepositoryWeb();

// ===== 导出便捷方法 =====

export const getLUTs = (options?: LUTFilterOptions) => lutRepository.getLUTs(options);
export const getLUTById = (id: string) => lutRepository.getLUTById(id);
export const getHotLUTs = () => lutRepository.getHotLUTs();
export const getNewLUTs = () => lutRepository.getNewLUTs();
export const getFeaturedLUTs = () => lutRepository.getFeaturedLUTs();
export const searchLUTs = (query: string) => lutRepository.searchLUTs(query);
export const downloadLUT = (options: LUTDownloadOptions) => lutRepository.downloadLUT(options);
export const getDownloadedLUTs = () => lutRepository.getDownloadedLUTs();
export const toggleFavorite = (lutId: string) => lutRepository.toggleFavorite(lutId);
export const getFavorites = () => lutRepository.getFavorites();
export const isFavorite = (lutId: string) => lutRepository.isFavorite(lutId);
export const isDownloaded = (lutId: string) => lutRepository.isDownloaded(lutId);
export const submitRating = (lutId: string, rating: number) => lutRepository.submitRating(lutId, rating);
export const getUserRating = (lutId: string) => lutRepository.getUserRating(lutId);