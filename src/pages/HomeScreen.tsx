import React, { useState, useCallback, useEffect } from 'react';
import { useAppStore, homePresets } from '../store/appStore';
import { Heart, Search, RefreshCw, Sparkles, Crown, Download, Star, Filter, TrendingUp, Zap } from 'lucide-react';

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

/**
 * ============================================
 * 首页 - ColorOS 16 全面优化版
 * 沉浸式标题栏 + 智能推荐 + 动态效果
 * ============================================
 */
const HomeScreen: React.FC = () => {
  const { selectedTab, setSelectedTab, presetSources, fetchedPresets, setFetchedPresets } = useAppStore();
  const [searchQuery, setSearchQuery] = useState('');
  const [activeBrand, setActiveBrand] = useState('all');
  const [sortBy, setSortBy] = useState<'newest' | 'popular' | 'rating'>('newest');
  const [refreshing, setRefreshing] = useState(false);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [isLoading, setIsLoading] = useState(false);
  
  const allPresets = [...homePresets, ...fetchedPresets];

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

  const handleRefresh = useCallback(() => {
    setRefreshing(true);
    fetchPresetsFromSources();
  }, [fetchPresetsFromSources]);
  
  useEffect(() => {
    fetchPresetsFromSources();
  }, []);

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

  const getFilteredPresets = useCallback(() => {
    let result = [...allPresets];

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

    if (activeBrand !== 'all') {
      result = result.filter(p => p.brand === activeBrand);
    }

    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      result = result.filter(p =>
        p.name.toLowerCase().includes(q) ||
        p.author.toLowerCase().includes(q) ||
        p.tags.some(t => t.toLowerCase().includes(q))
      );
    }

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
      className="h-full flex flex-col overflow-hidden animate-liquid-fade dynamic-bg"
      style={{ background: 'var(--color-bg-primary)' }}
    >
      {/* 沉浸式标题栏 - ColorOS 16 风格 */}
      <div className="immersive-header animate-liquid-slide-down">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h1 className="immersive-title">
              小O帮帮
            </h1>
            {/* 哈苏大师徽章 - 增强版 */}
            <div className="badge-hncs flex items-center gap-1">
              <Crown size={12} />
              <span>哈苏大师</span>
            </div>
          </div>
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            aria-label="刷新预设列表"
            className="glass-button flex items-center gap-2"
          >
            <RefreshCw 
              size={16} 
              className={refreshing ? 'animate-liquid-spin' : ''}
            />
            <span className="text-xs font-medium">刷新</span>
          </button>
        </div>
      </div>

      {/* 搜索栏 - 增强版液态玻璃 */}
      <div className="px-4 pb-3">
        <div className="relative">
          <Search 
            size={16} 
            className="absolute left-4 top-1/2 -translate-y-1/2"
            style={{ color: 'var(--color-text-muted)' }}
          />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索预设 / 作者 / 标签"
            aria-label="搜索预设"
            className="w-full pl-11 pr-4 py-3 glass-input text-sm"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full hover:bg-white/10 transition-fast"
              aria-label="清除搜索"
            >
              <span style={{ color: 'var(--color-text-muted)' }} className="text-xs">✕</span>
            </button>
          )}
        </div>
      </div>

      {/* 智能Tab栏 - ColorOS 16 风格 */}
      <div className="smart-tab-bar px-4 animate-liquid-fade">
        <div className="flex gap-1 overflow-x-auto scrollbar-hide" role="tablist">
          {tabs.map((tab, index) => {
            const count = getTabCount(tab.key);
            const isSelected = selectedTab === index;
            return (
              <button
                key={tab.key}
                onClick={() => setSelectedTab(index)}
                role="tab"
                aria-selected={isSelected}
                className={`smart-tab-item ${isSelected ? 'smart-tab-item-active' : ''} ripple-container`}
              >
                <span className="flex items-center gap-2">
                  <span className="text-sm">{tab.label}</span>
                  {count > 0 && (
                    <span
                      className="text-[10px] px-1.5 py-0.5 rounded-full"
                      style={{
                        background: isSelected ? 'rgba(255, 107, 53, 0.2)' : 'rgba(255, 255, 255, 0.08)',
                        color: isSelected ? 'var(--color-accent-primary)' : 'var(--color-text-muted)'
                      }}
                    >
                      {count}
                    </span>
                  )}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* 品牌筛选芯片 - 增强版 */}
      <div className="px-4 py-3 flex items-center gap-2 overflow-x-auto scrollbar-hide animate-liquid-fade">
        {brands.map((brand) => (
          <button
            key={brand.key}
            onClick={() => setActiveBrand(brand.key)}
            aria-label={`筛选${brand.label}品牌`}
            aria-pressed={activeBrand === brand.key}
            className={`glass-chip ${activeBrand === brand.key ? 'glass-chip-active' : ''} ripple-container whitespace-nowrap`}
          >
            <span className="text-xs font-medium">{brand.label}</span>
          </button>
        ))}

        {/* 排序下拉 */}
        <div className="flex items-center gap-1 ml-auto">
          <Filter size={12} style={{ color: 'var(--color-text-muted)' }} />
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'newest' | 'popular' | 'rating')}
            aria-label="排序方式"
            className="bg-transparent text-xs outline-none cursor-pointer"
            style={{ color: 'var(--color-text-tertiary)' }}
          >
            <option value="newest" className="bg-[#1a1a1a]">最新</option>
            <option value="popular" className="bg-[#1a1a1a]">最热</option>
            <option value="rating" className="bg-[#1a1a1a]">评分</option>
          </select>
        </div>
      </div>

      {/* 下拉刷新指示器 */}
      {refreshing && (
        <div className="flex items-center justify-center py-2 animate-liquid-fade">
          <div className="loading-spinner" />
        </div>
      )}

      {/* 预设网格 - 增强版液态玻璃卡片 */}
      <div 
        className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide custom-scrollbar"
        role="tabpanel"
      >
        {filteredPresets.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 animate-liquid-fade">
            <Sparkles 
              size={40} 
              style={{ color: 'var(--color-text-muted)' }}
              className="mb-4 animate-liquid-float"
            />
            <p style={{ color: 'var(--color-text-tertiary)' }} className="text-base mb-2">
              未找到匹配的预设
            </p>
            <p style={{ color: 'var(--color-text-muted)' }} className="text-sm">
              请调整筛选条件
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-4">
            {filteredPresets.map((preset, index) => (
              <div
                key={preset.id}
                className={`group relative glass-card ${getImageHeight(index)} animate-liquid-slide-up`}
                style={{
                  animationDelay: `${index * 60}ms`,
                  animationFillMode: 'both'
                }}
                role="article"
              >
                {/* 图片 */}
                <img
                  src={preset.coverPath}
                  alt={preset.name}
                  className="w-full h-full object-cover transition-liquid group-hover:scale-105"
                  loading="lazy"
                />

                {/* 渐变遮罩 */}
                <div 
                  className="absolute inset-0"
                  style={{
                    background: 'linear-gradient(to top, rgba(0, 0, 0, 0.85) 0%, rgba(0, 0, 0, 0.3) 50%, transparent 100%)'
                  }}
                />

                {/* HNCS徽章 - 增强版 */}
                {preset.isHncs && (
                  <div className="badge-hncs absolute top-3 left-3 flex items-center gap-1">
                    <Crown size={12} />
                    <span>HNCS</span>
                  </div>
                )}

                {/* NEW徽章 - 增强版 */}
                {preset.isNew && !preset.isHncs && (
                  <div className="badge-new absolute top-3 left-3 flex items-center gap-1">
                    <Sparkles size={12} />
                    <span>NEW</span>
                  </div>
                )}

                {/* 收藏按钮 - 增强版 */}
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    toggleFavorite(preset.id);
                  }}
                  aria-label={favorites.has(preset.id) ? '取消收藏' : '添加收藏'}
                  aria-pressed={favorites.has(preset.id)}
                  className="absolute top-3 right-3 p-2.5 rounded-full z-20 transition-spring-soft ripple-container"
                  style={{
                    background: favorites.has(preset.id) 
                      ? 'rgba(244, 67, 54, 0.2)' 
                      : 'rgba(0, 0, 0, 0.4)',
                    backdropFilter: 'blur(12px)',
                    boxShadow: favorites.has(preset.id) 
                      ? '0 0 15px rgba(244, 67, 54, 0.3)' 
                      : 'none'
                  }}
                >
                  <Heart
                    size={18}
                    style={{
                      color: favorites.has(preset.id) ? '#F44336' : 'var(--color-text-secondary)',
                      fill: favorites.has(preset.id) ? '#F44336' : 'transparent',
                      transition: 'all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)'
                    }}
                  />
                </button>

                {/* 内容区 - 液态玻璃遮罩 */}
                <div 
                  className="absolute bottom-0 left-0 right-0 p-4"
                  style={{
                    background: 'linear-gradient(to top, rgba(0, 0, 0, 0.9) 0%, transparent 100%)'
                  }}
                >
                  <h3 
                    className="font-bold text-base mb-1 truncate"
                    style={{ color: 'var(--color-text-primary)' }}
                  >
                    {preset.name}
                  </h3>
                  <p className="text-xs truncate mb-2" style={{ color: 'var(--color-text-tertiary)' }}>
                    {preset.author}
                  </p>

                  {/* 统计信息 */}
                  <div className="flex items-center gap-4">
                    <div className="flex items-center gap-1">
                      <Star 
                        size={12} 
                        style={{ color: '#FFD700' }}
                        className="fill-yellow-400"
                      />
                      <span style={{ color: 'var(--color-text-muted)' }} className="text-xs">
                        4.{index + 6}
                      </span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Download size={12} style={{ color: 'var(--color-text-muted)' }} />
                      <span style={{ color: 'var(--color-text-muted)' }} className="text-xs">
                        {(index + 1) * 2.3}w
                      </span>
                    </div>
                    {preset.brand && (
                      <div className="flex items-center gap-1 ml-auto">
                        <Zap size={10} style={{ color: 'var(--color-accent-primary)' }} />
                        <span style={{ color: 'var(--color-accent-primary)' }} className="text-xs font-medium">
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

        {/* 底部提示 - 增强版 */}
        {filteredPresets.length > 0 && (
          <div className="py-10 text-center animate-liquid-fade">
            <div 
              className="w-20 h-0.5 mx-auto mb-4"
              style={{
                background: 'linear-gradient(90deg, transparent 0%, var(--color-accent-primary) 50%, transparent 100%)',
                boxShadow: '0 0 8px rgba(255, 107, 53, 0.3)'
              }}
            />
            <p 
              className="text-sm font-semibold tracking-wider animate-glow-breathe"
              style={{ color: 'var(--color-accent-primary)' }}
            >
              持续更新 敬请期待
            </p>
            <div 
              className="w-20 h-0.5 mx-auto mt-4"
              style={{
                background: 'linear-gradient(90deg, transparent 0%, var(--color-accent-primary) 50%, transparent 100%)',
                boxShadow: '0 0 8px rgba(255, 107, 53, 0.3)'
              }}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default HomeScreen;