import React, { useState, useEffect } from 'react';
import { useAppStore } from '../../store/appStore';
import { cloudSyncService, syncModules, cdnSources, SyncModule } from '../../services/cloudSyncService';
import { ArrowLeft, Cloud, CloudOff, RefreshCw, Check, Link, Unlink, Smartphone, Database, Palette, Heart, Settings } from 'lucide-react';

const moduleIcons: Record<string, React.ElementType> = {
  presets: Database,
  watermarks: Palette,
  favorites: Heart,
  settings: Settings,
};

const CloudSyncPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [isConnected, setIsConnected] = useState(true);
  const [selectedBrand, setSelectedBrand] = useState<string>('oppo');
  const [modules, setModules] = useState<SyncModule[]>([]);
  const [isFullSyncing, setIsFullSyncing] = useState(false);

  useEffect(() => {
    // 初始化时从服务获取状态
    const state = cloudSyncService.getState();
    setIsConnected(state.isConnected);
    setSelectedBrand(state.connectedBrand || 'oppo');
    setModules(state.modules);
  }, []);

  const handleConnect = (brandId: string) => {
    setSelectedBrand(brandId);
    setIsConnected(true);
    cloudSyncService.connect(brandId);
  };

  const handleDisconnect = () => {
    setIsConnected(false);
    setSelectedBrand('');
    cloudSyncService.disconnect();
  };

  // 同步单个模块
  const handleSyncModule = async (moduleId: string) => {
    setModules(prev => prev.map(m => 
      m.id === moduleId ? { ...m, status: 'syncing', progress: 0 } : m
    ));

    await cloudSyncService.syncModule(moduleId, (progress) => {
      setModules(prev => prev.map(m => 
        m.id === moduleId ? { ...m, progress } : m
      ));
    });

    setModules(prev => prev.map(m => 
      m.id === moduleId ? { ...m, status: 'completed', progress: 100 } : m
    ));
  };

  // 同步所有模块
  const handleSyncAll = async () => {
    setIsFullSyncing(true);
    
    await cloudSyncService.syncAllModules((moduleId, progress) => {
      setModules(prev => prev.map(m => 
        m.id === moduleId ? { ...m, status: 'syncing', progress } : m
      ));
    });

    // 更新所有模块为完成状态
    setModules(prev => prev.map(m => ({ ...m, status: 'completed', progress: 100 })));
    setIsFullSyncing(false);
  };

  const selectedBrandInfo = cdnSources[selectedBrand];

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
        {isConnected && (
          <div className="ml-auto px-2 py-1 rounded-full bg-[#4CAF50]/20 border border-[#4CAF50]/30">
            <span className="text-[#4CAF50] text-xs font-medium">已连接</span>
          </div>
        )}
      </div>

      {/* Connection Status */}
      <div className="px-4 py-4">
        <div className={`p-4 rounded-2xl ${isConnected ? 'bg-[#4CAF50]/20 border border-[#4CAF50]/30' : 'bg-white/5'}`}>
          <div className="flex items-center gap-4">
            <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${isConnected ? 'bg-[#4CAF50]/20' : 'bg-white/10'}`}>
              {isConnected ? (
                <Cloud size={24} className="text-[#4CAF50]" />
              ) : (
                <CloudOff size={24} className="text-white/50" />
              )}
            </div>
            <div className="flex-1">
              <p className={`font-medium ${isConnected ? 'text-[#4CAF50]' : 'text-white'}`}>
                {isConnected ? '已连接' : '未连接'}
              </p>
              <p className="text-white/50 text-xs">
                {isConnected 
                  ? `已连接到 ${selectedBrandInfo?.name} CDN` 
                  : '选择品牌进行连接'}
              </p>
            </div>
            {isConnected && (
              <button
                onClick={handleDisconnect}
                className="p-2 rounded-lg bg-white/10 hover:bg-white/20 transition-colors"
              >
                <Unlink size={18} className="text-white/60" />
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Sync Modules */}
      {isConnected && (
        <div className="px-4 pb-4">
          <div className="flex items-center justify-between mb-3">
            <p className="text-white/50 text-xs">同步模块</p>
            <button
              onClick={handleSyncAll}
              disabled={isFullSyncing}
              className={`px-3 py-1.5 rounded-lg flex items-center gap-2 text-xs font-medium transition-all ${
                isFullSyncing 
                  ? 'bg-white/5 text-white/50' 
                  : 'bg-[#FF6B35]/20 text-[#FF6B35] hover:bg-[#FF6B35]/30'
              }`}
            >
              <RefreshCw size={14} className={isFullSyncing ? 'animate-spin' : ''} />
              <span>{isFullSyncing ? '同步中...' : '全部同步'}</span>
            </button>
          </div>

          <div className="space-y-2">
            {modules.map((module) => {
              const Icon = moduleIcons[module.id] || Database;
              const isSyncing = module.status === 'syncing';
              
              return (
                <div 
                  key={module.id}
                  className="p-3 rounded-xl bg-white/5 flex items-center gap-3"
                >
                  <div className="w-10 h-10 rounded-lg bg-[#FF6B35]/10 flex items-center justify-center">
                    <Icon size={18} className="text-[#FF6B35]" />
                  </div>
                  <div className="flex-1">
                    <p className="text-white text-sm font-medium">{module.name}</p>
                    <div className="flex items-center gap-2 mt-1">
                      {isSyncing ? (
                        <div className="flex items-center gap-2">
                          <div className="h-1.5 w-24 bg-white/10 rounded-full overflow-hidden">
                            <div 
                              className="h-full bg-[#FF6B35] transition-all duration-200"
                              style={{ width: `${module.progress}%` }}
                            />
                          </div>
                          <span className="text-white/50 text-xs">{module.progress}%</span>
                        </div>
                      ) : module.status === 'completed' ? (
                        <div className="flex items-center gap-1">
                          <Check size={12} className="text-[#4CAF50]" />
                          <span className="text-white/50 text-xs">已同步</span>
                        </div>
                      ) : (
                        <span className="text-white/30 text-xs">待同步</span>
                      )}
                    </div>
                  </div>
                  <button
                    onClick={() => handleSyncModule(module.id)}
                    disabled={isSyncing || isFullSyncing}
                    className={`p-2 rounded-lg transition-colors ${
                      isSyncing || isFullSyncing
                        ? 'bg-white/5 text-white/30'
                        : 'bg-[#FF6B35]/10 text-[#FF6B35] hover:bg-[#FF6B35]/20'
                    }`}
                  >
                    <RefreshCw size={16} className={isSyncing ? 'animate-spin' : ''} />
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Brand Selection */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <p className="text-white/50 text-xs mb-3">选择品牌 CDN</p>
        
        <div className="space-y-2">
          {Object.entries(cdnSources).map(([id, source]) => {
            const isSelected = selectedBrand === id;
            
            return (
              <button
                key={id}
                onClick={() => !isConnected && handleConnect(id)}
                disabled={isConnected && !isSelected}
                className={`w-full p-3 rounded-xl flex items-center gap-3 transition-all ${
                  isSelected
                    ? 'bg-gradient-to-r from-[#FF6B35]/20 to-transparent border border-[#FF6B35]/30'
                    : isConnected
                      ? 'bg-white/5 opacity-50'
                      : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                <div 
                  className="w-10 h-10 rounded-lg flex items-center justify-center font-bold text-white"
                  style={{ backgroundColor: `${source.color}20`, color: source.color }}
                >
                  {source.name.charAt(0)}
                </div>
                <div className="flex-1">
                  <p className="text-white text-sm font-medium">{source.name}</p>
                  <p className="text-white/50 text-xs">CDN 数据同步服务</p>
                </div>
                {isSelected && isConnected && (
                  <div className="w-5 h-5 rounded-full bg-[#FF6B35] flex items-center justify-center">
                    <Check size={12} className="text-white" />
                  </div>
                )}
              </button>
            );
          })}
        </div>

        {/* Sync Info */}
        <div className="mt-4 p-3 rounded-xl bg-white/5">
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
        <div className="mt-3 p-3 rounded-xl bg-white/5">
          <p className="text-white/50 text-xs">
            提示：云同步功能需要品牌手机系统支持。连接后可跨设备同步您的影像参数配置。
          </p>
        </div>
      </div>
    </div>
  );
};

export default CloudSyncPage;