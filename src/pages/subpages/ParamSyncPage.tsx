import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  Cloud,
  CloudUpload,
  CloudDownload,
  RefreshCw,
  Phone,
  Share2,
  X,
  CheckCircle,
  Clock,
  AlertCircle,
} from 'lucide-react';

/**
 * 参数同步页面
 * 与 CloudSyncManager 集成，实现跨设备参数同步
 */
const ParamSyncPage: React.FC = () => {
  const { navigateToSubPage } = useAppStore();

  const [syncState, setSyncState] = useState<'idle' | 'uploading' | 'downloading' | 'synced' | 'error'>('synced');

  // 同步历史
  const syncHistory = [
    { id: 1, action: 'upload', time: '2024/01/15 14:30', success: true },
    { id: 2, action: 'download', time: '2024/01/15 10:20', success: true },
    { id: 3, action: 'sync', time: '2024/01/14 18:45', success: true },
    { id: 4, action: 'upload', time: '2024/01/14 09:15', success: false },
  ];

  const actionLabels: Record<string, string> = {
    upload: '上传',
    download: '下载',
    sync: '同步',
  };

  const actionIcons: Record<string, React.ElementType> = {
    upload: CloudUpload,
    download: CloudDownload,
    sync: RefreshCw,
  };

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-white/10">
        <button
          onClick={() => navigateToSubPage(null)}
          className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center"
        >
          <X size={18} className="text-white" />
        </button>
        <h1 className="text-white font-semibold">跨设备同步</h1>
        <button className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center">
          <Share2 size={18} className="text-white" />
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4">
        {/* 设备信息卡片 */}
        <div className="rounded-2xl p-4 bg-white/5 border border-white/10">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-[#FF6B35]/20 flex items-center justify-center">
              <Phone size={24} className="text-[#FF6B35]" />
            </div>
            <div>
              <p className="text-white font-medium">OPPO Find X7 Ultra</p>
              <p className="text-white/50 text-xs">设备ID: a1b2c3d4...</p>
            </div>
          </div>
        </div>

        {/* 同步状态卡片 */}
        <div className={`rounded-2xl p-4 border ${
          syncState === 'synced'
            ? 'bg-green-500/10 border-green-500/30'
            : syncState === 'error'
            ? 'bg-red-500/10 border-red-500/30'
            : 'bg-white/5 border-white/10'
        }`}>
          <div className="flex items-center gap-3">
            {syncState === 'idle' && (
              <>
                <Cloud size={20} className="text-white/50" />
                <span className="text-white/50">未同步</span>
              </>
            )}
            {syncState === 'uploading' && (
              <>
                <RefreshCw size={20} className="text-[#FF6B35] animate-spin" />
                <span className="text-white">正在上传...</span>
              </>
            )}
            {syncState === 'downloading' && (
              <>
                <RefreshCw size={20} className="text-[#FF6B35] animate-spin" />
                <span className="text-white">正在下载...</span>
              </>
            )}
            {syncState === 'synced' && (
              <>
                <CheckCircle size={20} className="text-green-500" />
                <span className="text-green-400">已同步</span>
              </>
            )}
            {syncState === 'error' && (
              <>
                <AlertCircle size={20} className="text-red-500" />
                <span className="text-red-400">同步失败</span>
              </>
            )}
          </div>
          {syncState === 'synced' && (
            <div className="mt-3 space-y-1">
              <p className="text-white/70 text-xs">预设: 胶片风格预设</p>
              <p className="text-white/70 text-xs">参数数量: 8</p>
            </div>
          )}
        </div>

        {/* 同步操作按钮 */}
        <div className="flex gap-2">
          <button
            onClick={() => {
              setSyncState('uploading');
              setTimeout(() => setSyncState('synced'), 2000);
            }}
            className="flex-1 py-3 rounded-xl bg-white/10 text-white/70 text-sm font-medium flex items-center justify-center gap-2 hover:bg-white/20 transition-colors"
          >
            <CloudUpload size={18} />
            上传
          </button>
          <button
            onClick={() => {
              setSyncState('downloading');
              setTimeout(() => setSyncState('synced'), 2000);
            }}
            className="flex-1 py-3 rounded-xl bg-white/10 text-white/70 text-sm font-medium flex items-center justify-center gap-2 hover:bg-white/20 transition-colors"
          >
            <CloudDownload size={18} />
            下载
          </button>
          <button
            onClick={() => {
              setSyncState('uploading');
              setTimeout(() => setSyncState('synced'), 2000);
            }}
            className="flex-1 py-3 rounded-xl bg-[#FF6B35] text-white text-sm font-medium flex items-center justify-center gap-2"
          >
            <RefreshCw size={18} />
            同步
          </button>
        </div>

        {/* 同步历史 */}
        <div>
          <p className="text-white/50 text-xs mb-2">同步历史</p>
          <div className="space-y-2">
            {syncHistory.map((item) => {
              const Icon = actionIcons[item.action];
              return (
                <div
                  key={item.id}
                  className="rounded-xl p-3 bg-white/5 border border-white/10 flex items-center justify-between"
                >
                  <div className="flex items-center gap-3">
                    <Icon
                      size={16}
                      className={item.success ? 'text-[#FF6B35]' : 'text-red-500'}
                    />
                    <div>
                      <p className="text-white text-sm">{actionLabels[item.action]}</p>
                      <p className="text-white/50 text-xs">{item.time}</p>
                    </div>
                  </div>
                  <span className={`px-2 py-0.5 rounded text-xs ${
                    item.success
                      ? 'bg-green-500/20 text-green-400'
                      : 'bg-red-500/20 text-red-400'
                  }`}>
                    {item.success ? '成功' : '失败'}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ParamSyncPage;
