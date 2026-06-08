import React, { useState, useCallback } from 'react';
import { useAppStore, featuredPresets } from '../store/appStore';
import { Search, Filter, Heart, Sparkles, Check, RefreshCw, Download, Star, Crown } from 'lucide-react';

const brands = ['OPPO', 'realme', 'vivo', '荣耀', '小米'];
const scenes = ['人像', '风景', '夜景', '美食', '街拍', '建筑'];

const FeaturedScreen: React.FC = () => {
  const { selectedBrand, setSelectedBrand, selectedScene, setSelectedScene } = useAppStore();
  const [searchQuery, setSearchQuery] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());

  // 下拉刷新
  const handleRefresh = useCallback(() => {
    setRefreshing(true);
    setTimeout(() => setRefreshing(false), 800);
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

  const filteredPresets = featuredPresets.filter((preset) => {
    const brandMatch = !selectedBrand || preset.brand === selectedBrand;
    const sceneMatch = !selectedScene || preset.tags.includes(selectedScene);
    const searchMatch = !searchQuery || 
      preset.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      preset.author.toLowerCase().includes(searchQuery.toLowerCase());
    return brandMatch && sceneMatch && searchMatch;
  });

  return (
    <div 
      className="h-full flex flex-col overflow-hidden animate-liquid-fade"
      style={{ background: 'var(--color-bg-primary)' }}
    >
      {/* Header - 液态玻璃效果 */}
      <div className="px-4 pt-3 pb-3 flex items-center justify-between">
        <div>
          <h1 
            className="text-xl font-bold"
            style={{ color: 'var(--color-text-primary)' }}
          >
            精选推荐
          </h1>
          <p style={{ color: 'var(--color-text-tertiary)' }} className="text-xs">
            大师级影像参数库
          </p>
        </div>
        <div className="flex gap-2">
          <button 
            aria-label="搜索"
            className="p-2 rounded-full glass-button transition-spring-soft"
          >
            <Search size={18} style={{ color: 'var(--color-text-secondary)' }} />
          </button>
          <button
            onClick={handleRefresh}
            aria-label="刷新"
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
            placeholder="搜索精选预设..."
            aria-label="搜索精选预设"
            className="w-full pl-9 pr-4 py-2 rounded-full glass-input transition-smooth"
          />
        </div>
      </div>

      {/* Brand Filter - 液态玻璃芯片 */}
      <div className="px-4 pb-2">
        <div className="flex gap-2 overflow-x-auto scrollbar-hide" role="group" aria-label="品牌筛选">
          <button
            onClick={() => setSelectedBrand(null)}
            aria-label="全部品牌"
            aria-pressed={!selectedBrand}
            className="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-spring-soft ripple-container"
            style={{
              background: !selectedBrand ? 'var(--color-accent-primary)' : 'rgba(255, 255, 255, 0.05)',
              color: !selectedBrand ? 'var(--color-text-primary)' : 'var(--color-text-tertiary)',
              boxShadow: !selectedBrand ? '0 2px 8px rgba(255, 107, 53, 0.3)' : 'none'
            }}
          >
            全部品牌
          </button>
          {brands.map((brand) => (
            <button
              key={brand}
              onClick={() => setSelectedBrand(brand)}
              aria-label={brand}
              aria-pressed={selectedBrand === brand}
              className="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-spring-soft ripple-container"
              style={{
                background: selectedBrand === brand ? 'var(--color-accent-primary)' : 'rgba(255, 255, 255, 0.05)',
                color: selectedBrand === brand ? 'var(--color-text-primary)' : 'var(--color-text-tertiary)',
                boxShadow: selectedBrand === brand ? '0 2px 8px rgba(255, 107, 53, 0.3)' : 'none'
              }}
            >
              {brand}
            </button>
          ))}
        </div>
      </div>

      {/* Scene Filter - 液态玻璃芯片 */}
      <div className="px-4 pb-3">
        <div className="flex gap-2 overflow-x-auto scrollbar-hide" role="group" aria-label="场景筛选">
          <button
            onClick={() => setSelectedScene(null)}
            aria-label="全部场景"
            aria-pressed={!selectedScene}
            className="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-spring-soft ripple-container"
            style={{
              background: !selectedScene ? 'rgba(255, 107, 53, 0.7)' : 'rgba(255, 255, 255, 0.05)',
              color: 'var(--color-text-primary)',
              boxShadow: !selectedScene ? '0 2px 6px rgba(255, 107, 53, 0.25)' : 'none'
            }}
          >
            全部场景
          </button>
          {scenes.map((scene) => (
            <button
              key={scene}
              onClick={() => setSelectedScene(scene)}
              aria-label={scene}
              aria-pressed={selectedScene === scene}
              className="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-spring-soft ripple-container"
              style={{
                background: selectedScene === scene ? 'rgba(255, 107, 53, 0.7)' : 'rgba(255, 255, 255, 0.05)',
                color: 'var(--color-text-primary)',
                boxShadow: selectedScene === scene ? '0 2px 6px rgba(255, 107, 53, 0.25)' : 'none'
              }}
            >
              {scene}
            </button>
          ))}
        </div>
      </div>

      {/* Filter Result Count */}
      {(selectedBrand || selectedScene || searchQuery) && (
        <div className="px-4 pb-2 flex items-center gap-2 animate-liquid-fade">
          <span style={{ color: 'var(--color-accent-primary)' }} className="text-xs">
            已筛选 {filteredPresets.length} 条
          </span>
          <button
            onClick={() => {
              setSelectedBrand(null);
              setSelectedScene(null);
              setSearchQuery('');
            }}
            aria-label="清空筛选"
            className="text-xs transition-fast hover:opacity-80"
            style={{ color: 'var(--color-text-muted)' }}
          >
            清空筛选
          </button>
        </div>
      )}

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
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide custom-scrollbar">
        <div className="grid grid-cols-2 gap-3">
          {filteredPresets.map((preset, index) => (
            <div
              key={preset.id}
              className="group relative rounded-2xl overflow-hidden cursor-pointer glass-card animate-liquid-slide-up"
              style={{
                animationDelay: `${index * 50}ms`,
                animationFillMode: 'both'
              }}
              role="article"
              aria-label={`精选预设: ${preset.name}`}
            >
              {/* Image */}
              <div className="aspect-[4/3] overflow-hidden">
                <img
                  src={preset.coverPath}
                  alt={preset.name}
                  className="w-full h-full object-cover transition-liquid group-hover:scale-110"
                  loading="lazy"
                />
              </div>

              {/* Overlay Gradient */}
              <div 
                className="absolute inset-0 pointer-events-none"
                style={{
                  background: 'linear-gradient(to top, rgba(0, 0, 0, 0.8) 0%, rgba(0, 0, 0, 0.2) 50%, transparent 100%)'
                }}
              />

              {/* NEW Badge */}
              {preset.isNew && (
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

              {/* Favorite Button */}
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  toggleFavorite(preset.id);
                }}
                aria-label={favorites.has(preset.id) ? '取消收藏' : '添加收藏'}
                aria-pressed={favorites.has(preset.id)}
                className="absolute top-2 right-2 p-2 rounded-lg z-20 transition-spring-soft"
                style={{
                  background: 'rgba(0, 0, 0, 0.5)',
                  backdropFilter: 'blur(8px)'
                }}
              >
                <Heart
                  size={16}
                  style={{
                    color: favorites.has(preset.id) ? '#F44336' : 'var(--color-text-secondary)',
                    fill: favorites.has(preset.id) ? '#F44336' : 'transparent'
                  }}
                />
              </button>

              {/* Content */}
              <div className="p-3">
                <h3 
                  style={{ color: 'var(--color-text-primary)' }}
                  className="font-semibold text-sm mb-1 truncate"
                >
                  {preset.name}
                </h3>
                <div className="flex items-center gap-1 mb-2">
                  <span style={{ color: 'var(--color-text-muted)' }} className="text-xs">
                    {preset.author}
                  </span>
                  {preset.isHncs && (
                    <>
                      <Check size={10} style={{ color: 'var(--color-accent-primary)' }} />
                      <span style={{ color: 'var(--color-accent-primary)' }} className="text-[10px]">
                        HNCS
                      </span>
                    </>
                  )}
                </div>

                {/* Stats */}
                <div className="flex items-center gap-3 mb-2">
                  <div className="flex items-center gap-1">
                    <Star 
                      size={10} 
                      style={{ color: '#FFD700' }}
                      className="fill-yellow-400"
                    />
                    <span style={{ color: 'var(--color-text-muted)' }} className="text-[10px]">
                      4.{index + 6}
                    </span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Download size={10} style={{ color: 'var(--color-text-muted)' }} />
                    <span style={{ color: 'var(--color-text-muted)' }} className="text-[10px]">
                      {(index + 1) * 3.2}w
                    </span>
                  </div>
                </div>

                {/* Apply Button - 液态玻璃按钮 */}
                <button 
                  aria-label="应用参数"
                  className="w-full flex items-center justify-center gap-1 py-2 rounded-lg glass-button transition-spring-soft"
                >
                  <Sparkles size={14} style={{ color: 'var(--color-accent-primary)' }} />
                  <span style={{ color: 'var(--color-accent-primary)' }} className="text-xs font-medium">
                    应用参数
                  </span>
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Empty State */}
        {filteredPresets.length === 0 && (
          <div className="flex flex-col items-center justify-center py-12 animate-liquid-fade">
            <div 
              className="w-16 h-16 rounded-full flex items-center justify-center mb-4"
              style={{
                background: 'rgba(255, 255, 255, 0.05)',
                boxShadow: '0 4px 12px rgba(0, 0, 0, 0.2)'
              }}
            >
              <Filter size={24} style={{ color: 'var(--color-text-muted)' }} />
            </div>
            <p style={{ color: 'var(--color-text-tertiary)' }} className="text-sm">
              暂无精选预设
            </p>
            <p style={{ color: 'var(--color-text-muted)' }} className="text-xs mt-1">
              请调整筛选条件
            </p>
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

export default FeaturedScreen;