import React, { useState, useCallback } from 'react';
import { useAppStore, featuredPresets } from '../store/appStore';
import { Search, Filter, Heart, Sparkles, Check, RefreshCw, Download, Star, Crown } from 'lucide-react';
import { tokens } from '../styles/designTokens';

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
    <div className="h-full flex flex-col bg-master-bg overflow-hidden">
      {/* Header */}
      <div className="px-lg pt-sm pb-md flex items-center justify-between">
        <div>
          <h1 className="text-h1 font-bold text-master-text-primary">精选推荐</h1>
          <p className="text-master-text-tertiary text-xs mt-0.5">大师级影像参数库</p>
        </div>
        <div className="flex gap-2">
          <button
            className="p-2 rounded-full bg-master-glass hover:bg-master-glass-strong border border-master-glass-border transition-all duration-normal active:scale-95"
            style={{ transitionTimingFunction: tokens.animation.easing.spring }}
          >
            <Search size={18} className="text-master-text-secondary" />
          </button>
          <button
            onClick={handleRefresh}
            className="p-2 rounded-full bg-master-glass hover:bg-master-glass-strong border border-master-glass-border transition-all duration-normal active:scale-95"
            style={{ transitionTimingFunction: tokens.animation.easing.spring }}
          >
            <RefreshCw size={18} className={`text-master-text-secondary ${refreshing ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Search Bar */}
      <div className="px-lg pb-md">
        <div
          className="relative rounded-full border transition-all duration-normal focus-within:border-master-accent focus-within:shadow-glow"
          style={{ background: tokens.colors.glass, borderColor: tokens.colors.glassBorder }}
        >
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-master-text-tertiary" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="搜索精选预设..."
            className="w-full pl-9 pr-4 py-2 bg-transparent text-sm text-master-text-primary outline-none placeholder:text-master-text-tertiary"
          />
        </div>
      </div>

      {/* Brand Filter */}
      <div className="px-lg pb-2">
        <div className="flex gap-2 overflow-x-auto scrollbar-hide">
          <FilterChip
            label="全部品牌"
            active={!selectedBrand}
            onClick={() => setSelectedBrand(null)}
            primary
          />
          {brands.map((brand) => (
            <FilterChip
              key={brand}
              label={brand}
              active={selectedBrand === brand}
              onClick={() => setSelectedBrand(brand)}
              primary
            />
          ))}
        </div>
      </div>

      {/* Scene Filter */}
      <div className="px-lg pb-md">
        <div className="flex gap-2 overflow-x-auto scrollbar-hide">
          <FilterChip
            label="全部场景"
            active={!selectedScene}
            onClick={() => setSelectedScene(null)}
          />
          {scenes.map((scene) => (
            <FilterChip
              key={scene}
              label={scene}
              active={selectedScene === scene}
              onClick={() => setSelectedScene(scene)}
            />
          ))}
        </div>
      </div>

      {/* Filter Result Count */}
      {(selectedBrand || selectedScene || searchQuery) && (
        <div className="px-lg pb-2 flex items-center gap-2 animate-fade-in-up">
          <span className="text-xs font-medium" style={{ color: tokens.colors.accent }}>
            已筛选 {filteredPresets.length} 条
          </span>
          <button
            onClick={() => {
              setSelectedBrand(null);
              setSelectedScene(null);
              setSearchQuery('');
            }}
            className="text-master-text-tertiary text-xs hover:text-master-text-secondary transition-colors"
          >
            清空筛选
          </button>
        </div>
      )}

      {/* Pull to Refresh Indicator */}
      {refreshing && (
        <div className="flex items-center justify-center py-2">
          <RefreshCw size={20} className="animate-spin" style={{ color: tokens.colors.accent }} />
        </div>
      )}

      {/* Preset Grid */}
      <div className="flex-1 overflow-y-auto px-lg pb-lg scrollbar-hide">
        <div className="grid grid-cols-2 gap-3">
          {filteredPresets.map((preset, index) => (
            <div
              key={preset.id}
              className="group relative rounded-2xl overflow-hidden bg-master-surface cursor-pointer transition-all duration-slow active:scale-[0.98] hover:shadow-medium"
              style={{
                transitionTimingFunction: tokens.animation.easing.spring,
                animation: `fade-in-up 0.4s ${tokens.animation.easing.smooth} ${index * 0.05}s both`,
              }}
            >
              {/* Glass Border */}
              <div
                className="absolute inset-0 rounded-2xl border transition-colors duration-normal group-hover:border-master-glass-border-hover z-10 pointer-events-none"
                style={{ borderColor: tokens.colors.glassBorder }}
              />

              {/* Image */}
              <div className="aspect-[4/3] overflow-hidden">
                <img
                  src={preset.coverPath}
                  alt={preset.name}
                  className="w-full h-full object-cover transition-transform duration-slower group-hover:scale-110"
                  loading="lazy"
                />
              </div>

              {/* Overlay */}
              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent pointer-events-none" />

              {/* Badges */}
              {preset.isHncs && (
                <div
                  className="absolute top-2 left-2 px-2 py-1 rounded-lg text-micro font-bold text-white z-20 flex items-center gap-1"
                  style={{
                    background: `linear-gradient(135deg, ${tokens.colors.accent}, ${tokens.colors.accentLight})`,
                    boxShadow: tokens.shadows.glow,
                  }}
                >
                  <Crown size={10} />
                  <span>HNCS</span>
                </div>
              )}
              {preset.isNew && !preset.isHncs && (
                <div
                  className="absolute top-2 left-2 px-2 py-1 rounded-lg text-micro font-bold text-white z-20 flex items-center gap-1"
                  style={{ background: tokens.colors.success }}
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
                className="absolute top-2 right-2 p-2 rounded-lg transition-all duration-normal hover:scale-110 active:scale-95 z-20"
                style={{
                  background: 'rgba(0,0,0,0.45)',
                  backdropFilter: 'blur(8px)',
                }}
              >
                <Heart
                  size={16}
                  className={favorites.has(preset.id) ? 'text-red-500 fill-red-500' : 'text-white/80'}
                />
              </button>

              {/* Content */}
              <div className="p-3">
                <h3 className="text-master-text-primary font-semibold text-sm mb-1 truncate">{preset.name}</h3>
                <div className="flex items-center gap-1 mb-2">
                  <span className="text-master-text-tertiary text-xs">{preset.author}</span>
                  {preset.isHncs && (
                    <>
                      <Check size={10} style={{ color: tokens.colors.accent }} />
                      <span className="text-micro" style={{ color: tokens.colors.accent }}>HNCS</span>
                    </>
                  )}
                </div>

                {/* Stats */}
                <div className="flex items-center gap-3 mb-3">
                  <div className="flex items-center gap-1">
                    <Star size={10} className="text-yellow-400 fill-yellow-400" />
                    <span className="text-master-text-tertiary text-micro">4.{index + 6}</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Download size={10} className="text-master-text-tertiary" />
                    <span className="text-master-text-tertiary text-micro">{(index + 1) * 3.2}w</span>
                  </div>
                </div>

                {/* Apply Button */}
                <button
                  className="w-full flex items-center justify-center gap-1 py-2 rounded-lg border transition-all duration-normal active:scale-95"
                  style={{
                    background: `${tokens.colors.accent}15`,
                    borderColor: `${tokens.colors.accent}30`,
                    color: tokens.colors.accent,
                    transitionTimingFunction: tokens.animation.easing.spring,
                  }}
                >
                  <Sparkles size={14} />
                  <span className="text-xs font-medium">应用参数</span>
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Empty State */}
        {filteredPresets.length === 0 && (
          <div className="flex flex-col items-center justify-center py-12 animate-fade-in-up">
            <div
              className="w-16 h-16 rounded-2xl flex items-center justify-center mb-4"
              style={{ background: tokens.colors.glass, border: `1px solid ${tokens.colors.glassBorder}` }}
            >
              <Filter size={24} className="text-master-text-tertiary" />
            </div>
            <p className="text-master-text-secondary text-sm">暂无精选预设</p>
            <p className="text-master-text-tertiary text-xs mt-1">请调整筛选条件</p>
          </div>
        )}

        {/* Loading Hint */}
        {filteredPresets.length > 0 && (
          <div className="py-8 text-center">
            <div
              className="w-16 h-0.5 mx-auto mb-3 rounded-full"
              style={{ background: `linear-gradient(90deg, transparent, ${tokens.colors.accent}80, transparent)` }}
            />
            <p className="text-xs font-medium tracking-wider" style={{ color: tokens.colors.accent }}>
              持续更新 敬请期待
            </p>
            <div
              className="w-16 h-0.5 mx-auto mt-3 rounded-full"
              style={{ background: `linear-gradient(90deg, transparent, ${tokens.colors.accent}80, transparent)` }}
            />
          </div>
        )}
      </div>

      {/* Styles */}
      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

/** 筛选胶囊按钮 */
interface FilterChipProps {
  label: string;
  active: boolean;
  onClick: () => void;
  primary?: boolean;
}

const FilterChip: React.FC<FilterChipProps> = ({ label, active, onClick, primary }) => (
  <button
    onClick={onClick}
    className="px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-all duration-normal active:scale-95"
    style={{
      background: active
        ? (primary ? tokens.colors.accent : `${tokens.colors.accent}70`)
        : tokens.colors.glass,
      color: active ? tokens.colors.textPrimary : tokens.colors.textSecondary,
      boxShadow: active ? tokens.shadows.glow : 'none',
      border: `1px solid ${active ? 'transparent' : tokens.colors.glassBorder}`,
      transitionTimingFunction: tokens.animation.easing.spring,
    }}
  >
    {label}
  </button>
);

export default FeaturedScreen;
