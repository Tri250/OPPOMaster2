import React, { useState, useRef, useCallback, useEffect } from 'react';
import { useAppStore } from '../store/appStore';
import { useCloudPresets } from '../hooks/useCloudPresets';
import { CloudPreset } from '../types/cloudPreset';
import { fetchCloudPresets, CloudPreset as CloudPresetType } from '../services/presetCloudService';
import PresetDetailModal from '../components/PresetDetailModal';
import { fetchMergedPresets, RemotePreset } from '../services/remotePresetService';
import { 
  Heart, Cloud, RefreshCw, Filter, Star, Download, Sparkles, Search, TrendingUp,
  Camera, Sun, Moon, Award, Crown,
  Flame, Eye, Users, MapPin, Leaf, Sunset
} from 'lucide-react';

// Tab 配置 - 2026年小红书风格
const tabs = [
  { key: 'all', label: '发现', icon: Sparkles },
  { key: 'favorites', label: '收藏', icon: Heart },
  { key: 'hncs', label: '哈苏', icon: Crown },
  { key: 'new', label: '上新', icon: Flame },
  { key: 'follow', label: '关注', icon: Users },
];

// 2026年流行场景 - 小红书/微博博主风格
const hotScenes2026 = [
  { key: 'oxygen', label: '氧气感', icon: Leaf, color: '#87CEEB', hot: true },
  { key: 'morandi', label: '莫兰迪', icon: Sun, color: '#988B7E', hot: true },
  { key: 'sunset', label: '落日余晖', icon: Sunset, color: '#FF6B35', hot: false },
  { key: 'citynight', label: '城市夜景', icon: Moon, color: '#4DABF7', hot: true },
  { key: 'portrait', label: '人像写真', icon: Camera, color: '#FF6B6B', hot: false },
  { key: 'food', label: '美食探店', icon: Star, color: '#FFD700', hot: true },
  { key: 'travel', label: '旅行打卡', icon: MapPin, color: '#69DB7C', hot: false },
  { key: 'street', label: '街拍纪实', icon: Eye, color: '#9C27B0', hot: false },
];

// 2026年热门话题
const hotTopics2026 = [
  { tag: '#2026春日写真', views: '2.3亿', trend: '+28%' },
  { tag: '#哈苏大师模式', views: '1.8亿', trend: '+45%' },
  { tag: '#胶片感调色', views: '1.5亿', trend: '+32%' },
  { tag: '#人像摄影技巧', views: '1.2亿', trend: '+18%' },
];

const brands = [
  { key: 'all', label: '全部品牌' },
  { key: 'OPPO', label: 'OPPO' },
  { key: 'OnePlus', label: '一加' },
  { key: 'realme', label: '真我' },
  { key: 'vivo', label: 'vivo' },
  { key: 'Hasselblad', label: '哈苏' },
];

const HomeScreen: React.FC = () => {
  useAppStore(); // Keep store connected
  const { presets, state, loading, refresh, toggleFavorite } = useCloudPresets();
  
  // 当前选中的 Tab 索引（参考 OMaster selectedTab）
  const [selectedTab, setSelectedTab] = useState(0);
  const [refreshing, setRefreshing] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeBrand, setActiveBrand] = useState('all');
  const [sortBy, setSortBy] = useState<'newest' | 'popular' | 'rating'>('newest');
  
  // 样张详情弹窗状态
  const [selectedPreset, setSelectedPreset] = useState<CloudPresetType | null>(null);
  const [selectedRemotePreset, setSelectedRemotePreset] = useState<(RemotePreset & { brand?: string; brandName?: string }) | null>(null);
  const [cloudPresetsData, setCloudPresetsData] = useState<CloudPresetType[]>([]);
  
  // 远程预设数据状态
  const [remotePresets, setRemotePresets] = useState<Array<RemotePreset & { brand?: string; brandName?: string }>>([]);
  
  // 参考 OMaster：当前页面与 Pager 双向同步
  const [currentPage, setCurrentPage] = useState(0);
  const pagerRef = useRef<HTMLDivElement>(null);
  const tabRefs = useRef<(HTMLButtonElement | null)[]>([]);
  
  // 加载云同步数据
  useEffect(() => {
    fetchCloudPresets().then(setCloudPresetsData);
  }, []);

  // 加载远程预设数据
  useEffect(() => {
    const loadRemotePresets = async () => {
      try {
        const data = await fetchMergedPresets();
        setRemotePresets(data);
      } catch (error) {
        console.error('Failed to load remote presets:', error);
      }
    };
    loadRemotePresets();
  }, []);

  // Pager -> Tab 同步（参考 OMaster LaunchedEffect(pagerState.currentPage)）
  useEffect(() => {
    if (currentPage !== selectedTab) {
      setSelectedTab(currentPage);
    }
  }, [currentPage, selectedTab]);

  // Tab -> Pager 同步（参考 OMaster LaunchedEffect(selectedTab)）
  useEffect(() => {
    if (currentPage !== selectedTab) {
      setCurrentPage(selectedTab);
      // 滚动到对应页面
      if (pagerRef.current) {
        pagerRef.current.scrollTo({
          left: pagerRef.current.offsetWidth * selectedTab,
          behavior: 'smooth'
        });
      }
    }
  }, [selectedTab, currentPage]);

  // 下拉刷新
  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    await refresh();
    setTimeout(() => setRefreshing(false), 500);
  }, [refresh]);

  // 应用预设 - 点击样张显示详情弹窗
  const handleApplyPreset = useCallback((preset: CloudPreset) => {
    // 查找对应的云同步预设详情
    const cloudPreset = cloudPresetsData.find(p => p.name === preset.name);
    if (cloudPreset) {
      setSelectedPreset(cloudPreset);
    } else {
      // 如果没有找到，创建一个基础的详情
      setSelectedPreset({
        id: preset.id,
        name: preset.name,
        coverPath: preset.coverPath,
        author: preset.author,
        brand: preset.brand,
        tags: preset.tags,
        isNew: preset.isNew,
        isHncs: preset.isHncs,
        rating: preset.rating,
        downloadCount: preset.downloadCount,
        cameraParams: {
          saturation: preset.params.saturation,
          contrast: preset.params.contrast,
          brightness: preset.params.brightness,
          warmth: preset.params.warmth,
          sharpness: 15,
          clarity: 10,
          highlights: 0,
          shadows: 0,
          hue: 0,
          vibrance: 10,
        },
        updatedAt: new Date().toISOString(),
      });
    }
  }, [cloudPresetsData]);

  // 处理远程预设点击
  const handleRemotePresetClick = useCallback((preset: RemotePreset & { brand?: string; brandName?: string }) => {
    setSelectedRemotePreset(preset);
  }, []);

  // Pager 滚动处理
  const handlePagerScroll = useCallback(() => {
    if (!pagerRef.current) return;
    const page = Math.round(pagerRef.current.scrollLeft / pagerRef.current.offsetWidth);
    if (page !== currentPage) {
      setCurrentPage(page);
    }
  }, [currentPage]);

  // 计算每个 Tab 的计数（参考 OMaster Tab + 计数徽章）
  const getTabCount = useCallback((tabKey: string) => {
    switch (tabKey) {
      case 'all': return presets.length + remotePresets.length;
      case 'favorites': return presets.filter(p => p.isFavorite).length;
      case 'hncs': return presets.filter(p => p.isHncs).length;
      case 'new': return presets.filter(p => p.isNew).length + remotePresets.filter(p => p.isNew).length;
      default: return 0;
    }
  }, [presets, remotePresets]);

  // 过滤和排序（按 Tab 过滤）
  const getFilteredPresets = useCallback((tabKey: string) => {
    let result = [...presets];
    
    // Tab 过滤
    switch (tabKey) {
      case 'favorites':
        result = result.filter(p => p.isFavorite);
        break;
      case 'hncs':
        result = result.filter(p => p.isHncs);
        break;
      case 'new':
        result = result.filter(p => p.isNew);
        break;
    }

    // 品牌过滤
    if (activeBrand !== 'all') {
      result = result.filter(p => p.brand === activeBrand);
    }

    // 搜索过滤
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      result = result.filter(p =>
        p.name.toLowerCase().includes(q) ||
        p.author.toLowerCase().includes(q) ||
        p.tags.some(t => t.toLowerCase().includes(q))
      );
    }

    // 排序
    result.sort((a, b) => {
      switch (sortBy) {
        case 'newest': return b.updatedAt - a.updatedAt;
        case 'popular': return b.downloadCount - a.downloadCount;
        case 'rating': return b.rating - a.rating;
        default: return 0;
      }
    });

    // 置顶置顶
    return [
      ...result.filter(p => p.isPinned),
      ...result.filter(p => !p.isPinned),
    ];
  }, [presets, activeBrand, searchQuery, sortBy]);

  // 获取远程预设（过滤后）
  const getFilteredRemotePresets = useCallback(() => {
    let result = [...remotePresets];
    
    // 品牌过滤
    if (activeBrand !== 'all') {
      const brandLower = activeBrand.toLowerCase();
      result = result.filter(p => 
        p.brand?.toLowerCase() === brandLower || 
        p.brandName?.toLowerCase() === brandLower
      );
    }

    // 搜索过滤
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      result = result.filter(p =>
        p.name.toLowerCase().includes(q) ||
        p.author.toLowerCase().includes(q) ||
        p.tags?.some(t => t.toLowerCase().includes(q))
      );
    }

    return result;
  }, [remotePresets, activeBrand, searchQuery]);

  // 计算瀑布流高度 - 参考 OMaster 的 staggered grid
  const getImageHeight = (index: number) => {
    switch (index % 3) {
      case 0: return 'h-[220px]';
      case 1: return 'h-[180px]';
      default: return 'h-[260px]';
    }
  };

  // 同步状态指示器
  const SyncIndicator = () => {
    if (state.status === 'syncing' || refreshing) {
      return (
        <div className="flex items-center gap-1.5 text-xs text-white/60">
          <RefreshCw size={12} className="animate-spin" />
          <span>同步中...</span>
        </div>
      );
    }
    if (state.status === 'success' && state.lastSyncTime > 0) {
      return (
        <div className="flex items-center gap-1.5 text-xs text-white/60">
          <Cloud size={12} className="text-[#4CAF50]" />
          <span>云端 {state.newCount > 0 ? `+${state.newCount}` : '已同步'}</span>
        </div>
      );
    }
    return null;
  };

  // 渲染预设卡片（瀑布流中的单个项目）
  const renderPresetCard = (preset: CloudPreset, index: number) => {
    const heightClass = getImageHeight(index);
    return (
      <div
        key={preset.id}
        onClick={() => handleApplyPreset(preset)}
        className={`group relative rounded-2xl overflow-hidden bg-[#1a1a1a] cursor-pointer transition-all duration-300 hover:scale-[1.02] hover:shadow-lg ${heightClass}`}
      >
        {/* Glass Border Effect */}
        <div className="absolute inset-0 rounded-2xl border border-white/5 group-hover:border-white/10 transition-colors z-10 pointer-events-none" />
        
        {/* Image */}
        <img
          src={preset.coverPath}
          alt={preset.name}
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
          loading="lazy"
          style={{
            filter: `saturate(${100 + preset.params.saturation}%) contrast(${100 + preset.params.contrast}%) brightness(${100 + preset.params.brightness}%)`,
          }}
        />

        {/* Overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />

        {/* HNCS Badge - 哈苏自然色彩解决方案 */}
        {preset.isHncs && (
          <div className="absolute top-2 left-2 px-2 py-1 bg-gradient-to-r from-[#FF6B35] to-[#FF9800] backdrop-blur-sm rounded-lg text-[9px] font-bold text-white z-20 flex items-center gap-1 shadow-lg shadow-orange-500/30">
            <Crown size={10} />
            <span>HNCS 3.0</span>
          </div>
        )}

        {/* NEW Badge */}
        {preset.isNew && !preset.isHncs && (
          <div className="absolute top-2 left-2 px-2 py-1 bg-[#4CAF50] backdrop-blur-sm rounded-lg text-[9px] font-bold text-white z-20 flex items-center gap-1">
            <Sparkles size={10} />
            <span>NEW</span>
          </div>
        )}

        {/* Pinned Badge */}
        {preset.isPinned && (
          <div className="absolute top-2 right-2 px-2 py-1 bg-yellow-500/80 backdrop-blur-sm rounded-lg text-[9px] font-bold text-white z-20 flex items-center gap-1">
            <Award size={10} />
            <span>精选</span>
          </div>
        )}

        {/* Favorite Button */}
        <button
          onClick={(e) => {
            e.stopPropagation();
            toggleFavorite(preset.id);
          }}
          className="absolute bottom-2 right-2 p-2 rounded-full bg-black/40 backdrop-blur-sm transition-all duration-200 hover:bg-black/60 z-20"
        >
          <Heart
            size={16}
            className={preset.isFavorite ? 'text-red-500 fill-red-500' : 'text-white/70'}
          />
        </button>

        {/* Content */}
        <div className="absolute bottom-0 left-0 right-0 p-3 pr-12">
          <h3 className="text-white font-semibold text-sm mb-0.5 truncate">{preset.name}</h3>
          <p className="text-white/60 text-xs truncate">{preset.author}</p>
          
          {/* Stats */}
          <div className="flex items-center gap-3 mt-1.5">
            <div className="flex items-center gap-1">
              <Star size={10} className="text-yellow-400 fill-yellow-400" />
              <span className="text-white/50 text-[10px]">{preset.rating.toFixed(1)}</span>
            </div>
            <div className="flex items-center gap-1">
              <Download size={10} className="text-white/40" />
              <span className="text-white/50 text-[10px]">
                {preset.downloadCount > 10000 ? `${(preset.downloadCount / 10000).toFixed(1)}w` : preset.downloadCount}
              </span>
            </div>
            {/* 设备标识 */}
            {preset.brand && (
              <div className="flex items-center gap-1 ml-auto">
                <Camera size={10} className="text-white/40" />
                <span className="text-white/40 text-[10px]">{preset.brand}</span>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  // 渲染远程预设卡片
  const renderRemotePresetCard = (preset: RemotePreset & { brand?: string; brandName?: string }, index: number) => {
    const heightClass = getImageHeight(index);
    return (
      <div
        key={`remote-${preset.name}-${index}`}
        onClick={() => handleRemotePresetClick(preset)}
        className={`group relative rounded-2xl overflow-hidden bg-[#1a1a1a] cursor-pointer transition-all duration-300 hover:scale-[1.02] hover:shadow-lg ${heightClass}`}
      >
        {/* Glass Border Effect */}
        <div className="absolute inset-0 rounded-2xl border border-white/5 group-hover:border-white/10 transition-colors z-10 pointer-events-none" />
        
        {/* Image */}
        <img
          src={preset.coverPath}
          alt={preset.name}
          className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
          loading="lazy"
        />

        {/* Overlay */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />

        {/* Brand Badge */}
        {preset.brandName && (
          <div className="absolute top-2 left-2 px-2 py-1 bg-gradient-to-r from-blue-500 to-purple-500 backdrop-blur-sm rounded-lg text-[9px] font-bold text-white z-20 flex items-center gap-1">
            <Crown size={10} />
            <span>{preset.brandName}</span>
          </div>
        )}

        {/* NEW Badge */}
        {preset.isNew && (
          <div className="absolute top-2 right-2 px-2 py-1 bg-[#4CAF50] backdrop-blur-sm rounded-lg text-[9px] font-bold text-white z-20 flex items-center gap-1">
            <Sparkles size={10} />
            <span>NEW</span>
          </div>
        )}

        {/* Content */}
        <div className="absolute bottom-0 left-0 right-0 p-3">
          <h3 className="text-white font-semibold text-sm mb-0.5 truncate">{preset.name}</h3>
          <p className="text-white/60 text-xs truncate">{preset.author}</p>
          
          {/* Tags */}
          {preset.tags && preset.tags.length > 0 && (
            <div className="flex items-center gap-2 mt-1.5 overflow-hidden">
              {preset.tags.slice(0, 2).map((tag, idx) => (
                <span key={idx} className="text-white/40 text-[10px]">#{tag}</span>
              ))}
            </div>
          )}
        </div>
      </div>
    );
  };

  // 渲染瀑布流内容（合并本地和远程预设）
  const renderPresetGrid = (tabKey: string) => {
    const filteredPresets = getFilteredPresets(tabKey);
    const filteredRemotePresets = getFilteredRemotePresets();
    
    if (loading && presets.length === 0 && remotePresets.length === 0) {
      return (
        <div className="flex flex-col items-center justify-center py-20">
          <RefreshCw size={32} className="text-[#FF6B35] animate-spin mb-3" />
          <p className="text-white/50 text-sm">加载云端样张中...</p>
        </div>
      );
    }

    if (filteredPresets.length === 0 && filteredRemotePresets.length === 0) {
      return (
        <div className="flex flex-col items-center justify-center py-20">
          <Cloud size={32} className="text-white/20 mb-3" />
          <p className="text-white/50 text-sm mb-2">未找到匹配的预设</p>
          <p className="text-white/30 text-xs">下拉刷新或切换筛选条件</p>
        </div>
      );
    }

    // 合并预设列表（远程预设放在前面作为云端新增内容）
    const allPresets = [...filteredRemotePresets, ...filteredPresets];

    return (
      <div className="grid grid-cols-2 gap-4">
        {allPresets.map((preset, index) => {
          // 判断是远程预设还是本地预设
          if ('sections' in preset && 'coverPath' in preset) {
            // 远程预设
            return renderRemotePresetCard(preset as RemotePreset & { brand?: string; brandName?: string }, index);
          } else {
            // 本地预设
            return renderPresetCard(preset as CloudPreset, index);
          }
        })}
      </div>
    );
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header - OPPO哈苏大师模式风格 */}
      <div className="px-4 pt-2 pb-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-white">OMaster</h1>
            <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-[9px] font-bold text-white">
              <Crown size={10} />
              <span>哈苏大师</span>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <SyncIndicator />
            <button
              onClick={handleRefresh}
              disabled={refreshing}
              className="p-2 rounded-full hover:bg-white/10 transition-colors"
            >
              <RefreshCw size={18} className={`text-white/70 ${refreshing ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </div>

      {/* Search Bar - 小红书风格 */}
      <div className="px-4 pb-2">
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索预设 / 作者 / 话题 / 场景"
            className="w-full pl-9 pr-12 py-2.5 rounded-full bg-white/5 text-white text-sm border border-white/10 focus:border-[#FF6B35] outline-none transition-colors"
          />
          <button className="absolute right-2 top-1/2 -translate-y-1/2 p-1.5 rounded-lg bg-[#FF6B35]">
            <Camera size={14} className="text-white" />
          </button>
        </div>
      </div>

      {/* 2026热门场景 - 小红书风格横向滚动 */}
      <div className="px-4 pb-3">
        <div className="flex items-center gap-2 mb-2">
          <Flame size={14} className="text-[#FF6B35]" />
          <span className="text-white/70 text-xs font-medium">2026热门场景</span>
        </div>
        <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-1">
          {hotScenes2026.map((scene) => {
            const SceneIcon = scene.icon;
            return (
              <button
                key={scene.key}
                className="flex-shrink-0 flex items-center gap-2 px-3 py-2 rounded-xl bg-white/5 hover:bg-white/10 transition-all border border-white/5"
              >
                <div 
                  className="w-8 h-8 rounded-lg flex items-center justify-center"
                  style={{ backgroundColor: `${scene.color}20` }}
                >
                  <SceneIcon size={16} style={{ color: scene.color }} />
                </div>
                <div className="text-left">
                  <div className="flex items-center gap-1">
                    <span className="text-white text-xs font-medium">{scene.label}</span>
                    {scene.hot && (
                      <Flame size={10} className="text-[#FF6B35]" />
                    )}
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* 2026热门话题 - 微博博主风格 */}
      <div className="px-4 pb-3">
        <div className="flex items-center gap-2 mb-2">
          <TrendingUp size={14} className="text-blue-400" />
          <span className="text-white/70 text-xs font-medium">热门话题</span>
        </div>
        <div className="flex gap-2 overflow-x-auto scrollbar-hide">
          {hotTopics2026.map((topic, idx) => (
            <button
              key={idx}
              className="flex-shrink-0 px-3 py-1.5 rounded-full bg-gradient-to-r from-blue-500/10 to-purple-500/10 border border-blue-500/20 hover:from-blue-500/20 transition-all"
            >
              <span className="text-blue-400 text-xs font-medium">{topic.tag}</span>
              <span className="text-white/40 text-[10px] ml-1">{topic.views}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Tab Bar - 2026小红书风格 */}
      <div className="px-4 pb-2">
        <div className="flex gap-1 overflow-x-auto scrollbar-hide border-b border-white/5">
          {tabs.map((tab, index) => {
            const count = getTabCount(tab.key);
            const isSelected = selectedTab === index;
            const TabIcon = tab.icon;
            return (
              <button
                key={tab.key}
                ref={(el) => (tabRefs.current[index] = el)}
                onClick={() => setSelectedTab(index)}
                className={`flex-shrink-0 relative px-4 py-2.5 text-sm font-medium transition-all ${
                  isSelected ? 'text-white' : 'text-white/50'
                }`}
              >
                <span className="flex items-center gap-1.5">
                  <TabIcon size={14} className={isSelected ? 'text-[#FF6B35]' : ''} />
                  <span>{tab.label}</span>
                  {count > 0 && (
                    <span 
                      className={`text-[10px] px-1.5 rounded-full ${
                        isSelected 
                          ? 'bg-[#FF6B35]/20 text-[#FF6B35]' 
                          : 'bg-white/5 text-white/40'
                      }`}
                    >
                      {count}
                    </span>
                  )}
                </span>
                {isSelected && (
                  <div 
                    className="absolute bottom-0 left-2 right-2 h-[3px] bg-[#FF6B35] rounded-t-full"
                    style={{
                      animation: 'tabIndicator 200ms ease-out',
                    }}
                  />
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Brand Filter & Sort */}
      <div className="px-4 pb-2 flex items-center gap-2 overflow-x-auto scrollbar-hide">
        {brands.map((brand) => (
          <button
            key={brand.key}
            onClick={() => setActiveBrand(brand.key)}
            className={`flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
              activeBrand === brand.key
                ? 'bg-[#FF6B35] text-white'
                : 'bg-white/5 text-white/60'
            }`}
          >
            {brand.label}
          </button>
        ))}
        
        {/* Sort Dropdown */}
        <div className="flex-shrink-0 ml-auto flex items-center gap-1">
          <Filter size={12} className="text-white/40" />
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'newest' | 'popular' | 'rating')}
            className="bg-transparent text-white/60 text-xs outline-none cursor-pointer"
          >
            <option value="newest" className="bg-[#1a1a1a]">最新</option>
            <option value="popular" className="bg-[#1a1a1a]">最热</option>
            <option value="rating" className="bg-[#1a1a1a]">评分</option>
          </select>
        </div>
      </div>

      {/* Pull to Refresh Indicator */}
      {refreshing && (
        <div className="flex items-center justify-center py-2">
          <RefreshCw size={20} className="text-[#FF6B35] animate-spin" />
        </div>
      )}

      {/* HorizontalPager - 参考 OMaster HorizontalPager 模式 */}
      <div
        ref={pagerRef}
        onScroll={handlePagerScroll}
        className="flex-1 overflow-x-auto overflow-y-hidden snap-x snap-mandatory scrollbar-hide"
        style={{ scrollBehavior: 'smooth' }}
      >
        <div className="flex h-full" style={{ width: `${tabs.length * 100}%` }}>
          {tabs.map((tab) => (
            <div
              key={tab.key}
              className="snap-start snap-always h-full overflow-y-auto px-4 pb-4 scrollbar-hide"
              style={{ width: `${100 / tabs.length}%` }}
            >
              {renderPresetGrid(tab.key)}
              
              {/* Loading Hint - 参考 OMaster LoadingMoreTip */}
              {getFilteredPresets(tab.key).length > 0 && (
                <div className="py-8 text-center">
                  <div className="w-16 h-0.5 mx-auto bg-gradient-to-r from-transparent via-[#FF6B35]/50 to-transparent mb-3" />
                  <p className="text-[#FF6B35]/80 text-xs font-medium tracking-wider flex items-center justify-center gap-1.5">
                    <TrendingUp size={12} />
                    <span>持续更新 敬请期待</span>
                  </p>
                  <div className="w-16 h-0.5 mx-auto bg-gradient-to-r from-transparent via-[#FF6B35]/50 to-transparent mt-3" />
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Inject animation keyframes */}
      <style>{`
        @keyframes tabIndicator {
          from { transform: scaleX(0); opacity: 0; }
          to { transform: scaleX(1); opacity: 1; }
        }
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
        @keyframes slide-up {
          from { transform: translateY(100%); }
          to { transform: translateY(0); }
        }
        .animate-slide-up {
          animation: slide-up 0.3s ease-out;
        }
      `}</style>
      
      {/* 样张详情弹窗 */}
      {selectedPreset && (
        <PresetDetailModal 
          preset={selectedPreset as unknown as Parameters<typeof PresetDetailModal>[0]['preset']} 
          onClose={() => setSelectedPreset(null)} 
        />
      )}
      
      {/* 远程样张详情弹窗 */}
      {selectedRemotePreset && (
        <PresetDetailModal 
          preset={selectedRemotePreset} 
          onClose={() => setSelectedRemotePreset(null)} 
        />
      )}
    </div>
  );
};

export default HomeScreen;
