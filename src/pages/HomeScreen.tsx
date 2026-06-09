import React, { useState, useCallback, useEffect, useRef } from 'react';
import { useAppStore, homePresets } from '../store/appStore';
import { Heart, Search, RefreshCw, Sparkles, Crown, Download, Star, Filter } from 'lucide-react';
import { useFadeInUp, useStaggeredFadeIn, useHeartBounce, animationKeyframes } from '../hooks/useAnimations';

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

// Hero横幅组件 - 淡入动画
const HeroBanner: React.FC<{ onRefresh: () => void; refreshing: boolean }> = ({ onRefresh, refreshing }) => {
  const { style: heroStyle } = useFadeInUp(0, 400);
  
  return (
    <div style={heroStyle} className="px-4 pt-2 pb-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h1 className="text-xl font-bold text-white">OMaster</h1>
          <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-[9px] font-bold text-white">
            <Crown size={10} />
            <span>哈苏大师</span>
          </div>
        </div>
        <button
          onClick={onRefresh}
          disabled={refreshing}
          className="p-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <RefreshCw size={18} className={`text-white/70 ${refreshing ? 'animate-spin' : ''}`} />
        </button>
      </div>
    </div>
  );
};

// 搜索栏组件 - 淡入动画
const SearchBar: React.FC<{ searchQuery: string; setSearchQuery: (q: string) => void }> = ({ searchQuery, setSearchQuery }) => {
  const { style } = useFadeInUp(50, 400);
  
  return (
    <div style={style} className="px-4 pb-2">
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
  );
};

// Tab栏组件 - 带滑动指示器
const TabBar: React.FC<{
  selectedTab: number;
  setSelectedTab: (i: number) => void;
  getTabCount: (key: string) => number;
}> = ({ selectedTab, setSelectedTab, getTabCount }) => {
  const { style } = useFadeInUp(100, 400);
  const [indicatorPos, setIndicatorPos] = useState({ left: 8, width: 32 });
  const tabRefs = useRef<(HTMLButtonElement | null)[]>([]);

  // 更新指示器位置
  useEffect(() => {
    const tab = tabRefs.current[selectedTab];
    if (tab) {
      const rect = tab.getBoundingClientRect();
      setIndicatorPos({
        left: tab.offsetLeft + 8,
        width: rect.width - 16,
      });
    }
  }, [selectedTab]);

  return (
    <div style={style} className="px-4 pb-2">
      <div className="relative flex gap-1 overflow-x-auto scrollbar-hide border-b border-white/5">
        {tabs.map((tab, index) => {
          const count = getTabCount(tab.key);
          const isSelected = selectedTab === index;
          return (
            <button
              key={tab.key}
              ref={el => tabRefs.current[index] = el}
              onClick={() => setSelectedTab(index)}
              className={`flex-shrink-0 relative px-4 py-2.5 text-sm font-medium transition-colors duration-200 ${
                isSelected ? 'text-white' : 'text-white/50'
              }`}
            >
              <span className="flex items-center gap-1.5">
                <span>{tab.label}</span>
                {count > 0 && (
                  <span
                    className={`text-[10px] px-1.5 rounded-full transition-colors duration-200 ${
                      isSelected
                        ? 'bg-[#FF6B35]/20 text-[#FF6B35]'
                        : 'bg-white/5 text-white/40'
                    }`}
                  >
                    {count}
                  </span>
                )}
              </span>
            </button>
          );
        })}
        {/* 滑动指示器 */}
        <div
          className="absolute bottom-0 h-[3px] bg-[#FF6B35] rounded-t-full transition-all duration-300 ease-out"
          style={{
            left: indicatorPos.left,
            width: indicatorPos.width,
          }}
        />
      </div>
    </div>
  );
};

// 品牌过滤器组件 - 错开动画
const BrandFilter: React.FC<{
  activeBrand: string;
  setActiveBrand: (b: string) => void;
  sortBy: string;
  setSortBy: (s: 'newest' | 'popular' | 'rating') => void;
}> = ({ activeBrand, setActiveBrand, sortBy, setSortBy }) => {
  return (
    <div className="px-4 pb-2 flex items-center gap-2 overflow-x-auto scrollbar-hide">
      {brands.map((brand, index) => {
        const { style } = useStaggeredFadeIn(index, 150, 30);
        return (
          <button
            key={brand.key}
            style={style}
            onClick={() => setActiveBrand(brand.key)}
            className={`flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-200 ${
              activeBrand === brand.key
                ? 'bg-[#FF6B35] text-white'
                : 'bg-white/5 text-white/60'
            }`}
          >
            {brand.label}
          </button>
        );
      })}

      {/* Sort Dropdown */}
      <div className="flex-shrink-0 ml-auto flex items-center gap-1" style={useStaggeredFadeIn(brands.length, 150, 30).style}>
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
  );
};

// 预设卡片组件 - 带滚动渐入和心形弹跳动画
const PresetCard: React.FC<{
  preset: any;
  index: number;
  isFavorite: boolean;
  onToggleFavorite: (id: string) => void;
  onClick: () => void;
}> = ({ preset, index, isFavorite, onToggleFavorite, onClick }) => {
  const [isVisible, setIsVisible] = useState(false);
  const [hasAnimated, setHasAnimated] = useState(false);
  const [isHeartAnimating, setIsHeartAnimating] = useState(false);
  const [showFlash, setShowFlash] = useState(false);
  const cardRef = useRef<HTMLDivElement>(null);

  // 滚动可视区检测
  useEffect(() => {
    const element = cardRef.current;
    if (!element) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !hasAnimated) {
          setIsVisible(true);
          setHasAnimated(true);
          observer.unobserve(element);
        }
      },
      { threshold: 0.1 }
    );

    observer.observe(element);
    return () => observer.disconnect();
  }, [hasAnimated]);

  // 心形弹跳动画
  const handleFavoriteClick = useCallback((e: React.MouseEvent) => {
    e.stopPropagation();
    if (!isFavorite) {
      setIsHeartAnimating(true);
      setShowFlash(true);
      setTimeout(() => setIsHeartAnimating(false), 300);
      setTimeout(() => setShowFlash(false), 200);
    }
    onToggleFavorite(preset.id);
  }, [isFavorite, onToggleFavorite, preset.id]);

  const imageHeight = index % 3 === 0 ? 'aspect-[3/4]' : index % 3 === 1 ? 'aspect-square' : 'aspect-[4/5]';

  return (
    <div
      ref={cardRef}
      onClick={onClick}
      className={`group relative rounded-2xl overflow-hidden bg-[#1a1a1a] cursor-pointer transition-all duration-300 hover:scale-[1.02] hover:shadow-lg ${imageHeight}`}
      style={{
        opacity: isVisible ? 1 : 0,
        transform: isVisible ? 'translateY(0)' : 'translateY(20px)',
        transition: `opacity 400ms ease-out ${index * 30}ms, transform 400ms ease-out ${index * 30}ms`,
      }}
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

      {/* Favorite Button with Bounce Animation */}
      <button
        onClick={handleFavoriteClick}
        className={`absolute top-2 right-2 p-2 rounded-full bg-black/40 backdrop-blur-sm transition-all duration-200 hover:bg-black/60 z-20 ${
          showFlash ? 'ring-2 ring-[#FF6B35] ring-opacity-50' : ''
        }`}
        style={{
          transform: isHeartAnimating ? 'scale(1.3)' : 'scale(1)',
          transition: 'transform 150ms ease-out',
        }}
      >
        <Heart
          size={16}
          className={`transition-colors duration-200 ${
            isFavorite ? 'text-red-500 fill-red-500' : 'text-white/70'
          }`}
        />
        {/* 哈苏橙色闪光效果 */}
        {showFlash && (
          <div className="absolute inset-0 rounded-full bg-[#FF6B35]/30 animate-ping" />
        )}
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
  );
};

// 下拉刷新指示器
const PullToRefreshIndicator: React.FC<{ refreshing: boolean }> = ({ refreshing }) => {
  return (
    <div 
      className={`flex items-center justify-center py-2 transition-all duration-300 ${
        refreshing ? 'opacity-100' : 'opacity-0'
      }`}
    >
      <div className="relative">
        <RefreshCw size={20} className="text-[#FF6B35] animate-spin" />
        {/* 哈苏橙光圈旋转动画 */}
        {refreshing && (
          <div className="absolute inset-0 rounded-full border-2 border-transparent border-t-[#FF6B35] animate-spin" style={{ animationDuration: '0.8s' }} />
        )}
      </div>
    </div>
  );
};

const HomeScreen: React.FC = () => {
  const { selectedTab, setSelectedTab, presetSources, fetchedPresets, setFetchedPresets, setCurrentSubPage } = useAppStore();
  const [searchQuery, setSearchQuery] = useState('');
  const [activeBrand, setActiveBrand] = useState('all');
  const [sortBy, setSortBy] = useState<'newest' | 'popular' | 'rating'>('newest');
  const [refreshing, setRefreshing] = useState(false);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [isLoading, setIsLoading] = useState(false);
  
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

  const filteredPresets = getFilteredPresets();

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Hero Banner with Fade In Animation */}
      <HeroBanner onRefresh={handleRefresh} refreshing={refreshing} />

      {/* Search Bar with Fade In Animation */}
      <SearchBar searchQuery={searchQuery} setSearchQuery={setSearchQuery} />

      {/* Tab Bar with Sliding Indicator */}
      <TabBar selectedTab={selectedTab} setSelectedTab={setSelectedTab} getTabCount={getTabCount} />

      {/* Brand Filter with Staggered Animation */}
      <BrandFilter
        activeBrand={activeBrand}
        setActiveBrand={setActiveBrand}
        sortBy={sortBy}
        setSortBy={setSortBy}
      />

      {/* Pull to Refresh Indicator */}
      <PullToRefreshIndicator refreshing={refreshing} />

      {/* Preset Grid with Scroll Reveal Animation */}
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
              <PresetCard
                key={preset.id}
                preset={preset}
                index={index}
                isFavorite={favorites.has(preset.id)}
                onToggleFavorite={toggleFavorite}
                onClick={() => setCurrentSubPage('preset-detail')}
              />
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

      {/* Animation Styles */}
      <style>{`
        ${animationKeyframes}
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default HomeScreen;