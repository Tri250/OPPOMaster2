import React, { useState } from 'react';
import { useAppStore, featuredPresets } from '../store/appStore';
import { 
  Search, Filter, Heart, Sparkles, Check, Crown, Star, Download, Camera, Aperture, Gauge, Sun,
  Users, MessageCircle, Share2, Bookmark, Flame, TrendingUp, Eye, Zap, Award
} from 'lucide-react';

const brands = ['OPPO', 'realme', 'vivo', '荣耀', '小米'];
const scenes = ['人像', '风景', '夜景', '美食', '街拍', '建筑'];

// 2026年热门博主推荐
const hotBloggers2026 = [
  { 
    id: 1, 
    name: '摄影师小王', 
    avatar: '📷', 
    followers: '128.5w', 
    posts: 256, 
    verified: true,
    specialty: '人像写真',
    hot: true,
  },
  { 
    id: 2, 
    name: '风光大师', 
    avatar: '🏔️', 
    followers: '89.2w', 
    posts: 189, 
    verified: true,
    specialty: '风景摄影',
    hot: false,
  },
  { 
    id: 3, 
    name: '胶片控', 
    avatar: '🎬', 
    followers: '67.8w', 
    posts: 142, 
    verified: true,
    specialty: '胶片调色',
    hot: true,
  },
  { 
    id: 4, 
    name: '美食猎人', 
    avatar: '🍜', 
    followers: '45.6w', 
    posts: 98, 
    verified: false,
    specialty: '美食探店',
    hot: false,
  },
];

const FeaturedScreen: React.FC = () => {
  const { selectedBrand, setSelectedBrand, selectedScene, setSelectedScene } = useAppStore();
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [selectedPreset, setSelectedPreset] = useState<string | null>(null);

  const filteredPresets = featuredPresets.filter((preset) => {
    const brandMatch = !selectedBrand || preset.brand === selectedBrand;
    const sceneMatch = !selectedScene || preset.tags.includes(selectedScene);
    return brandMatch && sceneMatch;
  });

  const toggleFavorite = (id: string) => {
    setFavorites(prev => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header - 哈苏大师模式风格 */}
      <div className="px-4 pt-2 pb-3 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-xl font-bold text-white">精选推荐</h1>
            <div className="flex items-center gap-1 px-2 py-0.5 rounded-full bg-gradient-to-r from-[#FF6B35] to-[#FF9800] text-[9px] font-bold text-white">
              <Crown size={10} />
              <span>大师级</span>
            </div>
          </div>
          <p className="text-white/50 text-xs">OPPO Find X7 Ultra 影像参数库</p>
        </div>
        <div className="flex gap-2">
          <button className="p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors">
            <Search size={18} className="text-white/70" />
          </button>
          <button className="p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors">
            <Filter size={18} className="text-white/70" />
          </button>
        </div>
      </div>

      {/* 2026热门博主推荐 - 微博风格 */}
      <div className="px-4 pb-3">
        <div className="flex items-center justify-between mb-2">
          <div className="flex items-center gap-2">
            <Users size={14} className="text-[#FF6B35]" />
            <span className="text-white/70 text-xs font-medium">热门博主</span>
          </div>
          <button className="text-[#FF6B35] text-xs">查看全部</button>
        </div>
        <div className="flex gap-3 overflow-x-auto scrollbar-hide pb-1">
          {hotBloggers2026.map((blogger) => (
            <div
              key={blogger.id}
              className="flex-shrink-0 w-28 p-3 rounded-xl bg-white/5 border border-white/5 text-center"
            >
              <div className="relative w-12 h-12 mx-auto mb-2">
                <div className="w-12 h-12 rounded-full bg-gradient-to-br from-[#FF6B35] to-[#FF9800] flex items-center justify-center text-2xl">
                  {blogger.avatar}
                </div>
                {blogger.verified && (
                  <div className="absolute -bottom-0.5 -right-0.5 w-4 h-4 rounded-full bg-blue-500 flex items-center justify-center">
                    <Check size={10} className="text-white" />
                  </div>
                )}
              </div>
              <h4 className="text-white text-xs font-medium truncate">{blogger.name}</h4>
              <p className="text-white/40 text-[10px] mt-0.5">{blogger.followers}粉丝</p>
              <div className="flex items-center justify-center gap-1 mt-1">
                {blogger.hot && <Flame size={10} className="text-[#FF6B35]" />}
                <span className="text-white/50 text-[10px]">{blogger.specialty}</span>
              </div>
            </div>
          ))}
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
            全部
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
      {(selectedBrand || selectedScene) && (
        <div className="px-4 pb-2 flex items-center gap-2">
          <span className="text-[#FF6B35] text-xs">已筛选 {filteredPresets.length} 条</span>
          <button
            onClick={() => {
              setSelectedBrand(null);
              setSelectedScene(null);
            }}
            className="text-white/40 text-xs hover:text-white/60"
          >
            清空筛选
          </button>
        </div>
      )}

      {/* Preset Grid */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <div className="grid grid-cols-2 gap-3">
          {filteredPresets.map((preset) => (
            <div
              key={preset.id}
              className="group relative rounded-2xl overflow-hidden bg-[#1a1a1a] cursor-pointer"
              onClick={() => setSelectedPreset(selectedPreset === preset.id ? null : preset.id)}
            >
              {/* Image */}
              <div className="aspect-[4/3] overflow-hidden">
                <img
                  src={preset.coverPath}
                  alt={preset.name}
                  className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
                />
              </div>

              {/* HNCS Badge */}
              {preset.isHncs && (
                <div className="absolute top-2 left-2 px-2 py-1 bg-gradient-to-r from-[#FF6B35] to-[#FF9800] backdrop-blur-sm rounded-lg text-[9px] font-bold text-white z-20 flex items-center gap-1 shadow-lg shadow-orange-500/30">
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
                className="absolute top-2 right-2 p-2 rounded-lg bg-black/50 backdrop-blur-sm transition-all duration-200 hover:bg-black/70"
                onClick={(e) => {
                  e.stopPropagation();
                  toggleFavorite(preset.id);
                }}
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

                {/* 参数预览 */}
                <div className="grid grid-cols-2 gap-1.5 mb-3">
                  <div className="flex items-center gap-1 text-[10px] text-white/40">
                    <Sun size={10} />
                    <span>饱和度 {preset.saturation > 0 ? '+' : ''}{preset.saturation}</span>
                  </div>
                  <div className="flex items-center gap-1 text-[10px] text-white/40">
                    <Aperture size={10} />
                    <span>对比度 {preset.contrast > 0 ? '+' : ''}{preset.contrast}</span>
                  </div>
                </div>

                {/* Apply Button */}
                <button className="w-full flex items-center justify-center gap-1 py-2 rounded-lg bg-[#FF6B35]/20 hover:bg-[#FF6B35]/30 transition-colors">
                  <Sparkles size={14} className="text-[#FF6B35]" />
                  <span className="text-[#FF6B35] text-xs font-medium">应用参数</span>
                </button>
              </div>

              {/* 展开详情 */}
              {selectedPreset === preset.id && (
                <div className="px-3 pb-3 border-t border-white/5">
                  <div className="pt-3 space-y-2">
                    <h4 className="text-xs font-semibold text-white/80">影像参数</h4>
                    <div className="grid grid-cols-2 gap-2">
                      <div className="bg-white/5 rounded-lg p-2">
                        <div className="flex items-center gap-1 mb-1">
                          <Sun size={10} className="text-white/40" />
                          <span className="text-[10px] text-white/40">饱和度</span>
                        </div>
                        <span className="text-sm font-bold text-[#FF6B35]">{preset.saturation > 0 ? '+' : ''}{preset.saturation}</span>
                      </div>
                      <div className="bg-white/5 rounded-lg p-2">
                        <div className="flex items-center gap-1 mb-1">
                          <Aperture size={10} className="text-white/40" />
                          <span className="text-[10px] text-white/40">对比度</span>
                        </div>
                        <span className="text-sm font-bold text-[#FF6B35]">{preset.contrast > 0 ? '+' : ''}{preset.contrast}</span>
                      </div>
                      <div className="bg-white/5 rounded-lg p-2">
                        <div className="flex items-center gap-1 mb-1">
                          <Gauge size={10} className="text-white/40" />
                          <span className="text-[10px] text-white/40">色温</span>
                        </div>
                        <span className="text-sm font-bold text-[#FF6B35]">{preset.warmth > 0 ? '+' : ''}{preset.warmth}</span>
                      </div>
                      <div className="bg-white/5 rounded-lg p-2">
                        <div className="flex items-center gap-1 mb-1">
                          <Camera size={10} className="text-white/40" />
                          <span className="text-[10px] text-white/40">锐度</span>
                        </div>
                        <span className="text-sm font-bold text-[#FF6B35]">{preset.sharpness > 0 ? '+' : ''}{preset.sharpness}</span>
                      </div>
                    </div>
                  </div>
                </div>
              )}
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
      </div>
    </div>
  );
};

export default FeaturedScreen;
