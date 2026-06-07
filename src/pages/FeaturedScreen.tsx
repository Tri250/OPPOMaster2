import React from 'react';
import { useAppStore, featuredPresets } from '../store/appStore';
import { Search, Filter, Heart, Sparkles, Check } from 'lucide-react';

const brands = ['OPPO', 'realme', 'vivo', '荣耀', '小米'];
const scenes = ['人像', '风景', '夜景', '美食', '街拍', '建筑'];

const FeaturedScreen: React.FC = () => {
  const { selectedBrand, setSelectedBrand, selectedScene, setSelectedScene } = useAppStore();

  const filteredPresets = featuredPresets.filter((preset) => {
    const brandMatch = !selectedBrand || preset.brand === selectedBrand;
    const sceneMatch = !selectedScene || preset.tags.includes(selectedScene);
    return brandMatch && sceneMatch;
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
          <button className="p-2 rounded-full bg-white/5 hover:bg-white/10 transition-colors">
            <Filter size={18} className="text-white/70" />
          </button>
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
            >
              {/* Image */}
              <div className="aspect-[4/3] overflow-hidden">
                <img
                  src={preset.coverPath}
                  alt={preset.name}
                  className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
                />
              </div>

              {/* NEW Badge */}
              {preset.isNew && (
                <div className="absolute top-2 left-2 px-1.5 py-0.5 bg-[#FF6B35] rounded text-[8px] font-bold text-white">
                  NEW
                </div>
              )}

              {/* Favorite Button */}
              <button className="absolute top-2 right-2 p-1.5 rounded-lg bg-black/50 backdrop-blur-sm transition-all duration-200 hover:bg-black/70">
                <Heart size={14} className="text-white/70" />
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
      </div>
    </div>
  );
};

export default FeaturedScreen;
