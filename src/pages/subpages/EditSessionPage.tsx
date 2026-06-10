import React, { useState } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  History,
  Image,
  Palette,
  X,
  Trash2,
  Clock,
  CheckCircle,
  Download,
  RotateCcw,
} from 'lucide-react';

/**
 * 编辑会话历史页面
 * 展示历史编辑记录，支持恢复编辑
 */
const EditSessionPage: React.FC = () => {
  const { navigateToSubPage } = useAppStore();

  // 模拟会话数据
  const sessions = [
    {
      id: 1,
      imageName: 'IMG_20240115_142030.jpg',
      presetName: '胶片风格',
      status: 'in_progress',
      updatedAt: '2024/01/15 14:30',
      paramCount: 8,
    },
    {
      id: 2,
      imageName: 'Portrait_001.dng',
      presetName: '人像优化',
      status: 'completed',
      updatedAt: '2024/01/15 10:20',
      paramCount: 5,
    },
    {
      id: 3,
      imageName: 'Landscape_Sunset.jpg',
      presetName: '风景增强',
      status: 'exported',
      updatedAt: '2024/01/14 18:45',
      paramCount: 12,
    },
    {
      id: 4,
      imageName: 'Street_Photo.dng',
      presetName: '街头摄影',
      status: 'completed',
      updatedAt: '2024/01/14 09:15',
      paramCount: 6,
    },
  ];

  const statusConfig: Record<string, { label: string; color: string; bg: string }> = {
    in_progress: { label: '编辑中', color: 'text-blue-400', bg: 'bg-blue-500/20' },
    completed: { label: '已完成', color: 'text-green-400', bg: 'bg-green-500/20' },
    exported: { label: '已导出', color: 'text-purple-400', bg: 'bg-purple-500/20' },
  };

  const [showRestoreBanner, setShowRestoreBanner] = useState(true);

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
        <h1 className="text-white font-semibold">编辑历史</h1>
        <button className="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center">
          <Trash2 size={18} className="text-white" />
        </button>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4">
        {/* 恢复提示横幅 */}
        {showRestoreBanner && (
          <div className="rounded-2xl p-4 bg-gradient-to-r from-[#FF6B35]/20 to-[#FF6B35]/5 border border-[#FF6B35]/30">
            <div className="flex items-center gap-3">
              <RotateCcw size={20} className="text-[#FF6B35]" />
              <div className="flex-1">
                <p className="text-white font-medium text-sm">恢复上次编辑</p>
                <p className="text-white/50 text-xs">IMG_20240115_142030.jpg</p>
              </div>
              <button
                onClick={() => setShowRestoreBanner(false)}
                className="px-3 py-1.5 rounded-lg bg-white/10 text-white/70 text-xs"
              >
                忽略
              </button>
              <button className="px-3 py-1.5 rounded-lg bg-[#FF6B35] text-white text-xs">
                恢复
              </button>
            </div>
          </div>
        )}

        {/* 会话列表 */}
        <div className="space-y-3">
          {sessions.map((session) => {
            const status = statusConfig[session.status];
            return (
              <div
                key={session.id}
                className="rounded-2xl p-4 bg-white/5 border border-white/10 hover:bg-white/10 transition-colors cursor-pointer"
              >
                <div className="flex items-start gap-3">
                  {/* 缩略图占位 */}
                  <div className="w-16 h-16 rounded-xl bg-white/10 flex items-center justify-center flex-shrink-0">
                    <Image size={24} className="text-white/30" />
                  </div>

                  {/* 内容 */}
                  <div className="flex-1 min-w-0">
                    <p className="text-white font-medium text-sm truncate">
                      {session.imageName}
                    </p>

                    {/* 预设名称 */}
                    <div className="flex items-center gap-1.5 mt-1">
                      <Palette size={12} className="text-[#FF6B35]" />
                      <span className="text-[#FF6B35] text-xs">{session.presetName}</span>
                    </div>

                    {/* 时间和状态 */}
                    <div className="flex items-center gap-2 mt-2">
                      <div className="flex items-center gap-1">
                        <Clock size={12} className="text-white/30" />
                        <span className="text-white/50 text-xs">{session.updatedAt}</span>
                      </div>
                      <span className={`px-2 py-0.5 rounded text-xs ${status.bg} ${status.color}`}>
                        {status.label}
                      </span>
                    </div>

                    {/* 参数数量 */}
                    <p className="text-white/30 text-xs mt-1">
                      {session.paramCount} 个参数已调节
                    </p>
                  </div>

                  {/* 删除按钮 */}
                  <button className="w-8 h-8 rounded-lg bg-white/5 flex items-center justify-center hover:bg-white/10 transition-colors">
                    <Trash2 size={14} className="text-white/30" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>

        {/* 参数快照预览示例 */}
        <div className="rounded-2xl p-4 bg-white/5 border border-white/10">
          <p className="text-white font-medium text-sm mb-3">参数快照示例</p>
          <div className="space-y-2">
            {[
              { name: '饱和度', value: 15 },
              { name: '对比度', value: 8 },
              { name: '亮度', value: 5 },
              { name: '冷暖', value: -3 },
              { name: '锐度', value: 10 },
            ].map((param, i) => (
              <div key={i} className="flex items-center justify-between">
                <span className="text-white/70 text-xs">{param.name}</span>
                <span className={`text-xs font-medium ${
                  param.value > 0 ? 'text-[#FF6B35]' : param.value < 0 ? 'text-blue-400' : 'text-white/50'
                }`}>
                  {param.value >= 0 ? `+${param.value}` : param.value}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default EditSessionPage;
