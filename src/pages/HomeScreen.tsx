import React, { useState, useCallback, useMemo, useEffect } from 'react';
import { useAppStore, homePresets, Preset } from '../store/appStore';
import { fetchPresetsFromSources as loadPresetsFromService } from '../services/presetService';
import { Heart, Search, RefreshCw, Sparkles, Crown, Download, Star, Filter, X, Zap, Grid as GridIcon, Edit2 } from 'lucide-react';
import PresetImageGallery from '../components/PresetImageGallery';
import PresetParameters, { PresetStats, ShootingTipsCard, UserComments, ApplyPresetButton, FavoriteButton, SimpleRelatedPresets } from '../components/PresetParameters';
import { tokens } from '../styles/designTokens';

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
  const [selectedPreset, setSelectedPreset] = useState<Preset | null>(null);

  // 合并本地预设和网络获取的预设
  const allPresets = useMemo(() => [...homePresets, ...fetchedPresets], [fetchedPresets]);

  // 从预设源获取预设（统一走 presetService）
  const fetchPresetsFromSources = useCallback(async () => {
    try {
      const result = await loadPresetsFromService(presetSources);
      setFetchedPresets(result.presets);
    } catch (err) {
      console.error('Failed to fetch presets:', err);
    } finally {
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
  }, [fetchPresetsFromSources]);

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
    <div className="h-full flex flex-col bg-master-bg overflow-hidden">
      {/* Header */}
      <div className="px-lg pt-sm pb-md">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-sm">
            <h1 className="text-h1 font-bold text-master-text-primary">OMaster</h1>
            <div
              className="flex items-center gap-1 px-2.5 py-0.5 rounded-full text-micro font-bold text-white"
              style={{
                background: `linear-gradient(135deg, ${tokens.colors.accent}, ${tokens.colors.accentLight})`,
                boxShadow: tokens.shadows.glow,
              }}
            >
              <Crown size={10} />
              <span>哈苏大师</span>
            </div>
          </div>
          <button
            onClick={handleRefresh}
            disabled={refreshing}
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
            placeholder="搜索预设 / 作者 / 标签"
            className="w-full pl-9 pr-4 py-2.5 bg-transparent text-body text-master-text-primary outline-none placeholder:text-master-text-tertiary"
          />
        </div>
      </div>

      {/* Tab Bar */}
      <div className="px-lg pb-md">
        <div className="flex gap-1 overflow-x-auto scrollbar-hide border-b border-master-glass-border">
          {tabs.map((tab, index) => {
            const count = getTabCount(tab.key);
            const isSelected = selectedTab === index;
            return (
              <button
                key={tab.key}
                onClick={() => setSelectedTab(index)}
                className="flex-shrink-0 relative px-4 py-2.5 text-sm font-medium transition-all duration-normal"
                style={{
                  color: isSelected ? tokens.colors.textPrimary : tokens.colors.textTertiary,
                  transitionTimingFunction: tokens.animation.easing.spring,
                }}
              >
                <span className="flex items-center gap-1.5">
                  <span>{tab.label}</span>
                  {count > 0 && (
                    <span
                      className="text-micro px-1.5 rounded-full transition-colors duration-normal"
                      style={{
                        background: isSelected ? `${tokens.colors.accent}20` : tokens.colors.glass,
                        color: isSelected ? tokens.colors.accent : tokens.colors.textTertiary,
                      }}
                    >
                      {count}
                    </span>
                  )}
                </span>
                {isSelected && (
                  <div
                    className="absolute bottom-0 left-2 right-2 h-[3px] rounded-t-full"
                    style={{ background: tokens.colors.accent }}
                  />
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Brand Filter & Sort */}
      <div className="px-lg pb-md flex items-center gap-2 overflow-x-auto scrollbar-hide">
        {brands.map((brand) => {
          const isActive = activeBrand === brand.key;
          return (
            <button
              key={brand.key}
              onClick={() => setActiveBrand(brand.key)}
              className="flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium transition-all duration-normal active:scale-95"
              style={{
                background: isActive ? tokens.colors.accent : tokens.colors.glass,
                color: isActive ? tokens.colors.textPrimary : tokens.colors.textSecondary,
                boxShadow: isActive ? tokens.shadows.glow : 'none',
                transitionTimingFunction: tokens.animation.easing.spring,
              }}
            >
              {brand.label}
            </button>
          );
        })}

        {/* Sort Dropdown */}
        <div className="flex-shrink-0 ml-auto flex items-center gap-1">
          <Filter size={12} className="text-master-text-tertiary" />
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'newest' | 'popular' | 'rating')}
            className="bg-transparent text-master-text-tertiary text-xs outline-none cursor-pointer"
          >
            <option value="newest" className="bg-master-surface">最新</option>
            <option value="popular" className="bg-master-surface">最热</option>
            <option value="rating" className="bg-master-surface">评分</option>
          </select>
        </div>
      </div>

      {/* Pull to Refresh Indicator */}
      {refreshing && (
        <div className="flex items-center justify-center py-2">
          <RefreshCw size={20} className="animate-spin" style={{ color: tokens.colors.accent }} />
        </div>
      )}

      {/* Preset Grid */}
      <div className="flex-1 overflow-y-auto px-lg pb-lg scrollbar-hide">
        {filteredPresets.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 animate-fade-in-up">
            <div
              className="w-16 h-16 rounded-2xl flex items-center justify-center mb-4"
              style={{ background: tokens.colors.glass, border: `1px solid ${tokens.colors.glassBorder}` }}
            >
              <Sparkles size={28} className="text-master-text-tertiary" />
            </div>
            <p className="text-master-text-secondary text-sm mb-2">未找到匹配的预设</p>
            <p className="text-master-text-tertiary text-xs">请调整筛选条件</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {filteredPresets.map((preset, index) => (
              <div
                key={preset.id}
                onClick={() => setSelectedPreset(preset)}
                className={`group relative rounded-2xl overflow-hidden cursor-pointer transition-all duration-slow active:scale-[0.98] hover:shadow-medium ${getImageHeight(
                  index
                )}`}
                style={{
                  background: tokens.colors.surface,
                  transitionTimingFunction: tokens.animation.easing.spring,
                  animation: `fade-in-up 0.4s ${tokens.animation.easing.smooth} ${index * 0.04}s both`,
                }}
              >
                {/* Glass Border Effect */}
                <div
                  className="absolute inset-0 rounded-2xl border transition-colors duration-normal group-hover:border-master-glass-border-hover z-10 pointer-events-none"
                  style={{ borderColor: tokens.colors.glassBorder }}
                />

                {/* Image */}
                <img
                  src={preset.coverPath}
                  alt={preset.name}
                  className="w-full h-full object-cover transition-transform duration-slower group-hover:scale-110"
                  loading="lazy"
                />

                {/* Overlay */}
                <div className="absolute inset-0 bg-gradient-to-t from-black/85 via-black/25 to-transparent" />

                {/* HNCS Badge */}
                {preset.isHncs && (
                  <div
                    className="absolute top-2 left-2 px-2 py-1 backdrop-blur-md rounded-lg text-micro font-bold text-white z-20 flex items-center gap-1"
                    style={{
                      background: `linear-gradient(135deg, ${tokens.colors.accent}, ${tokens.colors.accentLight})`,
                      boxShadow: tokens.shadows.glow,
                    }}
                  >
                    <Crown size={10} />
                    <span>HNCS</span>
                  </div>
                )}

                {/* NEW Badge */}
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
                  className="absolute top-2 right-2 p-2 rounded-full transition-all duration-normal hover:scale-110 active:scale-95 z-20"
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
                <div className="absolute bottom-0 left-0 right-0 p-3 pr-12">
                  <h3 className="text-white font-semibold text-sm mb-0.5 truncate">{preset.name}</h3>
                  <p className="text-white/70 text-xs truncate">{preset.author}</p>

                  {/* Stats */}
                  <div className="flex items-center gap-3 mt-1.5">
                    <div className="flex items-center gap-1">
                      <Star size={10} className="text-yellow-400 fill-yellow-400" />
                      <span className="text-white/60 text-[10px]">4.{index + 5}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Download size={10} className="text-white/50" />
                      <span className="text-white/60 text-[10px]">{(index + 1) * 2.3}w</span>
                    </div>
                    {preset.brand && (
                      <div className="flex items-center gap-1 ml-auto">
                        <span className="text-white/50 text-[10px]">{preset.brand}</span>
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

      {/* Preset Detail Modal - 哈苏大师配方卡 */}
      {selectedPreset && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
          style={{ background: 'rgba(0,0,0,0.8)', backdropFilter: 'blur(12px)' }}
          onClick={() => setSelectedPreset(null)}
        >
          <div
            className="relative w-full max-w-md rounded-2xl overflow-hidden shadow-medium animate-scale-in max-h-[90vh] overflow-y-auto"
            style={{
              background: tokens.colors.background,
              border: `1px solid ${tokens.colors.glassBorder}`,
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* 顶栏 */}
            <div
              className="sticky top-0 z-10 px-4 py-3 flex items-center justify-between border-b"
              style={{
                background: `${tokens.colors.background}f2`,
                backdropFilter: 'blur(12px)',
                borderColor: tokens.colors.glassBorder,
              }}
            >
              <span className="text-master-text-secondary text-sm">← 预设详情</span>
              <div className="flex items-center gap-2">
                <button className="p-1.5 rounded-lg bg-master-glass hover:bg-master-glass-strong border border-master-glass-border transition-colors">
                  <GridIcon size={16} className="text-master-text-tertiary" />
                </button>
                <button className="p-1.5 rounded-lg bg-master-glass hover:bg-master-glass-strong border border-master-glass-border transition-colors">
                  <Edit2 size={16} className="text-master-text-tertiary" />
                </button>
                <button
                  onClick={() => toggleFavorite(selectedPreset.id)}
                  className="p-1.5 rounded-lg transition-colors"
                  style={{
                    background: favorites.has(selectedPreset.id) ? 'rgba(239,68,68,0.15)' : tokens.colors.glass,
                    border: `1px solid ${favorites.has(selectedPreset.id) ? 'rgba(239,68,68,0.3)' : tokens.colors.glassBorder}`,
                  }}
                >
                  <Heart size={16} className={favorites.has(selectedPreset.id) ? 'text-red-400 fill-red-400' : 'text-master-text-tertiary'} />
                </button>
                <button className="p-1.5 rounded-lg bg-master-glass hover:bg-master-glass-strong border border-master-glass-border transition-colors">
                  <X size={16} className="text-master-text-tertiary" />
                </button>
              </div>
            </div>

            {/* 图片轮播画廊 */}
            <div className="px-4 pt-4">
              <PresetImageGallery
                images={[
                  selectedPreset.coverPath,
                  selectedPreset.coverPath + '?v=2',
                  selectedPreset.coverPath + '?v=3'
                ]}
                isPro={selectedPreset.isHncs}
              />
            </div>

            {/* 预设信息 */}
            <div className="px-4 py-4">
              <div className="flex items-center gap-2 mb-1">
                <h2 className="text-lg font-bold text-master-text-primary">{selectedPreset.name}</h2>
                {selectedPreset.isHncs && (
                  <span
                    className="px-1.5 py-0.5 rounded text-micro font-bold text-white flex items-center gap-0.5"
                    style={{ background: tokens.colors.accent }}
                  >
                    <Crown size={10} />
                    HNCS
                  </span>
                )}
                {selectedPreset.isHncs && (
                  <span
                    className="px-1.5 py-0.5 rounded text-micro font-bold text-white flex items-center gap-0.5"
                    style={{ background: tokens.colors.warning }}
                  >
                    <Zap size={10} />
                    PRO
                  </span>
                )}
              </div>
              <p className="text-master-text-secondary text-sm mb-2">@{selectedPreset.author}</p>

              {/* 标签 */}
              <div className="flex flex-wrap gap-1.5 mb-4">
                {selectedPreset.tags.map((tag) => (
                  <span
                    key={tag}
                    className="px-2 py-0.5 rounded-full text-micro text-master-text-secondary"
                    style={{ background: tokens.colors.glass, border: `1px solid ${tokens.colors.glassBorder}` }}
                  >
                    #{tag}
                  </span>
                ))}
              </div>

              {/* 统计数据 */}
              <PresetStats
                downloads={12580}
                rating={4.9}
                ratingCount={856}
              />
            </div>

            {/* 拍摄建议 */}
            <div className="px-4 pb-4">
              <ShootingTipsCard
                description={{
                  title: '拍摄建议',
                  content: '【环境建议】日间户外或充足自然光【场景推荐】街拍、人像、风景、建筑【拍摄要点】适合追求经典胶片质感，建议使用黄金时刻拍摄'
                }}
              />
            </div>

            {/* 调色参数 */}
            <div className="px-4 pb-4">
              <PresetParameters
                sections={[
                  {
                    title: '🎨 调色参数',
                    items: [
                      { label: '滤镜', value: '复古100%', span: 1 },
                      { label: '饱和度', value: `+${selectedPreset.saturation}`, span: 1 },
                      { label: '对比度', value: `+${selectedPreset.contrast}`, span: 1 },
                      { label: '锐度', value: `+${selectedPreset.sharpness}`, span: 1 },
                      { label: '暗角', value: '开', span: 2 },
                    ]
                  }
                ]}
              />
            </div>

            {/* 关联推荐 */}
            <div className="px-4 pb-4">
              <SimpleRelatedPresets
                presets={[
                  { id: 'r1', name: '电影胶片', coverPath: selectedPreset.coverPath + '?r=1' },
                  { id: 'r2', name: '复古人像', coverPath: selectedPreset.coverPath + '?r=2' },
                  { id: 'r3', name: '清新风景', coverPath: selectedPreset.coverPath + '?r=3' },
                  { id: 'r4', name: '黑白经典', coverPath: selectedPreset.coverPath + '?r=4' },
                ]}
                onSelect={(id) => console.log('Selected:', id)}
              />
            </div>

            {/* 用户评价 */}
            <div className="px-4 pb-4">
              <UserComments
                comments={[
                  { id: 'c1', user: '摄影爱好者', content: '非常好用的预设！色彩还原很准确', rating: 5 },
                  { id: 'c2', user: '专业摄影师', content: '配合哈苏大师模式使用效果绝佳', rating: 5 },
                ]}
                onViewAll={() => console.log('View all comments')}
              />
            </div>

            {/* 底部操作栏 */}
            <div
              className="sticky bottom-0 px-4 py-3 border-t flex gap-3"
              style={{
                background: `${tokens.colors.background}f2`,
                backdropFilter: 'blur(12px)',
                borderColor: tokens.colors.glassBorder,
              }}
            >
              <FavoriteButton
                isFavorite={favorites.has(selectedPreset.id)}
                onToggle={() => toggleFavorite(selectedPreset.id)}
              />
              <ApplyPresetButton
                onApply={() => console.log('Apply preset:', selectedPreset.id)}
              />
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
