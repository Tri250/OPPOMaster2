import React, { useState, useCallback, useEffect } from 'react';
import { useAppStore, featuredPresets } from '../store/appStore';
import { Search, Sparkles, RefreshCw } from 'lucide-react';
import PresetCard from '../components/PresetCard';
import { SkeletonPresetCard } from '../components/Skeleton';

/**
 * ============================================
 * 精选页 - ColorOS 16 优化版
 * 简洁设计 + 哈苏橙风格
 * ============================================
 */
const FeaturedScreen: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const timer = setTimeout(() => setIsLoading(false), 600);
    return () => clearTimeout(timer);
  }, []);

  const handleRefresh = useCallback(() => {
    setRefreshing(true);
    setIsLoading(true);
    setTimeout(() => {
      setRefreshing(false);
      setIsLoading(false);
    }, 800);
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
      {searchQuery && !isLoading && (
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
        {isLoading ? (
          <SkeletonPresetCard count={6} />
        ) : (
          <>
            <div className="grid grid-cols-2 gap-4">
              {filteredPresets.map((preset, index) => (
                <PresetCard
                  key={preset.id}
                  preset={preset}
                  variant="full"
                  index={index}
                />
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
          </>
        )}
      </div>
    </div>
  );
};

export default FeaturedScreen;
