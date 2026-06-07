import React, { useState, useMemo, useRef, useCallback } from 'react';
import { useAppStore } from '../store/appStore';
import { useCloudPresets } from '../hooks/useCloudPresets';
import { CloudPreset } from '../types/cloudPreset';
import { 
  Heart, Cloud, RefreshCw, Filter, Star, Download, Sparkles, Search,
  Camera, Palette, Droplets, Cpu, Image as ImageIcon, Sliders,
  ChevronRight, Zap, Crown, TrendingUp
} from 'lucide-react';

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'favorites', label: '收藏' },
  { key: 'hncs', label: '哈苏' },
  { key: 'new', label: '上新' },
];

const brands = [
  { key: 'all', label: '全部品牌' },
  { key: 'OPPO', label: 'OPPO' },
  { key: 'OnePlus', label: '一加' },
  { key: 'realme', label: '真我' },
  { key: 'vivo', label: 'vivo' },
  { key: 'Hasselblad', label: '哈苏' },
];

// 功能入口配置 - 参考 iCurrer/OMaster 首页设计
const featureEntries = [
  { id: 'ai-scene', title: 'AI场景', icon: Camera, color: '#4CAF50', route: 'ai-scene' as const },
  { id: 'ai-fine-tune', title: 'AI微调', icon: Palette, color: '#9C27B0', route: 'ai-fine-tune' as const },
  { id: 'watermark', title: '水印', icon: Droplets, color: '#00BCD4', route: 'watermark' as const },
  { id: 'smart-optimize', title: '优化', icon: Cpu, color: '#2196F3', route: 'smart-optimize' as const },
  { id: 'preset-manager', title: '预设', icon: ImageIcon, color: '#FF9800', route: 'preset-manager' as const },
  { id: 'param-adjust', title: '参数', icon: Sliders, color: '#E91E63', route: 'param-adjust' as const },
];

const HomeScreen: React.FC = () => {
  const { selectedTab, setSelectedTab, setAiParam, navigateToSubPage } = useAppStore();
  const { presets, state, loading, refresh, toggleFavorite } = useCloudPresets();
  const [refreshing, setRefreshing] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeBrand, setActiveBrand] = useState('all');
  const [sortBy, setSortBy] = useState<'newest' | 'popular' | 'rating'>('newest');
  const [pullDistance, setPullDistance] = useState(0);
  const scrollRef = useRef<HTMLDivElement>(null);

  // 下拉刷新
  const handleRefresh = useCallback(async () => {
    setRefreshing(true);
    await refresh(true);
    setTimeout(() => setRefreshing(false), 500);
  }, [refresh]);

  // 应用预设
  const handleApplyPreset = useCallback((preset: CloudPreset) => {
    setAiParam('saturation', preset.params.saturation);
    setAiParam('contrast', preset.params.contrast);
    setAiParam('brightness', preset.params.brightness);
    setAiParam('warmth', preset.params.warmth);
  }, [setAiParam]);

  // 功能入口点击
  const handleFeatureClick = useCallback((route: typeof featureEntries[0]['route']) => {
    navigateToSubPage(route);
  }, [navigateToSubPage]);

  // 过滤和排序
  const filteredPresets = useMemo(() => {
    let result = [...presets];
    
    // Tab过滤
    switch (tabs[selectedTab]?.key) {
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
  }, [presets, selectedTab, activeBrand, searchQuery, sortBy]);

  // 计算瀑布流高度 - 参考 iCurrer/OMaster 的 staggered grid
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

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header - 参考 iCurrer/OMaster 紧凑标题栏 */}
      <div className="px-4 pt-2 pb-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-white">OMaster</h1>
            <div className="px-2 py-0.5 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-[9px] font-bold text-white">
              2026
            </div>
          </div>
          <div className="flex items-center gap-2">
            <SyncIndicator />
            <button
              onClick={handleRefresh}
              disabled={refreshing}
              className="p-1.5 rounded-full hover:bg-white/10 transition-colors"
            >
              <RefreshCw size={16} className={`text-white/70 ${refreshing ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>
      </div>

      {/* 功能入口卡片行 - 参考 iCurrer/OMaster FeatureEntryRow */}
      <div className="px-4 py-2">
        <div className="grid grid-cols-6 gap-2">
          {featureEntries.map((feature) => {
            const Icon = feature.icon;
            return (
              <button
                key={feature.id}
                onClick={() => handleFeatureClick(feature.route)}
                className="group flex flex-col items-center justify-center py-3 rounded-xl transition-all hover:scale-105 active:scale-95"
                style={{ backgroundColor: `${feature.color}15` }}
              >
                <div 
                  className="w-8 h-8 rounded-lg flex items-center justify-center mb-1 transition-transform group-hover:scale-110"
                  style={{ backgroundColor: `${feature.color}25` }}
                >
                  <Icon size={16} style={{ color: feature.color }} />
                </div>
                <span 
                  className="text-[10px] font-medium"
                  style={{ color: feature.color }}
                >
                  {feature.title}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Search Bar */}
      <div className="px-4 pb-2">
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索预设 / 作者 / 标签"
            className="w-full pl-9 pr-4 py-2 rounded-xl bg-white/5 text-white text-sm border border-white/10 focus:border-[#FF6B35] outline-none transition-colors"
          />
        </div>
      </div>

      {/* Tab Bar - 参考 iCurrer/OMaster ScrollableTabRow */}
      <div className="px-4 pb-2">
        <div className="flex gap-1 overflow-x-auto scrollbar-hide">
          {tabs.map((tab, index) => {
            const count = tab.key === 'all' ? presets.length :
                          tab.key === 'favorites' ? presets.filter(p => p.isFavorite).length :
                          tab.key === 'hncs' ? presets.filter(p => p.isHncs).length :
                          presets.filter(p => p.isNew).length;
            return (
              <button
                key={tab.key}
                onClick={() => setSelectedTab(index)}
                className={`flex-shrink-0 relative px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                  selectedTab === index 
                    ? 'bg-[#FF6B35] text-white' 
                    : 'text-white/50 hover:text-white/70 hover:bg-white/5'
                }`}
              >
                <span>{tab.label}</span>
                {count > 0 && (
                  <span className={`ml-1 text-[10px] ${selectedTab === index ? 'text-white/80' : 'text-white/30'}`}>
                    {count}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Brand Filter */}
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

      {/* Pull to Refresh Indicator - 参考 iCurrer/OMaster */}
      {refreshing && (
        <div className="flex items-center justify-center py-2">
          <RefreshCw size={20} className="text-[#FF6B35] animate-spin" />
        </div>
      )}

      {/* Preset Grid - 瀑布流布局 */}
      <div 
        ref={scrollRef}
        className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide"
      >
        {loading && presets.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20">
            <RefreshCw size={32} className="text-[#FF6B35] animate-spin mb-3" />
            <p className="text-white/50 text-sm">加载云端样张中...</p>
          </div>
        ) : filteredPresets.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20">
            <Cloud size={32} className="text-white/20 mb-3" />
            <p className="text-white/50 text-sm mb-2">未找到匹配的预设</p>
            <p className="text-white/30 text-xs">下拉刷新或切换筛选条件</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-4">
            {filteredPresets.map((preset, index) => {
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

                  {/* HNCS Badge */}
                  {preset.isHncs && (
                    <div className="absolute top-2 left-2 px-1.5 py-0.5 bg-gradient-to-r from-[#FF6B35] to-[#FF9800] backdrop-blur-sm rounded text-[9px] font-bold text-white z-20 flex items-center gap-0.5">
                      <Sparkles size={8} />
                      <span>HNCS</span>
                    </div>
                  )}

                  {/* NEW Badge */}
                  {preset.isNew && !preset.isHncs && (
                    <div className="absolute top-2 left-2 px-1.5 py-0.5 bg-[#4CAF50] backdrop-blur-sm rounded text-[9px] font-bold text-white z-20">
                      NEW
                    </div>
                  )}

                  {/* Pinned Badge */}
                  {preset.isPinned && (
                    <div className="absolute top-2 right-2 px-1.5 py-0.5 bg-yellow-500/80 backdrop-blur-sm rounded text-[9px] font-bold text-white z-20">
                      📌
                    </div>
                  )}

                  {/* Favorite Button */}
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleFavorite(preset.id);
                    }}
                    className="absolute bottom-2 right-2 p-1.5 rounded-full bg-black/40 backdrop-blur-sm transition-all duration-200 hover:bg-black/60 z-20"
                  >
                    <Heart
                      size={14}
                      className={preset.isFavorite ? 'text-red-500 fill-red-500' : 'text-white/70'}
                    />
                  </button>

                  {/* Content */}
                  <div className="absolute bottom-0 left-0 right-0 p-3 pr-10">
                    <h3 className="text-white font-semibold text-sm mb-0.5 truncate">{preset.name}</h3>
                    <p className="text-white/60 text-xs truncate">{preset.author}</p>
                    
                    {/* Stats */}
                    <div className="flex items-center gap-2 mt-1">
                      <div className="flex items-center gap-0.5">
                        <Star size={9} className="text-yellow-400 fill-yellow-400" />
                        <span className="text-white/50 text-[10px]">{preset.rating.toFixed(1)}</span>
                      </div>
                      <div className="flex items-center gap-0.5">
                        <Download size={9} className="text-white/40" />
                        <span className="text-white/50 text-[10px]">
                          {preset.downloadCount > 10000 ? `${(preset.downloadCount / 10000).toFixed(1)}w` : preset.downloadCount}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Loading Hint - 参考 iCurrer/OMaster LoadingMoreTip */}
        <div className="py-8 text-center">
          <div className="w-16 h-0.5 mx-auto bg-gradient-to-r from-transparent via-[#FF6B35]/50 to-transparent mb-3" />
          <p className="text-[#FF6B35]/80 text-xs font-medium tracking-wider flex items-center justify-center gap-1.5">
            <TrendingUp size={12} />
            <span>持续更新 敬请期待</span>
          </p>
          <div className="w-16 h-0.5 mx-auto bg-gradient-to-r from-transparent via-[#FF6B35]/50 to-transparent mt-3" />
        </div>
      </div>
    </div>
  );
};

export default HomeScreen;
