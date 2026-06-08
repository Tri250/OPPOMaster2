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
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header */}
      <div className="px-4 pt-2 pb-3 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-white">精选推荐</h1>
          <p className="text-white/50 text-xs">大师级影像参数库</p>
        </div>
        <div className="flex gap-2">
          <button className="p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors">
            <Search size={18} className="text-white/70" />
          </button>
          <button
            onClick={handleRefresh}
            className="p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors"
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
            placeholder="搜索精选预设..."
            className="w-full pl-9 pr-4 py-2 rounded-full bg-white/5 text-white text-sm border border-white/10 focus:border-[#FF6B35] outline-none transition-colors"
          />
        </div>
      </div>

      {/* Brand Filter */}
      <div className="px-4 pb-2">
        <div className="flex gap-2 overflow-x-auto scrollbar-hide">
          <button
            onClick={() => setSelectedBrand(null)}
            className={`px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-all duration-200 ${
              !selectedBrand
                ? 'bg-[#FF6B35] text-white'
                : 'bg-white/10 text-white/70 hover:bg-white/20'
            }`}
          >
            全部品牌
          </button>
          {brands.map((brand) => (
            <button
              key={brand}
              onClick={() => setSelectedBrand(brand)}
              className={`px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-all duration-200 ${
                selectedBrand === brand
                  ? 'bg-[#FF6B35] text-white'
                  : 'bg-white/10 text-white/70 hover:bg-white/20'
              }`}
            >
              {brand}
            </button>
          ))}
        </div>
      </div>

      {/* Scene Filter */}
      <div className="px-4 pb-3">
        <div className="flex gap-2 overflow-x-auto scrollbar-hide">
          <button
            onClick={() => setSelectedScene(null)}
            className={`px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-all duration-200 ${
              !selectedScene
                ? 'bg-[#FF6B35]/70 text-white'
                : 'bg-white/10 text-white/70 hover:bg-white/20'
            }`}
          >
            全部场景
          </button>
          {scenes.map((scene) => (
            <button
              key={scene}
              onClick={() => setSelectedScene(scene)}
              className={`px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-all duration-200 ${
                selectedScene === scene
                  ? 'bg-[#FF6B35]/70 text-white'
                  : 'bg-white/10 text-white/70 hover:bg-white/20'
              }`}
            >
              {scene}
            </button>
          ))}
        </div>
      </div>

      {/* Filter Result Count */}
      {(selectedBrand || selectedScene || searchQuery) && (
        <div className="px-4 pb-2 flex items-center gap-2">
          <span className="text-[#FF6B35] text-xs">已筛选 {filteredPresets.length} 条</span>
          <button
            onClick={() => {
              setSelectedBrand(null);
              setSelectedScene(null);
              setSearchQuery('');
            }}
            className="text-white/40 text-xs hover:text-white/60"
          >
            清空筛选
          </button>
        </div>
      )}

      {/* Pull to Refresh Indicator */}
      {refreshing && (
        <div className="flex items-center justify-center py-2">
          <RefreshCw size={20} className="text-[#FF6B35] animate-spin" />
        </div>
      )}

      {/* Preset Grid */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <div className="grid grid-cols-2 gap-3">
          {filteredPresets.map((preset, index) => (
            <div
              key={preset.id}
              className="group relative rounded-2xl overflow-hidden bg-[#1a1a1a] cursor-pointer transition-all duration-300 hover:scale-[1.02] hover:shadow-lg"
            >
              {/* Image */}
              <div className="aspect-[4/3] overflow-hidden">
                <img
                  src={preset.coverPath}
                  alt={preset.name}
                  className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
                  loading="lazy"
                />
              </div>

              {/* Overlay */}
              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent pointer-events-none" />

              {/* NEW Badge */}
              {preset.isNew && (
                <div className="absolute top-2 left-2 px-2 py-1 bg-[#4CAF50] rounded-lg text-[9px] font-bold text-white z-20 flex items-center gap-1">
                  <Sparkles size={10} />
                  <span>NEW</span>
                </div>
              )}

              {/* HNCS Badge */}
              {preset.isHncs && (
                <div className="absolute top-2 left-2 px-2 py-1 bg-gradient-to-r from-[#FF6B35] to-[#FF9800] rounded-lg text-[9px] font-bold text-white z-20 flex items-center gap-1">
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
                className="absolute top-2 right-2 p-2 rounded-lg bg-black/50 backdrop-blur-sm transition-all duration-200 hover:bg-black/70 z-20"
              >
                <Heart
                  size={16}
                  className={favorites.has(preset.id) ? 'text-red-500 fill-red-500' : 'text-white/70'}
                />
              </button>

              {/* Content */}
              <div className="p-3">
                <h3 className="text-white font-semibold text-sm mb-1 truncate">{preset.name}</h3>
                <div className="flex items-center gap-1 mb-2">
                  <span className="text-white/50 text-xs">{preset.author}</span>
                  {preset.isHncs && (
                    <>
                      <Check size={10} className="text-[#FF6B35]" />
                      <span className="text-[#FF6B35] text-[10px]">HNCS</span>
                    </>
                  )}
                </div>

                {/* Stats */}
                <div className="flex items-center gap-3 mb-2">
                  <div className="flex items-center gap-1">
                    <Star size={10} className="text-yellow-400 fill-yellow-400" />
                    <span className="text-white/50 text-[10px]">4.{index + 6}</span>
                  </div>
                  <div className="flex items-center gap-1">
                    <Download size={10} className="text-white/40" />
                    <span className="text-white/50 text-[10px]">{(index + 1) * 3.2}w</span>
                  </div>
                </div>

                {/* Apply Button */}
                <button className="w-full flex items-center justify-center gap-1 py-2 rounded-lg bg-[#FF6B35]/20 hover:bg-[#FF6B35]/30 transition-colors">
                  <Sparkles size={14} className="text-[#FF6B35]" />
                  <span className="text-[#FF6B35] text-xs font-medium">应用参数</span>
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Empty State */}
        {filteredPresets.length === 0 && (
          <div className="flex flex-col items-center justify-center py-12">
            <div className="w-16 h-16 rounded-full bg-white/5 flex items-center justify-center mb-4">
              <Filter size={24} className="text-white/30" />
            </div>
            <p className="text-white/50 text-sm">暂无精选预设</p>
            <p className="text-white/30 text-xs mt-1">请调整筛选条件</p>
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

      {/* Styles */}
      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default FeaturedScreen;
