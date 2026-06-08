import React, { useState, useEffect } from 'react';
import { useAppStore } from '../store/appStore';
import { cloudSyncService, Preset as CloudPreset, BrandSyncState } from '../services/cloudSyncService';
import { Heart, RefreshCw, Cloud, Star, Download } from 'lucide-react';

const tabs = ['全部', '收藏', '我的'];

// 扩展预设类型以兼容本地预设
interface DisplayPreset extends CloudPreset {
  isLocal?: boolean;
}

const HomeScreen: React.FC = () => {
  const { selectedTab, setSelectedTab, navigateToSubPage } = useAppStore();
  const [presets, setPresets] = useState<DisplayPreset[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [brands, setBrands] = useState<BrandSyncState[]>([]);
  const [connectedCount, setConnectedCount] = useState(0);
  const [selectedPreset, setSelectedPreset] = useState<DisplayPreset | null>(null);

  // 订阅服务层状态变化（自动刷新）
  useEffect(() => {
    const unsubscribe = cloudSyncService.subscribe((state, syncedPresets) => {
      setBrands(state.brands);
      setConnectedCount(state.brands.filter(b => b.isConnected).length);
      
      // 合并云端预设和本地预设
      const localPresets: DisplayPreset[] = [
        {
          id: 'local_1',
          name: '清新CC胶片',
          coverPath: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=500&fit=crop',
          author: '@大师预设',
          brand: '本地',
          brandId: 'local',
          tags: ['胶片', '清新'],
          isNew: false,
          isHncs: true,
          isLocal: true,
          saturation: 5,
          contrast: 8,
          warmth: 3,
          sharpness: 10,
          description: '经典胶片风格，清新自然',
          downloadCount: 3200,
          rating: 4.2,
        },
        {
          id: 'local_2',
          name: '夜景氛围',
          coverPath: 'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=400&h=280&fit=crop',
          author: '@夜景专家',
          brand: '本地',
          brandId: 'local',
          tags: ['夜景', '氛围'],
          isNew: false,
          isHncs: false,
          isLocal: true,
          saturation: 15,
          contrast: 20,
          warmth: -5,
          sharpness: 18,
          description: '夜景氛围感强',
          downloadCount: 1800,
          rating: 4.0,
        },
      ];
      
      // 合并预设（云端预设优先）
      setPresets([...syncedPresets, ...localPresets]);
    });

    return () => unsubscribe();
  }, []);

  // 手动刷新
  const handleRefresh = async () => {
    setIsLoading(true);
    await cloudSyncService.refreshPresets();
    setIsLoading(false);
  };

  // 点击预设查看详情
  const handlePresetClick = (preset: DisplayPreset) => {
    setSelectedPreset(preset);
    // 导航到预设详情页面
    navigateToSubPage('preset-detail');
  };

  // 格式化下载量
  const formatDownloadCount = (count: number) => {
    if (count >= 10000) {
      return `${(count / 10000).toFixed(1)}万`;
    }
    return count.toString();
  };

  // 根据标签筛选预设
  const filteredPresets = presets.filter(preset => {
    if (selectedTab === 1) {
      // 收藏标签 - 这里暂时显示所有，实际应该有收藏状态
      return true;
    }
    if (selectedTab === 2) {
      // 我的 - 显示本地预设
      return preset.isLocal;
    }
    return true; // 全部
  });

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a] overflow-hidden">
      {/* Header */}
      <div className="px-4 pt-2 pb-3 flex items-center justify-between">
        <h1 className="text-xl font-bold text-white">OMaster</h1>
        <div className="flex items-center gap-2">
          {connectedCount > 0 && (
            <div className="flex items-center gap-1 px-2 py-1 rounded-full bg-[#4CAF50]/20">
              <Cloud size={12} className="text-[#4CAF50]" />
              <span className="text-[#4CAF50] text-xs">{connectedCount}品牌同步</span>
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
            {filteredPresets.map((preset, index) => (
              <div
                key={preset.id}
                onClick={() => handlePresetClick(preset)}
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
                  <h3 className="text-white font-semibold text-sm mb-0.5 truncate">{preset.name}</h3>
                  <p className="text-white/60 text-xs truncate">{preset.author}</p>
                  {/* Stats */}
                  <div className="flex items-center gap-2 mt-1">
                    <div className="flex items-center gap-1">
                      <Star size={10} className="text-[#FF6B35]" fill="#FF6B35" />
                      <span className="text-white/50 text-xs">{preset.rating || 4.5}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Download size={10} className="text-white/40" />
                      <span className="text-white/40 text-xs">{formatDownloadCount(preset.downloadCount || 0)}</span>
                    </div>
                  </div>
                </div>

                {/* HNCS Badge */}
                {preset.isHncs && (
                  <div className="absolute top-2 left-2 px-1.5 py-0.5 bg-[#FF6B35]/80 backdrop-blur-sm rounded text-[8px] font-bold text-white z-20">
                    HNCS
                  </div>
                )}

                {/* Cloud Sync Badge */}
                {!preset.isLocal && preset.isNew && (
                  <div className="absolute top-2 left-2 ml-10 px-1.5 py-0.5 bg-[#4CAF50]/80 backdrop-blur-sm rounded text-[8px] font-bold text-white z-20">
                    云端
                  </div>
                )}

                {/* Local Badge */}
                {preset.isLocal && (
                  <div className="absolute top-2 left-2 ml-10 px-1.5 py-0.5 bg-white/20 backdrop-blur-sm rounded text-[8px] text-white/70 z-20">
                    本地
                  </div>
                )}

                {/* Brand Badge */}
                {preset.brand && preset.brand.split(', ').length <= 2 && (
                  <div className="absolute top-2 right-2 mt-6 px-1.5 py-0.5 bg-white/20 backdrop-blur-sm rounded text-[8px] text-white/70 z-20">
                    {preset.brand}
                  </div>
                )}

                {/* Multi-brand Badge */}
                {preset.brand && preset.brand.split(', ').length > 2 && (
                  <div className="absolute top-2 right-2 mt-6 px-1.5 py-0.5 bg-[#FF6B35]/20 backdrop-blur-sm rounded text-[8px] text-[#FF6B35] z-20">
                    {preset.brand.split(', ').length}品牌
                  </div>
                )}

                {/* Favorite Button */}
                <button 
                  onClick={(e) => e.stopPropagation()}
                  className="absolute top-2 right-2 p-1.5 rounded-full bg-black/40 backdrop-blur-sm transition-all duration-200 hover:bg-black/60 z-20"
                >
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