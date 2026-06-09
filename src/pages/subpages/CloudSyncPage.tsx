import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft, Cloud, Check, Smartphone, Wifi, RefreshCw,
  Shield, Clock, Upload
} from 'lucide-react';

const CLOUD_PROVIDERS = [
  { id: 'oppo', name: 'OPPO Cloud', icon: Smartphone, color: '#1E90FF', status: 'connected' },
  { id: 'realme', name: 'realme Cloud', icon: Smartphone, color: '#FFD700', status: 'disconnected' },
  { id: 'vivo', name: 'vivo Cloud', icon: Smartphone, color: '#4169E1', status: 'disconnected' },
  { id: 'honor', name: '荣耀 Cloud', icon: Smartphone, color: '#32CD32', status: 'disconnected' },
];

const SYNC_ITEMS = [
  { id: 'presets', name: '预设同步', icon: Upload, enabled: true, lastSync: '2分钟前' },
  { id: 'luts', name: 'LUT 资源同步', icon: Upload, enabled: true, lastSync: '5分钟前' },
  { id: 'settings', name: '设置同步', icon: RefreshCw, enabled: false, lastSync: '从未同步' },
];

const CloudSyncPage: React.FC = () => {
  const { setCurrentSubPage } = useAppStore();
  const [isSyncing, setIsSyncing] = useState(false);

  const handleSync = () => {
    setIsSyncing(true);
    setTimeout(() => setIsSyncing(false), 2000);
  };

  return (
    <div className="h-full w-full bg-gray-50 flex flex-col overflow-hidden">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-4 py-3 flex items-center gap-3 shadow-sm">
        <button
          onClick={() => setCurrentSubPage(null)}
          className="p-2 -ml-2 hover:bg-gray-100 rounded-full transition-colors"
        >
          <ArrowLeft size={20} className="text-gray-700" />
        </button>
        <div className="flex items-center gap-2">
          <Cloud size={20} className="text-blue-500" />
          <h1 className="text-lg font-semibold text-gray-900">云同步</h1>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {/* Sync Status */}
        <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-2xl p-5 text-white">
          <div className="flex items-start gap-4">
            <div className="p-3 bg-white/20 rounded-xl">
              <Cloud size={32} className="text-white" />
            </div>
            <div className="flex-1">
              <h2 className="text-xl font-bold mb-1">同步状态</h2>
              <p className="text-blue-100 text-sm">自动同步已开启</p>
              <div className="flex items-center gap-2 mt-3">
                <span className="text-blue-100 text-xs">最后同步：2分钟前</span>
              </div>
            </div>
          </div>
          <button
            onClick={handleSync}
            disabled={isSyncing}
            className="w-full mt-4 py-3 bg-white/20 hover:bg-white/30 rounded-xl font-medium flex items-center justify-center gap-2 transition-colors disabled:opacity-50"
          >
            <RefreshCw size={20} className={isSyncing ? 'animate-spin' : ''} />
            {isSyncing ? '同步中...' : '立即同步'}
          </button>
        </div>

        {/* Cloud Providers */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">云服务提供商</h3>
          <div className="space-y-2">
            {CLOUD_PROVIDERS.map(provider => {
              const Icon = provider.icon;
              return (
                <div
                  key={provider.id}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-xl"
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-lg" style={{ backgroundColor: `${provider.color}20` }}>
                      <Icon size={20} style={{ color: provider.color }} />
                    </div>
                    <div>
                      <span className="text-sm font-medium text-gray-900">{provider.name}</span>
                      <p className="text-xs text-gray-500">
                        {provider.status === 'connected' ? '已连接' : '未连接'}
                      </p>
                    </div>
                  </div>
                  {provider.status === 'connected' ? (
                    <Check size={20} className="text-green-500" />
                  ) : (
                    <button className="text-sm text-blue-600 font-medium">连接</button>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Sync Items */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">同步内容</h3>
          <div className="space-y-2">
            {SYNC_ITEMS.map(item => {
              const Icon = item.icon;
              return (
                <div
                  key={item.id}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-xl"
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-blue-100 rounded-lg">
                      <Icon size={20} className="text-blue-600" />
                    </div>
                    <div>
                      <span className="text-sm font-medium text-gray-900">{item.name}</span>
                      <p className="text-xs text-gray-500">最后同步：{item.lastSync}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className={`text-xs px-2 py-1 rounded-full ${
                      item.enabled ? 'bg-green-100 text-green-700' : 'bg-gray-200 text-gray-600'
                    }`}>
                      {item.enabled ? '已开启' : '已关闭'}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Features */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">同步特性</h3>
          <div className="space-y-3">
            <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-xl">
              <div className="p-2 bg-green-100 rounded-lg">
                <Shield size={18} className="text-green-600" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-gray-900">端到端加密</h4>
                <p className="text-xs text-gray-500 mt-0.5">您的数据完全加密，安全可靠</p>
              </div>
            </div>
            <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-xl">
              <div className="p-2 bg-purple-100 rounded-lg">
                <Wifi size={18} className="text-purple-600" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-gray-900">Wi-Fi 自动同步</h4>
                <p className="text-xs text-gray-500 mt-0.5">仅在 Wi-Fi 下自动同步，节省流量</p>
              </div>
            </div>
            <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-xl">
              <div className="p-2 bg-orange-100 rounded-lg">
                <Clock size={18} className="text-orange-600" />
              </div>
              <div>
                <h4 className="text-sm font-medium text-gray-900">历史版本</h4>
                <p className="text-xs text-gray-500 mt-0.5">保留 30 天历史版本，随时回退</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CloudSyncPage;
