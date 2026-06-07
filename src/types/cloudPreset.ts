// 云端预设数据类型定义
// 用于解析云同步返回的JSON样张数据

export interface CloudPreset {
  id: string;                          // 预设唯一标识
  name: string;                        // 预设名称
  author: string;                      // 创作者
  coverPath: string;                   // 样张封面图URL
  galleryImages: string[];             // 样张展示图列表
  brand: string;                       // 适配品牌 (oppo, realme, vivo, honor, oneplus)
  category: string;                    // 分类 (auto/pro)
  tags: string[];                      // 标签 (人像/风景/夜景/胶片/哈苏等)
  description: string;                 // 描述说明
  deviceModel: string[];               // 适配机型列表
  params: {                            // 调色参数
    saturation: number;
    contrast: number;
    brightness: number;
    warmth: number;
    sharpness: number;
    highlights: number;
    shadows: number;
    clarity: number;
    noiseReduction: number;
    vignette: number;
    skinSmooth: number;
  };
  isHncs: boolean;                     // 是否哈苏色彩认证
  isPinned: boolean;                   // 是否置顶
  isFavorite: boolean;                 // 是否收藏
  isNew: boolean;                      // 是否新上(7天内)
  isSystem: boolean;                   // 是否系统预设
  downloadCount: number;               // 下载量
  favoriteCount: number;               // 收藏数
  rating: number;                      // 评分(0-5)
  build: number;                       // 版本号
  createdAt: number;                   // 创建时间戳
  updatedAt: number;                   // 更新时间戳
  version: string;                     // 语义化版本
}

export interface CloudPresetResponse {
  version: number;                     // 协议版本
  build: number;                       // 构建号
  total: number;                       // 总数
  updatedAt: number;                   // 数据更新时间
  hasMore: boolean;                    // 是否有更多
  presets: CloudPreset[];              // 预设列表
}

export interface PresetFilter {
  brands: string[];                    // 品牌筛选
  tags: string[];                      // 标签筛选
  isHncsOnly: boolean;                 // 仅哈苏
  isNewOnly: boolean;                  // 仅新上
  searchQuery: string;                 // 搜索关键词
  sortBy: 'newest' | 'popular' | 'rating' | 'downloads'; // 排序方式
}

export interface SyncState {
  status: 'idle' | 'syncing' | 'success' | 'error';
  newCount: number;
  updatedCount: number;
  lastSyncTime: number;
  errorMessage?: string;
}
