import React, { useState, useCallback } from 'react';
import { useAppStore, featuredPresets } from '../store/appStore';
import { Search, Heart, Sparkles, Check, RefreshCw, Download, Star, Crown } from 'lucide-react';

/**
 * ============================================
 * 精选页 - ColorOS 16 优化版
 * 简洁设计 + 哈苏橙风格
 * ============================================
 */
const FeaturedScreen: React.FC = () => {
  const { selectedBrand, setSelectedBrand, selectedScene, setSelectedScene } = useAppStore();
  const [searchQuery, setSearchQuery] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());

  const handleRefresh = useCallback(() => {
    setRefreshing(true);
    setTimeout(() => setRefreshing(false), 800);
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

  const filteredPresets = featuredPresets.filter((preset) => {
    const searchMatch = !searchQuery || 
      preset.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      preset.author.toLowerCase().includes(searchQuery.toLowerCase());
    return searchMatch;
  });

  return (
    <div 
      className="h-full flex flex-col overflow-hidden animate-liquid-fade dynamic-bg"
      style={{ background: 'var(--color-bg-primary)' }}
    >
      {/* 沉浸式标题栏 */}
      <div className="immersive-header animate-liquid-slide-down">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="immersive-title">精选推荐</h1>
            <p className="text-xs mt-2" style={{ color: 'var(--color-text-tertiary)' }}>
              大师级影像参数库
            </p>
          </div>
          <div className="flex gap-2">
            <button 
              aria-label="搜索"
              className="p-2 rounded-xl"
              style={{ 
                background: 'rgba(255, 255, 255, 0.08)',
                border: '1px solid rgba(255, 255, 255, 0.1)'
              }}
            >
              <Search size={16} style={{ color: 'var(--color-text-secondary)' }} />
            </button>
            <button
              onClick={handleRefresh}
              aria-label="刷新"
              className="p-2 rounded-xl"
              style={{ 
                background: 'rgba(255, 255, 255, 0.08)',
                border: '1px solid rgba(255, 255, 255, 0.1)'
              }}
            >
              <RefreshCw 
                size={16} 
                className={refreshing ? 'animate-liquid-spin' : ''}
                style={{ color: 'var(--color-text-secondary)' }}
              />
            </button>
          </div>
        </div>
      </div>

      {/* 搜索栏 */}
      <div className="px-4 pb-3">
        <div className="relative">
          <Search 
            size={16} 
            className="absolute left-4 top-1/2 -translate-y-1/2"
            style={{ color: 'var(--color-text-tertiary)' }}
          />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索精选预设..."
            aria-label="搜索精选预设"
            className="w-full pl-11 pr-4 py-3 text-sm rounded-xl outline-none transition-all"
            style={{
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              color: 'var(--color-text-primary)'
            }}
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-full hover:bg-white/10 transition-fast"
              aria-label="清除搜索"
            >
              <span style={{ color: 'var(--color-text-tertiary)' }} className="text-xs">✕</span>
            </button>
          )}
        </div>
      </div>

      {/* 搜索结果统计 */}
      {searchQuery && (
        <div className="px-4 pb-2 flex items-center gap-3 animate-liquid-fade">
          <span 
            className="text-xs px-2 py-1 rounded-md font-bold"
            style={{ background: '#FF6B35', color: '#FFFFFF' }}
          >
            搜索结果
          </span>
          <span className="text-sm font-semibold" style={{ color: 'var(--color-accent-primary)' }}>
            {filteredPresets.length} 条
          </span>
        </div>
      )}

      {/* 下拉刷新指示器 */}
      {refreshing && (
        <div className="flex items-center justify-center py-2 animate-liquid-fade">
          <div className="loading-spinner" />
        </div>
      )}

      {/* 预设网格 */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide custom-scrollbar">
        <div className="grid grid-cols-2 gap-4">
          {filteredPresets.map((preset, index) => (
            <div
              key={preset.id}
              className="group relative animate-liquid-slide-up"
              style={{
                animationDelay: `${index * 60}ms`,
                animationFillMode: 'both',
                background: 'rgba(255, 255, 255, 0.05)',
                borderRadius: '20px',
                overflow: 'hidden',
                border: '1px solid rgba(255, 255, 255, 0.08)'
              }}
              role="article"
            >
              {/* 图片 */}
              <div className="aspect-[4/3] overflow-hidden rounded-t-2xl">
                <img
                  src={preset.coverPath}
                  alt={preset.name}
                  className="w-full h-full object-cover transition-liquid group-hover:scale-110"
                  loading="lazy"
                />
              </div>

              {/* NEW徽章 - 白色边框 */}
              {preset.isNew && (
                <div 
                  className="absolute top-3 left-3 flex items-center gap-1 px-2 py-1 rounded-md"
                  style={{ 
                    background: 'transparent',
                    border: '1px solid rgba(255, 255, 255, 0.5)'
                  }}
                >
                  <Sparkles size={12} style={{ color: '#FFFFFF' }} />
                  <span className="text-xs font-bold" style={{ color: '#FFFFFF' }}>NEW</span>
                </div>
              )}

              {/* HNCS徽章 */}
              {preset.isHncs && (
                <div 
                  className="absolute top-3 left-3 flex items-center gap-1 px-2 py-1 rounded-md"
                  style={{ background: 'rgba(255, 107, 53, 0.9)' }}
                >
                  <Crown size={12} style={{ color: '#FFFFFF' }} />
                  <span className="text-xs font-bold" style={{ color: '#FFFFFF' }}>HNCS</span>
                </div>
              )}

              {/* 收藏按钮 - 白色 */}
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
                    : 'rgba(0, 0, 0, 0.5)',
                  backdropFilter: 'blur(12px)'
                }}
              >
                <Heart
                  size={18}
                  style={{
                    color: favorites.has(preset.id) ? '#F44336' : '#FFFFFF',
                    fill: favorites.has(preset.id) ? '#F44336' : 'transparent'
                  }}
                />
              </button>

              {/* 内容区 */}
              <div className="p-4">
                <h3 
                  className="font-bold text-base mb-1 truncate"
                  style={{ color: '#FFFFFF' }}
                >
                  {preset.name}
                </h3>
                <div className="flex items-center gap-2 mb-3">
                  <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>
                    {preset.author}
                  </span>
                  {preset.isHncs && (
                    <>
                      <Check size={12} style={{ color: '#FF6B35' }} />
                      <span className="text-xs font-medium" style={{ color: '#FF6B35' }}>
                        HNCS认证
                      </span>
                    </>
                  )}
                </div>

                {/* 统计信息 */}
                <div className="flex items-center gap-4 mb-3">
                  <div className="flex items-center gap-1">
                    <Star 
                      size={12} 
                      style={{ color: '#FFD700' }}
                      className="fill-yellow-400"
                    />
                    <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>
                      4.{index + 7}
                    </span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Download size={12} style={{ color: 'rgba(255, 255, 255, 0.5)' }} />
                    <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.5)' }}>
                      {(index + 1) * 3.5}w
                    </span>
                  </div>
                </div>

                {/* 应用按钮 - 橙色实心 */}
                <button 
                  aria-label="应用参数"
                  className="w-full py-3 rounded-xl font-semibold text-sm flex items-center justify-center gap-2 transition-all active:scale-95"
                  style={{ 
                    background: '#FF6B35',
                    color: '#FFFFFF'
                  }}
                >
                  <Sparkles size={16} />
                  应用参数
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* 空状态 */}
        {filteredPresets.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16 animate-liquid-fade">
            <div 
              className="w-20 h-20 rounded-2xl flex items-center justify-center mb-4"
              style={{
                background: 'rgba(255, 255, 255, 0.05)',
                border: '1px solid rgba(255, 255, 255, 0.08)'
              }}
            >
              <Search size={32} style={{ color: 'var(--color-text-tertiary)' }} />
            </div>
            <p className="text-base mb-2" style={{ color: 'var(--color-text-tertiary)' }}>
              暂无精选预设
            </p>
            <p className="text-sm" style={{ color: 'var(--color-text-tertiary)' }}>
              请尝试其他关键词
            </p>
          </div>
        )}

        {/* 底部提示 */}
        {filteredPresets.length > 0 && (
          <div className="py-10 text-center animate-liquid-fade">
            <div 
              className="w-16 h-px mx-auto mb-4"
              style={{ background: 'rgba(255, 255, 255, 0.2)' }}
            />
            <p 
              className="text-sm tracking-wider"
              style={{ color: 'var(--color-text-tertiary)' }}
            >
              持续更新 敬请期待
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default FeaturedScreen;
