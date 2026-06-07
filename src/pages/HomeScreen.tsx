import React from 'react';
import { useAppStore, homePresets } from '../store/appStore';
import { Camera, Palette, Droplets, Cpu, Images, SlidersHorizontal, Heart, ChevronRight } from 'lucide-react';

const featureEntries = [
  { id: 'scene', title: 'AI场景', icon: Camera, color: '#4CAF50', route: 'ai-fine-tune' as const },
  { id: 'fine-tune', title: 'AI微调', icon: Palette, color: '#9C27B0', route: 'ai-fine-tune' as const },
  { id: 'watermark', title: '水印', icon: Droplets, color: '#00BCD4', route: 'watermark' as const },
  { id: 'optimize', title: '优化', icon: Cpu, color: '#2196F3', route: 'ai-fine-tune' as const },
  { id: 'preset', title: '预设', icon: Images, color: '#FF9800', route: null },
  { id: 'param', title: '参数', icon: SlidersHorizontal, color: '#E91E63', route: 'param-adjust' as const },
];

const tabs = ['全部', '收藏', '我的'];

const HomeScreen: React.FC = () => {
  const { selectedTab, setSelectedTab, navigateToSubPage } = useAppStore();

  const handleFeatureClick = (route: string | null) => {
    if (route) {
      navigateToSubPage(route as any);
    }
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header */}
      <div className="px-4 pt-2 pb-3">
        <h1 className="text-xl font-bold text-white">OMaster</h1>
      </div>

      {/* Feature Entry Row */}
      <div className="px-4 pb-3">
        <div className="flex gap-2">
          {featureEntries.map((entry) => (
            <button
              key={entry.id}
              onClick={() => handleFeatureClick(entry.route)}
              className="flex-1 flex flex-col items-center gap-1 p-2 rounded-xl transition-all duration-200 hover:scale-105 active:scale-95 group relative overflow-hidden"
              style={{ backgroundColor: `${entry.color}15` }}
            >
              {/* Glass Effect */}
              <div className="absolute inset-0 bg-gradient-to-br from-white/10 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
              
              <div className="relative z-10">
                <entry.icon size={18} style={{ color: entry.color }} />
              </div>
              <span className="relative z-10 text-[10px] font-medium" style={{ color: entry.color }}>
                {entry.title}
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Tab Bar */}
      <div className="px-4 pb-3">
        <div className="flex gap-6 border-b border-white/10">
          {tabs.map((tab, index) => (
            <button
              key={tab}
              onClick={() => setSelectedTab(index)}
              className={`relative pb-2 text-sm font-medium transition-colors duration-200 ${
                selectedTab === index ? 'text-[#FF6B35]' : 'text-white/50 hover:text-white/70'
              }`}
            >
              {tab}
              {selectedTab === index && (
                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#FF6B35] rounded-full" />
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Preset Grid */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 scrollbar-hide">
        <div className="grid grid-cols-2 gap-3">
          {homePresets.map((preset, index) => (
            <div
              key={preset.id}
              className={`group relative rounded-2xl overflow-hidden bg-[#1a1a1a] cursor-pointer transition-all duration-300 hover:scale-[1.02] hover:shadow-lg ${
                index % 3 === 0 ? 'aspect-[3/4]' : index % 3 === 1 ? 'aspect-square' : 'aspect-[4/5]'
              }`}
            >
              {/* Glass Border Effect */}
              <div className="absolute inset-0 rounded-2xl border border-white/5 group-hover:border-white/10 transition-colors z-10 pointer-events-none" />
              
              {/* Image */}
              <img
                src={preset.coverPath}
                alt={preset.name}
                className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
              />

              {/* Overlay */}
              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />

              {/* Content */}
              <div className="absolute bottom-0 left-0 right-0 p-3">
                <h3 className="text-white font-semibold text-sm mb-0.5">{preset.name}</h3>
                <p className="text-white/60 text-xs">{preset.author}</p>
              </div>

              {/* HNCS Badge */}
              {preset.isHncs && (
                <div className="absolute top-2 left-2 px-1.5 py-0.5 bg-[#FF6B35]/80 backdrop-blur-sm rounded text-[8px] font-bold text-white z-20">
                  HNCS
                </div>
              )}

              {/* Favorite Button */}
              <button className="absolute top-2 right-2 p-1.5 rounded-full bg-black/40 backdrop-blur-sm transition-all duration-200 hover:bg-black/60 z-20">
                <Heart size={14} className="text-white/70" />
              </button>
            </div>
          ))}
        </div>

        {/* Loading Hint */}
        <div className="py-6 text-center">
          <div className="w-16 h-0.5 mx-auto bg-gradient-to-r from-transparent via-[#FF6B35]/50 to-transparent mb-3" />
          <p className="text-[#FF6B35]/80 text-xs font-medium tracking-wider">持续更新 敬请期待</p>
          <div className="w-16 h-0.5 mx-auto bg-gradient-to-r from-transparent via-[#FF6B35]/50 to-transparent mt-3" />
        </div>
      </div>
    </div>
  );
};

export default HomeScreen;
