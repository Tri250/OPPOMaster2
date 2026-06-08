import React, { useState, useEffect } from 'react';
import { useAppStore } from '../../store/appStore';
import { cloudSyncService, syncModules, cdnSources, BrandSyncState, SyncModule } from '../../services/cloudSyncService';
import { ArrowLeft, Cloud, CloudOff, RefreshCw, Check, Smartphone, Database, Palette, Heart, Settings, ToggleLeft, ToggleRight } from 'lucide-react';

const moduleIcons: Record<string, React.ElementType> = {
  presets: Database,
  watermarks: Palette,
  favorites: Heart,
  settings: Settings,
};

const CloudSyncPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [brands, setBrands] = useState<BrandSyncState[]>([]);
  const [modules, setModules] = useState<SyncModule[]>([]);
  const [isFullSyncing, setIsFullSyncing] = useState(false);
  const [syncingBrandId, setSyncingBrandId] = useState<string | null>(null);

  useEffect(() => {
    // 初始化时从服务获取状态
    const state = cloudSyncService.getState();
    setBrands(state.brands);
    setModules(state.modules);
  }, []);

  // 切换品牌连接状态
  const handleToggleBrand = (brandId: string) => {
    const brand = brands.find(b => b.id === brandId);
    if (brand) {
      if (brand.isConnected) {
        cloudSyncService.disconnectBrand(brandId);
        brand.isConnected = false;
      } else {
        cloudSyncService.connectBrand(brandId);
        brand.isConnected = true;
      }
      setBrands([...brands]);
    }
  };

  // 同步单个品牌
  const handleSyncBrand = async (brandId: string) => {
    setSyncingBrandId(brandId);
    
    await cloudSyncService.syncBrandModules(brandId, (moduleId, progress) => {
      setBrands(prev => prev.map(b => {
        if (b.id === brandId) {
          return {
            ...b,
            modules: b.modules.map(m => 
              m.id === moduleId ? { ...m, status: 'syncing', progress } : m
            )
          };
        }
        return b;
      }));
    });

    // 更新品牌状态为完成
    setBrands(prev => prev.map(b => {
      if (b.id === brandId) {
        return {
          ...b,
          modules: b.modules.map(m => ({ ...m, status: 'completed', progress: 100 })),
          lastSyncTime: Date.now()
        };
      }
      return b;
    }));
    
    setSyncingBrandId(null);
  };

  // 同步所有品牌
  const handleSyncAll = async () => {
    setIsFullSyncing(true);
    
    await cloudSyncService.syncAllBrands((brandId, moduleId, progress) => {
      setBrands(prev => prev.map(b => {
        if (b.id === brandId) {
          return {
            ...b,
            modules: b.modules.map(m => 
              m.id === moduleId ? { ...m, status: 'syncing', progress } : m
            )
          };
        }
        return b;
      }));
    });

    // 更新所有品牌为完成状态
    setBrands(prev => prev.map(b => ({
      ...b,
      modules: b.modules.map(m => ({ ...m, status: 'completed', progress: 100 })),
      lastSyncTime: Date.now()
    })));
    setIsFullSyncing(false);
  };

  // 计算已连接品牌数量
  const connectedCount = brands.filter(b => b.isConnected).length;

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button 
          onClick={goBack}
          className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">云同步</h1>
        <div className="ml-auto flex items-center gap-2">
          <div className="px-2 py-1 rounded-full bg-[#4CAF50]/20 border border-[#4CAF50]/30">
            <span className="text-[#4CAF50] text-xs font-medium">{connectedCount}/{brands.length} 已连接</span>
          </div>
        </div>
      </div>

      {/* Global Sync */}
      <div className="px-4 py-4">
        <div className="p-4 rounded-2xl bg-[#FF6B35]/10 border border-[#FF6B35]/20">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-[#FF6B35]/20 flex items-center justify-center">
                <Cloud size={20} className="text-[#FF6B35]" />
              </div>
              <div>
                <p className="text-white font-medium">全部同步</p>
                <p className="text-white/50 text-xs">同步所有已连接品牌的数据</p>
              </div>
            </div>
            <button
              onClick={handleSyncAll}
              disabled={isFullSyncing}
              className={`px-4 py-2 rounded-xl flex items-center gap-2 text-sm font-medium transition-all ${
                isFullSyncing 
                  ? 'bg-white/5 text-white/50' 
                  : 'bg-[#FF6B35] text-white hover:bg-[#FF6B35]/80'
              }`}
            >
              <RefreshCw size={16} className={isFullSyncing ? 'animate-spin' : ''} />
              <span>{isFullSyncing ? '同步中...' : '立即同步'}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Brand List */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <p className="text-white/50 text-xs mb-3">品牌 CDN 数据源</p>
        
        <div className="space-y-3">
          {brands.map((brand) => {
            const isSyncing = syncingBrandId === brand.id;
            const source = cdnSources[brand.id];
            
            return (
              <div 
                key={brand.id}
                className={`p-4 rounded-2xl transition-all ${
                  brand.isConnected 
                    ? 'bg-gradient-to-r from-white/10 to-white/5 border border-white/10' 
                    : 'bg-white/5 opacity-60'
                }`}
              >
                {/* Brand Header */}
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <div 
                      className="w-12 h-12 rounded-xl flex items-center justify-center font-bold text-white"
                      style={{ backgroundColor: `${brand.color}20`, color: brand.color }}
                    >
                      {brand.name.charAt(0)}
                    </div>
                    <div>
                      <p className="text-white font-medium">{brand.name}</p>
                      <p className="text-white/50 text-xs">
                        {brand.isConnected ? '已连接' : '未连接'}
                      </p>
                    </div>
                  </div>
                  
                  <div className="flex items-center gap-2">
                    {/* Toggle Button */}
                    <button
                      onClick={() => handleToggleBrand(brand.id)}
                      className="p-2 rounded-lg transition-colors"
                    >
                      {brand.isConnected ? (
                        <ToggleRight size={24} className="text-[#4CAF50]" />
                      ) : (
                        <ToggleLeft size={24} className="text-white/30" />
                      )}
                    </button>
                    
                    {/* Sync Button */}
                    {brand.isConnected && (
                      <button
                        onClick={() => handleSyncBrand(brand.id)}
                        disabled={isSyncing || isFullSyncing}
                        className={`p-2 rounded-lg transition-colors ${
                          isSyncing || isFullSyncing
                            ? 'bg-white/5 text-white/30'
                            : 'bg-[#FF6B35]/10 text-[#FF6B35] hover:bg-[#FF6B35]/20'
                        }`}
                      >
                        <RefreshCw size={18} className={isSyncing ? 'animate-spin' : ''} />
                      </button>
                    )}
                  </div>
                </div>

                {/* Modules */}
                {brand.isConnected && (
                  <div className="space-y-2 mt-3 pt-3 border-t border-white/10">
                    {brand.modules.map((module) => {
                      const Icon = moduleIcons[module.id] || Database;
                      const moduleSyncing = module.status === 'syncing';
                      
                      return (
                        <div 
                          key={module.id}
                          className="flex items-center gap-2 p-2 rounded-lg bg-white/5"
                        >
                          <div className="w-8 h-8 rounded-lg bg-[#FF6B35]/10 flex items-center justify-center">
                            <Icon size={14} className="text-[#FF6B35]" />
                          </div>
                          <div className="flex-1">
                            <p className="text-white text-xs font-medium">{module.name}</p>
                            {moduleSyncing ? (
                              <div className="flex items-center gap-2 mt-1">
                                <div className="h-1 w-16 bg-white/10 rounded-full overflow-hidden">
                                  <div 
                                    className="h-full bg-[#FF6B35] transition-all duration-200"
                                    style={{ width: `${module.progress}%` }}
                                  />
                                </div>
                                <span className="text-white/50 text-xs">{module.progress}%</span>
                              </div>
                            ) : module.status === 'completed' ? (
                              <div className="flex items-center gap-1 mt-1">
                                <Check size={10} className="text-[#4CAF50]" />
                                <span className="text-white/50 text-xs">已同步</span>
                              </div>
                            ) : (
                              <span className="text-white/30 text-xs mt-1">待同步</span>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Sync Info */}
        <div className="mt-4 p-4 rounded-2xl bg-white/5">
          <div className="flex items-start gap-3">
            <Smartphone size={18} className="text-[#FF6B35] mt-0.5" />
            <div>
              <p className="text-white text-sm font-medium">同步内容</p>
              <ul className="text-white/50 text-xs mt-2 space-y-1">
                <li>• 预设参数配置</li>
                <li>• 自定义水印模板</li>
                <li>• 收藏的预设列表</li>
                <li>• 应用设置偏好</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Tips */}
        <div className="mt-3 p-4 rounded-2xl bg-white/5">
          <p className="text-white/50 text-xs">
            提示：所有品牌 CDN 默认已开启同步。点击开关可单独控制每个品牌的同步状态。
          </p>
        </div>
      </div>
    </div>
  );
};

export default CloudSyncPage;