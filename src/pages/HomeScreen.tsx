import React, { useState, useCallback, useEffect } from 'react';
import { useAppStore, homePresets, Preset } from '../store/appStore';
import { Heart, Search, RefreshCw, Sparkles, Crown, Download, Star, Filter, X, SlidersHorizontal, Palette, Thermometer, Sun, Droplets } from 'lucide-react';

const tabs = [
  { key: 'all', label: '发现' },
  { key: 'favorites', label: '收藏' },
  { key: 'hncs', label: '哈苏' },
  { key: 'new', label: '上新' },
];

const brands = [
  { key: 'all', label: '全部' },
  { key: 'OPPO', label: 'OPPO' },
  { key: 'realme', label: '真我' },
  { key: 'vivo', label: 'vivo' },
  { key: '荣耀', label: '荣耀' },
  { key: '小米', label: '小米' },
];

const HomeScreen: React.FC = () => {
  const { selectedTab, setSelectedTab, presetSources, fetchedPresets, setFetchedPresets } = useAppStore();
  const [searchQuery, setSearchQuery] = useState('');
  const [activeBrand, setActiveBrand] = useState('all');
  const [sortBy, setSortBy] = useState<'newest' | 'popular' | 'rating'>('newest');
  const [refreshing, setRefreshing] = useState(false);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [isLoading, setIsLoading] = useState(false);
  const [selectedPreset, setSelectedPreset] = useState<Preset | null>(null);
  
  // 合并本地预设和网络获取的预设
  const allPresets = [...homePresets, ...fetchedPresets];

  // 从预设源获取预设
  const fetchPresetsFromSources = useCallback(async () => {
    setIsLoading(true);
    try {
      const allPresets: any[] = [];
      
      for (const source of presetSources) {
        if (!source.enabled) continue;
        
        try {
          const response = await fetch(source.url);
          if (response.ok) {
            const data = await response.json();
            const presets = (data.presets || data || []).map((p: any) => ({
              id: `${source.id}-${p.id || Date.now() + Math.random()}`,
              name: p.name || '未命名预设',
              coverPath: p.coverPath || p.cover || 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&h=500&fit=crop',
              author: p.author || source.name,
              brand: p.brand || source.name,
              tags: p.tags || ['预设'],
              isNew: true,
              isHncs: p.isHncs || false,
              saturation: p.saturation || 10,
              contrast: p.contrast || 5,
              warmth: p.warmth || 0,
              sharpness: p.sharpness || 15,
            }));
            allPresets.push(...presets);
          }
        } catch (err) {
          console.error(`Failed to fetch from ${source.name}:`, err);
        }
      }
      
      setFetchedPresets(allPresets);
    } catch (err) {
      console.error('Failed to fetch presets:', err);
    } finally {
      setIsLoading(false);
      setRefreshing(false);
    }
  }, [presetSources, setFetchedPresets]);

  // 下拉刷新
  const handleRefresh = useCallback(() => {
    setRefreshing(true);
    fetchPresetsFromSources();
  }, [fetchPresetsFromSources]);
  
  // 初始化加载
  useEffect(() => {
    fetchPresetsFromSources();
  }, []);

  // 切换收藏
  const toggleFavorite = useCallback((id: string) => {
    setFavorites(prev => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }, []);

  // 过滤和排序
  const getFilteredPresets = useCallback(() => {
    let result = [...allPresets];

    // Tab 过滤
    const tabKey = tabs[selectedTab]?.key;
    switch (tabKey) {
      case 'favorites':
        result = result.filter(p => favorites.has(p.id));
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
        case 'newest':
          return (b.isNew ? 1 : 0) - (a.isNew ? 1 : 0);
        case 'popular':
          return (b.clarity || 0) - (a.clarity || 0);
        case 'rating':
          return (b.sharpness || 0) - (a.sharpness || 0);
        default:
          return 0;
      }
    });

    return result;
  }, [selectedTab, activeBrand, searchQuery, sortBy, favorites, allPresets]);

  // 计算每个 Tab 的计数
  const getTabCount = useCallback((tabKey: string) => {
    switch (tabKey) {
      case 'all':
        return allPresets.length;
      case 'favorites':
        return favorites.size;
      case 'hncs':
        return allPresets.filter(p => p.isHncs).length;
      case 'new':
        return allPresets.filter(p => p.isNew).length;
      default:
        return 0;
    }
  }, [favorites, allPresets]);

  // 计算瀑布流高度
  const getImageHeight = (index: number) => {
    switch (index % 3) {
      case 0:
        return 'aspect-[3/4]';
      case 1:
        return 'aspect-square';
      default:
        return 'aspect-[4/5]';
    }
  };

  const filteredPresets = getFilteredPresets();

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header */}
      <div className="px-4 pt-2 pb-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-white">OMaster</h1>
            <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-[9px] font-bold text-white">
              <Crown size={10} />
              <span>哈苏大师</span>
            </div>
          </div>
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="p-2 rounded-full hover:bg-white/10 transition-colors"
          >
            <RefreshCw size={18} className={`text-white/70 ${refreshing ? 'animate-spin' : ''}`} />
          </button>
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
            className="w-full pl-9 pr-4 py-2.5 rounded-full bg-white/5 text-white text-sm border border-white/10 focus:border-[#FF6B35] outline-none transition-colors"
          />
        </div>
      </div>

      {/* Tab Bar */}
      <div className="px-4 pb-2">
        <div className="flex gap-1 overflow-x-auto scrollbar-hide border-b border-white/5">
          {tabs.map((tab, index) => {
            const count = getTabCount(tab.key);
            const isSelected = selectedTab === index;
            return (
              <button
                key={tab.key}
                onClick={() => setSelectedTab(index)}
                className={`flex-shrink-0 relative px-4 py-2.5 text-sm font-medium transition-all ${
                  isSelected ? 'text-white' : 'text-white/50'
                }`}
              >
                <span className="flex items-center gap-1.5">
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
                  <div className="absolute bottom-0 left-2 right-2 h-[3px] bg-[#FF6B35] rounded-t-full" />
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
            <option value="newest" className="bg-[#1a1a1a]">
              最新
            </option>
            <option value="popular" className="bg-[#1a1a1a]">
              最热
            </option>
            <option value="rating" className="bg-[#1a1a1a]">
              评分
            </option>
          </select>
        </div>
      </div>

      {/* Pull to Refresh Indicator */}
      {refreshing && (
        <div className="flex items-center justify-center py-2">
          <RefreshCw size={20} className="text-[#FF6B35] animate-spin" />
        </div>
      )}

      {/* Preset Grid */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide">
        {filteredPresets.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20">
            <Sparkles size={32} className="text-white/20 mb-3" />
            <p className="text-white/50 text-sm mb-2">未找到匹配的预设</p>
            <p className="text-white/30 text-xs">请调整筛选条件</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {filteredPresets.map((preset, index) => (
              <div
                key={preset.id}
                onClick={() => setSelectedPreset(preset)}
                className={`group relative rounded-2xl overflow-hidden bg-[#1a1a1a] cursor-pointer transition-all duration-300 hover:scale-[1.02] hover:shadow-lg ${getImageHeight(
                  index
                )}`}
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

                {/* HNCS Badge */}
                {preset.isHncs && (
                  <div className="absolute top-2 left-2 px-2 py-1 bg-gradient-to-r from-[#FF6B35] to-[#FF9800] backdrop-blur-sm rounded-lg text-[9px] font-bold text-white z-20 flex items-center gap-1">
                    <Crown size={10} />
                    <span>HNCS</span>
                  </div>
                )}

                {/* NEW Badge */}
                {preset.isNew && !preset.isHncs && (
                  <div className="absolute top-2 left-2 px-2 py-1 bg-[#4CAF50] backdrop-blur-sm rounded-lg text-[9px] font-bold text-white z-20 flex items-center gap-1">
                    <Sparkles size={10} />
                    <span>NEW</span>
                  </div>
                )}

                {/* Favorite Button */}
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    toggleFavorite(preset.id);
                  }}
                  className="absolute top-2 right-2 p-2 rounded-full bg-black/40 backdrop-blur-sm transition-all duration-200 hover:bg-black/60 z-20"
                >
                  <Heart
                    size={16}
                    className={favorites.has(preset.id) ? 'text-red-500 fill-red-500' : 'text-white/70'}
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
                      <span className="text-white/50 text-[10px]">4.{index + 5}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Download size={10} className="text-white/40" />
                      <span className="text-white/50 text-[10px]">{(index + 1) * 2.3}w</span>
                    </div>
                    {preset.brand && (
                      <div className="flex items-center gap-1 ml-auto">
                        <span className="text-white/40 text-[10px]">{preset.brand}</span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Loading Hint */}
        {filteredPresets.length > 0 && (
          <div className="py-8 text-center">
            <div className="w-16 h-0.5 mx-auto bg-gradient-to-r from-transparent via-[#FF6B35]/50 to-transparent mb-3" />
            <p className="text-[#FF6B35]/80 text-xs font-medium tracking-wider">持续更新 敬请期待</p>
            <div className="w-16 h-0.5 mx-auto bg-gradient-to-r from-transparent via-[#FF6B35]/50 to-transparent mt-3" />
          </div>
        )}
      </div>

      {/* Preset Detail Modal */}
      {selectedPreset && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm"
          onClick={() => setSelectedPreset(null)}
        >
          <div
            className="relative w-full max-w-sm bg-[#1a1a1a] rounded-3xl overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Close Button */}
            <button
              onClick={() => setSelectedPreset(null)}
              className="absolute top-3 right-3 z-10 p-2 rounded-full bg-black/50 backdrop-blur-sm transition-colors hover:bg-black/70"
            >
              <X size={18} className="text-white" />
            </button>

            {/* Preset Image */}
            <div className="relative aspect-[4/3]">
              <img
                src={selectedPreset.coverPath}
                alt={selectedPreset.name}
                className="w-full h-full object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#1a1a1a] via-transparent to-transparent" />

              {/* Badges */}
              <div className="absolute top-3 left-3 flex gap-2">
                {selectedPreset.isHncs && (
                  <div className="px-2 py-1 bg-gradient-to-r from-[#FF6B35] to-[#FF9800] rounded-lg text-[10px] font-bold text-white flex items-center gap-1">
                    <Crown size={10} />
                    <span>HNCS</span>
                  </div>
                )}
                {selectedPreset.isNew && !selectedPreset.isHncs && (
                  <div className="px-2 py-1 bg-[#4CAF50] rounded-lg text-[10px] font-bold text-white flex items-center gap-1">
                    <Sparkles size={10} />
                    <span>NEW</span>
                  </div>
                )}
              </div>

              {/* Preset Info Overlay */}
              <div className="absolute bottom-0 left-0 right-0 p-4">
                <h2 className="text-xl font-bold text-white mb-1">{selectedPreset.name}</h2>
                <p className="text-white/70 text-sm">{selectedPreset.author}</p>
                <div className="flex items-center gap-2 mt-2">
                  <span className="px-2 py-0.5 bg-white/10 rounded text-[10px] text-white/60">
                    {selectedPreset.brand}
                  </span>
                  {selectedPreset.tags.map((tag) => (
                    <span
                      key={tag}
                      className="px-2 py-0.5 bg-white/10 rounded text-[10px] text-white/60"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
            </div>

            {/* Parameters Section */}
            <div className="p-4">
              <h3 className="text-sm font-semibold text-white/90 mb-3 flex items-center gap-2">
                <SlidersHorizontal size={14} className="text-[#FF6B35]" />
                参数调节
              </h3>

              <div className="space-y-3">
                {/* Saturation */}
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-[#FF6B35]/10 flex items-center justify-center">
                    <Palette size={14} className="text-[#FF6B35]" />
                  </div>
                  <div className="flex-1">
                    <div className="flex justify-between mb-1">
                      <span className="text-xs text-white/60">饱和度</span>
                      <span className="text-xs text-white/90">{selectedPreset.saturation}</span>
                    </div>
                    <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] rounded-full transition-all"
                        style={{ width: `${Math.max(0, Math.min(100, (selectedPreset.saturation + 100) / 2))}%` }}
                      />
                    </div>
                  </div>
                </div>

                {/* Contrast */}
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-[#2196F3]/10 flex items-center justify-center">
                    <Sun size={14} className="text-[#2196F3]" />
                  </div>
                  <div className="flex-1">
                    <div className="flex justify-between mb-1">
                      <span className="text-xs text-white/60">对比度</span>
                      <span className="text-xs text-white/90">{selectedPreset.contrast}</span>
                    </div>
                    <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-[#2196F3] to-[#64B5F6] rounded-full transition-all"
                        style={{ width: `${Math.max(0, Math.min(100, (selectedPreset.contrast + 100) / 2))}%` }}
                      />
                    </div>
                  </div>
                </div>

                {/* Warmth */}
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-[#FF9800]/10 flex items-center justify-center">
                    <Thermometer size={14} className="text-[#FF9800]" />
                  </div>
                  <div className="flex-1">
                    <div className="flex justify-between mb-1">
                      <span className="text-xs text-white/60">色温</span>
                      <span className="text-xs text-white/90">{selectedPreset.warmth}</span>
                    </div>
                    <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-[#FF9800] to-[#FFC107] rounded-full transition-all"
                        style={{ width: `${Math.max(0, Math.min(100, (selectedPreset.warmth + 100) / 2))}%` }}
                      />
                    </div>
                  </div>
                </div>

                {/* Sharpness */}
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-lg bg-[#9C27B0]/10 flex items-center justify-center">
                    <Droplets size={14} className="text-[#9C27B0]" />
                  </div>
                  <div className="flex-1">
                    <div className="flex justify-between mb-1">
                      <span className="text-xs text-white/60">锐度</span>
                      <span className="text-xs text-white/90">{selectedPreset.sharpness}</span>
                    </div>
                    <div className="h-1.5 bg-white/10 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-gradient-to-r from-[#9C27B0] to-[#CE93D8] rounded-full transition-all"
                        style={{ width: `${Math.max(0, Math.min(100, selectedPreset.sharpness * 2))}%` }}
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex gap-3 mt-5">
                <button
                  onClick={() => toggleFavorite(selectedPreset.id)}
                  className={`flex-1 py-3 rounded-xl font-medium text-sm transition-all ${
                    favorites.has(selectedPreset.id)
                      ? 'bg-red-500/20 text-red-400 border border-red-500/30'
                      : 'bg-white/5 text-white/80 border border-white/10 hover:bg-white/10'
                  }`}
                >
                  {favorites.has(selectedPreset.id) ? '已收藏' : '收藏'}
                </button>
                <button className="flex-1 py-3 rounded-xl font-medium text-sm bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-white hover:opacity-90 transition-opacity">
                  应用预设
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Styles */}
      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default HomeScreen;
