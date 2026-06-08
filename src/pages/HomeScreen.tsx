import React, { useState, useCallback, useEffect } from 'react';
import { useAppStore, homePresets } from '../store/appStore';
import { Heart, Search, RefreshCw, Sparkles, Crown, Download, Star, Filter } from 'lucide-react';

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
    <div 
      className="h-full flex flex-col overflow-hidden animate-liquid-fade"
      style={{ background: 'var(--color-bg-primary)' }}
    >
      {/* Header - 液态玻璃效果 */}
      <div className="px-4 pt-3 pb-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h1 
              className="text-xl font-bold"
              style={{ color: 'var(--color-text-primary)' }}
            >
              小O帮帮
            </h1>
            <div 
              className="flex items-center gap-1 px-2 py-0.5 rounded-full text-[9px] font-bold text-white animate-liquid-pulse"
              style={{
                background: 'linear-gradient(135deg, var(--color-accent-primary) 0%, #FF9800 100%)',
                boxShadow: '0 2px 8px rgba(255, 107, 53, 0.3)'
              }}
            >
              <Crown size={10} />
              <span>哈苏大师</span>
            </div>
          </div>
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            aria-label="刷新预设列表"
            className="p-2 rounded-full glass-button transition-spring-soft"
          >
            <RefreshCw 
              size={18} 
              style={{ 
                color: 'var(--color-text-secondary)',
                animation: refreshing ? 'liquid-spin 1s linear infinite' : 'none'
              }} 
            />
          </button>
        </div>
      </div>

      {/* Search Bar - 液态玻璃输入框 */}
      <div className="px-4 pb-2">
        <div className="relative">
          <Search 
            size={16} 
            className="absolute left-3 top-1/2 -translate-y-1/2"
            style={{ color: 'var(--color-text-muted)' }}
          />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索预设 / 作者 / 标签"
            aria-label="搜索预设"
            className="w-full pl-9 pr-4 py-2.5 rounded-full glass-input transition-smooth"
          />
        </div>
      </div>

      {/* Tab Bar - 液态玻璃导航 */}
      <div className="px-4 pb-2">
        <div 
          className="flex gap-1 overflow-x-auto scrollbar-hide"
          style={{ borderBottom: '1px solid var(--color-border-light)' }}
          role="tablist"
          aria-label="预设分类标签"
        >
          {tabs.map((tab, index) => {
            const count = getTabCount(tab.key);
            const isSelected = selectedTab === index;
            return (
              <button
                key={tab.key}
                onClick={() => setSelectedTab(index)}
                role="tab"
                aria-selected={isSelected}
                aria-controls={`tabpanel-${tab.key}`}
                className={`flex-shrink-0 relative px-4 py-2.5 text-sm font-medium transition-liquid focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#FF6B35] rounded-lg ${
                  isSelected ? 'text-white' : 'text-white/50 hover:text-white/70'
                }`}
              >
                <span className="flex items-center gap-1.5">
                  <span>{tab.label}</span>
                  {count > 0 && (
                    <span
                      className="text-[10px] px-1.5 rounded-full transition-liquid"
                      style={{
                        background: isSelected ? 'rgba(255, 107, 53, 0.2)' : 'rgba(255, 255, 255, 0.05)',
                        color: isSelected ? 'var(--color-accent-primary)' : 'var(--color-text-muted)'
                      }}
                    >
                      {count}
                    </span>
                  )}
                </span>
                {isSelected && (
                  <div 
                    className="absolute bottom-0 left-2 right-2 h-[3px] rounded-t-full animate-liquid-fade"
                    style={{
                      background: 'var(--color-accent-primary)',
                      boxShadow: '0 0 8px rgba(255, 107, 53, 0.4)'
                    }}
                  />
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Brand Filter & Sort - 液态玻璃芯片 */}
      <div className="px-4 pb-3 flex items-center gap-2 overflow-x-auto scrollbar-hide">
        {brands.map((brand) => (
          <button
            key={brand.key}
            onClick={() => setActiveBrand(brand.key)}
            aria-label={`筛选${brand.label}品牌`}
            aria-pressed={activeBrand === brand.key}
            className="flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-spring-soft ripple-container"
            style={{
              background: activeBrand === brand.key 
                ? 'var(--color-accent-primary)' 
                : 'rgba(255, 255, 255, 0.05)',
              color: activeBrand === brand.key 
                ? 'var(--color-text-primary)' 
                : 'var(--color-text-tertiary)',
              boxShadow: activeBrand === brand.key 
                ? '0 2px 8px rgba(255, 107, 53, 0.3)' 
                : 'none'
            }}
          >
            {brand.label}
          </button>
        ))}

        {/* Sort Dropdown */}
        <div className="flex-shrink-0 ml-auto flex items-center gap-1">
          <Filter size={12} style={{ color: 'var(--color-text-muted)' }} />
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'newest' | 'popular' | 'rating')}
            aria-label="排序方式"
            className="bg-transparent text-xs outline-none cursor-pointer transition-fast"
            style={{ color: 'var(--color-text-tertiary)' }}
          >
            <option value="newest" className="bg-[#1a1a1a]">最新</option>
            <option value="popular" className="bg-[#1a1a1a]">最热</option>
            <option value="rating" className="bg-[#1a1a1a]">评分</option>
          </select>
        </div>
      </div>

      {/* Pull to Refresh Indicator */}
      {refreshing && (
        <div className="flex items-center justify-center py-2 animate-liquid-fade">
          <RefreshCw 
            size={20} 
            style={{ color: 'var(--color-accent-primary)' }}
            className="animate-liquid-spin"
          />
        </div>
      )}

      {/* Preset Grid - 液态玻璃卡片 */}
      <div 
        className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide custom-scrollbar"
        role="tabpanel"
        aria-label="预设列表"
      >
        {filteredPresets.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 animate-liquid-fade">
            <Sparkles 
              size={32} 
              style={{ color: 'var(--color-text-muted)' }}
              className="mb-3 animate-liquid-float"
            />
            <p style={{ color: 'var(--color-text-tertiary)' }} className="text-sm mb-2">
              未找到匹配的预设
            </p>
            <p style={{ color: 'var(--color-text-muted)' }} className="text-xs">
              请调整筛选条件
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {filteredPresets.map((preset, index) => (
              <div
                key={preset.id}
                className={`group relative rounded-2xl overflow-hidden cursor-pointer glass-card ${getImageHeight(index)}`}
                style={{
                  animationDelay: `${index * 50}ms`,
                  animation: 'liquid-slide-up 0.3s ease forwards'
                }}
                role="article"
                aria-label={`预设: ${preset.name}`}
              >
                {/* Image */}
                <img
                  src={preset.coverPath}
                  alt={preset.name}
                  className="w-full h-full object-cover transition-liquid group-hover:scale-110"
                  loading="lazy"
                />

                {/* Overlay Gradient */}
                <div 
                  className="absolute inset-0"
                  style={{
                    background: 'linear-gradient(to top, rgba(0, 0, 0, 0.8) 0%, rgba(0, 0, 0, 0.2) 50%, transparent 100%)'
                  }}
                />

                {/* HNCS Badge */}
                {preset.isHncs && (
                  <div 
                    className="absolute top-2 left-2 px-2 py-1 rounded-lg text-[9px] font-bold text-white z-20 flex items-center gap-1 animate-liquid-pulse"
                    style={{
                      background: 'linear-gradient(135deg, var(--color-accent-primary) 0%, #FF9800 100%)',
                      boxShadow: '0 2px 8px rgba(255, 107, 53, 0.4)'
                    }}
                  >
                    <Crown size={10} />
                    <span>HNCS</span>
                  </div>
                )}

                {/* NEW Badge */}
                {preset.isNew && !preset.isHncs && (
                  <div 
                    className="absolute top-2 left-2 px-2 py-1 rounded-lg text-[9px] font-bold text-white z-20 flex items-center gap-1 animate-liquid-breathe"
                    style={{
                      background: 'var(--color-success)',
                      boxShadow: '0 2px 6px rgba(76, 175, 80, 0.3)'
                    }}
                  >
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
                  aria-label={favorites.has(preset.id) ? '取消收藏' : '添加收藏'}
                  aria-pressed={favorites.has(preset.id)}
                  className="absolute top-2 right-2 p-2 rounded-full z-20 transition-spring-soft"
                  style={{
                    background: 'rgba(0, 0, 0, 0.4)',
                    backdropFilter: 'blur(8px)'
                  }}
                >
                  <Heart
                    size={16}
                    style={{
                      color: favorites.has(preset.id) ? '#F44336' : 'var(--color-text-secondary)',
                      fill: favorites.has(preset.id) ? '#F44336' : 'transparent',
                      transition: 'all 0.2s ease'
                    }}
                  />
                </button>

                {/* Content */}
                <div className="absolute bottom-0 left-0 right-0 p-3 pr-12">
                  <h3 
                    style={{ color: 'var(--color-text-primary)' }}
                    className="font-semibold text-sm mb-0.5 truncate"
                  >
                    {preset.name}
                  </h3>
                  <p style={{ color: 'var(--color-text-tertiary)' }} className="text-xs truncate">
                    {preset.author}
                  </p>

                  {/* Stats */}
                  <div className="flex items-center gap-3 mt-1.5">
                    <div className="flex items-center gap-1">
                      <Star 
                        size={10} 
                        style={{ color: '#FFD700' }}
                        className="fill-yellow-400"
                      />
                      <span style={{ color: 'var(--color-text-muted)' }} className="text-[10px]">
                        4.{index + 5}
                      </span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Download size={10} style={{ color: 'var(--color-text-muted)' }} />
                      <span style={{ color: 'var(--color-text-muted)' }} className="text-[10px]">
                        {(index + 1) * 2.3}w
                      </span>
                    </div>
                    {preset.brand && (
                      <div className="flex items-center gap-1 ml-auto">
                        <span style={{ color: 'var(--color-text-muted)' }} className="text-[10px]">
                          {preset.brand}
                        </span>
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
          <div className="py-8 text-center animate-liquid-fade">
            <div 
              className="w-16 h-0.5 mx-auto mb-3"
              style={{
                background: 'linear-gradient(90deg, transparent 0%, rgba(255, 107, 53, 0.5) 50%, transparent 100%)'
              }}
            />
            <p 
              style={{ color: 'var(--color-accent-primary)' }}
              className="text-xs font-medium tracking-wider opacity-80"
            >
              持续更新 敬请期待
            </p>
            <div 
              className="w-16 h-0.5 mx-auto mt-3"
              style={{
                background: 'linear-gradient(90deg, transparent 0%, rgba(255, 107, 53, 0.5) 50%, transparent 100%)'
              }}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default HomeScreen;