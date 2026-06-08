import React, { useState, useEffect } from 'react';
import { useAppStore, homePresets, Preset } from '../store/appStore';
import { cloudSyncService } from '../services/cloudSyncService';
import { Heart, RefreshCw, Cloud } from 'lucide-react';

const tabs = ['全部', '收藏', '我的'];

const HomeScreen: React.FC = () => {
  const { selectedTab, setSelectedTab } = useAppStore();
  const [presets, setPresets] = useState<Preset[]>(homePresets);
  const [isLoading, setIsLoading] = useState(false);
  const [isSynced, setIsSynced] = useState(false);

  // 自动从云同步加载预设
  useEffect(() => {
    const loadPresetsFromCloud = async () => {
      const state = cloudSyncService.getState();
      if (state.isConnected) {
        setIsLoading(true);
        try {
          const cloudPresets = await cloudSyncService.fetchPresets(state.connectedBrand);
          // 合并本地预设和云端预设
          setPresets([...cloudPresets, ...homePresets.slice(0, 3)]);
          setIsSynced(true);
        } catch (e) {
          // 使用本地预设
          setPresets(homePresets);
        }
        setIsLoading(false);
      } else {
        setPresets(homePresets);
      }
    };

    loadPresetsFromCloud();
  }, []);

  // 手动刷新
  const handleRefresh = async () => {
    setIsLoading(true);
    try {
      const cloudPresets = await cloudSyncService.fetchPresets();
      setPresets([...cloudPresets, ...homePresets.slice(0, 3)]);
      setIsSynced(true);
    } catch (e) {
      setPresets(homePresets);
    }
    setIsLoading(false);
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header */}
      <div className="px-4 pt-2 pb-3 flex items-center justify-between">
        <h1 className="text-xl font-bold text-white">OMaster</h1>
        <div className="flex items-center gap-2">
          {isSynced && (
            <div className="flex items-center gap-1 px-2 py-1 rounded-full bg-[#4CAF50]/20">
              <Cloud size={12} className="text-[#4CAF50]" />
              <span className="text-[#4CAF50] text-xs">已同步</span>
            </div>
          )}
          <button
            onClick={handleRefresh}
            disabled={isLoading}
            className="p-2 rounded-lg bg-white/10 hover:bg-white/20 transition-colors"
          >
            <RefreshCw size={16} className={`text-white/70 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
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
        {isLoading && presets.length === 0 ? (
          <div className="flex items-center justify-center h-64">
            <RefreshCw size={24} className="text-[#FF6B35] animate-spin" />
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-3">
            {presets.map((preset, index) => (
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

                {/* Cloud Sync Badge */}
                {preset.isNew && (
                  <div className="absolute top-2 left-2 ml-10 px-1.5 py-0.5 bg-[#4CAF50]/80 backdrop-blur-sm rounded text-[8px] font-bold text-white z-20">
                    云端
                  </div>
                )}

                {/* Favorite Button */}
                <button className="absolute top-2 right-2 p-1.5 rounded-full bg-black/40 backdrop-blur-sm transition-all duration-200 hover:bg-black/60 z-20">
                  <Heart size={14} className="text-white/70" />
                </button>
              </div>
            ))}
          </div>
        )}

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