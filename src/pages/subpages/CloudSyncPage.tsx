import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Cloud, CloudOff, RefreshCw, Check, Link, Unlink, Smartphone } from 'lucide-react';

const brands = [
  { id: 'oppo', name: 'OPPO', color: '#1BAA52', icon: 'O' },
  { id: 'realme', name: 'realme', color: '#FFC107', icon: 'R' },
  { id: 'vivo', name: 'vivo', color: '#415FFF', icon: 'V' },
  { id: 'honor', name: '荣耀', color: '#00BFFF', icon: 'H' },
  { id: 'xiaomi', name: '小米', color: '#FF6900', icon: 'M' },
];

const CloudSyncPage: React.FC = () => {
  const { goBack } = useAppStore();
  const [isConnected, setIsConnected] = useState(true);
  const [selectedBrand, setSelectedBrand] = useState<string>('oppo');
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncProgress, setSyncProgress] = useState(0);

  const handleConnect = (brandId: string) => {
    setSelectedBrand(brandId);
    setIsConnected(true);
  };

  const handleDisconnect = () => {
    setIsConnected(false);
    setSelectedBrand(null);
  };

  const handleSync = () => {
    setIsSyncing(true);
    setSyncProgress(0);
    
    const interval = setInterval(() => {
      setSyncProgress(prev => {
        if (prev >= 100) {
          clearInterval(interval);
          setIsSyncing(false);
          return 100;
        }
        return prev + 10;
      });
    }, 300);
  };

  const selectedBrandInfo = brands.find(b => b.id === selectedBrand);

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

      {/* Sync Progress */}
      {isSyncing && (
        <div className="px-4 pb-4">
          <div className="p-4 rounded-2xl bg-white/5">
            <div className="flex items-center gap-3 mb-3">
              <RefreshCw size={18} className="text-[#FF6B35] animate-spin" />
              <span className="text-white text-sm">正在同步...</span>
            </div>
            <div className="h-2 bg-white/10 rounded-full overflow-hidden">
              <div 
                className="h-full bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] transition-all duration-300"
                style={{ width: `${syncProgress}%` }}
              />
            </div>
            <p className="text-white/50 text-xs mt-2">{syncProgress}% 完成</p>
          </div>
        </div>
      )}

      {/* Sync Button */}
      {isConnected && !isSyncing && (
        <div className="px-4 pb-4">
          <button
            onClick={handleSync}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90 active:scale-98"
          >
            <RefreshCw size={18} />
            <span>立即同步</span>
          </button>
        </div>
      )}

      {/* Brand Selection */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <p className="text-white/50 text-xs mb-3">选择品牌 CDN</p>
        
        <div className="space-y-3">
          {brands.map((brand) => {
            const isSelected = selectedBrand === brand.id;
            
            return (
              <button
                key={brand.id}
                onClick={() => !isConnected && handleConnect(brand.id)}
                disabled={isConnected && !isSelected}
                className={`w-full p-4 rounded-2xl flex items-center gap-4 transition-all ${
                  isSelected
                    ? 'bg-gradient-to-r from-[#FF6B35]/20 to-transparent border border-[#FF6B35]/30'
                    : isConnected
                      ? 'bg-white/5 opacity-50'
                      : 'bg-white/5 hover:bg-white/10'
                }`}
              >
                <div 
                  className="w-12 h-12 rounded-xl flex items-center justify-center font-bold text-white"
                  style={{ backgroundColor: `${brand.color}20`, color: brand.color }}
                >
                  {brand.icon}
                </div>
                <div className="flex-1">
                  <p className="text-white font-medium">{brand.name}</p>
                  <p className="text-white/50 text-xs">CDN 数据同步服务</p>
                </div>
                {isSelected && (
                  <div className="w-6 h-6 rounded-full bg-[#FF6B35] flex items-center justify-center">
                    <Check size={14} className="text-white" />
                  </div>
                )}
              </button>
            );
          })}
        </div>

        {/* Sync Info */}
        <div className="mt-6 p-4 rounded-2xl bg-white/5">
          <div className="flex items-start gap-3">
            <Smartphone size={20} className="text-[#FF6B35] mt-0.5" />
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
        <div className="mt-4 p-4 rounded-2xl bg-white/5">
          <p className="text-white/50 text-xs">
            提示：云同步功能需要品牌手机系统支持。连接后可跨设备同步您的影像参数配置。
          </p>
        </div>
      </div>
    </div>
  );
};

export default CloudSyncPage;