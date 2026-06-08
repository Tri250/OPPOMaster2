import React, { useState, useCallback } from 'react';
import { useAppStore, featuredPresets } from '../store/appStore';
import { Search, Filter, Heart, Sparkles, Check, RefreshCw, Download, Star, Crown, TrendingUp, Flame } from 'lucide-react';

const brands = ['OPPO', 'realme', 'vivo', '荣耀', '小米'];
const scenes = ['人像', '风景', '夜景', '美食', '街拍', '建筑'];

/**
 * ============================================
 * 精选页 - ColorOS 16 全面优化版
 * 智能分组筛选 + 可视化场景图标
 * ============================================
 */
const FeaturedScreen: React.FC = () => {
  const { selectedBrand, setSelectedBrand, selectedScene, setSelectedScene } = useAppStore();
  const [searchQuery, setSearchQuery] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [expandedBrandGroup, setExpandedBrandGroup] = useState(true);
  const [expandedSceneGroup, setExpandedSceneGroup] = useState(true);

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
    const brandMatch = !selectedBrand || preset.brand === selectedBrand;
    const sceneMatch = !selectedScene || preset.tags.includes(selectedScene);
    const searchMatch = !searchQuery || 
      preset.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      preset.author.toLowerCase().includes(searchQuery.toLowerCase());
    return brandMatch && sceneMatch && searchMatch;
  });

  // 热门预设（前3个）
  const hotPresets = filteredPresets.slice(0, 3);

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
              className="glass-button p-2"
            >
              <Search size={16} style={{ color: 'var(--color-text-secondary)' }} />
            </button>
            <button
              onClick={handleRefresh}
              aria-label="刷新"
              className="glass-button p-2"
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
            style={{ color: 'var(--color-text-muted)' }}
          />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索精选预设..."
            aria-label="搜索精选预设"
            className="w-full pl-11 pr-4 py-3 glass-input text-sm"
          />
        </div>
      </div>

      {/* 智能分组筛选 - 品牌组 */}
      <div className="px-4 pb-2 animate-liquid-fade">
        <button
          onClick={() => setExpandedBrandGroup(!expandedBrandGroup)}
          className="flex items-center gap-2 w-full py-2"
          aria-expanded={expandedBrandGroup}
        >
          <TrendingUp size={14} style={{ color: 'var(--color-accent-primary)' }} />
          <span className="text-sm font-semibold" style={{ color: 'var(--color-text-primary)' }}>
            品牌筛选
          </span>
          <span className="text-xs px-2 py-0.5 rounded-full" style={{ background: 'rgba(255, 255, 255, 0.1)', color: 'var(--color-text-muted)' }}>
            {brands.length}
          </span>
          <span className="ml-auto text-xs" style={{ color: 'var(--color-text-muted)' }}>
            {expandedBrandGroup ? '收起' : '展开'}
          </span>
        </button>
        
        {expandedBrandGroup && (
          <div className="flex gap-2 overflow-x-auto scrollbar-hide pt-2 animate-liquid-slide-down" role="group">
            <button
              onClick={() => setSelectedBrand(null)}
              aria-label="全部品牌"
              aria-pressed={!selectedBrand}
              className={`glass-chip ${!selectedBrand ? 'glass-chip-active' : ''} ripple-container whitespace-nowrap`}
            >
              <span className="text-xs font-medium">全部品牌</span>
            </button>
            {brands.map((brand) => (
              <button
                key={brand}
                onClick={() => setSelectedBrand(brand)}
                aria-label={brand}
                aria-pressed={selectedBrand === brand}
                className={`glass-chip ${selectedBrand === brand ? 'glass-chip-active' : ''} ripple-container whitespace-nowrap`}
              >
                <span className="text-xs font-medium">{brand}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* 智能分组筛选 - 场景组 */}
      <div className="px-4 pb-3 animate-liquid-fade">
        <button
          onClick={() => setExpandedSceneGroup(!expandedSceneGroup)}
          className="flex items-center gap-2 w-full py-2"
          aria-expanded={expandedSceneGroup}
        >
          <Flame size={14} style={{ color: 'var(--color-feature-scene)' }} />
          <span className="text-sm font-semibold" style={{ color: 'var(--color-text-primary)' }}>
            场景筛选
          </span>
          <span className="text-xs px-2 py-0.5 rounded-full" style={{ background: 'rgba(255, 255, 255, 0.1)', color: 'var(--color-text-muted)' }}>
            {scenes.length}
          </span>
          <span className="ml-auto text-xs" style={{ color: 'var(--color-text-muted)' }}>
            {expandedSceneGroup ? '收起' : '展开'}
          </span>
        </button>
        
        {expandedSceneGroup && (
          <div className="flex gap-2 overflow-x-auto scrollbar-hide pt-2 animate-liquid-slide-down" role="group">
            <button
              onClick={() => setSelectedScene(null)}
              aria-label="全部场景"
              aria-pressed={!selectedScene}
              className={`glass-chip ${!selectedScene ? 'glass-chip-active' : ''} ripple-container whitespace-nowrap`}
            >
              <span className="text-xs font-medium">全部场景</span>
            </button>
            {scenes.map((scene) => (
              <button
                key={scene}
                onClick={() => setSelectedScene(scene)}
                aria-label={scene}
                aria-pressed={selectedScene === scene}
                className={`glass-chip ${selectedScene === scene ? 'glass-chip-active' : ''} ripple-container whitespace-nowrap`}
              >
                <span className="text-xs font-medium">{scene}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* 筛选结果统计 */}
      {(selectedBrand || selectedScene || searchQuery) && (
        <div className="px-4 pb-2 flex items-center gap-3 animate-liquid-fade">
          <span className="smart-recommend-badge">已筛选</span>
          <span className="text-sm font-semibold" style={{ color: 'var(--color-accent-primary)' }}>
            {filteredPresets.length} 条
          </span>
          <button
            onClick={() => {
              setSelectedBrand(null);
              setSelectedScene(null);
              setSearchQuery('');
            }}
            aria-label="清空筛选"
            className="glass-button text-xs px-3 py-1"
          >
            清空筛选
          </button>
        </div>
      )}

      {/* 下拉刷新指示器 */}
      {refreshing && (
        <div className="flex items-center justify-center py-2 animate-liquid-fade">
          <div className="loading-spinner" />
        </div>
      )}

      {/* 热门推荐区块 */}
      {hotPresets.length > 0 && !selectedBrand && !selectedScene && (
        <div className="px-4 pb-4 animate-liquid-fade">
          <div className="smart-recommend p-4">
            <div className="flex items-center gap-2 mb-3">
              <Flame size={16} style={{ color: 'var(--color-error)' }} />
              <span className="text-sm font-bold" style={{ color: 'var(--color-text-primary)' }}>
                热门精选
              </span>
              <div className="badge-hot">HOT</div>
            </div>
            <div className="flex gap-3 overflow-x-auto scrollbar-hide">
              {hotPresets.map((preset, index) => (
                <div
                  key={`hot-${preset.id}`}
                  className="flex-shrink-0 w-32 h-40 rounded-2xl overflow-hidden relative animate-liquid-slide-left"
                  style={{
                    animationDelay: `${index * 100}ms`,
                    boxShadow: '0 8px 20px rgba(0, 0, 0, 0.3)'
                  }}
                >
                  <img
                    src={preset.coverPath}
                    alt={preset.name}
                    className="w-full h-full object-cover"
                    loading="lazy"
                  />
                  <div 
                    className="absolute inset-0"
                    style={{
                      background: 'linear-gradient(to top, rgba(0, 0, 0, 0.8) 0%, transparent 60%)'
                    }}
                  />
                  <div className="absolute bottom-2 left-2 right-2">
                    <p className="text-xs font-semibold truncate" style={{ color: 'var(--color-text-primary)' }}>
                      {preset.name}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 预设网格 - 增强版 */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide custom-scrollbar">
        <div className="grid grid-cols-2 gap-4">
          {filteredPresets.map((preset, index) => (
            <div
              key={preset.id}
              className="group relative glass-card animate-liquid-slide-up"
              style={{
                animationDelay: `${index * 60}ms`,
                animationFillMode: 'both'
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

              {/* NEW徽章 */}
              {preset.isNew && (
                <div className="badge-new absolute top-3 left-3 flex items-center gap-1">
                  <Sparkles size={12} />
                  <span>NEW</span>
                </div>
              )}

              {/* HNCS徽章 */}
              {preset.isHncs && (
                <div className="badge-hncs absolute top-3 left-3 flex items-center gap-1">
                  <Crown size={12} />
                  <span>HNCS</span>
                </div>
              )}

              {/* 收藏按钮 */}
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
                    fill: favorites.has(preset.id) ? '#F44336' : 'transparent'
                  }}
                />
              </button>

              {/* 内容区 */}
              <div className="p-4">
                <h3 
                  className="font-bold text-base mb-1 truncate"
                  style={{ color: 'var(--color-text-primary)' }}
                >
                  {preset.name}
                </h3>
                <div className="flex items-center gap-2 mb-3">
                  <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                    {preset.author}
                  </span>
                  {preset.isHncs && (
                    <>
                      <Check size={12} style={{ color: 'var(--color-accent-primary)' }} />
                      <span className="text-xs font-medium" style={{ color: 'var(--color-accent-primary)' }}>
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
                    <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                      4.{index + 7}
                    </span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Download size={12} style={{ color: 'var(--color-text-muted)' }} />
                    <span className="text-xs" style={{ color: 'var(--color-text-muted)' }}>
                      {(index + 1) * 3.5}w
                    </span>
                  </div>
                </div>

                {/* 应用按钮 - 增强版 */}
                <button 
                  aria-label="应用参数"
                  className="w-full glass-button flex items-center justify-center gap-2 py-3 ripple-container"
                >
                  <Sparkles size={16} style={{ color: 'var(--color-accent-primary)' }} />
                  <span className="text-sm font-semibold" style={{ color: 'var(--color-accent-primary)' }}>
                    应用参数
                  </span>
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* 空状态 */}
        {filteredPresets.length === 0 && (
          <div className="flex flex-col items-center justify-center py-16 animate-liquid-fade">
            <div 
              className="w-20 h-20 rounded-2xl flex items-center justify-center mb-4 glass-card"
            >
              <Filter size={32} style={{ color: 'var(--color-text-muted)' }} />
            </div>
            <p className="text-base mb-2" style={{ color: 'var(--color-text-tertiary)' }}>
              暂无精选预设
            </p>
            <p className="text-sm" style={{ color: 'var(--color-text-muted)' }}>
              请调整筛选条件
            </p>
          </div>
        )}

        {/* 底部提示 */}
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

export default FeaturedScreen;