import React, { useState, useCallback, useMemo, useEffect } from 'react';
import { useAppStore, homePresets, Preset } from '../store/appStore';
import { fetchPresetsFromSources as loadPresetsFromService } from '../services/presetService';
import { Heart, Search, RefreshCw, Sparkles, Crown, Download, Star, Filter, X, Zap, Grid as GridIcon, Edit2 } from 'lucide-react';
import PresetImageGallery from '../components/PresetImageGallery';
import PresetParameters, { PresetStats, ShootingTipsCard, UserComments, ApplyPresetButton, FavoriteButton, SimpleRelatedPresets } from '../components/PresetParameters';

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

      {/* Preset Detail Modal - 哈苏大师配方卡 */}
      {selectedPreset && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm"
          onClick={() => setSelectedPreset(null)}
        >
          <div
            className="relative w-full max-w-md bg-[#0a0a0a] rounded-2xl overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 max-h-[90vh] overflow-y-auto"
            onClick={(e) => e.stopPropagation()}
          >
            {/* 顶栏 */}
            <div className="sticky top-0 z-10 bg-[#0a0a0a]/95 backdrop-blur-sm px-4 py-3 flex items-center justify-between border-b border-white/5">
              <span className="text-white/50 text-sm">← 预设详情</span>
              <div className="flex items-center gap-2">
                <button className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 transition-colors">
                  <GridIcon size={16} className="text-white/50" />
                </button>
                <button className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 transition-colors">
                  <Edit2 size={16} className="text-white/50" />
                </button>
                <button
                  onClick={() => toggleFavorite(selectedPreset.id)}
                  className={`p-1.5 rounded-lg transition-colors ${
                    favorites.has(selectedPreset.id)
                      ? 'bg-red-500/20'
                      : 'bg-white/5 hover:bg-white/10'
                  }`}
                >
                  <Heart size={16} className={favorites.has(selectedPreset.id) ? 'text-red-400 fill-red-400' : 'text-white/50'} />
                </button>
                <button
                  onClick={() => setSelectedPreset(null)}
                  className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 transition-colors"
                >
                  <X size={16} className="text-white/50" />
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
                <h2 className="text-lg font-bold text-white">{selectedPreset.name}</h2>
                {selectedPreset.isHncs && (
                  <span className="px-1.5 py-0.5 rounded bg-[#FF6B35] text-[9px] font-bold text-white flex items-center gap-0.5">
                    <Crown size={10} />
                    HNCS
                  </span>
                )}
                {selectedPreset.isHncs && (
                  <span className="px-1.5 py-0.5 rounded bg-yellow-500/80 text-[9px] font-bold text-white flex items-center gap-0.5">
                    <Zap size={10} />
                    PRO
                  </span>
                )}
              </div>
              <p className="text-white/50 text-sm mb-2">@{selectedPreset.author}</p>
              
              {/* 标签 */}
              <div className="flex flex-wrap gap-1.5 mb-4">
                {selectedPreset.tags.map((tag) => (
                  <span
                    key={tag}
                    className="px-2 py-0.5 bg-white/5 rounded-full text-[10px] text-white/60"
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
            <div className="sticky bottom-0 bg-[#0a0a0a]/95 backdrop-blur-sm px-4 py-3 border-t border-white/5 flex gap-3">
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
